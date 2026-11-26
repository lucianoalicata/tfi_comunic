package view;

import core.ModoEnlace;
import core.TramaVlc;
import io.ComunicacionSerie;
import service.EnvioVlc;
import service.RecepcionVlc;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class VlcApp extends JFrame {

    private ComunicacionSerie comunicacion;
    private EnvioVlc servicioEnvio;

    private final PanelTx panelTx = new PanelTx();
    private final PanelRx panelRx = new PanelRx();
    private final PanelHamming panelHamming = new PanelHamming();
    private final JLabel badgePort = TemaVlc.badge("CONEXIÓN: BUSCANDO...", TemaVlc.TEXT_MUTED);
    private final JLabel badgeCrc  = TemaVlc.badge("CANAL: REPOSO", TemaVlc.TEXT_MUTED);
    private final PanelValidacion panelValidacion = new PanelValidacion(badgeCrc);
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("TRANSMITIR TRAMA VLC");
    private final JCheckBox chkHamming = new JCheckBox("FEC Hamming (12,8)", true);

    public VlcApp() {
        configurarVentana();
        construirLayout();
        conectarHardware();
    }

    private void configurarVentana() {
        setTitle("Sistema VLC - Monitor de Capa de Enlace & Integridad CRC-8");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1100, 750));
        getContentPane().setBackground(TemaVlc.BG_DARK);
        setLayout(new BorderLayout(14, 14));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(14, 18, 14, 18));
    }

    private void construirLayout() {
        add(crearHeader(), BorderLayout.NORTH);
        add(crearSplitPrincipal(), BorderLayout.CENTER);
        add(crearBottomBar(), BorderLayout.SOUTH);
    }

    private JPanel crearHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(TemaVlc.BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(TemaVlc.BORDER_CARD, 1, true),
                new EmptyBorder(10, 16, 10, 16)));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        JLabel t1 = new JLabel("ESTACIÓN DE TRANSMISIÓN ÓPTICA VLC");
        t1.setFont(new Font("Segoe UI", Font.BOLD, 17));
        t1.setForeground(TemaVlc.TEXT_WHITE);
        JLabel t2 = new JLabel(
            "Capa Física OOK (LED/LDR) — Protocolo de Enlace framing 01111110 & Polinomio CRC-8");
        t2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t2.setForeground(TemaVlc.CYAN_ACCENT);
        titles.add(t1);
        titles.add(t2);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        badges.setOpaque(false);
        badges.add(badgePort);
        badges.add(badgeCrc);

        header.add(titles, BorderLayout.WEST);
        header.add(badges, BorderLayout.EAST);
        return header;
    }

    private JSplitPane crearSplitPrincipal() {
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setOpaque(false);
        split.setBorder(null);
        split.setDividerSize(6);
        split.setResizeWeight(0.5);

        JPanel dataPanels = new JPanel(new GridLayout(1, 2, 14, 0));
        dataPanels.setOpaque(false);
        dataPanels.add(panelTx);
        dataPanels.add(panelRx);

        JSplitPane inferior = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                panelHamming, panelValidacion);
        inferior.setOpaque(false);
        inferior.setBorder(null);
        inferior.setDividerSize(6);
        inferior.setResizeWeight(0.6);

        split.setTopComponent(dataPanels);
        split.setBottomComponent(inferior);
        return split;
    }

    private JPanel crearBottomBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(TemaVlc.BG_CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(TemaVlc.BORDER_CARD, 1, true),
                new EmptyBorder(8, 12, 8, 12)));

        JLabel prompt = new JLabel("Mensaje UTF-8 (Carga Útil):");
        prompt.setFont(new Font("Segoe UI", Font.BOLD, 13));
        prompt.setForeground(TemaVlc.TEXT_WHITE);
        bar.add(prompt, BorderLayout.WEST);

        inputField.setBackground(TemaVlc.BG_INPUT);
        inputField.setForeground(TemaVlc.TEXT_WHITE);
        inputField.setCaretColor(TemaVlc.CYAN_ACCENT);
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(TemaVlc.BORDER_CARD, 1, true),
                new EmptyBorder(6, 10, 6, 10)));
        bar.add(inputField, BorderLayout.CENTER);

        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sendButton.setBackground(TemaVlc.CYAN_ACCENT);
        sendButton.setForeground(TemaVlc.BG_DARK);
        sendButton.setFocusPainted(false);
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.setBorder(new EmptyBorder(8, 20, 8, 20));
        sendButton.addActionListener(e -> enviarMensaje());
        inputField.addActionListener(e -> enviarMensaje());

        chkHamming.setOpaque(false);
        chkHamming.setForeground(TemaVlc.TEXT_WHITE);
        chkHamming.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chkHamming.setFocusPainted(false);
        chkHamming.setToolTipText("Activado: corrige errores simples con Hamming y verifica con CRC-8. "
                + "Desactivado: solo CRC-8 (detecta y descarta).");

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        acciones.setOpaque(false);
        acciones.add(chkHamming);
        acciones.add(sendButton);
        bar.add(acciones, BorderLayout.EAST);
        return bar;
    }

    private void conectarHardware() {
        RecepcionVlc servicioRx = new RecepcionVlc(this::onTramaValidada);
        comunicacion = new ComunicacionSerie(servicioRx);
        ComunicacionSerie.ResultadoConexion r = comunicacion.conectar();

        if (r.exito()) {
            TemaVlc.actualizarBadge(badgePort,
                    "CONECTADO: " + comunicacion.getNombrePuerto(), TemaVlc.COLOR_OK);
            comunicacion.iniciarEscucha();
            servicioEnvio = new EnvioVlc(comunicacion, servicioRx);
        } else {
            TemaVlc.actualizarBadge(badgePort, "HARDWARE: DESCONECTADO", TemaVlc.COLOR_ERR);
            SwingUtilities.invokeLater(() -> panelValidacion.mostrarErrorConexion(r.mensaje()));
        }
    }

    private void enviarMensaje() {
        if (servicioEnvio == null) return;
        String texto = inputField.getText().trim();
        ModoEnlace modo = chkHamming.isSelected()
                ? ModoEnlace.CRC_HAMMING : ModoEnlace.SOLO_CRC;
        TramaVlc trama = servicioEnvio.enviarTexto(texto, modo);
        if (trama == null) return;

        panelTx.limpiar();
        panelRx.limpiar();
        panelHamming.limpiar();
        panelValidacion.limpiar();
        panelTx.mostrarTrama(trama);
        panelHamming.mostrarCodificacion(trama, modo);
        TemaVlc.actualizarBadge(badgeCrc, "TRANSMITIENDO...", TemaVlc.CYAN_ACCENT);
        inputField.setText("");
    }

    private void onTramaValidada(RecepcionVlc.ResultadoValidacion r) {
        SwingUtilities.invokeLater(() -> {
            panelRx.mostrar(r);
            panelHamming.mostrar(r);
            panelValidacion.mostrar(r);
        });
    }
}