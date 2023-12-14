import java.io.*;
import java.net.Socket;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import model.User;

class ClientHandler extends Thread {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

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

    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            out.println("Entrez votre nom d'utilisateur:");
            String username = in.readLine();
            out.println("Entrez votre mot de passe:");
            String password = in.readLine();

            if (username != null && password != null) {
                User user = new User(username, password);

                if (authenticateWithLDAP(user.getUsername(), user.getPassword())) {
                    out.println("Authentification réussie");
                    System.out.println("Authentification réussie de l'utilisateur: " + username);

                    // Vérifier si le dossier utilisateur existe, sinon le créer
                    File userDirectory = new File("./SavesUsers/" + username);
                    if (!userDirectory.exists()) {
                        userDirectory.mkdirs();
                        System.out.println("Dossier créé pour l'utilisateur: " + username);
                    }

                    // Gérer la communication client après l'authentification
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        System.out.println("Client dit: " + inputLine);
                        out.println("Echo: " + inputLine);
                    }
                } else {
                    out.println("Authentification échouée");
                }
            } else {
                out.println("Nom d'utilisateur ou mot de passe manquant");
            }
        } catch (IOException e) {
            System.out.println("Erreur lors de la communication avec le client: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
