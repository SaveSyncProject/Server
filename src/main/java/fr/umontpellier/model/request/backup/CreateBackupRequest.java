package fr.umontpellier.model.request.backup;

import fr.umontpellier.logging.LoggingService;
import fr.umontpellier.model.Backup;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import fr.umontpellier.model.encryption.EncryptionUtil;
import fr.umontpellier.model.request.Request;


public class CreateBackupRequest extends Request {

private final String username;

private final Backup backup;

    public CreateBackupRequest(Backup backup, String username) {
        this.backup = backup;
        this.username = username;
    }

    @Override
    public void execute() {
        LoggingService.getLogger().log("Starting backup for user: " + username);
        Path directoryPath = Paths.get(this.backup.getDirectoryPath());
        List<String> extensions = this.backup.getFileExtensions();
        Path backupRoot = Paths.get("./users", username);
        createDirectory(backupRoot);
        String backupName = createUnzippedDirectoryName(directoryPath);
        Path zipFilePath = createZipFilePath(directoryPath, backupRoot);
        zipDirectory(directoryPath, extensions, zipFilePath);
        unzipBackup(zipFilePath, backupRoot.resolve(backupName));
        Path backupDirectory = backupRoot.resolve(backupName);
        deleteFile(zipFilePath);
        try {
            SecretKey key = EncryptionUtil.generateKey();
            String keyString = Base64.getEncoder().encodeToString(key.getEncoded());
            saveKeyToCSV(backupName, keyString);
            EncryptionUtil.processFolder(key, backupDirectory.toFile(), true); // Encrypter le dossier de sauvegarde
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createDirectory(Path directory) {
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                LoggingService.getLogger().log("Error while creating directory: " + directory + " - " + e.getMessage());
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
            Files.walkFileTree(directoryPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (extensions.stream().anyMatch(ext -> file.toString().endsWith(ext))) {
                        zipFile(file, directoryPath, zipOut);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LoggingService.getLogger().log("Error while zipping directory: " + directoryPath + " - " + e.getMessage());
        }
    }

    private void zipFile(Path file, Path basePath, ZipOutputStream zipOut) throws IOException {
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
            LoggingService.getLogger().log("Error while unzipping backup: " + e.getMessage());
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
            LoggingService.getLogger().log("Error while deleting file: " + file + " - " + e.getMessage());
        }
    }

    private void saveKeyToCSV(String backupReference, String keyString) {
        try {
            Path csvFilePath = Paths.get("users/backup_keys.csv");
            LoggingService.getLogger().log("CSV file path: " + csvFilePath);

            Files.createDirectories(csvFilePath.getParent());

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath.toFile(), true))) {
                writer.write(backupReference + "," + keyString);
                writer.newLine();
            }
        } catch (Exception e) {
            LoggingService.getLogger().log("Error while writing to CSV file: " + e.getMessage());
        }
    }
}