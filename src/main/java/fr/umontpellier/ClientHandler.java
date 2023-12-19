package fr.umontpellier;

import fr.umontpellier.model.Backup;
import fr.umontpellier.model.User;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fr.umontpellier.model.auth.LDAPConnection.authenticateWithLDAP;

import fr.umontpellier.model.request.SaveRequest;
import fr.umontpellier.model.request.RestoreRequest;
import fr.umontpellier.model.request.DeleteBackup;
import fr.umontpellier.model.request.ListBackup;

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

            objectOut.flush();

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
                        Object receivedObject = objectIn.readObject(); // On reçoit la requête du client
                        // Requête de lister les backups
                        if ("LIST_BACKUPS_REQUEST".equals(receivedObject)) {
                            ListBackup listBackups = new ListBackup();
                            List<String> backupList = listBackups.listBackups(user.getUsername());
                            objectOut.writeObject(backupList);
                            objectOut.flush();
                        }
                        // Requête de lister les fichiers dans un backup
                        else if ("LIST_FILES_REQUEST".equals(receivedObject)) {
                            ListBackup listFiles = new ListBackup();
                            String backupName = (String) objectIn.readObject();
                            List<String> files = listFiles.listFilesInBackup(user.getUsername(), backupName);
                            objectOut.writeObject(files);
                            objectOut.flush();
                        }
                        // Requête de sauvegarde d'un dossier
                        else if ("SAVE_REQUEST".equals(receivedObject)) {
                            Object receivedObject2 = objectIn.readObject();
                            Backup backupDetails = (Backup) receivedObject2;
                            SaveRequest saveRequest = new SaveRequest();
                            saveRequest.handleBackup(backupDetails, user.getUsername());
                        }
                        // Requête de restauration complète d'un backup
                        else if ("RESTORE_ALL_REQUEST".equals(receivedObject)) {
                            RestoreRequest restoreRequest = new RestoreRequest();
                            restoreRequest.handleRestoreRequest(user.getUsername(), objectOut);
                        }
                        // Requête de restauration partiel d'un backup
                        else if("RESTORE_PARTIAL_REQUEST".equals(receivedObject)) {
                            try {
                                List<String> filesToRestore = (List<String>) objectIn.readObject();
                                RestoreRequest restorePartialRequest = new RestoreRequest();
                                restorePartialRequest.restoreFiles(user.getUsername(), filesToRestore, objectOut);
                            } catch (ClassNotFoundException | IOException e) {
                                e.printStackTrace();
                            }
                        }
                        // Requête qui supprime un backup
                        else if("DELETE_BACKUP_REQUEST".equals(receivedObject)) {
                            try {
                                String backupName = (String) objectIn.readObject();
                                DeleteBackup deleteBackupRequest = new DeleteBackup();
                                boolean isSuccessful = deleteBackupRequest.deleteBackup(user.getUsername(), backupName);
                                objectOut.writeObject(isSuccessful ? "SUCCESS" : "ERROR");
                                objectOut.flush();
                            } catch (ClassNotFoundException | IOException e) {
                                e.printStackTrace();
                            }
                        }
                        // Requête de suppression de fichiers dans un backup
                        else if ("DELETE_FILES_REQUEST".equals(receivedObject)) {
                            try {
                                List<String> filesToDelete = (List<String>) objectIn.readObject();
                                DeleteBackup deleteFilesRequest = new DeleteBackup();
                                boolean isSuccessful = deleteFilesRequest.deleteFiles(user.getUsername(), filesToDelete);
                                objectOut.writeObject(isSuccessful ? "SUCCESS" : "ERROR");
                                objectOut.flush();
                            } catch (ClassNotFoundException | IOException e) {
                                e.printStackTrace();
                            }
                            // Requête de déconnexion
                        } else if ("END_CONNECTION".equals(receivedObject)) {
                            System.out.println("Le client a demandé la fin de la connexion.");
                            break;
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
}