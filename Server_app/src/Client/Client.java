package Client;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Client {
    public static void main(String[] args) {

        String userDirectory = System.getProperty("user.dir");
        Path keystorePath = Paths.get(userDirectory, "resources", "SSL","Client", "truststore.jks");
        String keystorePathString = keystorePath.toString();

        // Configurer le truststore
        System.setProperty("javax.net.ssl.trustStore", keystorePathString);
        System.setProperty("javax.net.ssl.trustStorePassword", "miaoumiaou");

        // Adresse du serveur et port
        String host = "localhost";
        int port = 1234;

        // Création d'une fabrique de socket SSL
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connecté au serveur : " + socket.getInetAddress());

            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                out.println(userInput); // Envoyer au serveur
                String response = in.readLine(); // Lire la réponse du serveur
                System.out.println("Réponse du serveur: " + response);
            }
        } catch (IOException e) {
            System.out.println("Erreur lors de la connexion au serveur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
