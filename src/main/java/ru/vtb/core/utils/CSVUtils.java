package ru.vtb.core.utils;

import com.opencsv.*;
import com.opencsv.exceptions.CsvException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CSVUtils {

    public static List<String[]> parse(File file) {
        CSVParser parser = new CSVParserBuilder().withSeparator('\t').build();
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(file))
                .withCSVParser(parser)
                .build()) {
            return reader.readAll();
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Unable to parse CSV file '%s'. Reason is: %s".formatted(file.getName(), e.getMessage()));
        }
    }

    public static void writeCSVToFile(List<String[]> csv, String pathTo) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(pathTo))) {
            writer.writeAll(csv);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write CSV file to '%s'. Reason is: %s".formatted(pathTo, e.getMessage()));
        }
    }
}
