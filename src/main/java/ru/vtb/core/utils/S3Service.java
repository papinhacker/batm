package ru.vtb.core.utils;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import ru.vtb.core.common.EnvConstants;
import ru.vtb.core.common.Log;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.utils.AttributeMap;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static ru.vtb.core.common.EnvConstants.FILE_SEPARATOR;
import static ru.vtb.core.common.EnvConstants.TEMP_FILES_FOLDER;

public class S3Service {

    private S3Client s3Client;

    public S3Service() {
        init();
    }

    public void init() {
        SdkHttpClient httpClient = new DefaultSdkHttpClientBuilder()
                .buildWithDefaults(AttributeMap.builder()
                        .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                        .build());

        this.s3Client = S3Client.builder()
                .region(Region.of(EnvConstants.S3_REGION))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(EnvConstants.S3_ACCESS_KEY, EnvConstants.S3_SECRET_KEY)))
                .endpointOverride(
                        URI.create(EnvConstants.S3_PROTOCOL + "://" + EnvConstants.S3_URI + ":" + EnvConstants.S3_PORT))
                .httpClient(httpClient)
                .forcePathStyle(true)
                .build();
    }

    public boolean doesBucketExist(String bucketName) {
        Log.step(String.format("Проверяем существаоание баккета: '%s'", bucketName));
        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(bucketName)
                .build();

        try {
            s3Client.headBucket(headBucketRequest);
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }

    public void createBucket(String bucketName) {
        Log.step(String.format("Создаём нового баккета: '%s'", bucketName));
        CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                .bucket(bucketName)
                .build();

        s3Client.createBucket(bucketRequest);
    }

    public void deleteBucket(String bucketName) {
        Log.step(String.format("Удаляем баккет: '%s'", bucketName));
        try {
            DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            s3Client.deleteBucket(deleteBucketRequest);
            Log.info(String.format("Баккет '%s' успешно удалён", bucketName));
        } catch (S3Exception e) {
            Log.warning(String.format("Не удалось удалить баккет: '%s' по причине %s", bucketName, e.awsErrorDetails()
                    .errorMessage()));
        }
    }

    public PutObjectResponse putObject(String bucketName, String keyPrefix, java.io.File file) {
        String fullKey = keyPrefix + file.getName();
        Log.step(String.format(
                "Отправляем в s3 файл '%s' в объект '%s' баккета '%s'", file.getName(), fullKey, bucketName));
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fullKey)
                .build();

        return s3Client.putObject(request, Path.of(file.toURI()));
    }

    public boolean waitForObjectsNamesContain(String bucketName, String objectKey, long timeoutInSecs) {
        Log.step(String.format(
                "Ожидаем в s3 появление файла '%s' в баккете '%s'", objectKey, bucketName));
        try {
            Awaitility.await().pollInSameThread()
                    .timeout(timeoutInSecs, TimeUnit.SECONDS)
                    .pollInterval(fibonacci(TimeUnit.SECONDS).offset(2))
                    .until(() -> getObjectsNames(bucketName).contains(objectKey));
        } catch (ConditionTimeoutException e) {
            Log.debug(getObjectsNames(bucketName));
            Log.warning("Не удалось дождаться появления объекта '%s' в баккете '%s'".formatted(objectKey, bucketName));
            return false;
        }
        return true;
    }

    public List<String> getObjectsNames(String bucketName) {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();
        ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);

        return listObjectsV2Response.contents().stream().map(S3Object::key).toList();
    }

    public File getObject(String bucketName, String objectKey, long... timeoutInSecs) {
        Log.step(String.format(
                "Получаем данные из объекта '%s' баккета '%s'", objectKey, bucketName));
        try {
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            if (timeoutInSecs.length > 0) {
                Awaitility.await().pollInSameThread()
                        .timeout(timeoutInSecs[0], TimeUnit.SECONDS)
                        .pollInterval(fibonacci(TimeUnit.SECONDS).offset(2))
                        .ignoreExceptions()
                        .until(() -> getObjectsNames(bucketName).contains(objectKey));
            }

            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(objectRequest);

            String[] arr = objectKey.split("/");
            String filename = arr[arr.length - 1];
            File file = FileHelper.writeFile(response.asByteArray(), TEMP_FILES_FOLDER + FILE_SEPARATOR +
                    "downloadedFiles" + FILE_SEPARATOR + filename);
            Log.info(String.format(
                    "Байты объекта '%s' баккета '%s' успешно записаны в файл", objectKey, bucketName));
            return file;
        } catch (S3Exception | ConditionTimeoutException e) {
            throw new RuntimeException(String.format("Не удалось получить байты объекта '%s' из баккета '%s' по причине:%n%s",
                    objectKey, bucketName, e.getMessage()));
        }
    }

    public CopyObjectResponse copyObject(
            String sourceBucketName,
            String sourceKey,
            String destinationBucketName,
            String destinationKey) {
        Log.step(String.format("Копируем объект '%s' баккета '%s' в объект '%s' баккета '%s'",
                sourceKey, sourceBucketName, destinationKey, destinationBucketName));
        CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                .sourceBucket(sourceBucketName)
                .sourceKey(sourceKey)
                .destinationBucket(destinationBucketName)
                .destinationKey(destinationKey)
                .build();

        return s3Client.copyObject(copyObjectRequest);
    }

    public void deleteObject(String bucketName, String objectKey) {
        Log.step(String.format(
                "Удаляем объект '%s' баккета '%s'", objectKey, bucketName));
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    public void deleteObjects(String bucketName, List<String> keys) {
        Log.step(String.format(
                "Удаляем объекты '%s' баккета '%s'", keys, bucketName));
        ArrayList<ObjectIdentifier> toDelete = new ArrayList<>();
        for (String objKey : keys) {
            toDelete.add(ObjectIdentifier.builder()
                    .key(objKey)
                    .build());
        }

        DeleteObjectsRequest deleteObjectRequest = DeleteObjectsRequest.builder()
                .bucket(bucketName)
                .delete(Delete.builder()
                        .objects(toDelete).build()).build();

        s3Client.deleteObjects(deleteObjectRequest);
    }

    public List<String> getBucketsNames() {
        Log.step("Получаем имена всех баккетов в хранилище");
        ListBucketsResponse listBucketsResponse = s3Client.listBuckets();
        return listBucketsResponse.buckets().stream().map(Bucket::name).toList();
    }

    public List<String> getAllObjectsInBucket(String bucketName) {
        Log.step(String.format("Получаем имена всех объектов из баккета '%s'", bucketName));
        String nextContinuationToken = null;
        List<String> result = new ArrayList<>();
        do {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .continuationToken(nextContinuationToken);

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
            nextContinuationToken = response.nextContinuationToken();

            response.contents().forEach(o -> result.add(o.toString()));
        } while (nextContinuationToken != null);
        return result;
    }

    public void cleanup() {
        this.s3Client.close();
    }
}
