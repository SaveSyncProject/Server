package fr.umontpellier;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import fr.umontpellier.model.BackupDetails;
import fr.umontpellier.model.User;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            int attempts = 0;
            boolean isAuthenticated = false;

            while (attempts < 3 && !isAuthenticated) {
                try {
                    user = (User) objectIn.readObject();

                    if (user != null && authenticateWithLDAP(user.getUsername(), user.getPassword())) {
                        synchronized (activeUsers) {
                            if (activeUsers.contains(user.getUsername())) {
                                out.println("Utilisateur déjà connecté.");
                                System.out.println("Tentative de connexion refusée pour " + user.getUsername() + ": déjà connecté.");
                                return;
                            } else {
                                activeUsers.add(user.getUsername());
                            }
                        }

                        isAuthenticated = true;
                        System.out.println("Authentification réussie pour l'utilisateur: " + user.getUsername());
                        out.println("OK");
                        createUserDirectory(user.getUsername());

                        while (true) {
                            Object receivedObject = objectIn.readObject();
                            if (receivedObject instanceof BackupDetails) {
                                BackupDetails backupDetails = (BackupDetails) receivedObject;
                                handleBackup(backupDetails, user.getUsername());
                            } else if (receivedObject instanceof String && "END_CONNECTION".equals(receivedObject)) {
                                System.out.println("Le client a demandé la fin de la connexion.");
                                break; // Sortir de la boucle pour fermer la connexion
                            }
                        }
                    } else {
                        out.println("Authentification échouée. Veuillez réessayer.");
                        attempts++;
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    System.out.println("Classe User non trouvée lors de la désérialisation: " + e.getMessage());
                    break;
                }
            }

            if (!isAuthenticated) {
                out.println("Nombre maximum de tentatives atteint. Connexion fermée.");
                System.out.println("Connexion fermée après plusieurs tentatives d'authentification infructueuses.");
            }

        } catch (IOException e) {
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
     * Méthode pour authentifier un utilisateur avec LDAP
     * @param username
     * @param password
     * @return
     */
    private boolean authenticateWithLDAP(String username, String password) {
        try {
            LDAPConnection connection = new LDAPConnection("localhost", 389);
            connection.bind("uid=" + username + ",ou=users,dc=example,dc=org", password);
            connection.close();
            return true;
        } catch (LDAPException e) {
            e.printStackTrace();
            return false;
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

    private void handleBackup(BackupDetails backupDetails, String username) {
        System.out.println("Démarrage de la sauvegarde pour l'utilisateur : " + username);
        String directoryPath = backupDetails.getDirectoryPath();
        List<String> extensions = backupDetails.getFileExtensions();

        File backupRoot = new File("./users/" + username);
        if (!backupRoot.exists()) {
            backupRoot.mkdirs();
        }
        HashMap<String, Long> lastModifiedMap = loadLastModifiedMap(backupRoot);
        try {
            Files.walkFileTree(Paths.get(directoryPath), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                    String fileName = filePath.getFileName().toString();
                    if (extensions.stream().anyMatch(fileName::endsWith)) {
                        String relativePath = backupRoot.toPath().resolve(directoryPath).relativize(filePath).toString();
                        File backupFile = new File(backupRoot, relativePath);

                        if (!lastModifiedMap.containsKey(relativePath) ||
                                attrs.lastModifiedTime().toMillis() > lastModifiedMap.get(relativePath)) {
                            backupFile(backupFile, filePath.toFile());
                            lastModifiedMap.put(relativePath, attrs.lastModifiedTime().toMillis());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            saveLastModifiedMap(lastModifiedMap, backupRoot);
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture des fichiers: " + e.getMessage());
        }
    }

    private void backupFile(File backupFile, File sourceFile) throws IOException {
        System.out.println("Sauvegarde du fichier : " + sourceFile.getPath());
        backupFile.getParentFile().mkdirs();
        try (FileInputStream in = new FileInputStream(sourceFile);
             FileOutputStream out = new FileOutputStream(backupFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    private HashMap<String, Long> loadLastModifiedMap(File backupRoot) {
        File lastModifiedFile = new File(backupRoot, "lastModifiedMap.ser");
        if (lastModifiedFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(lastModifiedFile))) {
                return (HashMap<String, Long>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Erreur de sauvegarde : " + e.getMessage());
                e.printStackTrace();
            }
        }
        return new HashMap<>();
    }

    private void saveLastModifiedMap(HashMap<String, Long> lastModifiedMap, File backupRoot) {
        File lastModifiedFile = new File(backupRoot, "lastModifiedMap.ser");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(lastModifiedFile))) {
            oos.writeObject(lastModifiedMap);
        } catch (IOException e) {
            e.printStackTrace();
            // Gérer l'exception si nécessaire
        }
    }

}
