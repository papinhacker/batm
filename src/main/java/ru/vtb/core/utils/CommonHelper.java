package ru.vtb.core.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.javafaker.Faker;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.joda.time.DateTime;
import org.testng.Assert;
import ru.vtb.core.common.Log;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CommonHelper {

    public static final Faker rusFaker = new Faker(new Locale("ru", "RUS"));
    public static final Faker usFaker = new Faker(Locale.US);

    private CommonHelper() {
    }

    // yyyy-MM-dd'T'HH:mm:ss.SSS
    public static String getCurrentDateInFormat(String pattern) {
        return new SimpleDateFormat(pattern).format(new Date());
    }

    public static String parseDateFromPatternToAnotherPattern(String originalPattern, String newPattern, String date) {
        try {
            return new SimpleDateFormat(newPattern).format(new SimpleDateFormat(originalPattern).parse(date));
        } catch (ParseException e) {
            throw new RuntimeException("Unable to parse date '%s' from pattern '%s' to pattern '%s'"
                    .formatted(date, originalPattern, newPattern));
        }
    }

    public static String serializeToJsonStringIgnoringNullableFields(Object model) {
        try {
            return new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .writerWithDefaultPrettyPrinter().writeValueAsString(model);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Не удалось сериализовать POJO в Json. Причина: " + e.getMessage());
        }
    }

    public static String serializeToJsonString(Object model) {
        try {
            return new ObjectMapper().setSerializationInclusion(JsonInclude.Include.ALWAYS)
                    .writerWithDefaultPrettyPrinter().writeValueAsString(model);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Не удалось сериализовать POJO в Json. Причина: " + e.getMessage());
        }
    }

    public static <T> T deserializeXmlToObject(String xml, Class<T> clazz) {
        try {
            return new XmlMapper().readValue(xml, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Unable to deserialize xml to object '%s'".formatted(clazz.getName()));
        }
    }

    public static <T> T deserializeXmlToObject(File xml, Class<T> clazz) {
        try {
            return new XmlMapper().readValue(xml, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Unable to deserialize xml to object '%s'".formatted(clazz.getName()));
        }
    }

    public static <T> T deserializeJsonToObject(String json, Class<T> clazz) {
        try {
            return new ObjectMapper().readValue(json, clazz);
        } catch (IOException e) {
            Log.error("Bad json:%n%s".formatted(json));
            throw new RuntimeException("Unable to deserialize json to object '%s'. Reason is: %s".formatted(clazz.getName(), e.getMessage()), e);
        }
    }

    public static <T> T deserializeJsonToObject(File json, Class<T> clazz) {
        try {
            return new ObjectMapper().readValue(json, clazz);
        } catch (IOException e) {
            Log.error("Bad json:%n%s".formatted(json));
            throw new RuntimeException("Unable to deserialize json to object '%s'. Reason is: %s".formatted(clazz.getName(), e.getMessage()), e);
        }
    }

    public static String convertJsonToXml(String json) {
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(json);
            return new XmlMapper().writeValueAsString(jsonNode)
                    .replace("<ObjectNode>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                    .replace("</ObjectNode>", "");
        } catch (IOException e) {
            throw new RuntimeException("Unable to parse json to xml. Reason is: " + e);
        }
    }

    /**
     * @return случайный день текущего года в формате "%03d" из диапазона [001, сегодня] включительно
     * (1 января — всегда "001")
     */
    public static String getNumericDayOfYearBeforeCurrentDate() {
        DateTime dateTime = new DateTime();
        return String.format("%03d",
                Integer.parseInt(dateTime.minusDays(ThreadLocalRandom.current().nextInt(0, dateTime.getDayOfYear()))
                        .dayOfYear().getAsString()));
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException("Возникла проблема при вызове метода Thread.sleep(). Причина: " + e.getMessage());
        }
    }

    public static <T> T waitForValueChangeAndGet(Supplier<T> supplier, long timeoutInSecs, String... logMessage) {
        if (logMessage.length > 0)
            Log.info(logMessage[0]);
        var init = supplier.get();
        try {
            Log.suppress();
            Awaitility.await().pollInSameThread()
                    .timeout(timeoutInSecs, TimeUnit.SECONDS)
                    .pollInSameThread()
                    .pollInterval(timeoutInSecs > 10 ? (int) timeoutInSecs / 10 : 1, TimeUnit.SECONDS)
                    .until(() -> !Objects.equals(supplier.get(), init)
                    );
        } catch (ConditionTimeoutException e) {
            Assert.fail("Не удалось дождаться изменения результата метода '%s' по таймуту.".formatted(supplier.toString()));
        } finally {
            Log.restore();
        }
        return supplier.get();
    }

    public static void checkTableIsEmpty(Supplier<?> supplier) {
        boolean passed = false;
        try {
            var res = supplier.get();
            if (res instanceof List) {
                passed = ((List<?>) res).isEmpty();
            } else if (res instanceof Map) {
                passed = ((Map<?, ?>) res).isEmpty();
            }
        } catch (IndexOutOfBoundsException ignored) {
            passed = true;
        }
        if (!passed)
            Assert.fail("Таблица, полученная при последнем запросе должна быть пуста");
    }

    public static <T> T getRandomFromList(List<T> list) {
        try {
            return list.get(ThreadLocalRandom.current().nextInt(list.size()));
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            throw new RuntimeException("Не удалось получить рандомный элемент списка. Причина: %s".formatted(e.getMessage()));
        }
    }

    public static Double formatDouble(double number, String... format) {
        return Double.valueOf(String.format(Locale.US, format.length > 0 ? format[0] : "%.2f", number));
    }
}
