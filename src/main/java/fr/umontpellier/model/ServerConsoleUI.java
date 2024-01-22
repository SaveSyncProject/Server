package fr.umontpellier.model;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;

public class ServerConsoleUI {
    private JTextArea textArea;

    public ServerConsoleUI() {
        initializeUI();
    }

    // Initialisation de l'interface graphique
    public void initializeUI() {
        JFrame frame = new JFrame("Server Console");
        textArea = new JTextArea(30, 50);
        setupTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        setupFrame(frame);
        redirectSystemStreams();
    }

    // Configuration du JTextArea
    private void setupTextArea() {
        textArea.setEditable(false);
        textArea.setBackground(new Color(30, 30, 30)); // Gris foncé
        textArea.setForeground(Color.WHITE);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        textArea.setText(setupAsciiBanner());
    }

    // Configuration de la banner ASCII
    private String setupAsciiBanner() {
        return " ____                       ____                      \n" +
                "/ ___|   __ _ __   __  ___ / ___|  _   _  _ __    ___ \n" +
                "\\___ \\  / _` |\\ \\ / / / _ \\\\___ \\ | | | || '_ \\  / __|\n" +
                " ___) || (_| | \\ V / |  __/ ___) || |_| || | | || (__ \n" +
                "|____/  \\__,_|  \\_/   \\___||____/  \\__, ||_| |_| \\___|\n" +
                "                                   |___/              \n\n";
    }

    /** Configuration de la fenêtre
     *
     * @param frame
     */
    private void setupFrame(JFrame frame) {
        frame.setTitle("SaveSync Server");
        frame.setIconImage(new ImageIcon(ServerConsoleUI.class.getResource("/img/icon.png")).getImage());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(578, 455);
        frame.setResizable(false);
        frame.setVisible(true);
    }

    // Redirection des sorties système vers le JTextArea
    private void redirectSystemStreams() {
        PrintStream printStream = new PrintStream(new CustomOutputStream(textArea));
        System.setOut(printStream);
        System.setErr(printStream);
    }

    // Classe interne pour rediriger les sorties système vers le JTextArea
    private static class CustomOutputStream extends OutputStream {
        private JTextArea textArea;

        public CustomOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) {
            textArea.append(String.valueOf((char) b));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }
}