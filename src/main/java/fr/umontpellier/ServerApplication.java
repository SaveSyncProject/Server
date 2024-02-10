package fr.umontpellier;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;

import fr.umontpellier.model.authentication.LDAPConnection;
import fr.umontpellier.model.logging.LoggingService;

public class ServerApplication {

    public static void main(String[] args) {

        int port = ApplicationConfig.getServerPort();
        String ldapHost = ApplicationConfig.getLdapHost();
        int ldapPort = ApplicationConfig.getLdapPort();
                
        LDAPConnection.configure(ldapHost, ldapPort);

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

            LoggingService.getLogger().log("Server is listening on " + InetAddress.getLocalHost().getHostAddress() + ":" + port);

            while (true) {
                SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
                new ClientHandler(sslSocket).start();
            }
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | KeyManagementException e) {
            e.printStackTrace();
            System.out.println("Error while initializing the server: " + e.getMessage());
        }
    }
}