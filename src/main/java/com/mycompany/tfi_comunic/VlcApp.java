package com.mycompany.tfi_comunic;

import com.fazecast.jSerialComm.SerialPort;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;

public class VlcApp extends JFrame {

    private SerialPort serialPort;
    
    private final JTextArea transLeftArea;
    private final JTextArea transRightArea;
    private final JTextArea recLeftArea;
    private final JTextArea recRightArea;
    private final JTextPane resultArea;
    private final JTextField inputField;
    private final JButton sendButton;
    
    private final java.io.ByteArrayOutputStream bufferRecepcion = new java.io.ByteArrayOutputStream();
    
    private byte[] ultimoMensajeEnviadoBytes;
    private String ultimoMensajeEnviadoTexto;

    private static final Color BG_DARK = new Color(15, 23, 42);       // #0F172A
    private static final Color BG_PANEL = new Color(30, 41, 59);      // #1E293B
    private static final Color BORDER_COLOR = new Color(51, 65, 85);   // #334155
    private static final Color TEXT_LIGHT = new Color(241, 245, 249);  // #F1F5F9
    private static final Color ACCENT_BLUE = new Color(56, 189, 248);  // #38BDF8
    private static final Color COLOR_OK = new Color(16, 185, 129);     // #10B981 (Verde)
    private static final Color COLOR_ERR = new Color(239, 68, 68);     // #EF4444 (Rojo)

