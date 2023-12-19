package fr.umontpellier.model.request;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DeleteBackup {

    /**
     * Supprime les fichiers spécifiés pour un utilisateur.
     *
     * @param username      Le nom d'utilisateur.
     * @param filesToDelete La liste des chemins de fichiers à supprimer.
     * @return true si tous les fichiers ont été supprimés avec succès, sinon false.
     */
    public boolean deleteFiles(String username, List<String> filesToDelete) {
        boolean allDeleted = true;
        for (String fullPath : filesToDelete) {
            File file = new File("./users/" + username + "/" + fullPath);
            if (file.exists() && !file.delete()) {
                allDeleted = false;
            }
        }
        return allDeleted;
    }


    /**
     * Supprime une sauvegarde spécifique pour un utilisateur.
     * @param username
     * @param backupName
     * @return
     */
    public boolean deleteBackup(String username, String backupName) {
        File backupDir = new File("./users/" + username + "/" + backupName);
        return deleteDirectory(backupDir);
    }

    private boolean deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        return directory.delete();
    }




}
