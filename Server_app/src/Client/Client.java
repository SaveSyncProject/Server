package Client;

import model.User;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        // Adresse du serveur et port
        String host = "localhost";
        int port = 1234;

        try (Socket socket = new Socket(host, port);
             ObjectOutputStream objectOut = new ObjectOutputStream(socket.getOutputStream());
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Scanner scanner = new Scanner(System.in);

            System.out.println("Entrez votre nom d'utilisateur:");
            String username = scanner.nextLine();
            System.out.println("Entrez votre mot de passe:");
            String password = scanner.nextLine();

            User user = new User(username, password);
            objectOut.writeObject(user);

            String response;
            while ((response = in.readLine()) != null) {
                System.out.println("Réponse du serveur: " + response);
            }

        } catch (IOException e) {
            System.out.println("Erreur lors de la connexion au serveur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
