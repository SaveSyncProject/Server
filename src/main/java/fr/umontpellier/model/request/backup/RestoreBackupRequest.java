package fr.umontpellier.model.request.backup;

import fr.umontpellier.logging.LoggingService;
import fr.umontpellier.model.encryption.EncryptionUtil;
import fr.umontpellier.model.request.Request;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Base64;
import java.util.List;
import java.util.logging.LoggingMXBean;

public class RestoreBackupRequest extends Request {

private final String username;
private final String backupName;
private final ObjectOutputStream objectOut;

    public RestoreBackupRequest(String username, String backupName, ObjectOutputStream objectOut) {
        this.username = username;
        this.backupName = backupName;
        this.objectOut = objectOut;
    }

    @Override
    public void execute() {
        try {
            Path backupDirectory = Paths.get("./users", this.username, this.backupName);
            if (Files.exists(backupDirectory)) {
                SecretKey key = getKeyForBackup(this.backupName);
                decryptFilesInDirectory(backupDirectory, key);
                sendFilesInDirectory(backupDirectory, this.objectOut);
                encryptFilesInDirectory(backupDirectory, key);
                LoggingService.getLogger().log("Starting full restore for user: " + this.username + " with name: " + this.backupName);
            } else {
                LoggingService.getLogger().log("No backup found for user: " + this.username + " with name: " + this.backupName);
            }
        } catch (Exception e) {
            LoggingService.getLogger().log("Error while restoring backup: " + e.getMessage());
        }
    }

    /**
     * Envoie tous les fichiers dans un répertoire
     *
     * @param directory le répertoire à envoyer
     * @param objectOut le flux de sortie
     */
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

    /**
     * Envoie un fichier
     *
     * @param file le fichier à envoyer
     * @param relativePath le chemin relatif du fichier
     * @param objectOut le flux de sortie
     */
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

    /**
     * Récupère la clé de chiffrement d'une sauvegarde
     *
     * @param backupName le nom de la sauvegarde
     * @return la clé de chiffrement
     */
    private SecretKey getKeyForBackup(String backupName) throws IOException {
        // Chemin relatif vers le fichier 'backup_keys.csv' dans le dossier 'users/'
        Path csvFilePath = Paths.get("users/backup_keys.csv");

        // Vérifier si le fichier existe
        if (!Files.exists(csvFilePath)) {
            throw new IllegalStateException("Fichier 'backup_keys.csv' non trouvé dans le dossier 'users/'");
        }

        // Lire toutes les lignes du fichier CSV
        List<String> lines = Files.readAllLines(csvFilePath);
        String keyString = lines.stream()
                .filter(line -> line.startsWith(backupName + ","))
                .findFirst()
                .map(line -> line.split(",")[1])
                .orElseThrow(() -> new IllegalStateException("Clé non trouvée pour la sauvegarde: " + backupName));

        // Décoder et créer la clé secrète
        byte[] decodedKey = Base64.getDecoder().decode(keyString);
        SecretKey key = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        return key;
    }

    /**
     * Déchiffre tous les fichiers dans un répertoire
     *
     * @param directory le répertoire à déchiffrer
     * @param key la clé de chiffrement
     */
    private void decryptFilesInDirectory(Path directory, SecretKey key) throws Exception {
        EncryptionUtil.processFolder(key, directory.toFile(), false);
    }

    /**
     * Chiffre tous les fichiers dans un répertoire
     *
     * @param directory le répertoire à chiffrer
     * @param key la clé de chiffrement
     */
    private void encryptFilesInDirectory(Path directory, SecretKey key) throws Exception {
        EncryptionUtil.processFolder(key, directory.toFile(), true);
    }
}