import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.KeyStoreException;

public class Main_Server {
    public static void main(String[] args) {
        int port = 1234; // Port du serveur

        try {
            // Charger le keystore
            String keystorePassword = "miaoumiaou";
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            String userDirectory = System.getProperty("user.dir");
            Path keystorePath = Paths.get(userDirectory, "resources", "SSL","Serveur", "keystore.jks");
            keystore.load(new FileInputStream(keystorePath.toFile()), keystorePassword.toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, keystorePassword.toCharArray());

            // Configurer SSLContext
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(keyManagerFactory.getKeyManagers(), null, null);

            // Créer SSLServerSocketFactory
            SSLServerSocketFactory factory = context.getServerSocketFactory();

            try (SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port)) {
                System.out.println("Serveur SSL démarré sur le port " + port);

                while (true) { // Boucle infinie pour accepter les connexions clients
                    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                    new ClientHandler(clientSocket).start(); // Crée un nouveau thread pour chaque client
                }
            } catch (IOException e) {
                System.out.println("Erreur lors de la création du serveur SSL: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException | KeyManagementException | UnrecoverableKeyException e) {
            e.printStackTrace();
        }
    }
}
