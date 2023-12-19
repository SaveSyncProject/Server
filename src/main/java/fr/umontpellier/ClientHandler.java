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
                        if("LIST_BACKUPS_REQUEST".equals(receivedObject)){
                            ListBackup listBackup = new ListBackup();
                            List<String> listBackupEnvoie = listBackup.listBackups(user.getUsername());
                            objectOut.writeObject(listBackupEnvoie);
                            objectOut.flush();

                        }else if ("SAVE_REQUEST".equals(receivedObject)) {

                            Object receivedObject2 = objectIn.readObject();
                            Backup backupDetails = (Backup) receivedObject2;
                            SaveRequest saveRequest = new SaveRequest();
                            saveRequest.handleBackup(backupDetails, user.getUsername());
                        }
                        else if ("RESTORE_REQUEST".equals(receivedObject)) {
                            RestoreRequest restoreRequest = new RestoreRequest();
                            restoreRequest.handleRestoreRequest(user.getUsername(), objectOut);
                        }

                        else if ("END_CONNECTION".equals(receivedObject)) {
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