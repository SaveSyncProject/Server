package Client;
import model.User;
import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.security.KeyStore;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 1234;

        // Récupérer le chemin du fichier truststore.jks
        URL truststoreResource = Client.class.getClassLoader().getResource("./SSL/Client/myClientKeystore.jks");
        if (truststoreResource == null) {
            throw new FileNotFoundException("Le fichier 'myClientKeystore.jks' est introuvable.");
        }
        String truststorePassword = "miaoumiaou"; // Remplacez par votre mot de passe de truststore

        // Charger le truststore
        KeyStore ts = KeyStore.getInstance("JKS");
        ts.load(truststoreResource.openStream(), truststorePassword.toCharArray());

        // Initialiser le TrustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);

        // Initialiser le SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        // Créer le SSLSocket
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(host, port);

        // Communiquer avec le serveur
        try (ObjectOutputStream objectOut = new ObjectOutputStream(sslSocket.getOutputStream());
             BufferedReader in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()))) {

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
