package com.example.dbstarter;

import java.awt.GraphicsEnvironment;
import java.sql.Connection;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("A graphical display is required to open the Game Library.");
            System.err.println("Run this app in a desktop/VNC session, not a headless shell.");
            return;
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            Connection connection = Database.connect();
            SchemaInitializer.apply(connection);
            SwingUtilities.invokeLater(() -> {
                GameLibraryFrame frame = new GameLibraryFrame(connection);
                frame.setVisible(true);
            });
        } catch (Exception exception) {
            exception.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Could not start the game library:\n" + exception.getMessage(),
                    "Startup error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
