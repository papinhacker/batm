package ru.vtb.selftest;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.vtb.core.utils.TestRunnerUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import static org.testng.Assert.*;

/**
 * Регрессия: распаковка main_resources из fat-jar раньше заменяла каталоги пустыми файлами
 * ("main_resources/kafka/" -> файл kafka) и падала с "Not a directory".
 */
public class TestRunnerUtilsTest {

    private Path workDir;

    @BeforeMethod
    public void createWorkDir() throws IOException {
        workDir = Files.createTempDirectory("selftest-unpack-");
    }

    @AfterMethod(alwaysRun = true)
    public void deleteWorkDir() throws IOException {
        try (Stream<Path> paths = Files.walk(workDir)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }

    /** Jar с тем же порядком записей, что и у fat-jar: сначала каталоги, затем файлы. */
    private Path buildJar() throws IOException {
        Path jar = workDir.resolve("fat.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            for (String dir : new String[]{"main_resources/", "main_resources/kafka/", "main_resources/properties/"}) {
                out.putNextEntry(new JarEntry(dir));
                out.closeEntry();
            }
            put(out, "main_resources/settings.xml", "<settings/>");
            put(out, "main_resources/kafka/test-batm-truststore.p12", "trust");
            put(out, "main_resources/properties/test.properties", "db.uri=jdbc:postgresql://localhost/");
            put(out, "main_resources_other/ignored.txt", "must not be unpacked");
            put(out, "test_resources/tempFiles/temp.json", "{}");
            put(out, "ru/vtb/Some.class", "x");
        }
        return jar;
    }

    private static void put(JarOutputStream out, String name, String content) throws IOException {
        out.putNextEntry(new JarEntry(name));
        out.write(content.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
    }

    @Test(groups = {"selftest"}, description = "main_resources распаковываются с каталогами в пустой каталог назначения")
    public void unpacksDirectoriesAndFiles() throws IOException {
        Path dest = workDir.resolve("src/main/resources");
        TestRunnerUtils.unpackMainResources(buildJar(), dest);

        assertTrue(Files.isDirectory(dest), "Каталог назначения должен остаться каталогом");
        assertTrue(Files.isDirectory(dest.resolve("kafka")), "kafka должен быть каталогом");
        assertTrue(Files.isDirectory(dest.resolve("properties")), "properties должен быть каталогом");
        assertEquals(Files.readString(dest.resolve("settings.xml")), "<settings/>");
        assertEquals(Files.readString(dest.resolve("kafka/test-batm-truststore.p12")), "trust");
        assertEquals(Files.readString(dest.resolve("properties/test.properties")), "db.uri=jdbc:postgresql://localhost/");
        try (Stream<Path> all = Files.walk(dest)) {
            assertEquals(all.filter(Files::isRegularFile).count(), 3,
                    "Распаковываются только записи main_resources/**");
        }
    }

    @Test(groups = {"selftest"}, description = "Повторная распаковка поверх уже существующих файлов не падает")
    public void unpackIsRepeatable() throws IOException {
        Path jar = buildJar();
        Path dest = workDir.resolve("src/main/resources");
        TestRunnerUtils.unpackMainResources(jar, dest);
        TestRunnerUtils.unpackMainResources(jar, dest);
        assertEquals(Files.readString(dest.resolve("kafka/test-batm-truststore.p12")), "trust");
    }

    @Test(groups = {"selftest"}, description = "Несуществующий jar -> понятная ошибка с путём и исходным исключением")
    public void missingJarFailsWithCause() {
        Path missing = workDir.resolve("missing.jar");
        RuntimeException e = expectThrows(RuntimeException.class,
                () -> TestRunnerUtils.unpackMainResources(missing, workDir.resolve("dest")));
        assertTrue(e.getMessage().startsWith("Unable to unpack resources from JAR file '" + missing + "'"), e.getMessage());
        assertTrue(e.getCause() instanceof IOException, "cause: " + e.getCause());
    }
}
