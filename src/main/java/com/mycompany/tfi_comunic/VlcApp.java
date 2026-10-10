package com.mycompany.tfi_comunic;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;

public class VlcApp extends JFrame {

    private ComunicacionSerie comunicacion;

    private final JTextArea transLeftArea;
    private final JTextArea transRightArea;
    private final JTextPane recLeftArea;  
    private final JTextPane recRightArea; 
    private final JTextPane resultArea;
    private final JTextField inputField;
    private final JButton sendButton;
    private final JLabel lblStatusPort;
    private final JLabel lblStatusCRC;

    private final java.io.ByteArrayOutputStream bufferRecepcion = new java.io.ByteArrayOutputStream();

    private byte[] ultimoMensajeEnviadoBytes;
    private String ultimoMensajeEnviadoTexto;

    private static final Color BG_DARK = new Color(11, 15, 25);
    private static final Color BG_CARD = new Color(20, 27, 45);
    private static final Color BG_INPUT = new Color(15, 23, 42);
    private static final Color BORDER_CARD = new Color(39, 51, 79);
    private static final Color TEXT_WHITE = new Color(248, 250, 252);
    private static final Color TEXT_MUTED = new Color(148, 163, 184);
    private static final Color CYAN_ACCENT = new Color(56, 189, 248);
    private static final Color COLOR_OK = new Color(34, 197, 94);
    private static final Color COLOR_ERR = new Color(239, 68, 68);
    private static final Color COLOR_WARN = new Color(245, 158, 11);

