package fr.umontpellier.model.request.file;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import fr.umontpellier.model.encryption.EncryptionUtil;
import fr.umontpellier.model.logging.LoggingService;
import fr.umontpellier.model.request.Request;

public class RestoreFileRequest extends Request {

    private final String username;
    private final String backupName;
    private final List<String> files;
    private final ObjectOutputStream objectOut;

    public RestoreFileRequest(String username, String backupName, List<String> files, ObjectOutputStream objectOut) {
        this.username = username;
        this.backupName = backupName;
        this.files = files;
        this.objectOut = objectOut;
    }

    @Override
    public void execute(){
        try{
            Path backupDirectory = Paths.get("./users", username, backupName);
            SecretKey key = getKeyForBackup(this.backupName);
            for (String filePath : this.files) {
                Path fileInBackup = backupDirectory.resolve(filePath);
                if (Files.exists(fileInBackup)) {
                    Path decryptedFile = decryptIndividualFile(fileInBackup, key);
                    sendFile(decryptedFile, filePath, objectOut);
                    Files.deleteIfExists(decryptedFile);
                }
            }
            LoggingService.getLogger().log("Starting partial restore for user: " + username + " with name: " + backupName);
            objectOut.writeObject("RESTORE_COMPLETE");
            objectOut.flush();
        } catch (Exception e) {
            LoggingService.getLogger().log("Error while restoring backup: " + e.getMessage());
        }
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
}