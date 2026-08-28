package com.artofvector.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.artofvector.log.AppLog;
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
        setBorder(UiTheme.panelBorder());

        text.setEditable(false);
        text.setBackground(UiTheme.BG_INPUT);
        text.setForeground(UiTheme.TEXT);
        text.setCaretColor(UiTheme.TEXT);
        text.setFont(UiTheme.MONO_SMALL);
        text.setMargin(new Insets(8, 10, 8, 10));

        document = text.getStyledDocument();
        infoStyle = style("info", UiTheme.TEXT);
        warnStyle = style("warn", UiTheme.WARNING);
        errorStyle = style("error", UiTheme.DANGER);
        debugStyle = style("debug", UiTheme.TEXT_DIM);

        JScrollPane scroll = new JScrollPane(text);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UiTheme.BG_INPUT);
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
        StyleConstants.setFontSize(style, 12);
        return style;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(800, 160);
    }

    public Font consoleFont() {
        return text.getFont();
    }
}
