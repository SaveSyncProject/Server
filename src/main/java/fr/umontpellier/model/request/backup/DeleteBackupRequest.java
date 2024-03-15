package fr.umontpellier.model.request.backup;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.umontpellier.logging.LoggingService;
import fr.umontpellier.model.request.Request;

public class DeleteBackupRequest extends Request{

    private final String username;
    private final String backupName;
    private final ObjectOutputStream objectOut;

    public DeleteBackupRequest(String username, String backupName, ObjectOutputStream objectOut) {
        this.username = username;
        this.backupName = backupName;
        this.objectOut = objectOut;
    }

    @Override
    public void execute() {
        try {
            LoggingService.getLogger().log("Starting backup deletion for user: " + username);
            Path backupDir = Paths.get("./users", username, backupName);
            boolean isDeleted = deleteDirectory(backupDir);
            if (isDeleted) {
                removeBackupFromCSV(backupName);
                LoggingService.getLogger().log("Backup deleted successfully for user: " + username);
                this.objectOut.writeObject("SUCCESS");
            }
            else{
                LoggingService.getLogger().log("Error while deleting backup for user: " + username);
                this.objectOut.writeObject("ERROR");
            }
            this.objectOut.flush();
        } catch (IOException e) {
            LoggingService.getLogger().log("Error while deleting backup: " + e.getMessage());
        }
    }

    /**
     * Supprime un répertoire et tous ses fichiers
     *
     * @param directory le chemin du répertoire à supprimer
     * @return true si le répertoire a été supprimé, false sinon
     */
    private boolean deleteDirectory(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            LoggingService.getLogger().log("Error while deleting file: " + path + " - " + e.getMessage());
                        }
                    });
            return !Files.exists(directory);
        } catch (IOException e) {
            LoggingService.getLogger().log("Error while deleting directory: " + directory + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Supprime une clé de la liste des clés
     * @param backupName
     */
    private void removeBackupFromCSV(String backupName) {
        try {
            Path csvFilePath = Paths.get("users/backup_keys.csv");
            if (!Files.exists(csvFilePath)) {
                throw new IllegalStateException("Fichier 'backup_keys.csv' non trouvé dans le dossier 'users/'");
            }
            List<String> lines = Files.readAllLines(csvFilePath);
            List<String> updatedLines = lines.stream()
                    .filter(line -> !line.startsWith(backupName + ","))
                    .collect(Collectors.toList());
            Files.write(csvFilePath, updatedLines, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            LoggingService.getLogger().log("Error while updating CSV file: " + e.getMessage());
        }
    }
}