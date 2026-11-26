package view;

import core.HammingCodec;
import core.HammingCodec.Estado;
import core.HammingCodec.Palabra;
import core.ModoEnlace;
import core.TramaVlc;
import service.RecepcionVlc.ResultadoValidacion;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Muestra el procedimiento de Hamming con el formato de la cátedra (Unidad 6, punto 12):
 * tabla n / p1..p4 / n+p a TX, y en recepción la tabla de controles, el síndrome
 * (cuyo valor decimal es la posición del bit erróneo) y la palabra corregida.
 */
public class PanelHamming extends JPanel {

    private static final int ANCHO_CELDA = 5;
    private static final String SEP = "-------------------------------------------------------------------------------\n";

    private record Celda(String texto, Color color, boolean negrita) { }

    private static final Celda VACIA = new Celda("", TemaVlc.TEXT_MUTED, false);

    private final JTextPane area = TemaVlc.textPaneMono();
    private int inicioDecodificacion;

    public PanelHamming() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(TemaVlc.bordeTitulado(
                "CORRECCIÓN FEC: HAMMING (12,8) — PARIDAD PAR", TemaVlc.COLOR_WARN));
        add(TemaVlc.scroll(area), BorderLayout.CENTER);
    }

    public void limpiar() {
        area.setText("");
    }

    /** Al transmitir: solo se conoce la codificación (lado TX). */
    public void mostrarCodificacion(TramaVlc trama, ModoEnlace modo) {
        area.setText("");
        if (modo != ModoEnlace.CRC_HAMMING) {
            avisoFecDesactivado();
            return;
        }
        escribirCodificacion(trama);
        append("\nEsperando la trama recibida para decodificar...\n", TemaVlc.TEXT_MUTED, false);
    }

    /** Al recibir: codificación + decodificación completa. */
    public void mostrar(ResultadoValidacion r) {
        area.setText("");
        if (r.modo() != ModoEnlace.CRC_HAMMING) {
            avisoFecDesactivado();
            return;
        }
        escribirCodificacion(r.tramaEsperada());
        escribirDecodificacion(r);
        SwingUtilities.invokeLater(this::irADecodificacion);
    }

    /** Deja a la vista la sección 2 (la tabla de TX queda arriba, un scroll más atrás). */
    private void irADecodificacion() {
        try {
            Rectangle2D r = area.modelToView2D(inicioDecodificacion);
            JViewport vp = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, area);
            if (r == null || vp == null) return;
            int maxY = Math.max(0, area.getPreferredSize().height - vp.getHeight());
            vp.setViewPosition(new Point(0, Math.max(0, Math.min((int) r.getY(), maxY))));
        } catch (BadLocationException ignored) { }
    }

    private void avisoFecDesactivado() {
        append("FEC desactivado: el enlace solo usa CRC-8.\n", TemaVlc.COLOR_WARN, true);
        append("El CRC-8 DETECTA errores pero no dice qué bit falló, así que no puede corregirlos:\n"
                        + "la trama se descarta y habría que pedir retransmisión (ARQ).\n\n"
                        + "Tildá \"FEC Hamming (12,8)\" para ver cómo se corrige hacia adelante.\n",
                TemaVlc.TEXT_MUTED, false);
    }

    // ------------------------------------------------------------------ TX

    private void escribirCodificacion(TramaVlc trama) {
        append("1) CODIFICACIÓN EN EL TRANSMISOR\n", TemaVlc.CYAN_ACCENT, true);
        append("n = 8 bits de datos, p = 4 bits de paridad -> 2^p >= n + p + 1  ->  16 >= 13\n"
                        + "Cada byte del cuerpo de la trama (LONGITUD, PAYLOAD, CRC-8) pasa a una palabra de 12 bits.\n"
                        + "Cada pk se fija para que la cantidad de unos de las posiciones que controla sea PAR.\n\n",
                TemaVlc.TEXT_MUTED, false);

        byte[] tx = trama.aBytesTransmision();   // [START][LONGITUD][PAYLOAD...][CRC-8]
        int total = tx.length - 1;               // una palabra por cada byte, menos START
        for (int i = 0; i < total; i++) {
            tablaCodificacion(trama, i, total, tx[i + 1] & 0xFF);
        }
    }

    /** Tabla de codificación (formato de la cátedra) de una palabra del cuerpo de la trama. */
    private void tablaCodificacion(TramaVlc trama, int idx, int total, int dato) {
        append(String.format("Palabra %d de %d: %s (%s)\n",
                        idx + 1, total, nombreCampo(trama, idx, total), bin(dato, 8)),
                TemaVlc.TEXT_WHITE, true);
        int palabra = HammingCodec.codificar(dato);
        encabezado();
        fila("n", pos -> HammingCodec.esPosicionParidad(pos)
                ? VACIA : celdaBit(palabra, pos, TemaVlc.TEXT_WHITE, false), null);
        for (int k = 1; k <= HammingCodec.BITS_PARIDAD; k++) {
            final int kk = k;
            fila("p" + k, pos -> {
                if (!HammingCodec.controla(kk, pos)) return VACIA;
                boolean esPk = pos == (1 << (kk - 1));
                return celdaBit(palabra, pos,
                        esPk ? TemaVlc.COLOR_WARN : TemaVlc.TEXT_WHITE, esPk);
            }, null);
        }
        fila("n+p a TX", pos -> celdaBit(palabra, pos, TemaVlc.CYAN_ACCENT, true), null);
        append("\n", TemaVlc.TEXT_WHITE, false);
    }

    // ------------------------------------------------------------------ RX

    private void escribirDecodificacion(ResultadoValidacion r) {
        List<Palabra> palabras = r.palabras();
        byte[] tx = r.tramaEsperada().aBytesTransmision();
        int total = palabras.size();

        long ok = palabras.stream().filter(p -> p.estado() == Estado.SIN_ERROR).count();
        long corr = palabras.stream().filter(p -> p.estado() == Estado.CORREGIDO).count();
        long inc = palabras.stream().filter(p -> p.estado() == Estado.INCORREGIBLE).count();

        inicioDecodificacion = area.getDocument().getLength();
        append(SEP, TemaVlc.BORDER_CARD, false);
        append("2) DECODIFICACIÓN EN EL RECEPTOR\n", TemaVlc.PURPLE_RX, true);
        append("Con los 4 controles de paridad se arma el SÍNDROME (p4 p3 p2 p1): su valor decimal\n"
                        + "es la posición del bit erróneo; si es 0000 no hay error.\n\n",
                TemaVlc.TEXT_MUTED, false);
        append(String.format("Palabras: %d  |  sin error: %d  |  corregidas: %d  |  incorregibles: %d\n\n",
                total, ok, corr, inc), TemaVlc.TEXT_WHITE, true);

        boolean huboDetalle = false;
        for (int i = 0; i < total; i++) {
            Palabra p = palabras.get(i);
            int palTx = HammingCodec.codificar(tx[i + 1] & 0xFF);
            if (p.estado() != Estado.SIN_ERROR || p.palabraRx() != palTx) {
                detalle(r, i, total, p, palTx);
                huboDetalle = true;
            }
        }
        if (!huboDetalle) {
            append("Síndrome 0000 en todas las palabras: SIN ERROR en el canal.\n\n",
                    TemaVlc.COLOR_OK, true);
        }

        append(SEP, TemaVlc.BORDER_CARD, false);
        append("RESUMEN POR PALABRA\n", TemaVlc.TEXT_WHITE, true);
        append(String.format(" %-3s %-13s %-14s %-14s %-8s %s\n",
                        "#", "Campo", "Palabra Tx", "Palabra Rx", "Síndr.", "Resultado"),
                TemaVlc.TEXT_MUTED, false);
        for (int i = 0; i < total; i++) {
            Palabra p = palabras.get(i);
            int palTx = HammingCodec.codificar(tx[i + 1] & 0xFF);
            append(String.format(" %-3d %-13s ", i + 1, nombreCampo(r, i, total)),
                    TemaVlc.TEXT_WHITE, false);
            append(bin(palTx, 12) + "   ", TemaVlc.TEXT_WHITE, false);
            palabraColoreada(p.palabraRx(), palTx);
            append("   " + bin(p.sindrome(), 4) + "    ", TemaVlc.TEXT_WHITE, false);
            resultado(p);
        }
        append("\n", TemaVlc.TEXT_WHITE, false);
    }

    private void detalle(ResultadoValidacion r, int idx, int total, Palabra p, int palTx) {
        append(SEP, TemaVlc.BORDER_CARD, false);
        append(String.format("PALABRA %d (%s) — CONTROLES DE PARIDAD EN RX\n",
                idx + 1, nombreCampo(r, idx, total)), TemaVlc.COLOR_WARN, true);

        int rx = p.palabraRx();
        encabezado();
        fila("n+p a RX", pos -> {
            boolean alterado = HammingCodec.bit(rx, pos) != HammingCodec.bit(palTx, pos);
            return celdaBit(rx, pos, alterado ? TemaVlc.COLOR_ERR : TemaVlc.TEXT_WHITE, alterado);
        }, null);
        for (int k = 1; k <= HammingCodec.BITS_PARIDAD; k++) {
            final int kk = k;
            int res = HammingCodec.paridadGrupo(rx, k);
            fila("p" + k, pos -> HammingCodec.controla(kk, pos)
                            ? celdaBit(rx, pos, TemaVlc.TEXT_WHITE, false) : VACIA,
                    new Celda("-> " + res, res == 0 ? TemaVlc.COLOR_OK : TemaVlc.COLOR_ERR, true));
        }

        String s = bin(p.sindrome(), 4);
        append(String.format("\nSíndrome (p4 p3 p2 p1) = %s  = %d\n", s, p.sindrome()),
                TemaVlc.TEXT_WHITE, true);

        switch (p.estado()) {
            case CORREGIDO -> {
                append(String.format("-> Error en la posición %d (%s): se invierte ese bit.\n",
                                p.posicionError(), HammingCodec.etiqueta(p.posicionError())),
                        TemaVlc.COLOR_WARN, true);
                int pos = p.posicionError();
                int corregida = p.palabraCorregida();
                fila("n+p corr.", c -> celdaBit(corregida, c,
                        c == pos ? TemaVlc.COLOR_OK : TemaVlc.TEXT_WHITE, c == pos), null);
                append(String.format("Dato corregido: %s = '%s' (0x%02X)\n",
                                bin(p.datoCorregido(), 8), caracter(p.datoCorregido()), p.datoCorregido()),
                        TemaVlc.COLOR_OK, true);
                if (p.palabraCorregida() != palTx) {
                    append("   Laboratorio: la corrección NO coincide con lo transmitido "
                                    + "(había 2 o más errores en esta palabra).\n"
                                    + "   Hamming no puede distinguirlo; lo detecta el CRC-8.\n",
                            TemaVlc.COLOR_ERR, false);
                }
            }
            case INCORREGIBLE -> append(
                    "-> El síndrome no apunta a ninguna de las 12 posiciones: error múltiple,\n"
                            + "   NO corregible. La trama queda a cargo del CRC-8 (descarte / ARQ).\n",
                    TemaVlc.COLOR_ERR, true);
            case SIN_ERROR -> append(
                    "-> Síndrome 0000 pero la palabra difiere de la transmitida (3 o más errores):\n"
                            + "   Hamming no lo nota. Lo detecta el CRC-8.\n",
                    TemaVlc.COLOR_ERR, true);
        }
        append("\n", TemaVlc.TEXT_WHITE, false);
    }

    // ------------------------------------------------------- formato tabla

    private void encabezado() {
        fila("n+p", pos -> new Celda(HammingCodec.etiqueta(pos),
                HammingCodec.esPosicionParidad(pos) ? TemaVlc.COLOR_WARN : TemaVlc.CYAN_ACCENT, true), null);
        fila("posición", pos -> new Celda(String.valueOf(pos), TemaVlc.TEXT_MUTED, false), null);
        fila("pos. bin.", pos -> new Celda(bin(pos, 4), TemaVlc.TEXT_MUTED, false), null);
    }

    /** Una fila de la tabla: columnas de la posición 12 a la 1, como en la diapositiva. */
    private void fila(String etiqueta, IntFunction<Celda> celda, Celda cola) {
        append(String.format("%-10s|", etiqueta), TemaVlc.TEXT_MUTED, false);
        for (int pos = HammingCodec.BITS_PALABRA; pos >= 1; pos--) {
            Celda c = celda.apply(pos);
            append(String.format("%" + ANCHO_CELDA + "s", c.texto()), c.color(), c.negrita());
        }
        if (cola != null) {
            append("   " + cola.texto(), cola.color(), cola.negrita());
        }
        append("\n", TemaVlc.TEXT_WHITE, false);
    }

    private Celda celdaBit(int palabra, int pos, Color color, boolean negrita) {
        return new Celda(String.valueOf(HammingCodec.bit(palabra, pos)), color, negrita);
    }

    private void palabraColoreada(int rx, int tx) {
        for (int pos = HammingCodec.BITS_PALABRA; pos >= 1; pos--) {
            boolean alterado = HammingCodec.bit(rx, pos) != HammingCodec.bit(tx, pos);
            append(String.valueOf(HammingCodec.bit(rx, pos)),
                    alterado ? TemaVlc.COLOR_ERR : TemaVlc.TEXT_WHITE, alterado);
        }
    }

    private void resultado(Palabra p) {
        switch (p.estado()) {
            case SIN_ERROR -> append("OK\n", TemaVlc.COLOR_OK, false);
            case CORREGIDO -> append(String.format("CORREGIDO (pos %d, %s)\n",
                            p.posicionError(), HammingCodec.etiqueta(p.posicionError())),
                    TemaVlc.COLOR_WARN, true);
            case INCORREGIBLE -> append("INCORREGIBLE\n", TemaVlc.COLOR_ERR, true);
        }
    }

    private String nombreCampo(ResultadoValidacion r, int idx, int total) {
        return nombreCampo(r.tramaEsperada(), idx, total);
    }

    private String nombreCampo(TramaVlc trama, int idx, int total) {
        if (idx == 0) return "LONGITUD";
        if (idx == total - 1) return "CRC-8";
        byte[] payload = trama.getPayload();
        return String.format("DATO %d '%s'", idx, caracter(payload[idx - 1] & 0xFF));
    }

    private static String caracter(int v) {
        return (v >= 32 && v <= 126) ? String.valueOf((char) v) : "?";
    }

    private static String bin(int valor, int ancho) {
        return String.format("%" + ancho + "s", Integer.toBinaryString(valor)).replace(' ', '0');
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
}