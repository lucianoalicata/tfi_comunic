package com.mycompany.tfi_comunic;

import view.VlcApp;

import javax.swing.*;

public class Application {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        SwingUtilities.invokeLater(() -> {
            VlcApp app = new VlcApp();
            app.setLocationRelativeTo(null);
            app.setVisible(true);
        });
    }
}