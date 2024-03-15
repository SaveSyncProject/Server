package fr.umontpellier.logging;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class GUILogger extends Logger {
    private JTextArea textArea;

    public GUILogger() {
        initializeUI();
    }

    // Initialisation et configuration de l'interface utilisateur
    private void initializeUI() {
        JFrame frame = new JFrame("Server Console");
        textArea = new JTextArea(30, 50);
        setupTextArea();
        JScrollPane scrollPane = new JScrollPane(textArea);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        setupFrame(frame);
    }

    // Configuration du JTextArea
    private void setupTextArea() {
        textArea.setEditable(false);
        textArea.setBackground(new Color(30, 30, 30)); // Gris foncé
        textArea.setForeground(Color.WHITE);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));
    }

    // Configuration de la fenêtre
    private void setupFrame(JFrame frame) {
        frame.setTitle("Server Console");
        frame.setIconImage(new ImageIcon(Objects.requireNonNull(this.getClass().getResource("/image/icon.png"))).getImage());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(578, 455);
        frame.setResizable(false);
        frame.setVisible(true);
    }

    @Override
    protected void writeLog(String message) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(message + "\n");
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }
}