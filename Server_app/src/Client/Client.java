package Client;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;

public class Client {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        // Adresse du serveur et port
        String host = "localhost";
        int port = 1234;
        // Chemin vers le keystore
        System.setProperty("javax.net.ssl.trustStore", "../../../Certificats/monkeystore.p12");
        // Mot de passe du keystore
        System.setProperty("javax.net.ssl.trustStorePassword", "miaoumiaou");

        // Création d'une fabrique de socket SSL
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
            // Affichage d'informations de connexion
            System.out.println("Connecté au serveur : " + socket.getInetAddress());

            // Envoi et réception de messages
            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // Envoi d'un message
                out.println("Bonjour serveur!");

                // Lecture de la réponse
                String response = in.readLine();
                System.out.println("Réponse du serveur: " + response);
            }
        } catch (IOException e) {
            System.out.println("Erreur lors de la connexion au serveur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
