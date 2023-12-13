package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client2 {
    public static void main(String[] args) {
        // Adresse du serveur et port
        String host = "localhost";
        int port = 1234;

        try (Socket socket = new Socket(host, port);
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
