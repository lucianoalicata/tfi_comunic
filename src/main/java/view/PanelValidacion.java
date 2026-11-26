package view;

import core.HammingCodec.Estado;
import core.ModoEnlace;
import core.ProtocoloVlc;
import core.TramaVlc;
import service.RecepcionVlc.ResultadoValidacion;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PanelValidacion extends JPanel {

    private static final String SEP =
            "---------------------------------------------------------------------------\n";

    private final JTextPane area = TemaVlc.textPaneMono();
    private final JLabel badgeEstado;

    public PanelValidacion(JLabel badgeEstado) {
        this.badgeEstado = badgeEstado;
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(TemaVlc.bordeTitulado(
                "VERIFICACIÓN DE CAPA 2 (CRC-8) Y MENSAJE FINAL",
                TemaVlc.COLOR_OK));
        add(TemaVlc.scroll(area), BorderLayout.CENTER);
    }

    public void limpiar() { area.setText(""); }

    public void mostrar(ResultadoValidacion r) {
        TramaVlc corregida = r.tramaCorregida();
        TramaVlc esperada = r.tramaEsperada();
        boolean hamming = r.modo() == ModoEnlace.CRC_HAMMING;

        byte crcRec = corregida.getCrc();
        byte crcCalc = r.crcCalculado();
        boolean crcOk = crcRec == crcCalc;
        boolean aceptada = r.estructuraValida() && crcOk;

        long corregidas = r.palabras().stream()
                .filter(p -> p.estado() == Estado.CORREGIDO).count();
        long incorregibles = r.palabras().stream()
                .filter(p -> p.estado() == Estado.INCORREGIBLE).count();
        boolean igualAlEnviado = Arrays.equals(corregida.getPayload(), esperada.getPayload());
        String mensaje = new String(corregida.getPayload(), StandardCharsets.UTF_8);

        area.setText("");

        if (hamming) {
            append("PASO 1 — FEC HAMMING\n", TemaVlc.COLOR_WARN, true);
            append(String.format("  • Palabras corregidas: %d  |  incorregibles: %d  |  total: %d\n\n",
                    corregidas, incorregibles, r.palabras().size()), TemaVlc.TEXT_WHITE, false);
            append("PASO 2 — CRC-8 SOBRE LA TRAMA CORREGIDA\n", TemaVlc.CYAN_ACCENT, true);
        } else {
            append("CRC-8 SOBRE LA TRAMA RECIBIDA\n", TemaVlc.CYAN_ACCENT, true);
        }
        append("  • CRC-8 recibido:    " + ProtocoloVlc.aBinario8(crcRec) + "\n",
                TemaVlc.TEXT_WHITE, false);
        append("  • CRC-8 recalculado: " + ProtocoloVlc.aBinario8(crcCalc)
                + " (polinomio 0x07)\n", TemaVlc.TEXT_WHITE, false);
        if (!r.estructuraValida()) {
            append("  • Estructura: START o LONGITUD incorrectos.\n", TemaVlc.COLOR_ERR, false);
        }
        append(crcOk ? "  -> COINCIDEN\n\n" : "  -> NO COINCIDEN\n\n",
                crcOk ? TemaVlc.COLOR_OK : TemaVlc.COLOR_ERR, true);

        if (aceptada && !igualAlEnviado) {
            TemaVlc.actualizarBadge(badgeEstado,
                    "ESTADO: ERROR NO DETECTADO", TemaVlc.COLOR_ERR);
            append("ADVERTENCIA: el CRC-8 coincide pero el mensaje difiere de lo transmitido.\n",
                    TemaVlc.COLOR_ERR, true);
            append("  • Mensaje entregado: \"" + mensaje + "\"\n", TemaVlc.TEXT_WHITE, false);
            append("  • Mensaje enviado:   \"" + new String(esperada.getPayload(),
                    StandardCharsets.UTF_8) + "\"\n", TemaVlc.TEXT_WHITE, false);
        } else if (aceptada && corregidas > 0) {
            TemaVlc.actualizarBadge(badgeEstado,
                    "ESTADO: CORREGIDA POR HAMMING (CRC OK)", TemaVlc.COLOR_WARN);
            append("TRAMA RECUPERADA: Hamming corrigió el error y el CRC-8 lo confirma\n",
                    TemaVlc.COLOR_WARN, true);
            append("  -> MENSAJE CORREGIDO: \"" + mensaje + "\"\n", TemaVlc.COLOR_OK, true);
        } else if (aceptada) {
            TemaVlc.actualizarBadge(badgeEstado,
                    "ESTADO: ENLACE ÍNTEGRO (OK)", TemaVlc.COLOR_OK);
            append("TRAMA VÁLIDA RECIBIDA CON ÉXITO\n", TemaVlc.COLOR_OK, true);
            append("  -> MENSAJE ÍNTEGRO: \"" + mensaje + "\"\n", TemaVlc.COLOR_OK, true);
        } else {
            TemaVlc.actualizarBadge(badgeEstado,
                    "ESTADO: TRAMA DESCARTADA (CRC ERROR)", TemaVlc.COLOR_ERR);
            append("FALLO DE INTEGRIDAD EN CAPA DE ENLACE: trama descartada, no se entrega el mensaje.\n",
                    TemaVlc.COLOR_ERR, true);
            if (hamming) {
                append("  • Quedaron errores que Hamming no pudo corregir (corrige 1 bit por palabra;\n"
                        + "    2 o más en la misma palabra, típico de una ráfaga, lo superan) y el CRC-8 lo detectó.\n",
                        TemaVlc.TEXT_MUTED, false);
            } else {
                append("  • El CRC-8 detecta pero no corrige (no sabe qué bit falló).\n"
                        + "    Tildá \"FEC Hamming (12,8)\" para corregir hacia adelante.\n",
                        TemaVlc.TEXT_MUTED, false);
            }
            append("  • Acción del protocolo: solicitar retransmisión (ARQ).\n",
                    TemaVlc.TEXT_MUTED, false);
        }
        append(SEP, TemaVlc.BORDER_CARD, false);
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
