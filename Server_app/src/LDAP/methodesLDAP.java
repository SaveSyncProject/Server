package LDAP;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;

public class methodesLDAP {

    private boolean authenticateWithLDAP(String username, String password) {
        try {
            LDAPConnection connection = new LDAPConnection("localhost", 389);
            connection.bind("uid=" + username + ",ou=utilisateurs,dc=example,dc=org", password);
            connection.close();
            return true;
        } catch (LDAPException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        methodesLDAP m = new methodesLDAP();
        // Utilisez l'identifiant uid pour la connexion
        System.out.println(m.authenticateWithLDAP("ggonfiantini", "gaga"));
    }
}
