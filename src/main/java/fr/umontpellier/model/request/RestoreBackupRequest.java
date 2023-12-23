package fr.umontpellier.model.request;

import fr.umontpellier.model.encryption.EncryptionUtil;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class RestoreBackupRequest {

    public void restore_All(String username, String backupName, ObjectOutputStream objectOut) throws Exception {
        Path backupDirectory = Paths.get("./users", username, backupName);
        if (Files.exists(backupDirectory)) {
            SecretKey key = getKeyForBackup(backupName);
            decryptFilesInDirectory(backupDirectory, key);
            sendFilesInDirectory(backupDirectory, objectOut);
            reencryptFilesInDirectory(backupDirectory, key);
            System.out.println("Restore ALL");
        } else {
            System.out.println("No backup found for user: " + username + " with name: " + backupName);
        }
    }

    public void restore_Partial(String username, List<String> filesToRestore, String backupName, ObjectOutputStream objectOut) throws Exception {
        Path backupDirectory = Paths.get("./users", username, backupName);
        SecretKey key = getKeyForBackup(backupName);

        for (String filePath : filesToRestore) {
            Path fileInBackup = backupDirectory.resolve(filePath);
            if (Files.exists(fileInBackup)) {
                Path decryptedFile = decryptIndividualFile(fileInBackup, key);
                sendFile(decryptedFile, filePath, objectOut);
                Files.deleteIfExists(decryptedFile);
            }
        }

        System.out.println("Restore Partial");
        objectOut.writeObject("RESTORE_COMPLETE");
        objectOut.flush();
    }


    private Path decryptIndividualFile(Path file, SecretKey key) throws Exception {
        Path tempDecryptedFile = Files.createTempFile("decrypted_", null);
        EncryptionUtil.decryptFile(key, file.toFile(), tempDecryptedFile.toFile());
        return tempDecryptedFile;
    }

    private void sendFilesInDirectory(Path directory, ObjectOutputStream objectOut) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relativePath = directory.relativize(file).toString();
                sendFile(file, relativePath, objectOut);
                return FileVisitResult.CONTINUE;
            }
        });
        objectOut.writeObject("RESTORE_COMPLETE");
        objectOut.flush();
    }

    private void sendFile(Path file, String relativePath, ObjectOutputStream objectOut) throws IOException {
        if (Files.exists(file)) {
            objectOut.writeObject(relativePath);
            objectOut.flush();

            try (InputStream fileStream = new BufferedInputStream(Files.newInputStream(file))) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileStream.read(buffer)) != -1) {
                    objectOut.write(buffer, 0, bytesRead);
                }
                objectOut.flush();
            }
        }
    }

    private SecretKey getKeyForBackup(String backupName) throws IOException, URISyntaxException {
        URL resourceUrl = RestoreBackupRequest.class.getClassLoader().getResource("key/backup_keys.csv");
        if (resourceUrl == null) {
            throw new IllegalStateException("Fichier 'backup_keys.csv' non trouvé dans les ressources");
        }
        Path csvFilePath = Paths.get(resourceUrl.toURI());
        List<String> lines = Files.readAllLines(csvFilePath);
        String keyString = lines.stream()
                .filter(line -> line.startsWith(backupName + ","))
                .findFirst()
                .map(line -> line.split(",")[1])
                .orElseThrow(() -> new IllegalStateException("Clé non trouvée pour la sauvegarde: " + backupName));

        byte[] decodedKey = Base64.getDecoder().decode(keyString);
        SecretKey key = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        return key;
    }

    private void decryptFilesInDirectory(Path directory, SecretKey key) throws Exception {
        EncryptionUtil.processFolder(key, directory.toFile(), false);
    }
    private void reencryptFilesInDirectory(Path directory, SecretKey key) throws Exception {
        EncryptionUtil.processFolder(key, directory.toFile(), true);
    }
}
