package com.artofvector.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.artofvector.log.AppLog;
import com.artofvector.ui.theme.UiIcons;
import com.artofvector.ui.theme.UiTheme;

/**
 * Shared bottom console. All modules write here through {@link AppLog}.
 */
public final class ConsolePanel extends JPanel implements AppLog.Listener {

    private final JTextPane text = new JTextPane();
    private final StyledDocument document;
    private final Style infoStyle;
    private final Style warnStyle;
    private final Style errorStyle;
    private final Style debugStyle;

    public ConsolePanel() {
        super(new BorderLayout());
        setBackground(UiTheme.BG_INPUT);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder()));

        JLabel header = new JLabel("Console", UiIcons.of(UiIcons.Glyph.CONSOLE, 14), JLabel.LEFT);
        header.setIconTextGap(8);
        header.setForeground(UiTheme.TEXT_MUTED);
        header.setFont(UiTheme.UI_FONT_BOLD);
        header.setBorder(new EmptyBorder(6, 12, 6, 12));
        header.setOpaque(true);
        header.setBackground(UiTheme.BG_ELEVATED);

        text.setEditable(false);
        text.setBackground(UiTheme.BG_INPUT);
        text.setForeground(UiTheme.TEXT);
        text.setCaretColor(UiTheme.TEXT);
        text.setFont(UiTheme.MONO_FONT);
        text.setMargin(new Insets(8, 10, 8, 10));

        document = text.getStyledDocument();
        infoStyle = style("info", UiTheme.TEXT);
        warnStyle = style("warn", UiTheme.WARNING);
        errorStyle = style("error", UiTheme.DANGER);
        debugStyle = style("debug", UiTheme.TEXT_DIM);

        JScrollPane scroll = new JScrollPane(text);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UiTheme.BG_INPUT);
        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        AppLog.addListener(this);
        AppLog.info("Console ready.");
    }

    public void clear() {
        text.setText("");
    }

    @Override
    public void onLog(AppLog.Level level, String message) {
        Style style = switch (level) {
            case WARN -> warnStyle;
            case ERROR -> errorStyle;
            case DEBUG -> debugStyle;
            default -> infoStyle;
        };
        Runnable append = () -> {
            try {
                document.insertString(document.getLength(), message + "\n", style);
                text.setCaretPosition(document.getLength());
            } catch (BadLocationException ignored) {
                // Document mutated off the EDT; skip the line rather than crash the UI.
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            append.run();
        } else {
            SwingUtilities.invokeLater(append);
        }
    }

    private Style style(String name, Color color) {
        Style style = text.addStyle(name, null);
        StyleConstants.setForeground(style, color);
        StyleConstants.setFontFamily(style, UiTheme.MONO_FONT.getFamily());
        StyleConstants.setFontSize(style, UiTheme.fontSize());
        return style;
    }

    public void applyFont() {
        text.setFont(UiTheme.MONO_FONT);
        for (Style style : new Style[]{infoStyle, warnStyle, errorStyle, debugStyle}) {
            StyleConstants.setFontFamily(style, UiTheme.MONO_FONT.getFamily());
            StyleConstants.setFontSize(style, UiTheme.fontSize());
        }
        text.repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(800, 160);
    }

    public Font consoleFont() {
        return text.getFont();
    }
}
