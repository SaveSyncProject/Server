import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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

    public static void main(String[] args){
        System.out.println(authenticateWithLDAP("ggonfiantini", "gaga"));
    }


    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Demander le nom d'utilisateur et le mot de passe
            out.println("Entrez votre nom d'utilisateur:");
            String username = in.readLine();
            out.println("Entrez votre mot de passe:");
            String password = in.readLine();

            User user = new User(username, password);

            // Authentifier l'utilisateur
            if (authenticateWithLDAP(user.getUsername(), user.getPassword())) {
                out.println("Authentification réussie");

                // Gérer la communication client après l'authentification
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Client dit: " + inputLine);
                    out.println("Echo: " + inputLine);
                }
            } else {
                out.println("Authentification échouée");
            }
        } catch (IOException e) {
            System.out.println("Erreur lors de la communication avec le client: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
