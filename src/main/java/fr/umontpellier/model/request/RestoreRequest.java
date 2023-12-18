package fr.umontpellier.model.request;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class RestoreRequest implements Request {

    /*
     * Méthode pour envoyer les fichiers de sauvegarde à l'utilisateur
     */
    public void handleRestoreRequest(String username, ObjectOutputStream objectOut) throws IOException {
        File userDirectory = new File("./users/" + username);
        if (userDirectory.exists()) {
            sendFilesInDirectory(userDirectory, username, objectOut);
        } else {
            System.out.println("Aucun dossier de sauvegarde trouvé pour l'utilisateur: " + username);
        }
    }

    private void sendFilesInDirectory(File directory, String username, ObjectOutputStream objectOut) throws IOException {
        Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Send file path relative to the user's directory
                String relativePath = directory.toPath().relativize(file).toString();
                objectOut.writeObject(relativePath);
                objectOut.flush();

                // Send file content
                try (InputStream fileStream = new BufferedInputStream(new FileInputStream(file.toFile()))) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileStream.read(buffer)) != -1) {
                        objectOut.write(buffer, 0, bytesRead);
                    }
                    objectOut.flush();
                }

                return FileVisitResult.CONTINUE;
            }
        });

        // Indicate the end of the file transfer
        objectOut.writeObject("RESTORE_COMPLETE");
        objectOut.flush();
    }
}
