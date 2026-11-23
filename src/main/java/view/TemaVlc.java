package view;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public final class TemaVlc {
    public static final Color BG_DARK     = new Color(11, 15, 25);
    public static final Color BG_CARD     = new Color(20, 27, 45);
    public static final Color BG_INPUT    = new Color(15, 23, 42);
    public static final Color BORDER_CARD = new Color(39, 51, 79);
    public static final Color TEXT_WHITE  = new Color(248, 250, 252);
    public static final Color TEXT_MUTED  = new Color(148, 163, 184);
    public static final Color CYAN_ACCENT = new Color(56, 189, 248);
    public static final Color PURPLE_RX   = new Color(168, 85, 247);
    public static final Color COLOR_OK    = new Color(34, 197, 94);
    public static final Color COLOR_ERR   = new Color(239, 68, 68);
    public static final Color COLOR_WARN  = new Color(245, 158, 11);

    public static final Font FONT_UI_BOLD  = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_UI_PLAIN = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_MONO     = new Font("Consolas", Font.PLAIN, 13);

    private TemaVlc() { }

    public static TitledBorder bordeTitulado(String titulo, Color color) {
        return BorderFactory.createTitledBorder(
                new LineBorder(BORDER_CARD, 1, true),
                " " + titulo + " ", TitledBorder.LEFT, TitledBorder.TOP,
                FONT_UI_BOLD, color);
    }

    public static JTextPane textPaneMono() {
        JTextPane p = new JTextPane();
        p.setEditable(false);
        p.setBackground(BG_CARD);
        p.setFont(FONT_MONO);
        p.setMargin(new Insets(8, 10, 8, 10));
        return p;
    }

    public static JTextArea textAreaMono() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setBackground(BG_CARD);
        a.setForeground(TEXT_WHITE);
        a.setFont(FONT_MONO);
        a.setMargin(new Insets(8, 10, 8, 10));
        return a;
    }

    public static JScrollPane scroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(new LineBorder(BORDER_CARD, 1, true));
        sp.getViewport().setBackground(BG_CARD);
        return sp;
    }

    public static JLabel badge(String texto, Color color) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(color);
        l.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(color, 1, true),
                new EmptyBorder(3, 8, 3, 8)));
        return l;
    }

    public static void actualizarBadge(JLabel l, String texto, Color color) {
        l.setText(texto);
        l.setForeground(color);
        l.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(color, 1, true),
                new EmptyBorder(3, 8, 3, 8)));
    }
}