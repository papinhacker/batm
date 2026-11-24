package ru.vtb.core.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileHelper {

    private FileHelper() {
    }

    public static File writeLinesToFile(List<String> lines, String path) {
        File file = new File(path);
        try (FileWriter fw = new FileWriter(file);
             BufferedWriter out = new BufferedWriter(fw)) {
            for (String line : lines) {
                out.write(line + "\n");
            }
            out.flush();
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Unable to write file in '%s' path. Reason is: ".formatted(path) + e);
        }
    }

    public static File writeFile(byte[] file, String path) {
        try {
            return Files.write(Path.of(path), file).toFile();
        } catch (IOException e) {
            throw new RuntimeException("Unable to write '%s'. Reason is: ".formatted(path) + e);
        }
    }

    public static List<String> readLinesFromTextFile(String path) {
        try {
            return Files.readAllLines(Path.of(path));
        } catch (IOException e) {
            throw new RuntimeException("Unable to read file on path '%s'. Reason is: ".formatted(path) + e.getMessage());
        }
    }

    public static File copyFile(String pathFrom, String pathTo) {
        try {
            return Files.copy(Path.of(pathFrom), Path.of(pathTo)).toFile();
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy file from '%s'%nto '%s'%nReason in: %s"
                    .formatted(pathFrom, pathTo, e.getMessage()));
        }
    }

    public static File saveInputStreamToFile(InputStream inputStream, String resultPath) {
        try {
            Files.copy(inputStream, Path.of(resultPath));
            return new File(resultPath);
        } catch (IOException e) {
            throw new RuntimeException("Unable to convert InputStream to File Reason is: " + e.getMessage());
        }
    }

    public static InputStream fileToInputStream(String path) {
        try {
            return Files.newInputStream(Path.of(path));
        } catch (IOException e) {
            throw new RuntimeException("Unable to convert File to InputStream Reason is: " + e.getMessage());
        }
    }
}
