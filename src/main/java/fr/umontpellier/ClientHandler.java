package fr.umontpellier;


import fr.umontpellier.model.Backup;
import fr.umontpellier.model.User;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static fr.umontpellier.model.auth.LDAPConnection.authenticateWithLDAP;

class ClientHandler extends Thread {
    private SSLSocket clientSocket;
    private static final Set<String> activeUsers = Collections.synchronizedSet(new HashSet<>());
    public ClientHandler(SSLSocket socket) {
        this.clientSocket = socket;
    }
    /**
     * Méthode pour gérer la communication avec le client
     */
    public void run() {
        User user = null;
        try (ObjectInputStream objectIn = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream objectOut = new ObjectOutputStream(clientSocket.getOutputStream())) {

            objectOut.flush(); // Important pour éviter le blocage du flux

            int attempts = 0;
            boolean isAuthenticated = false;

            while (attempts < 3 && !isAuthenticated) {
                user = (User) objectIn.readObject();

                if (user != null && authenticateWithLDAP(user.getUsername(), user.getPassword())) {
                    synchronized (activeUsers) {
                        if (activeUsers.contains(user.getUsername())) {
                            objectOut.writeObject("Utilisateur déjà connecté.");
                            System.out.println("Tentative de connexion refusée pour " + user.getUsername() + ": déjà connecté.");
                            return;
                        } else {
                            activeUsers.add(user.getUsername());
                        }
                    }

                    isAuthenticated = true;
                    System.out.println("Authentification réussie pour l'utilisateur: " + user.getUsername());
                    objectOut.writeObject("OK");

                    createUserDirectory(user.getUsername());

                    while (true) {
                        Object receivedObject = objectIn.readObject();
                        if (receivedObject instanceof Backup) {
                            Backup backupDetails = (Backup) receivedObject;
                            handleBackup(backupDetails, user.getUsername());
                        } else if (receivedObject instanceof String && "RESTORE_REQUEST".equals(receivedObject)) {
                            handleRestoreRequest(user.getUsername(), objectOut); // Ajout de objectOut comme argument
                        } else if ("END_CONNECTION".equals(receivedObject)) {
                            System.out.println("Le client a demandé la fin de la connexion.");
                            break; // Sortir de la boucle pour fermer la connexion
                        }
                    }

                } else {
                    objectOut.writeObject("Authentification échouée. Veuillez réessayer.");
                    attempts++;
                }
            }

            if (!isAuthenticated) {
                objectOut.writeObject("Nombre maximum de tentatives atteint. Connexion fermée.");
                System.out.println("Connexion fermée après plusieurs tentatives d'authentification infructueuses.");
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Erreur lors de la communication avec le client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (clientSocket != null && !clientSocket.isClosed()) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (user != null) {
                synchronized (activeUsers) {
                    activeUsers.remove(user.getUsername());
                    System.out.println("Utilisateur déconnecté: " + user.getUsername());
                }
            }
        }
    }

    /**
     * Méthode pour créer un dossier de sauvegarde pour un utilisateur si nécessaire
     */
    private static void createUserDirectory(String username) {
        File userDirectory = new File("./users/" + username);
        if (!userDirectory.exists()) {
            boolean isCreated = userDirectory.mkdirs();
            if (isCreated) {
                System.out.println("Dossier créé pour l'utilisateur: " + username);
            } else {
                System.out.println("Impossible de créer le dossier pour l'utilisateur: " + username);
            }
        }
    }

    /*
     * Méthode pour gérer la sauvegarde des fichiers
     */
    private void handleBackup(Backup backupDetails, String username) {
        System.out.println("Démarrage de la sauvegarde pour l'utilisateur : " + username);
        String directoryPath = backupDetails.getDirectoryPath();
        List<String> extensions = backupDetails.getFileExtensions();

        File backupRoot = new File("./users/" + username);
        if (!backupRoot.exists()) {
            backupRoot.mkdirs();
        }

        // Créer le fichier zip de sauvegarde
        // On  récupère la date est l'heure de la sauvegarde
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm"));
        String fileName = date + "-" + time + "_backup.zip";
        File zipFile = new File(backupRoot, fileName);
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) {
            Set<Path> addedDirs = new HashSet<>();

            Path startPath = Paths.get(directoryPath);
            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    // Créer une entrée zip pour chaque dossier
                    Path relDir = startPath.relativize(dir);
                    if (!relDir.toString().isEmpty() && addedDirs.add(relDir)) {
                        ZipEntry dirEntry = new ZipEntry(relDir.toString() + "/");
                        zipOut.putNextEntry(dirEntry);
                        zipOut.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                    String fileName = filePath.getFileName().toString();
                    if (extensions.stream().anyMatch(fileName::endsWith)) {
                        // Créer une entrée zip pour le fichier
                        String zipEntryName = startPath.relativize(filePath).toString();
                        zipOut.putNextEntry(new ZipEntry(zipEntryName));

                        // Lire le contenu du fichier et l'écrire dans le zip
                        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(filePath.toFile()))) {
                            byte[] buffer = new byte[4096];
                            int length;
                            while ((length = inputStream.read(buffer)) > 0) {
                                zipOut.write(buffer, 0, length);
                            }
                        }
                        zipOut.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du fichier zip: " + e.getMessage());
        }
    }

    /*
     * Méthode pour envoyer les fichiers de sauvegarde à l'utilisateur
     */
    private void handleRestoreRequest(String username, ObjectOutputStream objectOut) throws IOException {
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