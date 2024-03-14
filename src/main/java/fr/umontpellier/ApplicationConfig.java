package fr.umontpellier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ApplicationConfig {
    private static final String PROPERTIES_FILE = "server.properties";
    private final Properties properties;
    private static final String PORT_PROPERTY = "server.port";
    private static final String UI_HEADLESS_PROPERTY = "server.ui.headless";
    private static final String LDAP_HOST_PROPERTY = "ldap.host";
    private static final String LDAP_PORT_PROPERTY = "ldap.port";
    private static final String DEFAULT_LDAP_HOST = "localhost";
    private static final int DEFAULT_LDAP_PORT = 389;
    private static final int DEFAULT_PORT = 1234;

    public ApplicationConfig() {
        properties = new Properties();
        loadProperties();
    }

    private void loadProperties() {
        File propertiesFile = new File(PROPERTIES_FILE);
        if (!propertiesFile.exists()) {
            setDefaultProperties();
            saveProperties();
        } else {
            try (FileInputStream in = new FileInputStream(PROPERTIES_FILE)) {
                properties.load(in);
            } catch (IOException e) {
                System.err.println("Error reading properties file: " + e.getMessage());
                setDefaultProperties();
            }
        }
    }

    private void setDefaultProperties() {
        properties.setProperty(PORT_PROPERTY, String.valueOf(DEFAULT_PORT));
        properties.setProperty(LDAP_HOST_PROPERTY, DEFAULT_LDAP_HOST);
        properties.setProperty(LDAP_PORT_PROPERTY, String.valueOf(DEFAULT_LDAP_PORT));
        properties.setProperty(UI_HEADLESS_PROPERTY, "true");
    }

    private void saveProperties() {
        try (FileOutputStream out = new FileOutputStream(PROPERTIES_FILE)) {
            properties.store(out, "Server Properties");
        } catch (IOException e) {
            System.err.println("Error saving properties file: " + e.getMessage());
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static boolean isHeadless() {
        ApplicationConfig config = new ApplicationConfig();
        String headlessValue = config.getProperty(UI_HEADLESS_PROPERTY);
        return Boolean.parseBoolean(headlessValue);
    }
}