    public VlcApp() {
        setTitle("Proyecto VLC - Transmisión por Luz Visible & Capa de Enlace (CRC-8)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Configuración para abrir en pantalla completa maximizada
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1024, 720));
        
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(12, 12));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(16, 16, 16, 16));

        // Encabezado Superior Minimalista
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_DARK);
        headerPanel.setBorder(new EmptyBorder(0, 4, 8, 4));
        
        JLabel titleLabel = new JLabel("SISTEMA DE COMUNICACIÓN ÓPTICA VLC");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_LIGHT);
        
        JLabel subLabel = new JLabel("Control de Errores e Integridad de Capa de Enlace | Notación Binaria Pura");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subLabel.setForeground(ACCENT_BLUE);
        
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Panel Central: 3 Secciones distribuidas uniformemente
        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 12, 12));
        centerPanel.setBackground(BG_DARK);

        // 1. Panel Transmisión
        JPanel transPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        transPanel.setBackground(BG_DARK);
        transPanel.setBorder(crearBordeTitulado("1. TRANSMISIÓN (Capa de Aplicación -> Enlace)"));
        transLeftArea = crearTextAreaMonospaced();
        transRightArea = crearTextAreaMonospaced();
        transPanel.add(crearScrollPaneEstilizado(transLeftArea));
        transPanel.add(crearScrollPaneEstilizado(transRightArea));

        // 2. Panel Recepción
        JPanel recPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        recPanel.setBackground(BG_DARK);
        recPanel.setBorder(crearBordeTitulado("2. RECEPCIÓN & ANÁLISIS DIFERENCIAL (Capa Física -> Enlace)"));
        recLeftArea = crearTextAreaMonospaced();
        recRightArea = crearTextAreaMonospaced();
        recPanel.add(crearScrollPaneEstilizado(recLeftArea));
        recPanel.add(crearScrollPaneEstilizado(recRightArea));

        // 3. Panel Validación
        JPanel valPanel = new JPanel(new BorderLayout());
        valPanel.setBackground(BG_DARK);
        valPanel.setBorder(crearBordeTitulado("3. CONTROL DE INTEGRIDAD & RECONSTRUCCIÓN (CRC-8: Polinomio 00000111)"));
        resultArea = new JTextPane();
        resultArea.setEditable(false);
        resultArea.setBackground(BG_PANEL);
        resultArea.setFont(new Font("Consolas", Font.BOLD, 14));
        valPanel.add(crearScrollPaneEstilizado(resultArea), BorderLayout.CENTER);

        centerPanel.add(transPanel);
        centerPanel.add(recPanel);
        centerPanel.add(valPanel);
        add(centerPanel, BorderLayout.CENTER);

        // Panel Inferior: Barra de Envío
        JPanel bottomPanel = new JPanel(new BorderLayout(12, 0));
        bottomPanel.setBackground(BG_DARK);
        bottomPanel.setBorder(new EmptyBorder(8, 4, 4, 4));

        JLabel inputLabel = new JLabel("Mensaje a transmitir (Texto Plano): ");
        inputLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        inputLabel.setForeground(TEXT_LIGHT);
        bottomPanel.add(inputLabel, BorderLayout.WEST);

        inputField = new JTextField();
        inputField.setBackground(BG_PANEL);
        inputField.setForeground(TEXT_LIGHT);
        inputField.setCaretColor(ACCENT_BLUE);
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        inputField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        bottomPanel.add(inputField, BorderLayout.CENTER);

        sendButton = new JButton("TRANSMITIR TRAMA");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendButton.setBackground(ACCENT_BLUE);
        sendButton.setForeground(BG_DARK);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(new EmptyBorder(10, 24, 10, 24));
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> enviarMensaje());
        inputField.addActionListener(e -> enviarMensaje());

        conectarPuertoSerie();
    }

    private TitledBorder crearBordeTitulado(String titulo) {
        TitledBorder border = BorderFactory.createTitledBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                " " + titulo + " ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13),
                ACCENT_BLUE
        );
        return border;
    }

    private JScrollPane crearScrollPaneEstilizado(Component comp) {
        JScrollPane scroll = new JScrollPane(comp);
        scroll.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        scroll.getViewport().setBackground(BG_PANEL);
        return scroll;
    }

    private JTextArea crearTextAreaMonospaced() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setBackground(BG_PANEL);
        area.setForeground(TEXT_LIGHT);
        area.setFont(new Font("Consolas", Font.PLAIN, 14));
        area.setMargin(new Insets(10, 12, 10, 12));
        return area;
    }

    private void conectarPuertoSerie() {
        SerialPort[] puertos = SerialPort.getCommPorts();
        if (puertos.length == 0) {
            agregarLogColor("Error: No se detectó ningún hardware Arduino conectado.", COLOR_ERR);
            return;
        }

        serialPort = puertos[0];
        serialPort.setBaudRate(9600);

        if (serialPort.openPort()) {
            agregarLogColor("Sistema Conectado exitosamente al puerto " + serialPort.getSystemPortName() + " (9600 baudios, 8N1).", ACCENT_BLUE);
            iniciarEscucha();
        } else {
            agregarLogColor("Error crítico al intentar abrir el puerto de comunicación.", COLOR_ERR);
        }
    }

    private String aBinario8(int valor) {
        return String.format("%8s", Integer.toBinaryString(valor & 0xFF)).replace(' ', '0');
    }

    private void enviarMensaje() {
        String texto = inputField.getText().trim();
        if (texto.isEmpty() || serialPort == null || !serialPort.isOpen()) return;

        byte[] datos = texto.getBytes(StandardCharsets.UTF_8);
        int longitud = Math.min(datos.length, 16);

        recLeftArea.setText("");
        recRightArea.setText("");
        bufferRecepcion.reset();

        ultimoMensajeEnviadoTexto = texto.substring(0, longitud);
        ultimoMensajeEnviadoBytes = new byte[longitud];
        System.arraycopy(datos, 0, ultimoMensajeEnviadoBytes, 0, longitud);

        // 1. Panel Transmisión (Izquierda): Representación Textual
        StringBuilder transLeft = new StringBuilder();
        transLeft.append("CADENA ORIGINAL: \"").append(ultimoMensajeEnviadoTexto).append("\"\n");
        transLeft.append("LONGITUD ÚTIL:   ").append(longitud).append(" caracteres (")
                 .append(aBinario8(longitud)).append(" en binario)\n\n");
        transLeft.append("ESTRUCTURA DE ENLACE ENVIADA:\n");
        transLeft.append("• Sincronismo (START): 01111110\n");
        transLeft.append("• Campo Longitud:      ").append(aBinario8(longitud)).append("\n");
        transLeft.append("• Carga Útil:          ").append(longitud * 8).append(" bits de datos\n");
        transLeftArea.setText(transLeft.toString());

        // 1. Panel Transmisión (Derecha): Desglose Carácter -> Binario
        StringBuilder transRight = new StringBuilder();
        transRight.append("DESGLOSE BINARIO DE TRANSMISIÓN (ASCII 8 BITS):\n");
        transRight.append("------------------------------------------------\n");
        for (int i = 0; i < longitud; i++) {
            char c = (char) datos[i];
            transRight.append(String.format(" Carácter '%c' -> Binario: %s\n", c, aBinario8(datos[i])));
        }
        transRightArea.setText(transRight.toString());

        // Construcción de la Trama Física
        byte[] cuerpo = new byte[longitud + 1];
        cuerpo[0] = (byte) longitud;
        System.arraycopy(datos, 0, cuerpo, 1, longitud);

        byte crc = calcularCRC8(cuerpo);

        byte[] trama = new byte[cuerpo.length + 2];
        trama[0] = 0x7E; // delimitador de inicio (01111110)
        System.arraycopy(cuerpo, 0, trama, 1, cuerpo.length);
        trama[trama.length - 1] = crc;

        serialPort.writeBytes(trama, trama.length);
        inputField.setText("");
    }

    private void iniciarEscucha() {
        Thread thread = new Thread(() -> {
            while (serialPort.isOpen()) {
                if (serialPort.bytesAvailable() > 0) {
                    byte[] readBuffer = new byte[serialPort.bytesAvailable()];
                    serialPort.readBytes(readBuffer, readBuffer.length);
                    procesarBytesRecibidos(readBuffer);
                }
                try { Thread.sleep(50); } catch (Exception e) {}
            }
        });
        thread.start();
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

        // 1. Panel Recepción (Derecha): Desglose binario recibido
        StringBuilder binRecibidoLog = new StringBuilder();
        binRecibidoLog.append("DESGLOSE BINARIO DE RECEPCIÓN (8 BITS):\n");
        binRecibidoLog.append("------------------------------------------------\n");
        for (byte b : datosRecibidos) {
            int val = b & 0xFF;
            char c = (val >= 32 && val <= 126) ? (char) val : '?';
            binRecibidoLog.append(String.format(" Símbolo '%c' -> Binario: %s\n", c, aBinario8(val)));
        }
        recRightArea.setText(binRecibidoLog.toString());

        // 2. Panel Recepción (Izquierda): Matriz Diferencial de Bits (Tx vs Rx con XOR)
        StringBuilder detallesRecepcion = new StringBuilder();
        String textoRecibidoCrudo = new String(datosRecibidos, StandardCharsets.ISO_8859_1);
        detallesRecepcion.append("CADENA RECIBIDA: \"").append(textoRecibidoCrudo).append("\"\n\n");
        detallesRecepcion.append("MATRIZ DIFERENCIAL DE BITS POR CARÁCTER (XOR):\n");
        detallesRecepcion.append("=========================================================================\n");

        boolean huboErrorFisico = false;
        StringBuilder logCorreccion = new StringBuilder();

        for (int i = 0; i < dataLen; i++) {
            byte sent = ultimoMensajeEnviadoBytes[i];
            byte rec = datosRecibidos[i];
            char charSent = (char) sent;
            char charRec = ((rec & 0xFF) >= 32 && (rec & 0xFF) <= 126) ? (char) rec : '?';

            if (sent != rec) {
                huboErrorFisico = true;
                detallesRecepcion.append(String.format("Posición %d | Esperado: '%c' (%s) | Recibido: '%c' (%s)\n",
                        (i + 1), charSent, aBinario8(sent), charRec, aBinario8(rec)));
                detallesRecepcion.append("  -----------------------------------------------------------------------\n");
                detallesRecepcion.append("  Bit Nº   | Tx (Nominal) | Rx (Medio) | XOR (Error) | Diagnóstico\n");
                detallesRecepcion.append("  -----------------------------------------------------------------------\n");

                int xor = (sent ^ rec) & 0xFF;
                for (int b = 7; b >= 0; b--) {
                    int bitTx = (sent >> b) & 1;
                    int bitRx = (rec >> b) & 1;
                    int bitXor = (xor >> b) & 1;
                    int numBit = 8 - b;

                    String estado = (bitXor == 0) ? "OK (Íntegro)" : String.format("FALLO [%d -> %d]", bitTx, bitRx);
                    detallesRecepcion.append(String.format("  Bit %d    |      %d       |     %d      |      %d      | %s\n",
                            numBit, bitTx, bitRx, bitXor, estado));
                }
                detallesRecepcion.append("  -----------------------------------------------------------------------\n\n");

                logCorreccion.append(String.format("  - Carácter %d: Recibido %s -> Reconstruido a %s ('%c')\n",
                        (i + 1), aBinario8(rec), aBinario8(sent), charSent));
            }
        }

        if (!huboErrorFisico) {
            detallesRecepcion.append("Todos los pulsos binarios coincidieron sin alteraciones (BER = 0).\n");
        }
        recLeftArea.setText(detallesRecepcion.toString());

        // 3. Panel Validación (Capa de Enlace)
        byte[] cuerpoParaCrc = new byte[dataLen + 1];
        cuerpoParaCrc[0] = byteLongitud;
        System.arraycopy(datosRecibidos, 0, cuerpoParaCrc, 1, dataLen);
        byte crcCalculado = calcularCRC8(cuerpoParaCrc);

        boolean estructuraValida = (byteInicio == 0x7E) && (byteLongitud == dataLen);

        if (!estructuraValida || crcCalculado != crcRecibido || huboErrorFisico) {
            StringBuilder reporteError = new StringBuilder();
            reporteError.append("FALLO DE INTEGRIDAD EN CAPA DE ENLACE (CRC-8)\n");
            reporteError.append("  • CRC Recibido en Trama:  ").append(aBinario8(crcRecibido)).append("\n");
            reporteError.append("  • CRC Recalculado Local:  ").append(aBinario8(crcCalculado)).append(" (Polinomio: 00000111)\n");
            reporteError.append("  • Diagnóstico de Canal:   Discrepancia detectada. Trama descartada por ruido óptico/inversión.\n\n");
            reporteError.append("MECANISMO DE CORRECCIÓN DIDÁCTICA (Bit a Bit):\n");
            reporteError.append(logCorreccion.toString());
            reporteError.append("-> TRAMA RESTAURADA FINAL: \"").append(ultimoMensajeEnviadoTexto).append("\"\n");
            reporteError.append("------------------------------------------------------------------------------------------------\n");
            agregarLogColor(reporteError.toString(), COLOR_ERR);
        } else {
            StringBuilder reporteExito = new StringBuilder();
            reporteExito.append("TRAMA VÁLIDA RECIBIDA CON ÉXITO\n");
            reporteExito.append("  • Mensaje Íntegro: \"").append(ultimoMensajeEnviadoTexto).append("\"\n");
            reporteExito.append("  • Verificación CRC-8: ").append(aBinario8(crcRecibido))
                        .append(" == ").append(aBinario8(crcCalculado)).append(" [COINCIDENCIA EXACTA - SIN ERRORES]\n");
            reporteExito.append("------------------------------------------------------------------------------------------------\n");
            agregarLogColor(reporteExito.toString(), COLOR_OK);
        }
    }

    private byte calcularCRC8(byte[] data) {
        int crc = 0;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x80) != 0) {
                    crc = ((crc << 1) ^ 0x07) & 0xFF;
                } else {
                    crc = (crc << 1) & 0xFF;
                }
            }
        }
        return (byte) crc;
    }

    private void agregarLogColor(String mensaje, Color color) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = resultArea.getStyledDocument();
            Style style = resultArea.addStyle("ColorStyle", null);
            StyleConstants.setForeground(style, color);
            try {
                doc.insertString(doc.getLength(), mensaje + "\n", style);
                resultArea.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
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