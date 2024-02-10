package fr.umontpellier.model.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class Logger {
    protected DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public void log(String message) {
        String timestampedMessage = "[" + LocalDateTime.now().format(formatter) + "] " + message;
        writeLog(timestampedMessage);
    }

    protected abstract void writeLog(String message);
}