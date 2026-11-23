package view;

import core.ProtocoloVlc;
import core.TramaVlc;

import javax.swing.*;
import java.awt.*;

public class PanelTx extends JPanel {

    private final JTextArea areaResumen = TemaVlc.textAreaMono();
    private final JTextArea areaBinario = TemaVlc.textAreaMono();

    public PanelTx() {
        setLayout(new GridLayout(2, 1, 0, 8));
        setOpaque(false);
        setBorder(TemaVlc.bordeTitulado(
                "CANAL TX: TRANSMISIÓN (ORIGEN)", TemaVlc.CYAN_ACCENT));
        add(TemaVlc.scroll(areaResumen));
        add(TemaVlc.scroll(areaBinario));
    }

    public void limpiar() {
        areaResumen.setText("");
        areaBinario.setText("");
    }

    public void mostrarTrama(TramaVlc trama) {
        byte[] payload = trama.getPayload();
        int longitud = payload.length;
        String texto = new String(payload, java.nio.charset.StandardCharsets.UTF_8);

        StringBuilder r = new StringBuilder();
        r.append("CADENA ORIGINAL: \"").append(texto).append("\"\n");
        r.append("LONGITUD ÚTIL:   ").append(longitud).append(" caracteres (")
         .append(ProtocoloVlc.aBinario8(longitud)).append(")\n\n");
        r.append("ESTRUCTURA DE TRAMA FORMATEADA:\n");
        r.append("• START (Sincronismo): 01111110\n");
        r.append("• Campo Longitud (N):  ").append(ProtocoloVlc.aBinario8(longitud)).append("\n");
        r.append("• Tamaño Carga Útil:   ").append(longitud * 8).append(" bits de datos\n");
        areaResumen.setText(r.toString());

        StringBuilder b = new StringBuilder();
        b.append("DESGLOSE BINARIO NOMINAL (ASCII 8 BITS):\n");
        b.append("--------------------------------------------------\n");
        for (int i = 0; i < longitud; i++) {
            char c = (char) (payload[i] & 0xFF);
            b.append(String.format(" Carácter '%c' (Pos %d) -> %s\n",
                    c, (i + 1), ProtocoloVlc.aBinario8(payload[i])));
        }
        areaBinario.setText(b.toString());
    }
}