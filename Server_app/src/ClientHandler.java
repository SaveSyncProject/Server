import java.io.*;
import java.net.Socket;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import model.User;

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

            User user = (User) objectIn.readObject(); // Lecture de l'objet User envoyé par le client

            if (user != null && authenticateWithLDAP(user.getUsername(), user.getPassword())) {
                out.println("Authentification réussie");
                createUserDirectory(user.getUsername()); // Création du dossier de sauvegarde pour l'utilisateur si nécessaire

                // Communication avec le client après authentification
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Client dit: " + inputLine);
                    out.println("Echo: " + inputLine);
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
}
