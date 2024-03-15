package fr.umontpellier.logging;

public class SystemOutLogger extends Logger {
    @Override
    protected void writeLog(String message) {
        System.out.println(message);
    }
}