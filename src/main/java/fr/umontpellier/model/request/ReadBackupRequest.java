package fr.umontpellier.model.request;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReadBackupRequest {

    /**
     * Liste les sauvegardes d'un utilisateur
     *
     * @param username le nom de l'utilisateur
     * @return la liste des sauvegardes de l'utilisateur
     */
    public List<String> listBackups(String username) {
        Path userDir = Paths.get("./users", username);

        try (Stream<Path> stream = Files.list(userDir)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith("_backup"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture des sauvegardes: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Liste les fichiers d'une sauvegarde
     *
     * @param username   le nom de l'utilisateur
     * @param backupName le nom de la sauvegarde
     * @return la liste des fichiers de la sauvegarde
     */
    public List<String> listFiles(String username, String backupName) {
        Path backupDir = Paths.get("./users", username, backupName);

        if (Files.exists(backupDir) && Files.isDirectory(backupDir)) {
            try (Stream<Path> stream = Files.walk(backupDir)) {
                return stream
                        .filter(Files::isRegularFile)
                        .map(backupDir::relativize)
                        .map(Path::toString)
                        .collect(Collectors.toList());
            } catch (IOException e) {
                System.err.println("Erreur lors de la lecture des fichiers: " + e.getMessage());
            }
        }
        return List.of();
    }
}
