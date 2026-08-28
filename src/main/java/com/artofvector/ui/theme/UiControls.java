package com.artofvector.ui.theme;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * Shared toolbar buttons and section chrome so every panel looks like one product.
 */
public final class UiControls {

    private UiControls() {
    }

    public static JPanel toolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        bar.setBackground(UiTheme.BG_ELEVATED);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
                new EmptyBorder(2, 6, 2, 6)));
        return bar;
    }

    public static JButton toolButton(String text, UiIcons.Glyph glyph, Runnable action) {
        return button(text, UiIcons.of(glyph, 16), UiTheme.BG_HOVER, UiTheme.TEXT, action);
    }

    public static JButton primaryButton(String text, UiIcons.Glyph glyph, Runnable action) {
        return button(text, UiIcons.of(glyph, 16, Color.WHITE), UiTheme.ACCENT, UiTheme.BG_ROOT, action);
    }

    public static JButton dangerButton(String text, UiIcons.Glyph glyph, Runnable action) {
        return button(text, UiIcons.of(glyph, 16, Color.WHITE), UiTheme.DANGER, Color.WHITE, action);
    }

    public static JPanel section(String title, UiIcons.Glyph glyph) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        header.setOpaque(false);
        JLabel icon = new JLabel(UiIcons.of(glyph, 14));
        JLabel label = new JLabel(title);
        label.setForeground(UiTheme.TEXT_MUTED);
        label.setFont(UiTheme.UI_FONT_BOLD);
        header.add(icon);
        header.add(label);
        header.setBorder(new EmptyBorder(6, 8, 4, 8));
        return header;
    }

    private static JButton button(String text, Icon icon, Color background, Color foreground, Runnable action) {
        JButton button = new JButton(text, icon);
        button.setFont(UiTheme.UI_FONT);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setIconTextGap(8);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(6, 12, 6, 12));
        Color idle = background;
        Color hover = blend(background, Color.WHITE, 0.12f);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(idle);
            }
        });
        button.addActionListener(e -> action.run());
        return button;
    }

    private static Color blend(Color base, Color tint, float amount) {
        int r = Math.min(255, (int) (base.getRed() + (tint.getRed() - base.getRed()) * amount));
        int g = Math.min(255, (int) (base.getGreen() + (tint.getGreen() - base.getGreen()) * amount));
        int b = Math.min(255, (int) (base.getBlue() + (tint.getBlue() - base.getBlue()) * amount));
        return new Color(r, g, b);
    }
}
