package fr.umontpellier;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.security.KeyStore;

public class ServerApplication {
    public static void main(String[] args) throws Exception {
        int port = 1234;

        // Récupérer le chemin du fichier keystore.jks
        URL keystoreResource = ServerApplication.class.getClassLoader().getResource("ssl/server/mySrvKeystore.jks");
        if (keystoreResource == null) {
            throw new FileNotFoundException("Le fichier 'mySrvKeystore.jks' est introuvable.");
        }
        String keystorePassword = "miaoumiaou"; // Remplacez par votre mot de passe de keystore

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

        System.out.println("Serveur SSL en écoute sur le port " + port + " et sur l'adresse " + InetAddress.getLocalHost().getHostAddress());

        while (true) {
            SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
            new ClientHandler(sslSocket).start(); // Assurez-vous que fr.umontpellier.ClientHandler utilise SSLSocket
        }
    }
}