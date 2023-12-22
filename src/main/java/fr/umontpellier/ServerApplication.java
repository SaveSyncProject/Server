package fr.umontpellier;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.security.KeyManagementException;
import java.security.cert.CertificateException;

public class ServerApplication {
    public static void main(String[] args) {
        int port = 1234;

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

            System.out.println("Server is listening on port " + port + " and IP address " + InetAddress.getLocalHost().getHostAddress());

            while (true) {
                SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
                new ClientHandler(sslSocket).start(); // Assurez-vous que fr.umontpellier.ClientHandler utilise SSLSocket
            }
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | KeyManagementException e) {
            e.printStackTrace();
            System.out.println("Error while initializing the server: " + e.getMessage());
        }
    }
}
