package view;

import core.ProtocoloVlc;
import core.TramaVlc;
import service.RecepcionVlc.ResultadoValidacion;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public class PanelValidacion extends JPanel {

    private final JTextPane area = TemaVlc.textPaneMono();
    private final JLabel badgeEstado;

    public PanelValidacion(JLabel badgeEstado) {
        this.badgeEstado = badgeEstado;
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(TemaVlc.bordeTitulado(
                "VERIFICACIÓN DE CAPA 2 (MÁQUINA DE ESTADO CRC-8)",
                TemaVlc.COLOR_OK));
        add(TemaVlc.scroll(area), BorderLayout.CENTER);
    }

    public void limpiar() { area.setText(""); }

    public void mostrar(ResultadoValidacion r) {
        TramaVlc rec = r.tramaRecibida();
        TramaVlc esp = r.tramaEsperada();
        byte crcRec = rec.getCrc();
        byte crcCalc = r.crcCalculado();
        boolean ok = r.estructuraValida() && !r.huboErrorFisico() && crcRec == crcCalc;

        area.setText("");

        if (!ok) {
            TemaVlc.actualizarBadge(badgeEstado,
                    "ESTADO: TRAMA DESCARTADA (CRC ERROR)", TemaVlc.COLOR_ERR);
            append("FALLO DE INTEGRIDAD EN CAPA DE ENLACE (CRC-8)\n",
                    TemaVlc.COLOR_ERR, true);
            append("  • CRC Recibido en Trama:  " + ProtocoloVlc.aBinario8(crcRec) + "\n",
                    TemaVlc.TEXT_WHITE, false);
            append("  • CRC Recalculado Local:  " + ProtocoloVlc.aBinario8(crcCalc)
                    + " (Polinomio generador: 00000111)\n", TemaVlc.TEXT_WHITE, false);
            append("  • Estado del Enlace:      Discrepancia detectada. Bloque descartado "
                    + "por perturbación en Capa 1.\n\n", TemaVlc.COLOR_ERR, false);

            append("RECONSTRUCCIÓN DEMOSTRATIVA DE CARGA ÚTIL:\n",
                    TemaVlc.CYAN_ACCENT, true);
            StringBuilder log = new StringBuilder();
            byte[] sent = esp.getPayload(), recBytes = rec.getPayload();
            for (int i = 0; i < sent.length; i++) {
                if (sent[i] != recBytes[i]) {
                    log.append(String.format(
                        "  - Carácter %d: Recibido %s -> Restaurado a %s ('%c')\n",
                        i + 1, ProtocoloVlc.aBinario8(recBytes[i]),
                        ProtocoloVlc.aBinario8(sent[i]), (char) (sent[i] & 0xFF)));
                }
            }
            append(log.toString(), TemaVlc.TEXT_MUTED, false);
            String texto = new String(sent, java.nio.charset.StandardCharsets.UTF_8);
            append("-> CARGA ÚTIL RESTAURADA: \"" + texto + "\"\n",
                    TemaVlc.COLOR_OK, true);
            append("---------------------------------------------------------------------------\n",
                    TemaVlc.BORDER_CARD, false);
        } else {
            TemaVlc.actualizarBadge(badgeEstado,
                    "ESTADO: ENLACE ÍNTEGRO (OK)", TemaVlc.COLOR_OK);
            String texto = new String(rec.getPayload(),
                    java.nio.charset.StandardCharsets.UTF_8);
            append("TRAMA VÁLIDA RECIBIDA CON ÉXITO\n", TemaVlc.COLOR_OK, true);
            append("  • Mensaje Íntegro: \"" + texto + "\"\n", TemaVlc.TEXT_WHITE, false);
            append("  • Verificación CRC-8: " + ProtocoloVlc.aBinario8(crcRec)
                    + " == " + ProtocoloVlc.aBinario8(crcCalc)
                    + " [COINCIDENCIA EXACTA - SIN ERRORES]\n",
                    TemaVlc.COLOR_OK, false);
            append("---------------------------------------------------------------------------\n",
                    TemaVlc.BORDER_CARD, false);
        }
    }

    private void append(String texto, Color color, boolean bold) {
        StyledDocument doc = area.getStyledDocument();
        Style style = area.addStyle("s", null);
        StyleConstants.setForeground(style, color);
        StyleConstants.setBold(style, bold);
        try {
            doc.insertString(doc.getLength(), texto, style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    public void mostrarErrorConexion(String mensaje) {
        area.setText("");
        append(mensaje + "\n", TemaVlc.COLOR_ERR, false);
    }
}