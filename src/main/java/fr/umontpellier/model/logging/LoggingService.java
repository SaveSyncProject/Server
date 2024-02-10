package fr.umontpellier.model.logging;

import fr.umontpellier.ApplicationConfig;

public class LoggingService {
    private static Logger logger;

    public static synchronized Logger getLogger() {
        if (logger == null) {
            boolean isHeadless = ApplicationConfig.isHeadless();
            logger = LoggerFactory.getLogger(isHeadless);
        }
        return logger;
    }
}