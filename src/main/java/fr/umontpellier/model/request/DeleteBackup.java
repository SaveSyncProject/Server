package fr.umontpellier.model.request;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DeleteBackup {



    // Méthode pour supprimer une sauvegarde
    public boolean deleteBackup(String username, String backupName) {
        File backupFile = new File("./users/" + username + "/" + backupName);
        return backupFile.exists() && backupFile.delete();
    }

}
