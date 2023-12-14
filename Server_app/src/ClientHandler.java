import java.io.*;
import java.net.Socket;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import model.BackupDetails;
import model.User;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.SSLSocket;

class ClientHandler extends Thread {
    private SSLSocket clientSocket;

    public ClientHandler(SSLSocket socket) {
        this.clientSocket = socket;
    }
    /**
     * Méthode pour gérer la communication avec le client
     */
    public void run() {
        try (ObjectInputStream objectIn = new ObjectInputStream(clientSocket.getInputStream());
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            User user = (User) objectIn.readObject();

            if (user != null && authenticateWithLDAP(user.getUsername(), user.getPassword())) {
                System.out.println("Authentification réussie pour l'utilisateur: " + user.getUsername());

                out.println("Authentification réussie");
                createUserDirectory(user.getUsername()); // Création du dossier de sauvegarde pour l'utilisateur si nécessaire
                // Communication avec le client après authentification
                Object receivedObject = objectIn.readObject();
                if (receivedObject instanceof BackupDetails) {
                    BackupDetails backupDetails = (BackupDetails) receivedObject;
                    handleBackup(backupDetails, user.getUsername());
                }
            } else {
                out.println("Authentification échouée ou utilisateur invalide");
            }

        } catch (IOException e) {
            System.out.println("Erreur lors de la communication avec le client: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("Classe User non trouvée lors de la désérialisation: " + e.getMessage());
        }
    }




    /**
     * Méthode pour authentifier un utilisateur avec LDAP
     * @param username
     * @param password
     * @return
     */
    private static boolean authenticateWithLDAP(String username, String password) {
        try {
            LDAPConnection connection = new LDAPConnection("localhost", 389);
            connection.bind("uid=" + username + ",ou=utilisateurs,dc=example,dc=org", password);
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
        File userDirectory = new File("./SavesUsers/" + username);
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

        File backupRoot = new File("./SavesUsers/" + username);
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
