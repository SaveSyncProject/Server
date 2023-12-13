import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class ClientHandler extends Thread {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String inputLine;
            while ((inputLine = in.readLine()) != null) { // Lire les messages du client
                System.out.println("Client dit: " + inputLine);
                out.println("Echo: " + inputLine); // Répondre au client
            }
        } catch (IOException e) {
            System.out.println("Erreur lors de la communication avec le client: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
