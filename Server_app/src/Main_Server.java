import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main_Server {
    public static void main(String[] args) {
        int port = 1234; // Port du serveur

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur démarré sur le port " + port);

            while (true) { // Boucle infinie pour accepter les connexions clients
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start(); // Crée un nouveau thread pour chaque client
            }
        } catch (IOException e) {
            System.out.println("Erreur lors de la création du serveur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
