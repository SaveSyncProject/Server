package fr.umontpellier.model.request;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ListBackup {

    // Métohde pour lister les backups d'un utilisateur

    public List<String> listBackups(String username) {
        List<String> backupList = new ArrayList<>();
        File userDir = new File("./users/" + username);

        // Vérifier si le dossier de l'utilisateur existe et est un répertoire
        if (userDir.exists() && userDir.isDirectory()) {
            File[] files = userDir.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory() || (file.isFile() && file.getName().endsWith("_backup"))) {
                        backupList.add(file.getName());
                    }
                }
            }
        }
        return backupList;
    }

    // Méthode pour lister les fichiers dans une sauvegarde spécifique
    public List<String> listFilesInBackup(String username, String backupName) {
        List<String> filesList = new ArrayList<>();
        File backupDir = new File("./users/" + username + "/" + backupName);
        if (backupDir.exists() && backupDir.isDirectory()) {
            try {
                Files.walk(backupDir.toPath())
                        .filter(Files::isRegularFile)
                        .forEach(path -> filesList.add(backupDir.toPath().relativize(path).toString()));
            } catch (IOException e) {
                System.err.println("Erreur lors de la lecture des fichiers: " + e.getMessage());
            }
        }
        return filesList;
    }
}
