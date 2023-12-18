package fr.umontpellier.model.auth;

import com.unboundid.ldap.sdk.LDAPException;

public class LDAPConnection {

    /**
     * Méthode pour authentifier un utilisateur avec LDAP
     * @param username
     * @param password
     * @return
     */
    public static boolean authenticateWithLDAP(String username, String password) {
        try {
            com.unboundid.ldap.sdk.LDAPConnection connection = new com.unboundid.ldap.sdk.LDAPConnection("localhost", 389);
            connection.bind("uid=" + username + ",ou=users,dc=example,dc=org", password);
            connection.close();
            return true;
        } catch (LDAPException e) {
            e.printStackTrace();
            return false;
        }
    }


}