    public VlcApp() {
        setTitle("Sistema VLC - Monitor de Capa de Enlace & Integridad CRC-8");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1100, 750));
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(14, 14));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(14, 18, 14, 18));
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setBackground(BG_CARD);
        topHeader.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_CARD, 1, true),
                new EmptyBorder(10, 16, 10, 16)
        ));

        JPanel headerTitles = new JPanel(new GridLayout(2, 1, 0, 2));
        headerTitles.setOpaque(false);
        JLabel titleMain = new JLabel("ESTACIÓN DE TRANSMISIÓN ÓPTICA VLC");
        titleMain.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleMain.setForeground(TEXT_WHITE);
        JLabel titleSub = new JLabel("Capa Física OOK (LED/LDR) — Protocolo de Enlace framing 01111110 & Polinomio CRC-8");
        titleSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleSub.setForeground(CYAN_ACCENT);
        headerTitles.add(titleMain);
        headerTitles.add(titleSub);

        JPanel headerBadges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerBadges.setOpaque(false);
        lblStatusPort = crearBadge("CONEXIÓN: BUSCANDO...", TEXT_MUTED);
        lblStatusCRC = crearBadge("CANAL: REPOSO", TEXT_MUTED);
        headerBadges.add(lblStatusPort);
        headerBadges.add(lblStatusCRC);

        topHeader.add(headerTitles, BorderLayout.WEST);
        topHeader.add(headerBadges, BorderLayout.EAST);
        add(topHeader, BorderLayout.NORTH);

        JSplitPane splitPrincipal = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPrincipal.setOpaque(false);
        splitPrincipal.setBorder(null);
        splitPrincipal.setDividerSize(6);
        splitPrincipal.setResizeWeight(0.66);

        JPanel dataPanelsContainer = new JPanel(new GridLayout(1, 2, 14, 0));
        dataPanelsContainer.setOpaque(false);

        // Bloque TX
        JPanel txBox = new JPanel(new GridLayout(2, 1, 0, 8));
        txBox.setOpaque(false);
        txBox.setBorder(crearBordeTitulado("CANAL TX: TRANSMISIÓN (ORIGEN)", CYAN_ACCENT));
        transLeftArea = crearTextAreaMonospaced();
        transRightArea = crearTextAreaMonospaced();
        txBox.add(crearScrollPane(transLeftArea));
        txBox.add(crearScrollPane(transRightArea));

        // Bloque RX
        JPanel rxBox = new JPanel(new GridLayout(2, 1, 0, 8));
        rxBox.setOpaque(false);
        rxBox.setBorder(crearBordeTitulado("CANAL RX: RECEPCIÓN & MATRIZ DIFERENCIAL (DESTINO)", new Color(168, 85, 247)));
        recLeftArea = crearTextPaneMonospaced();
        recRightArea = crearTextPaneMonospaced();
        rxBox.add(crearScrollPane(recLeftArea));
        rxBox.add(crearScrollPane(recRightArea));

        dataPanelsContainer.add(txBox);
        dataPanelsContainer.add(rxBox);
        splitPrincipal.setTopComponent(dataPanelsContainer);

        // Sección Inferior: Validación CRC-8 y Reconstrucción
        JPanel valBox = new JPanel(new BorderLayout());
        valBox.setOpaque(false);
        valBox.setBorder(crearBordeTitulado("VERIFICACIÓN DE CAPA 2 (MÁQUINA DE ESTADO CRC-8)", COLOR_OK));
        resultArea = crearTextPaneMonospaced();
        valBox.add(crearScrollPane(resultArea), BorderLayout.CENTER);
        splitPrincipal.setBottomComponent(valBox);

        add(splitPrincipal, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new BorderLayout(12, 0));
        bottomBar.setBackground(BG_CARD);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_CARD, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel promptLabel = new JLabel("Mensaje UTF-8 (Carga Útil):");
        promptLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        promptLabel.setForeground(TEXT_WHITE);
        bottomBar.add(promptLabel, BorderLayout.WEST);

        inputField = new JTextField();
        inputField.setBackground(BG_INPUT);
        inputField.setForeground(TEXT_WHITE);
        inputField.setCaretColor(CYAN_ACCENT);
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_CARD, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        bottomBar.add(inputField, BorderLayout.CENTER);

        sendButton = new JButton("TRANSMITIR TRAMA VLC");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sendButton.setBackground(CYAN_ACCENT);
        sendButton.setForeground(BG_DARK);
        sendButton.setFocusPainted(false);
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.setBorder(new EmptyBorder(8, 20, 8, 20));
        bottomBar.add(sendButton, BorderLayout.EAST);

        add(bottomBar, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> enviarMensaje());
        inputField.addActionListener(e -> enviarMensaje());

        conectarPuertoSerie();
    }

    private JLabel crearBadge(String texto, Color col) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(col);
        l.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(col, 1, true),
                new EmptyBorder(3, 8, 3, 8)
        ));
        return l;
    }

    private void actualizarBadge(JLabel label, String texto, Color col) {
        label.setText(texto);
        label.setForeground(col);
        label.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(col, 1, true),
                new EmptyBorder(3, 8, 3, 8)
        ));
    }

    private TitledBorder crearBordeTitulado(String titulo, Color colorTitulo) {
        return BorderFactory.createTitledBorder(
                new LineBorder(BORDER_CARD, 1, true),
                " " + titulo + " ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                colorTitulo
        );
    }

    private JScrollPane crearScrollPane(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(new LineBorder(BORDER_CARD, 1, true));
        sp.getViewport().setBackground(BG_CARD);
        return sp;
    }

    private JTextArea crearTextAreaMonospaced() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setBackground(BG_CARD);
        area.setForeground(TEXT_WHITE);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        area.setMargin(new Insets(8, 10, 8, 10));
        return area;
    }

    private JTextPane crearTextPaneMonospaced() {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setBackground(BG_CARD);
        pane.setFont(new Font("Consolas", Font.PLAIN, 13));
        pane.setMargin(new Insets(8, 10, 8, 10));
        return pane;
    }

    private void conectarPuertoSerie() {
        comunicacion = new ComunicacionSerie(this::recibirBytes);
        String resultado = comunicacion.conectar();

        if (comunicacion.estaConectada()) {
            actualizarBadge(lblStatusPort, "CONECTADO: " + comunicacion.getNombrePuerto(), COLOR_OK);
            agregarLogColor(resultArea, resultado + "\n", CYAN_ACCENT);
            comunicacion.iniciarEscucha();
        } else {
            actualizarBadge(lblStatusPort, "HARDWARE: DESCONECTADO", COLOR_ERR);
            agregarLogColor(resultArea, resultado + "\n", COLOR_ERR);
        }
    }

    private void recibirBytes(byte[] bytes){
        SwingUtilities.invokeLater(() -> procesarBytesRecibidos(bytes));
    }

    private String aBinario8(int valor) {
        return ProtocoloVlc.aBinario8(valor);
    }

    private void enviarMensaje() {
        String texto = inputField.getText().trim();
        if (texto.isEmpty() || comunicacion == null || !comunicacion.estaConectada()) return;

        transLeftArea.setText("");
        transRightArea.setText("");
        recLeftArea.setText("");
        recRightArea.setText("");
        resultArea.setText("");
        bufferRecepcion.reset();

        byte[] datos = texto.getBytes(StandardCharsets.UTF_8);
        int longitud = Math.min(datos.length, 16);

        ultimoMensajeEnviadoBytes = new byte[longitud];
        System.arraycopy(datos, 0, ultimoMensajeEnviadoBytes, 0, longitud);
        ultimoMensajeEnviadoTexto = new String(ultimoMensajeEnviadoBytes, StandardCharsets.UTF_8);

        actualizarBadge(lblStatusCRC, "TRANSMITIENDO...", CYAN_ACCENT);

        StringBuilder transLeft = new StringBuilder();
        transLeft.append("CADENA ORIGINAL: \"").append(ultimoMensajeEnviadoTexto).append("\"\n");
        transLeft.append("LONGITUD ÚTIL:   ").append(longitud).append(" caracteres (")
                 .append(aBinario8(longitud)).append(")\n\n");
        transLeft.append("ESTRUCTURA DE TRAMA FORMATEADA:\n");
        transLeft.append("• START (Sincronismo): 01111110\n");
        transLeft.append("• Campo Longitud (N):  ").append(aBinario8(longitud)).append("\n");
        transLeft.append("• Tamaño Carga Útil:   ").append(longitud * 8).append(" bits de datos\n");
        transLeftArea.setText(transLeft.toString());

        StringBuilder transRight = new StringBuilder();
        transRight.append("DESGLOSE BINARIO NOMINAL (ASCII 8 BITS):\n");
        transRight.append("--------------------------------------------------\n");
        for (int i = 0; i < longitud; i++) {
            char c = (char) datos[i];
            transRight.append(String.format(" Carácter '%c' (Pos %d) -> %s\n", c, (i + 1), aBinario8(datos[i])));
        }
        transRightArea.setText(transRight.toString());

        // Construcción de la Trama
        byte[] trama = ProtocoloVlc.crearTrama(ultimoMensajeEnviadoBytes);
        comunicacion.enviar(trama);
        inputField.setText("");
    }

    private void procesarBytesRecibidos(byte[] buffer) {
        try {
            bufferRecepcion.write(buffer);
            byte[] datosCompletos = bufferRecepcion.toByteArray();

            int tamañoEsperado = (ultimoMensajeEnviadoBytes != null) ? (ultimoMensajeEnviadoBytes.length + 3) : 0;

            if (tamañoEsperado > 0 && datosCompletos.length >= tamañoEsperado) {
                byte[] tramaRecibida = new byte[tamañoEsperado];
                System.arraycopy(datosCompletos, 0, tramaRecibida, 0, tamañoEsperado);

                validarTramaDidactica(tramaRecibida);

                byte[] temp = new byte[datosCompletos.length - tamañoEsperado];
                System.arraycopy(datosCompletos, tamañoEsperado, temp, 0, temp.length);
                bufferRecepcion.reset();
                bufferRecepcion.write(temp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void validarTramaDidactica(byte[] trama) {
        if (ultimoMensajeEnviadoBytes == null) return;

        int dataLen = ultimoMensajeEnviadoBytes.length;
        byte byteInicio = trama[0];
        byte byteLongitud = trama[1];

        byte[] datosRecibidos = new byte[dataLen];
        System.arraycopy(trama, 2, datosRecibidos, 0, dataLen);
        byte crcRecibido = trama[trama.length - 1];

        //Panel Recepción (Derecha): Tira binaria con bits erróneos pintados
        recRightArea.setText("");
        agregarTextoEstilizado(recRightArea, "DESGLOSE BINARIO DE RECEPCIÓN (8 BITS):\n", TEXT_WHITE, true);
        agregarTextoEstilizado(recRightArea, "--------------------------------------------------\n", BORDER_CARD, false);

        for (int i = 0; i < datosRecibidos.length; i++) {
            byte sent = ultimoMensajeEnviadoBytes[i];
            byte rec = datosRecibidos[i];
            int val = rec & 0xFF;
            char c = (val >= 32 && val <= 126) ? (char) val : '?';

            agregarTextoEstilizado(recRightArea, String.format(" Símbolo '%c' (Pos %d) -> ", c, (i + 1)), TEXT_MUTED, false);

            for (int b = 7; b >= 0; b--) {
                int bitSent = (sent >> b) & 1;
                int bitRec = (rec >> b) & 1;
                String bitStr = String.valueOf(bitRec);

                if (bitSent != bitRec) {
                    agregarTextoEstilizado(recRightArea, bitStr, COLOR_ERR, true); // Bit corrupto en Rojo
                } else {
                    agregarTextoEstilizado(recRightArea, bitStr, TEXT_WHITE, false); // Bit sano en Blanco
                }
            }
            agregarTextoEstilizado(recRightArea, "\n", TEXT_WHITE, false);
        }

        //Panel Recepción (Izquierda): Matriz Diferencial XOR con colores por celda
        recLeftArea.setText("");
        String textoRecibidoCrudo = new String(datosRecibidos, StandardCharsets.ISO_8859_1);
        agregarTextoEstilizado(recLeftArea, "CADENA CAPTURADA DEL MEDIO: \"" + textoRecibidoCrudo + "\"\n\n", TEXT_WHITE, true);
        agregarTextoEstilizado(recLeftArea, "MATRIZ DIFERENCIAL DE BITS POR CARÁCTER (XOR):\n", CYAN_ACCENT, true);
        agregarTextoEstilizado(recLeftArea, "=========================================================================\n", BORDER_CARD, false);

        boolean huboErrorFisico = false;
        StringBuilder logCorreccion = new StringBuilder();

        for (int i = 0; i < dataLen; i++) {
            byte sent = ultimoMensajeEnviadoBytes[i];
            byte rec = datosRecibidos[i];
            char charSent = (char) sent;
            char charRec = ((rec & 0xFF) >= 32 && (rec & 0xFF) <= 126) ? (char) rec : '?';

            if (sent != rec) {
                huboErrorFisico = true;
                agregarTextoEstilizado(recLeftArea, String.format("Posición %d | Nominal: '%c' (%s) | Recibido: '%c' (%s)\n",
                        (i + 1), charSent, aBinario8(sent), charRec, aBinario8(rec)), COLOR_WARN, true);
                agregarTextoEstilizado(recLeftArea, "  -----------------------------------------------------------------------\n", BORDER_CARD, false);
                agregarTextoEstilizado(recLeftArea, "  Bit Nº   | Tx (Nominal) | Rx (Medio) | XOR (Error) | Diagnóstico\n", TEXT_MUTED, false);
                agregarTextoEstilizado(recLeftArea, "  -----------------------------------------------------------------------\n", BORDER_CARD, false);

                int xor = (sent ^ rec) & 0xFF;
                for (int b = 7; b >= 0; b--) {
                    int bitTx = (sent >> b) & 1;
                    int bitRx = (rec >> b) & 1;
                    int bitXor = (xor >> b) & 1;
                    int numBit = 8 - b;

                    agregarTextoEstilizado(recLeftArea, String.format("  Bit %d    |      %d       |     ", numBit, bitTx), TEXT_WHITE, false);

                    if (bitXor == 1) {
                        agregarTextoEstilizado(recLeftArea, String.valueOf(bitRx), COLOR_ERR, true);
                        agregarTextoEstilizado(recLeftArea, String.format("      |      %d      | ", bitXor), TEXT_WHITE, false);
                        agregarTextoEstilizado(recLeftArea, String.format("FALLO [%d -> %d]\n", bitTx, bitRx), COLOR_ERR, true);
                    } else {
                        agregarTextoEstilizado(recLeftArea, String.valueOf(bitRx), TEXT_WHITE, false);
                        agregarTextoEstilizado(recLeftArea, String.format("      |      %d      | ", bitXor), TEXT_WHITE, false);
                        agregarTextoEstilizado(recLeftArea, "OK (Íntegro)\n", COLOR_OK, false);
                    }
                }
                agregarTextoEstilizado(recLeftArea, "  -----------------------------------------------------------------------\n\n", BORDER_CARD, false);

                logCorreccion.append(String.format("  - Carácter %d: Recibido %s -> Restaurado a %s ('%c')\n",
                        (i + 1), aBinario8(rec), aBinario8(sent), charSent));
            }
        }

        if (!huboErrorFisico) {
            agregarTextoEstilizado(recLeftArea, "Canal Limpio: Todos los pulsos fotónicos muestreados con BER = 0.\n", COLOR_OK, false);
        }

        //Panel Validación (Capa de Enlace: CRC-8)
        byte[] cuerpoParaCrc = new byte[dataLen + 1];
        cuerpoParaCrc[0] = byteLongitud;
        System.arraycopy(datosRecibidos, 0, cuerpoParaCrc, 1, dataLen);
        byte crcCalculado = ProtocoloVlc.calcularCRC8(cuerpoParaCrc);

        boolean estructuraValida = (byteInicio == 0x7E) && (byteLongitud == dataLen);

        resultArea.setText("");
        if (!estructuraValida || crcCalculado != crcRecibido || huboErrorFisico) {
            actualizarBadge(lblStatusCRC, "ESTADO: TRAMA DESCARTADA (CRC ERROR)", COLOR_ERR);

            agregarTextoEstilizado(resultArea, "FALLO DE INTEGRIDAD EN CAPA DE ENLACE (CRC-8)\n", COLOR_ERR, true);
            agregarTextoEstilizado(resultArea, "  • CRC Recibido en Trama:  " + aBinario8(crcRecibido) + "\n", TEXT_WHITE, false);
            agregarTextoEstilizado(resultArea, "  • CRC Recalculado Local:  " + aBinario8(crcCalculado) + " (Polinomio generador: 00000111)\n", TEXT_WHITE, false);
            agregarTextoEstilizado(resultArea, "  • Estado del Enlace:      Discrepancia detectada. Bloque descartado por perturbación en Capa 1.\n\n", COLOR_ERR, false);

            agregarTextoEstilizado(resultArea, "RECONSTRUCCIÓN DEMOSTRATIVA DE CARGA ÚTIL:\n", CYAN_ACCENT, true);
            agregarTextoEstilizado(resultArea, logCorreccion.toString(), TEXT_MUTED, false);
            agregarTextoEstilizado(resultArea, "-> CARGA ÚTIL RESTAURADA: \"" + ultimoMensajeEnviadoTexto + "\"\n", COLOR_OK, true);
            agregarTextoEstilizado(resultArea, "------------------------------------------------------------------------------------------------\n", BORDER_CARD, false);
        } else {
            actualizarBadge(lblStatusCRC, "ESTADO: ENLACE ÍNTEGRO (OK)", COLOR_OK);

            agregarTextoEstilizado(resultArea, "TRAMA VÁLIDA RECIBIDA CON ÉXITO\n", COLOR_OK, true);
            agregarTextoEstilizado(resultArea, "  • Mensaje Íntegro: \"" + ultimoMensajeEnviadoTexto + "\"\n", TEXT_WHITE, false);
            agregarTextoEstilizado(resultArea, "  • Verificación CRC-8: " + aBinario8(crcRecibido) + " == " + aBinario8(crcCalculado) + " [COINCIDENCIA EXACTA - SIN ERRORES]\n", COLOR_OK, false);
            agregarTextoEstilizado(resultArea, "------------------------------------------------------------------------------------------------\n", BORDER_CARD, false);
        }
    }

    private void agregarTextoEstilizado(JTextPane pane, String texto, Color color, boolean bold) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = pane.getStyledDocument();
            Style style = pane.addStyle("CustomStyle", null);
            StyleConstants.setForeground(style, color);
            StyleConstants.setBold(style, bold);
            try {
                doc.insertString(doc.getLength(), texto, style);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private void agregarLogColor(JTextPane pane, String mensaje, Color color) {
        agregarTextoEstilizado(pane, mensaje, color, false);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            VlcApp app = new VlcApp();
            app.setVisible(true);
        });
    }
}