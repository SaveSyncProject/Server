package fr.umontpellier;

import fr.umontpellier.model.Backup;
import fr.umontpellier.model.User;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fr.umontpellier.model.authentication.LDAPConnection.authenticateWithLDAP;

import fr.umontpellier.model.request.CreateBackupRequest;
import fr.umontpellier.model.request.RestoreBackupRequest;
import fr.umontpellier.model.request.DeleteBackupRequest;
import fr.umontpellier.model.request.ReadBackupRequest;

class ClientHandler extends Thread {
    private final SSLSocket clientSocket;
    private static final Set<String> activeUsers = Collections.synchronizedSet(new HashSet<>());

    public ClientHandler(SSLSocket socket) {
        this.clientSocket = socket;
    }

    /**
     * Méthode pour gérer les requêtes du client
     */
    public void run() {
        User user = null;
        try (ObjectInputStream objectIn = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream objectOut = new ObjectOutputStream(clientSocket.getOutputStream())) {

            objectOut.flush();

            int attempts = 0;
            boolean isAuthenticated = false;

            while (attempts < 3) {
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

                    System.out.println("Authentification réussie pour l'utilisateur: " + user.getUsername());
                    objectOut.writeObject("OK");

                    createUserDirectory(user.getUsername());

                    while (true) {
                        // Lecture de la requête du client
                        String requestType = (String) objectIn.readObject();

                        // Traitement de la requête en fonction de l'entête
                        switch (requestType) {
                            case "LIST_BACKUPS_REQUEST" -> {
                                ReadBackupRequest listBackups = new ReadBackupRequest();
                                List<String> backupList = listBackups.listBackups(user.getUsername());
                                objectOut.writeObject(backupList);
                                objectOut.flush();
                            }
                            case "LIST_FILES_REQUEST" -> {
                                ReadBackupRequest listFiles = new ReadBackupRequest();
                                String backupName = (String) objectIn.readObject();
                                List<String> files = listFiles.listFiles(user.getUsername(), backupName);
                                objectOut.writeObject(files);
                                objectOut.flush();
                            }
                            case "SAVE_REQUEST" -> {
                                Backup backupDetails = (Backup) objectIn.readObject();
                                CreateBackupRequest createBackupRequest = new CreateBackupRequest();
                                createBackupRequest.handleBackup(backupDetails, user.getUsername());
                            }
                            case "RESTORE_ALL_REQUEST" -> {
                                RestoreBackupRequest restoreBackupRequest = new RestoreBackupRequest();
                                restoreBackupRequest.handleRestoreRequest(user.getUsername(), objectOut);
                            }
                            case "RESTORE_PARTIAL_REQUEST" -> {
                                List<String> filesToRestore = (List<String>) objectIn.readObject();
                                RestoreBackupRequest restorePartialRequest = new RestoreBackupRequest();
                                restorePartialRequest.restoreFiles(user.getUsername(), filesToRestore, objectOut);
                            }
                            case "DELETE_BACKUP_REQUEST" -> {
                                String deleteBackupName = (String) objectIn.readObject();
                                DeleteBackupRequest deleteRequestBackupRequest = new DeleteBackupRequest();
                                boolean deleteSuccessful = deleteRequestBackupRequest.deleteBackup(user.getUsername(), deleteBackupName);
                                objectOut.writeObject(deleteSuccessful ? "SUCCESS" : "ERROR");
                                objectOut.flush();
                            }
                            case "DELETE_FILES_REQUEST" -> {
                                List<String> filesToDelete = (List<String>) objectIn.readObject();
                                DeleteBackupRequest deleteFilesRequest = new DeleteBackupRequest();
                                boolean deleteFilesSuccessful = deleteFilesRequest.deleteFiles(user.getUsername(), filesToDelete);
                                objectOut.writeObject(deleteFilesSuccessful ? "SUCCESS" : "ERROR");
                                objectOut.flush();
                            }
                            case "END_CONNECTION" -> {
                                System.out.println("Le client a demandé la fin de la connexion.");
                                return; // Sortir de la boucle
                            }
                            default -> System.out.println("Requête non reconnue: " + requestType);
                        }
                    }

                } else {
                    objectOut.writeObject("Authentification échouée. Veuillez réessayer.");
                    attempts++;
                }
            }

            // Si l'utilisateur n'a pas été authentifié après 3 tentatives, on ferme la connexion
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
}