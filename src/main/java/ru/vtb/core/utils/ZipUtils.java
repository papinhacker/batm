package ru.vtb.core.utils;

import org.apache.commons.compress.archivers.*;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;

public class ZipUtils {

    public static File compressToZip(File fileToCompress, String... pathTo) {
        File resultFile = new File(pathTo.length > 0 ? pathTo[0] : fileToCompress.getPath() + ".zip");
        try (ArchiveOutputStream<ZipArchiveEntry> out = new ZipArchiveOutputStream(resultFile)) {
            ZipArchiveEntry entry = out.createArchiveEntry(fileToCompress, fileToCompress.getName());
            out.putArchiveEntry(entry);
            try (InputStream input = Files.newInputStream(fileToCompress.toPath())) {
                IOUtils.copy(input, out);
            }
            out.closeArchiveEntry();
            out.finish();

        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        return resultFile;
    }

    public static void extractEntry(String archivePath, String entryName, String destinationPath) {
        try (InputStream input = Files.newInputStream(Path.of(archivePath));
             BufferedInputStream buffer = new BufferedInputStream(input);
             ArchiveInputStream<?> archive = new ArchiveStreamFactory()
                     .createArchiveInputStream(buffer)) {
            ArchiveEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                if (entry.getName().equals(entryName)) {
                    Path outFile = Path.of(destinationPath).resolve(entryName);
                    Files.createDirectories(outFile.getParent());
                    try (OutputStream os = Files.newOutputStream(outFile)) {
                        IOUtils.copy(archive, os);
                    }
                    break;
                }
            }
        } catch (IOException | ArchiveException e) {
            throw new RuntimeException("Unable to unarchive entry '%s' from archive '%s' to path '%s'%nReason is: %s"
                    .formatted(entryName, archivePath, destinationPath, e.getMessage()));
        }
    }

    public static void extractFolder(String archivePath, String folderName, String destinationPath) {
        String sep = FileSystems.getDefault().getSeparator();
        if (!folderName.endsWith(sep)) {
            folderName += sep;
        }
        try (ZipFile zipFile = ZipFile.builder().setFile(Path.of(archivePath).toFile()).get()) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (!entryName.startsWith(folderName))
                    continue;
                String relativePath = entryName.substring(folderName.length());
                if (relativePath.isEmpty())
                    continue;
                Path outputPath = Path.of(destinationPath).resolve(relativePath).normalize();
                if (!outputPath.startsWith(destinationPath))
                    throw new RuntimeException("zip slim right here");
                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                } else {
                    Files.createDirectories(outputPath.getParent());
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        Files.copy(is, outputPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to copy entry '%s' to path '%s'. Reason is: %s"
                                .formatted(entry.getName(), outputPath, e.getMessage()));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to unarchive folder '%s' from archive '%s' to path '%s'%nReason is: %s"
                    .formatted(folderName, archivePath, destinationPath, e.getMessage()));

        }

    }
}
