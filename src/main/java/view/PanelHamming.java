package view;

import core.HammingCodec;
import core.HammingCodec.Estado;
import core.HammingCodec.Palabra;
import core.ModoEnlace;
import core.TramaVlc;
import service.RecepcionVlc.ResultadoValidacion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Procedimiento de Hamming (12,8) con el formato de la cátedra (Unidad 6, punto 12).
 * Una instancia se usa del lado emisor (codificación) y otra del lado receptor (decodificación).
 * Arriba hay un selector con una ficha por palabra; abajo se ve solo la tabla de la elegida.
 */
public class PanelHamming extends JPanel {

    private static final int ANCHO_CELDA = 5;
    private static final String SEP = "-------------------------------------------------------------------------------\n";

    private record Celda(String texto, Color color, boolean negrita) { }
    private record Chip(JLabel etiqueta, Color color, Runnable vista) { }

    private static final Celda VACIA = new Celda("", TemaVlc.TEXT_MUTED, false);

    private final JTextPane area = TemaVlc.textPaneMono();
    private final JPanel barra = new JPanel(new GridLayout(0, 9, 4, 4));
    private final List<Chip> chips = new ArrayList<>();

    public PanelHamming(boolean emisor) {
        setLayout(new BorderLayout(0, 6));
        setOpaque(false);
        setBorder(TemaVlc.bordeTitulado(
                emisor ? "CODIFICACIÓN FEC: HAMMING (12,8) — TRANSMISOR"
                        : "CORRECCIÓN FEC: HAMMING (12,8) — RECEPTOR",
                emisor ? TemaVlc.CYAN_ACCENT : TemaVlc.COLOR_WARN));

        barra.setOpaque(false);
        add(barra, BorderLayout.NORTH);
        add(TemaVlc.scroll(area), BorderLayout.CENTER);

        if (!emisor) {
            JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
            leyenda.setOpaque(false);
            leyenda.add(textoLeyenda("● Sin error", TemaVlc.COLOR_OK));
            leyenda.add(textoLeyenda("● Corregida", TemaVlc.COLOR_WARN));
            leyenda.add(textoLeyenda("● Incorregible", TemaVlc.COLOR_ERR));
            add(leyenda, BorderLayout.SOUTH);
        }
    }

