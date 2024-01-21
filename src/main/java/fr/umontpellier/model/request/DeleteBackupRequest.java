package fr.umontpellier.model.request;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeleteBackupRequest {

    /**
     * Supprime une sauvegarde
     *
     * @param username   le nom de l'utilisateur
     * @param backupName le nom de la sauvegarde
     * @return true si la sauvegarde a été supprimée, false sinon
     */
    public boolean deleteBackup(String username, String backupName) {
        Path backupDir = Paths.get("./users", username, backupName);
        boolean isDeleted = deleteDirectory(backupDir);

        if (isDeleted) {
            removeBackupFromCSV(backupName);
        }

        return isDeleted;
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
                            System.err.println("Error while deleting file: " + path + " - " + e.getMessage());
                        }
                    });
            return !Files.exists(directory);
        } catch (IOException e) {
            System.err.println("Error while deleting directory: " + directory + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Supprime les fichiers d'une sauvegarde
     *
     * @param username      le nom de l'utilisateur
     * @param filesToDelete la liste des fichiers à supprimer
     * @return true si tous les fichiers ont été supprimés, false sinon
     */
    public boolean deleteFiles(String username, List<String> filesToDelete) {
        Path userDir = Paths.get("./users", username);
        return filesToDelete.stream()
                .map(userDir::resolve)
                .allMatch(this::deleteFile);
    }

    /**
     * Supprime un fichier
     *
     * @param path le chemin du fichier à supprimer
     * @return true si le fichier a été supprimé, false sinon
     */
    private boolean deleteFile(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("Error while deleting file: " + path + " - " + e.getMessage());
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
            System.err.println("Erreur lors de la mise à jour du fichier CSV: " + e.getMessage());
        }
    }
}