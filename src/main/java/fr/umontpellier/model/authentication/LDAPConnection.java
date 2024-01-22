package fr.umontpellier.model.authentication;

import com.unboundid.ldap.sdk.LDAPException;

public class LDAPConnection {

    private static String ldapHost = "localhost";
    private static int ldapPort = 389;

    /** Méthode pour configurer l'adresse et le port du serveur LDAP
     *
     * @param host
     * @param port
     */
    public static void configure(String host, int port) {
        ldapHost = host;
        ldapPort = port;
    }

    /**
     * Méthode pour authentifier un utilisateur avec LDAP
     * @param username nom d'utilisateur
     * @param password mot de passe de l'utilisateur
     * @return true si l'authentification est réussie, false sinon
     */
    public static boolean authenticateWithLDAP(String username, String password) {
        try {
            com.unboundid.ldap.sdk.LDAPConnection connection = new com.unboundid.ldap.sdk.LDAPConnection(ldapHost, ldapPort);
            connection.bind("uid=" + username + ",ou=users,dc=example,dc=org", password);
            connection.close();
            return true;
        } catch (LDAPException e) {
            e.printStackTrace();
            return false;
        }
    }
}