    private JLabel textoLeyenda(String texto, Color color) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(color);
        return l;
    }

    // ------------------------------------------------------------ selector

    public void limpiar() {
        area.setText("");
        barra.removeAll();
        chips.clear();
        barra.revalidate();
        barra.repaint();
    }

    public void mostrarEspera() {
        limpiar();
        append("Esperando la trama recibida para decodificar...\n", TemaVlc.TEXT_MUTED, false);
    }

    private void agregarChip(String texto, Color color, Runnable vista) {
        final int indice = chips.size();
        JLabel l = new JLabel(texto, SwingConstants.CENTER);
        l.setOpaque(true);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(color);
        l.setBackground(TemaVlc.BG_INPUT);
        l.setBorder(bordeChip(color, false));
        l.setCursor(new Cursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                activar(indice);
            }
        });
        chips.add(new Chip(l, color, vista));
        barra.add(l);
    }

    private javax.swing.border.Border bordeChip(Color color, boolean seleccionado) {
        int pad = seleccionado ? 2 : 4;
        return BorderFactory.createCompoundBorder(
                new LineBorder(color, seleccionado ? 3 : 1, true),
                new EmptyBorder(pad, pad, pad, pad));
    }

    private void activar(int indice) {
        for (int i = 0; i < chips.size(); i++) {
            Chip c = chips.get(i);
            boolean sel = i == indice;
            c.etiqueta().setBorder(bordeChip(c.color(), sel));
            c.etiqueta().setBackground(sel ? TemaVlc.BG_CARD : TemaVlc.BG_INPUT);
        }
        area.setText("");
        chips.get(indice).vista().run();
        area.setCaretPosition(0);
        barra.revalidate();
        barra.repaint();
    }

    // ------------------------------------------------------------------ TX

    /** Al transmitir: solo se conoce la codificación (lado emisor). */
    public void mostrarCodificacion(TramaVlc trama, ModoEnlace modo) {
        limpiar();
        if (modo != ModoEnlace.CRC_HAMMING) {
            avisoFecDesactivado();
            return;
        }
        byte[] tx = trama.aBytesTransmision();   // [START][LONGITUD][PAYLOAD...][CRC-8]
        int total = tx.length - 1;               // una palabra por cada byte, menos START

        agregarChip("RESUMEN", TemaVlc.TEXT_WHITE, () -> resumenCodificacion(trama, total, tx));
        for (int i = 0; i < total; i++) {
            final int idx = i;
            final int dato = tx[i + 1] & 0xFF;
            agregarChip(etiquetaChip(trama, i, total), TemaVlc.CYAN_ACCENT,
                    () -> tablaCodificacion(trama, idx, total, dato));
        }
        activar(0);
    }

    private void resumenCodificacion(TramaVlc trama, int total, byte[] tx) {
        append("CODIFICACIÓN EN EL TRANSMISOR\n", TemaVlc.CYAN_ACCENT, true);
        append("n = 8 bits de datos, p = 4 bits de paridad -> 2^p >= n + p + 1  ->  16 >= 13\n"
                        + "Cada byte del cuerpo de la trama (LONGITUD, PAYLOAD, CRC-8) pasa a una palabra de 12 bits.\n"
                        + "Cada pk se fija para que la cantidad de unos de las posiciones que controla sea PAR.\n"
                        + "Elegí una palabra arriba para ver su tabla.\n\n",
                TemaVlc.TEXT_MUTED, false);
        append(String.format(" %-3s %-13s %-10s %s\n", "#", "Campo", "Dato", "Palabra Tx (12 bits)"),
                TemaVlc.TEXT_MUTED, false);
        for (int i = 0; i < total; i++) {
            int dato = tx[i + 1] & 0xFF;
            append(String.format(" %-3d %-13s %-10s %s\n", i + 1, nombreCampo(trama, i, total),
                            bin(dato, 8), bin(HammingCodec.codificar(dato), 12)),
                    TemaVlc.TEXT_WHITE, false);
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
    }

    // ------------------------------------------------------------------ RX

    /** Al recibir: una ficha por palabra, coloreada según su estado. */
    public void mostrar(ResultadoValidacion r) {
        limpiar();
        if (r.modo() != ModoEnlace.CRC_HAMMING) {
            avisoFecDesactivado();
            return;
        }
        List<Palabra> palabras = r.palabras();
        byte[] tx = r.tramaEsperada().aBytesTransmision();
        int total = palabras.size();

        agregarChip("RESUMEN", TemaVlc.TEXT_WHITE, () -> resumenDecodificacion(palabras, tx, r, total));
        int primerProblema = 0;
        for (int i = 0; i < total; i++) {
            final int idx = i;
            final Palabra p = palabras.get(i);
            final int palTx = HammingCodec.codificar(tx[i + 1] & 0xFF);
            Color color = colorPalabra(p, palTx);
            if (primerProblema == 0 && color != TemaVlc.COLOR_OK) primerProblema = i + 1;
            agregarChip(etiquetaChip(r.tramaEsperada(), i, total), color,
                    () -> detalle(r, idx, total, p, palTx));
        }
        activar(primerProblema);   // 0 = resumen si no hubo problemas
    }

    private Color colorPalabra(Palabra p, int palTx) {
        if (p.estado() == Estado.INCORREGIBLE) return TemaVlc.COLOR_ERR;
        if (p.estado() == Estado.CORREGIDO) {
            return p.palabraCorregida() == palTx ? TemaVlc.COLOR_WARN : TemaVlc.COLOR_ERR;
        }
        return p.palabraRx() == palTx ? TemaVlc.COLOR_OK : TemaVlc.COLOR_ERR;
    }

    private void resumenDecodificacion(List<Palabra> palabras, byte[] tx,
                                       ResultadoValidacion r, int total) {
        long ok = palabras.stream().filter(p -> p.estado() == Estado.SIN_ERROR).count();
        long corr = palabras.stream().filter(p -> p.estado() == Estado.CORREGIDO).count();
        long inc = palabras.stream().filter(p -> p.estado() == Estado.INCORREGIBLE).count();

        append("DECODIFICACIÓN EN EL RECEPTOR\n", TemaVlc.PURPLE_RX, true);
        append("Con los 4 controles de paridad se arma el SÍNDROME (p4 p3 p2 p1): su valor decimal\n"
                        + "es la posición del bit erróneo; si es 0000 no hay error.\n"
                        + "Elegí una palabra arriba para ver su tabla de controles.\n\n",
                TemaVlc.TEXT_MUTED, false);
        append(String.format("Palabras: %d  |  sin error: %d  |  corregidas: %d  |  incorregibles: %d\n\n",
                total, ok, corr, inc), TemaVlc.TEXT_WHITE, true);

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
    }

    private void detalle(ResultadoValidacion r, int idx, int total, Palabra p, int palTx) {
        append(String.format("PALABRA %d (%s) — CONTROLES DE PARIDAD EN RX\n",
                idx + 1, nombreCampo(r, idx, total)), colorPalabra(p, palTx), true);

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
            case SIN_ERROR -> {
                if (p.palabraRx() == palTx) {
                    append("-> Síndrome 0000: la palabra llegó sin error.\n", TemaVlc.COLOR_OK, true);
                } else {
                    append("-> Síndrome 0000 pero la palabra difiere de la transmitida (3 o más errores):\n"
                                    + "   Hamming no lo nota. Lo detecta el CRC-8.\n",
                            TemaVlc.COLOR_ERR, true);
                }
            }
        }
    }

    private void avisoFecDesactivado() {
        append("FEC desactivado: el enlace solo usa CRC-8.\n", TemaVlc.COLOR_WARN, true);
        append("El CRC-8 DETECTA errores pero no dice qué bit falló, así que no puede corregirlos:\n"
                        + "la trama se descarta y habría que pedir retransmisión (ARQ).\n\n"
                        + "Tildá \"FEC Hamming (12,8)\" para ver cómo se corrige hacia adelante.\n",
                TemaVlc.TEXT_MUTED, false);
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

    // ------------------------------------------------------------- nombres

    private String etiquetaChip(TramaVlc trama, int idx, int total) {
        if (idx == 0) return "LONG";
        if (idx == total - 1) return "CRC-8";
        byte[] payload = trama.getPayload();
        return String.format("D%d '%s'", idx, caracter(payload[idx - 1] & 0xFF));
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