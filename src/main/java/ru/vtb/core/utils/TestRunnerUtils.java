package ru.vtb.core.utils;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class TestRunnerUtils {

    private static final String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();
    private static final String PROJECT_DIRECTORY = System.getProperty("user.dir");
    private static final String MAIN_RESOURCES_FOLDER = PROJECT_DIRECTORY + FILE_SEPARATOR
            + "src" + FILE_SEPARATOR
            + "main" + FILE_SEPARATOR
            + "resources";
    private static final String TEST_RESOURCES_FOLDER = PROJECT_DIRECTORY + FILE_SEPARATOR
            + "src" + FILE_SEPARATOR
            + "test" + FILE_SEPARATOR
            + "resources";

    public static void unpackResources() {
        File main = new File(MAIN_RESOURCES_FOLDER);
        File test = new File(TEST_RESOURCES_FOLDER);
        if (!main.mkdirs() || !test.mkdirs())
            return;

        String jarPath;
        try {
            jarPath = TestRunnerUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unable to get JAR file path. Reason is: " + e.getMessage());
        }

        if (!jarPath.endsWith(".jar"))
            return;

        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.startsWith("main_resources")) {
                    String relative = name.substring("main_resources".length()+1);
                    Path destDir = main.toPath();
                    Path outPath = destDir.resolve(relative);
                    Files.createDirectories(outPath);
                    try (InputStream is = jar.getInputStream(entry)) {
                        Files.copy(is, outPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to unpack resource '%s' to path '%s'. Reason is: %s");
                    }

//                    unpackSingleResource(jar, entry, new File(PROJECT_DIRECTORY + FILE_SEPARATOR + "main"));
                }
//                else if (name.startsWith("test_resources")) {
//                    unpackSingleResource(jar, entry, new File(PROJECT_DIRECTORY + FILE_SEPARATOR + "test"));
//                } else if (name.equals("custom.xml")) {
////                    try (InputStream is = jar.getInputStream(entry)){
////                        Path result = Paths.get(TestRunnerUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent().resolve("custom.xml");
////                        Files.copy(is, result, StandardCopyOption.REPLACE_EXISTING);
////                    }
//                    unpackSingleResource(jar, entry, new File(PROJECT_DIRECTORY + FILE_SEPARATOR + "custom.xml"));
//                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to unpack resources from JAR file. Reason is: " + e.getMessage());
        }
    }

    private static void unpackSingleResource(JarFile from, JarEntry resource, File to) {
        to.getParentFile().mkdirs();
        try (InputStream is = from.getInputStream(resource)) {
            Files.copy(is, to.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to unpack resource '%s' to path '%s'. Reason is: %s"
                    .formatted(resource.getName(), to.getPath(), e.getMessage()));
        }
    }

    public static void tryToUnpackFolder() {
        Path jarPath;
        try {
            jarPath = Paths.get(TestRunnerUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unable to get JAR file path. Reason is: " + e.getMessage());
        }
        ZipUtils.extractEntry(jarPath.toString(), "custom.xml", PROJECT_DIRECTORY);
        ZipUtils.extractFolder(jarPath.toString(), "main_resources", PROJECT_DIRECTORY);
    }

    public static void createTempFile() {
        List<String> lines = List.of("string1");
        FileHelper.writeLinesToFile(lines, PROJECT_DIRECTORY + FILE_SEPARATOR + "some.txt");
    }

}
