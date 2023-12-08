import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;

public class Main_Server {
    public static void main(String[] args) {
        int port = 1234; // Port du serveur
        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        try (SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port)) {
            System.out.println("Serveur SSL démarré sur le port " + port);
            while (true) {
                try {
                    // Accepte une nouvelle connexion client
                    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                    System.out.println("Client connecté : " + clientSocket.getInetAddress());

                    // Gère la connexion client dans un nouveau thread
                    new ClientHandler(clientSocket).start();
                } catch (IOException e) {
                    System.out.println("Erreur lors de la connexion avec le client: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.out.println("Erreur lors de la création du serveur SSL: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
