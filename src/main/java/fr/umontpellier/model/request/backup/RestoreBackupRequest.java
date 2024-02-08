package fr.umontpellier.model.request.backup;

import fr.umontpellier.model.encryption.EncryptionUtil;
import fr.umontpellier.model.request.Request;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Base64;
import java.util.List;

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
                System.out.println("Starting full restore for user: " + this.username + " with name: " + this.backupName);
            } else {
                System.out.println("No backup found for user: " + this.username + " with name: " + this.backupName);
            }
        } catch (Exception e) {
            System.out.println("Error while restoring backup: " + e.getMessage());
        }
    }

    /**
     * Restaure une sauvegarde partielle
     *
     * @param username       le nom de l'utilisateur
     * @param filesToRestore la liste des fichiers à restaurer
     * @param backupName     le nom de la sauvegarde
     * @param objectOut      le flux de sortie
     */
    public void partialRestore(String username, List<String> filesToRestore, String backupName, ObjectOutputStream objectOut) throws Exception {
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
        System.out.println("Starting partial restore for user: " + username + " with name: " + backupName);
        objectOut.writeObject("RESTORE_COMPLETE");
        objectOut.flush();
    }

    /**
     * Déchiffre un fichier individuel
     *
     * @param file le fichier à déchiffrer
     * @param key la clé de chiffrement
     */
    private Path decryptIndividualFile(Path file, SecretKey key) throws Exception {
        Path tempDecryptedFile = Files.createTempFile("decrypted_", null);
        EncryptionUtil.decryptFile(key, file.toFile(), tempDecryptedFile.toFile());
        return tempDecryptedFile;
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