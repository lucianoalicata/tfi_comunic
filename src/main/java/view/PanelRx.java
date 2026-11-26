package view;

import core.ProtocoloVlc;
import core.TramaVlc;
import service.RecepcionVlc.ResultadoValidacion;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;

public class PanelRx extends JPanel {

    private final JTextPane areaMatriz = TemaVlc.textPaneMono();
    private final JTextPane areaBinario = TemaVlc.textPaneMono();

    public PanelRx() {
        setLayout(new GridLayout(2, 1, 0, 8));
        setOpaque(false);
        setBorder(TemaVlc.bordeTitulado(
                "CANAL RX: RECEPCIÓN & MATRIZ DIFERENCIAL (DESTINO)",
                TemaVlc.PURPLE_RX));
        add(TemaVlc.scroll(areaMatriz));
        add(TemaVlc.scroll(areaBinario));
    }

    public void limpiar() {
        areaMatriz.setText("");
        areaBinario.setText("");
    }

    public void mostrar(ResultadoValidacion r) {
        pintarBinarioRecibido(r);
        pintarMatrizDiferencial(r);
    }

    private void pintarBinarioRecibido(ResultadoValidacion r) {
        TramaVlc esperada = r.tramaEsperada();
        TramaVlc recibida = r.tramaRecibida();
        byte[] sent = esperada.getPayload();
        byte[] rec  = recibida.getPayload();

        areaBinario.setText("");
        append(areaBinario, "DESGLOSE BINARIO DE RECEPCIÓN (8 BITS):\n", TemaVlc.TEXT_WHITE, true);
        append(areaBinario, "--------------------------------------------------\n",
                TemaVlc.BORDER_CARD, false);

        for (int i = 0; i < Math.min(rec.length, sent.length); i++) {
            int s = sent[i] & 0xFF, rr = rec[i] & 0xFF;
            char c = (rr >= 32 && rr <= 126) ? (char) rr : '?';
            append(areaBinario, String.format(" Símbolo '%c' (Pos %d) -> ", c, i + 1),
                    TemaVlc.TEXT_MUTED, false);
            for (int b = 7; b >= 0; b--) {
                int bitS = (s >> b) & 1, bitR = (rr >> b) & 1;
                append(areaBinario, String.valueOf(bitR),
                        bitS != bitR ? TemaVlc.COLOR_ERR : TemaVlc.TEXT_WHITE,
                        bitS != bitR);
            }
            append(areaBinario, "\n", TemaVlc.TEXT_WHITE, false);
        }
    }

    private void pintarMatrizDiferencial(ResultadoValidacion r) {
        byte[] sent = r.tramaEsperada().getPayload();
        byte[] rec  = r.tramaRecibida().getPayload();

        areaMatriz.setText("");
        String crudo = new String(rec, StandardCharsets.ISO_8859_1);
        append(areaMatriz, "CADENA CAPTURADA DEL MEDIO: \"" + crudo + "\"\n\n",
                TemaVlc.TEXT_WHITE, true);
        append(areaMatriz, "MATRIZ DIFERENCIAL DE BITS POR CARÁCTER (XOR):\n",
                TemaVlc.CYAN_ACCENT, true);
        append(areaMatriz, "=========================================================================\n",
                TemaVlc.BORDER_CARD, false);

        if (rec.length != sent.length) {
            append(areaMatriz, String.format(
                    "LONGITUD alterada en el canal: enviada %d, recibida %d.\n\n",
                    sent.length, rec.length), TemaVlc.COLOR_ERR, true);
        }
        for (int i = 0; i < Math.min(rec.length, sent.length); i++) {
            int s = sent[i] & 0xFF, rr = rec[i] & 0xFF;
            if (s == rr) continue;

            char cs = (char) s;
            char cr = (rr >= 32 && rr <= 126) ? (char) rr : '?';
            append(areaMatriz, String.format(
                    "Posición %d | Nominal: '%c' (%s) | Recibido: '%c' (%s)\n",
                    i + 1, cs, ProtocoloVlc.aBinario8(s), cr, ProtocoloVlc.aBinario8(rr)),
                    TemaVlc.COLOR_WARN, true);
            append(areaMatriz, "  -----------------------------------------------------------------------\n",
                    TemaVlc.BORDER_CARD, false);
            append(areaMatriz, "  Bit Nº   | Tx (Nominal) | Rx (Medio) | XOR (Error) | Diagnóstico\n",
                    TemaVlc.TEXT_MUTED, false);
            append(areaMatriz, "  -----------------------------------------------------------------------\n",
                    TemaVlc.BORDER_CARD, false);

            int xor = s ^ rr;
            for (int b = 7; b >= 0; b--) {
                int bitTx = (s >> b) & 1, bitRx = (rr >> b) & 1, bitXor = (xor >> b) & 1;
                append(areaMatriz, String.format("  Bit %d    |      %d       |     ",
                        8 - b, bitTx), TemaVlc.TEXT_WHITE, false);
                if (bitXor == 1) {
                    append(areaMatriz, String.valueOf(bitRx), TemaVlc.COLOR_ERR, true);
                    append(areaMatriz, String.format("      |      %d      | ", bitXor),
                            TemaVlc.TEXT_WHITE, false);
                    append(areaMatriz, String.format("FALLO [%d -> %d]\n", bitTx, bitRx),
                            TemaVlc.COLOR_ERR, true);
                } else {
                    append(areaMatriz, String.valueOf(bitRx), TemaVlc.TEXT_WHITE, false);
                    append(areaMatriz, String.format("      |      %d      | ", bitXor),
                            TemaVlc.TEXT_WHITE, false);
                    append(areaMatriz, "OK (Íntegro)\n", TemaVlc.COLOR_OK, false);
                }
            }
            append(areaMatriz, "  -----------------------------------------------------------------------\n\n",
                    TemaVlc.BORDER_CARD, false);
        }

        if (!r.huboErrorFisico()) {
            append(areaMatriz,
                    "Canal Limpio: Todos los pulsos fotónicos muestreados con BER = 0.\n",
                    TemaVlc.COLOR_OK, false);
        } else if (java.util.Arrays.equals(sent, rec)) {
            append(areaMatriz,
                    "Los bits de datos llegaron intactos; el canal alteró solo bits de paridad\n"
                            + "Hamming (ver panel de corrección FEC).\n",
                    TemaVlc.COLOR_WARN, false);
        }
    }

    private void append(JTextPane pane, String texto, Color color, boolean bold) {
        StyledDocument doc = pane.getStyledDocument();
        Style style = pane.addStyle("s", null);
        StyleConstants.setForeground(style, color);
        StyleConstants.setBold(style, bold);
        try {
            doc.insertString(doc.getLength(), texto, style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}