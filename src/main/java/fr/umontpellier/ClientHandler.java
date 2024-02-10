package fr.umontpellier;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLSocket;

import fr.umontpellier.logging.LoggingService;
import fr.umontpellier.model.Backup;
import fr.umontpellier.model.User;
import fr.umontpellier.model.authentication.LDAPConnection;
import fr.umontpellier.model.request.backup.CreateBackupRequest;
import fr.umontpellier.model.request.backup.DeleteBackupRequest;
import fr.umontpellier.model.request.backup.ReadBackupRequest;
import fr.umontpellier.model.request.backup.RestoreBackupRequest;
import fr.umontpellier.model.request.file.DeleteFileRequest;
import fr.umontpellier.model.request.file.ReadFileRequest;
import fr.umontpellier.model.request.file.RestoreFileRequest;

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
                LDAPConnection ldapConnection = new LDAPConnection();
                
                if (user != null && ldapConnection.authenticateWithLDAP(user.getUsername(), user.getPassword())) {
                    synchronized (activeUsers) {
                        if (activeUsers.contains(user.getUsername())) {
                            objectOut.writeObject("User already connected.");
                            LoggingService.getLogger().log("Connection refused for user: " + user.getUsername());
                            return;
                        } else {
                            activeUsers.add(user.getUsername());
                        }
                    }
                    LoggingService.getLogger().log("Connection accepted for user: " + user.getUsername());
                    objectOut.writeObject("OK");

                    createUserDirectory(user.getUsername());

                    while (true) {
                        String requestType = (String) objectIn.readObject();

                        switch (requestType) {
                            case "READ_BACKUP" -> {
                                ReadBackupRequest readBackupRequest = new ReadBackupRequest(objectOut, user.getUsername());
                                readBackupRequest.execute();
                            }
                            case "READ_FILE" -> {
                                String backupName = (String) objectIn.readObject();
                                ReadFileRequest readFileRequest = new ReadFileRequest(objectOut, user.getUsername(), backupName);
                                readFileRequest.execute();
                            }
                            case "CREATE_BACKUP" -> {
                                Backup backup = (Backup) objectIn.readObject();
                                CreateBackupRequest createBackupRequest = new CreateBackupRequest(backup, user.getUsername());
                                createBackupRequest.execute();
                            }
                            case "RESTORE_BACKUP" -> {
                                String backupName = (String) objectIn.readObject();
                                RestoreBackupRequest restoreBackupRequest = new RestoreBackupRequest(user.getUsername(), backupName, objectOut);
                                restoreBackupRequest.execute();
                            }
                            case "RESTORE_FILE" -> {
                                String backupName = (String) objectIn.readObject(); // Recevoir le nom de la sauvegarde
                                List<String> filesToRestore = (List<String>) objectIn.readObject();
                                RestoreFileRequest restorePartialRequest = new RestoreFileRequest(user.getUsername(), backupName, filesToRestore , objectOut);
                                restorePartialRequest.execute();
                            }
                            case "DELETE_BACKUP" -> {
                                String backupName = (String) objectIn.readObject();
                                DeleteBackupRequest deleteBackupRequest = new DeleteBackupRequest(user.getUsername(), backupName, objectOut);
                                deleteBackupRequest.execute();
                            }
                            case "DELETE_FILE" -> {
                                List<String> filesToDelete = (List<String>) objectIn.readObject();
                                DeleteFileRequest deleteFileRequest = new DeleteFileRequest(user.getUsername(), filesToDelete, objectOut);
                                deleteFileRequest.execute();
                            }
                            case "END_CONNECTION" -> {
                                LoggingService.getLogger().log("Client closed connection.");
                                return;
                            }
                            default -> LoggingService.getLogger().log("Unknown request type: " + requestType);
                        }
                    }

                } else {
                    objectOut.writeObject("Authentification échouée. Veuillez réessayer.");
                    attempts++;
                }
            }

            if (!isAuthenticated) {
                objectOut.writeObject("Nombre maximum de tentatives atteint. Connexion fermée.");
                LoggingService.getLogger().log("Connection refused for user: " + user.getUsername());
            }

        } catch (IOException | ClassNotFoundException e) {
            LoggingService.getLogger().log("Error while handling client request: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (clientSocket != null && !clientSocket.isClosed()) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    LoggingService.getLogger().log("Error while closing client socket: " + e.getMessage());
                }
            }
            if (user != null) {
                synchronized (activeUsers) {
                    activeUsers.remove(user.getUsername());
                    LoggingService.getLogger().log("User disconnected: " + user.getUsername());
                }
            }
        }
    }

    /**
     * Crée un dossier pour l'utilisateur
     *
     * @param username le nom de l'utilisateur
     */
    private static void createUserDirectory(String username) {
        File userDirectory = new File("./users/" + username);
        if (!userDirectory.exists()) {
            boolean isCreated = userDirectory.mkdirs();
            if (isCreated) {
                LoggingService.getLogger().log("User directory created for user: " + username);
            } else {
                LoggingService.getLogger().log("Error while creating user directory for user: " + username);
            }
        }
    }
}