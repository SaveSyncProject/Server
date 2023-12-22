package fr.umontpellier.model.request;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class RestoreBackupRequest {

    /**
     * Restaure les fichiers d'une sauvegarde
     *
     * @param username        le nom de l'utilisateur
     * @param objectOut       le flux de sortie
     * @throws IOException si une erreur d'entrée/sortie survient
     */
    public void handleRestoreRequest(String username, String backupName, ObjectOutputStream objectOut) throws IOException {
        Path backupDirectory = Paths.get("./users", username, backupName);
        if (Files.exists(backupDirectory)) {
            sendFilesInDirectory(backupDirectory, objectOut);
        } else {
            System.out.println("No backup found for user: " + username + " with name: " + backupName);
        }
    }


    /**
     * Restaure les fichiers d'une sauvegarde
     *
     * @param username        le nom de l'utilisateur
     * @param filesToRestore  la liste des fichiers à restaurer
     * @param objectOut       le flux de sortie
     * @throws IOException si une erreur d'entrée/sortie survient
     */
    public void restoreFiles(String username, List<String> filesToRestore, ObjectOutputStream objectOut) throws IOException {
        Path userDir = Paths.get("./users", username);
        for (String filePath : filesToRestore) {
            Path fullFilePath = userDir.resolve(filePath);
            Path path = Paths.get(filePath);
            Path relativeFilePath = path.subpath(1, path.getNameCount()); // Ignorez le nom de la sauvegarde
            System.out.println("Restoring file: " + fullFilePath);
            sendFile(fullFilePath, relativeFilePath.toString(), objectOut);
        }
        objectOut.writeObject("RESTORE_COMPLETE");
        objectOut.flush();
    }

    /**
     * Envoie les fichiers d'un répertoire
     *
     * @param directory le chemin du répertoire
     * @param objectOut le flux de sortie
     * @throws IOException si une erreur d'entrée/sortie survient
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
     * @param file         le chemin du fichier
     * @param relativePath le chemin relatif du fichier
     * @param objectOut    le flux de sortie
     * @throws IOException si une erreur d'entrée/sortie survient
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
}
