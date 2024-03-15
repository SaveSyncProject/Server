package fr.umontpellier.logging;

public abstract class Logger {

    public void log(String message) {
        writeLog(message);
    }

    protected abstract void writeLog(String message);
}