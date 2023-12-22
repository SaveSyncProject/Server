package fr.umontpellier.model.request;

import fr.umontpellier.model.Backup;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class CreateBackupRequest {

    /**
     * Crée un dossier pour l'utilisateur
     *
     * @param backupDetails les informations de la sauvegarde
     * @param username le nom de l'utilisateur
     */
    public void handleBackup(Backup backupDetails, String username) {
        System.out.println("Starting backup for user: " + username);
        Path directoryPath = Paths.get(backupDetails.getDirectoryPath());
        List<String> extensions = backupDetails.getFileExtensions();
        Path backupRoot = Paths.get("./users", username);

        createDirectory(backupRoot);

        Path zipFilePath = createZipFilePath(directoryPath, backupRoot);
        zipDirectory(directoryPath, extensions, zipFilePath);
        unzipBackup(zipFilePath, backupRoot.resolve(createUnzippedDirectoryName(directoryPath)));
        deleteFile(zipFilePath);
    }

    private void createDirectory(Path directory) {
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                System.err.println("Error while creating directory: " + directory + " - " + e.getMessage());
            }
        }
    }

    private Path createZipFilePath(Path directoryPath, Path backupRoot) {
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "-"
                + LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss"));
        String fileName = directoryPath.getFileName().toString() + "-" + timestamp + "_backup.zip";
        return backupRoot.resolve(fileName);
    }

    private void zipDirectory(Path directoryPath, List<String> extensions, Path zipFilePath) {
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
            Set<Path> addedDirs = new HashSet<>();
            Files.walkFileTree(directoryPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (extensions.stream().anyMatch(ext -> file.toString().endsWith(ext))) {
                        addToZip(file, directoryPath, zipOut);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Error while zipping directory: " + directoryPath + " - " + e.getMessage());
        }
    }

    private void addToZip(Path file, Path basePath, ZipOutputStream zipOut) throws IOException {
        ZipEntry zipEntry = new ZipEntry(basePath.relativize(file).toString());
        zipOut.putNextEntry(zipEntry);
        Files.copy(file, zipOut);
        zipOut.closeEntry();
    }

    private String createUnzippedDirectoryName(Path directoryPath) {
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "-"
                + LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss"));
        return directoryPath.getFileName().toString() + "-" + timestamp + "_backup";
    }

    private void unzipBackup(Path zipFile, Path outputPath) {
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path entryPath = outputPath.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    copyInputStreamToFile(zipInputStream, entryPath);
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la décompression du fichier zip: " + e.getMessage());
        }
    }

    private void copyInputStreamToFile(InputStream inputStream, Path filePath) throws IOException {
        try (OutputStream outputStream = new FileOutputStream(filePath.toFile())) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

    private void deleteFile(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            System.err.println("Error while deleting file: " + file + " - " + e.getMessage());
        }
    }
}
