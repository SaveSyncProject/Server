package LDAP;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
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
          System.out.println(m.authenticateWithLDAP("ggonfiantini", "gaga"));
    }

}
