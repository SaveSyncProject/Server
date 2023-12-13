package LDAP;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

public class methodesLDAP {
    public static boolean authenticate(String username, String password) {
        String ldapUrl = "ldap://ldap-server";
        String domain = "example.org";

        // Configuration des propriétés de l'environnement pour la connexion LDAP
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapUrl);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, username + "@" + domain); // Utiliser le format 'username@domain'
        env.put(Context.SECURITY_CREDENTIALS, password);

        try {
            // Tentative de connexion au serveur LDAP
            DirContext ctx = new InitialDirContext(env);
            System.out.println("Connexion réussie!");
            ctx.close();
            return true; // Authentification réussie
        } catch (NamingException e) {
            System.out.println("Erreur lors de la connexion: " + e.getMessage());
            return false; // Authentification échouée
        }
    }

    public static void main(String[] args) {
        // Exemple d'utilisation
        boolean isAuthenticated = authenticate("ggonfiantini", "gaga"); // Remplacer par les crédentials réels
        if (isAuthenticated) {
            System.out.println("Utilisateur authentifié avec succès.");
        } else {
            System.out.println("Échec de l'authentification.");
        }
    }

}
