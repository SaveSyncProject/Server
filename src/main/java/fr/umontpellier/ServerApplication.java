package fr.umontpellier;

import javax.net.ssl.*;
import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Properties;

import fr.umontpellier.model.ServerConsoleUI;

public class ServerApplication {

    private static final String PROPERTIES_FILE = "server.properties";
    private static final String PORT_PROPERTY = "server.port";
    private static final int DEFAULT_PORT = 1234;

    public static void main(String[] args) {

        // Initialisation de l'interface utilisateur
        SwingUtilities.invokeLater(ServerConsoleUI::new);

        // Configuration du serveur
        int port = loadPort();

        try {
            // Récupérer le chemin du fichier keystore.jks
            URL keystoreResource = ServerApplication.class.getClassLoader().getResource("ssl/server/mySrvKeystore.jks");
            if (keystoreResource == null) {
                throw new FileNotFoundException("Keystore file not found");
            }
            // Mot de passe du keystore
            String keystorePassword = "miaoumiaou";

            // Charger le keystore
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(keystoreResource.openStream(), keystorePassword.toCharArray());

            // Initialiser le KeyManagerFactory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, keystorePassword.toCharArray());

            // Initialiser le SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);

            // Créer le SSLServerSocket
            SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
            SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port);

            System.out.println("Server is listening on " + InetAddress.getLocalHost().getHostAddress() + ":" + port);

            while (true) {
                SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
                new ClientHandler(sslSocket).start();
            }
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | KeyManagementException e) {
            e.printStackTrace();
            System.out.println("Error while initializing the server: " + e.getMessage());
        }
    }

    private static int loadPort() {
        Properties properties = new Properties();
        File propertiesFile = new File(PROPERTIES_FILE);
        if (!propertiesFile.exists()) {
            properties.setProperty(PORT_PROPERTY, String.valueOf(DEFAULT_PORT));
            try (FileOutputStream out = new FileOutputStream(PROPERTIES_FILE)) {
                properties.store(out, "Server Properties");
            } catch (IOException e) {
                System.err.println("Error creating properties file: " + e.getMessage());
                return DEFAULT_PORT;
            }
        } else {
            try (FileInputStream in = new FileInputStream(PROPERTIES_FILE)) {
                properties.load(in);
                return Integer.parseInt(properties.getProperty(PORT_PROPERTY, String.valueOf(DEFAULT_PORT)));
            } catch (IOException | NumberFormatException e) {
                System.err.println("Error reading properties file: " + e.getMessage());
                return DEFAULT_PORT;
            }
        }
        return DEFAULT_PORT;
    }
}