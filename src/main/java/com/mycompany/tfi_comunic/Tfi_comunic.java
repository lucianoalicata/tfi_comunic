package com.mycompany.tfi_comunic;

import javax.swing.SwingUtilities;

public class Tfi_comunic {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VlcApp ventanaPrincipal = new VlcApp();
            ventanaPrincipal.setLocationRelativeTo(null); 
            ventanaPrincipal.setVisible(true);
        });
    }
}