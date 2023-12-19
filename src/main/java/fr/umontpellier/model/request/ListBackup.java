package fr.umontpellier.model.request;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ListBackup {
    // Méthode pour lister les sauvegardes
    public List<String> listBackups(String username) {
        File userDir = new File("./users/" + username);
        File[] files = userDir.listFiles((dir, name) -> name.endsWith("_backup.zip"));
        return Arrays.stream(files).map(File::getName).collect(Collectors.toList());
    }
}
