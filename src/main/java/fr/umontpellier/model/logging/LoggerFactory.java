package fr.umontpellier.model.logging;

public class LoggerFactory {
    public static Logger getLogger(boolean isHeadless) {
        if (isHeadless) {
            return new SystemOutLogger();
        } else {
            return new GUILogger();
        }
    }
}