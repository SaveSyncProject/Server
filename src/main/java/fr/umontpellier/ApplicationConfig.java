package fr.umontpellier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ApplicationConfig {
    private static final String PROPERTIES_FILE = "server.properties";
    private static final String PORT_PROPERTY = "server.port";
    private static final String IS_HEADLESS_PROPERTY = "server.ui.headless";
    private static final String LDAP_HOST_PROPERTY = "ldap.host";
    private static final String LDAP_PORT_PROPERTY = "ldap.port";
    private static final String DEFAULT_LDAP_HOST = "localhost";
    private static final int DEFAULT_LDAP_PORT = 389;
    private static final int DEFAULT_PORT = 1234;
    private final Properties properties;

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
        properties.setProperty(IS_HEADLESS_PROPERTY, "false");
    }

    private void saveProperties() {
        try (FileOutputStream out = new FileOutputStream(PROPERTIES_FILE)) {
            properties.store(out, "Server Properties");
        } catch (IOException e) {
            System.err.println("Error saving properties file: " + e.getMessage());
        }
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println("Error parsing property " + key + " to int: " + e.getMessage());
            }
        }
        return defaultValue;
    }

    public static boolean isHeadless() {
        ApplicationConfig config = new ApplicationConfig();
        String headlessValue = config.getProperty(IS_HEADLESS_PROPERTY, "false");
        return Boolean.parseBoolean(headlessValue);
    }
}
