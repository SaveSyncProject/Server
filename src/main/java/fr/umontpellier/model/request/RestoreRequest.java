package fr.umontpellier.model.request;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class RestoreRequest {

    /*
     * Méthode pour envoyer les fichiers de sauvegarde à l'utilisateur depuis le serveur
     * C'est la méthode pour la restauration complète
     */
    public void handleRestoreRequest(String username, ObjectOutputStream objectOut) throws IOException {
        File userDirectory = new File("./users/" + username);
        if (userDirectory.exists()) {
            sendFilesInDirectory(userDirectory, username, objectOut);
        } else {
            System.out.println("Aucun dossier de sauvegarde trouvé pour l'utilisateur: " + username);
        }
    }
    /*
     * Méthode pour envoyer les fichiers de sauvegarde à l'utilisateur depuis le serveur
     * C'est la méthode pour la restauration partielle
     */
    public void restoreFiles(String username, List<String> filesToRestore, ObjectOutputStream objectOut) throws IOException {
        Path userDir = Paths.get("users", username);

        for (String filePath : filesToRestore) {
            Path file = userDir.resolve(filePath);
            System.out.println("Restauration du fichier : " + file.toAbsolutePath().toString());

            if (Files.exists(file)) {
                objectOut.writeObject(filePath); // Envoyer le chemin relatif du fichier, y compris le dossier de sauvegarde
                try (InputStream fileIn = Files.newInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileIn.read(buffer)) != -1) {
                        objectOut.write(buffer, 0, bytesRead);
                    }
                }
                objectOut.flush();
            }
        }
        objectOut.writeObject("RESTORE_COMPLETE");
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
