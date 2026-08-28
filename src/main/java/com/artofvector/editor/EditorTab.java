package com.artofvector.editor;

import java.awt.event.InputEvent;
import java.awt.event.MouseWheelEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.artofvector.log.AppLog;
import com.artofvector.ui.theme.UiTheme;

/**
 * A single editor tab backed by RSyntaxTextArea.
 */
public final class EditorTab {

    private final RSyntaxTextArea textArea;
    private final RTextScrollPane scroll;
    private Path file;
    private boolean dirty;
    private Runnable dirtyListener = () -> {
    };

    public EditorTab(Path file) {
        this.file = file;
        textArea = new RSyntaxTextArea(24, 80);
        textArea.setSyntaxEditingStyle(SyntaxSupport.forFile(file));
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setAnimateBracketMatching(true);
        textArea.setBracketMatchingEnabled(true);
        textArea.setAutoIndentEnabled(true);
        textArea.setCloseCurlyBraces(true);
        textArea.setTabSize(4);
        textArea.setTabsEmulated(true);
        textArea.setFont(UiTheme.MONO_FONT);
        textArea.setBackground(UiTheme.BG_INPUT);
        textArea.setForeground(UiTheme.TEXT);
        textArea.setCaretColor(UiTheme.TEXT);
        textArea.setCurrentLineHighlightColor(UiTheme.BG_HOVER);
        textArea.setSelectionColor(UiTheme.ACCENT_DIM);
        textArea.setMarkOccurrences(true);

        applyDarkTheme();

        scroll = new RTextScrollPane(textArea);
        scroll.setFoldIndicatorEnabled(true);
        scroll.setLineNumbersEnabled(true);
        scroll.setIconRowHeaderEnabled(true);
        scroll.getGutter().setBackground(UiTheme.BG_PANEL);
        scroll.getGutter().setLineNumberColor(UiTheme.TEXT_DIM);
        scroll.getGutter().setBorderColor(UiTheme.BORDER);
        scroll.getGutter().setFoldBackground(UiTheme.BG_PANEL);
        scroll.setBorder(null);
        textArea.addMouseWheelListener(this::onMouseWheel);
        applyFont();

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                markDirty();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                markDirty();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                markDirty();
            }
        });

        if (file != null) {
            loadFromDisk();
        }
    }

    public static EditorTab untitled() {
        return new EditorTab(null);
    }

    public RTextScrollPane component() {
        return scroll;
    }

    public RSyntaxTextArea textArea() {
        return textArea;
    }

    public Gutter gutter() {
        return scroll.getGutter();
    }

    public Path file() {
        return file;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirtyListener(Runnable dirtyListener) {
        this.dirtyListener = Objects.requireNonNull(dirtyListener);
    }

    public String title() {
        String name = file == null ? "Untitled" : file.getFileName().toString();
        return dirty ? name + " *" : name;
    }

    public void setFile(Path file) {
        this.file = file;
        textArea.setSyntaxEditingStyle(SyntaxSupport.forFile(file));
    }

    public void save() throws IOException {
        if (file == null) {
            throw new IOException("No file associated with this tab");
        }
        Files.writeString(file, textArea.getText(), StandardCharsets.UTF_8);
        dirty = false;
        dirtyListener.run();
        AppLog.info("Saved " + file);
    }

    public boolean confirmClose() {
        if (!dirty) {
            return true;
        }
        int choice = JOptionPane.showConfirmDialog(
                scroll,
                "Save changes to " + title() + "?",
                "Unsaved changes",
                JOptionPane.YES_NO_CANCEL_OPTION
        );
        if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
            return false;
        }
        if (choice == JOptionPane.YES_OPTION) {
            try {
                save();
            } catch (IOException e) {
                AppLog.error("Could not save file", e);
                JOptionPane.showMessageDialog(scroll, e.getMessage(), "Save failed", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return true;
    }

    public void highlightLine(int line) {
        try {
            int start = textArea.getLineStartOffset(Math.max(0, line));
            textArea.setCaretPosition(start);
            textArea.setCurrentLineHighlightColor(UiTheme.RIP_HIGHLIGHT);
        } catch (Exception e) {
            AppLog.debug("Could not highlight line " + line + ": " + e.getMessage());
        }
    }

    private void loadFromDisk() {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            textArea.setText(content);
            textArea.setCaretPosition(0);
            dirty = false;
            AppLog.info("Opened " + file);
        } catch (IOException e) {
            AppLog.error("Failed to open " + file, e);
            textArea.setText("");
        }
    }

    private void markDirty() {
        if (!dirty) {
            dirty = true;
            dirtyListener.run();
        }
    }

    private void applyDarkTheme() {
        try {
            Theme theme = Theme.load(RSyntaxTextArea.class.getResourceAsStream(
                    "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
            theme.apply(textArea);
            textArea.setFont(UiTheme.MONO_FONT);
        } catch (Exception e) {
            AppLog.debug("RSyntaxTextArea dark theme not applied: " + e.getMessage());
        }
    }

    public void applyFont() {
        java.awt.Font font = UiTheme.MONO_FONT;
        SyntaxScheme scheme = textArea.getSyntaxScheme();
        if (scheme != null) {
            for (int i = 0; i < scheme.getStyleCount(); i++) {
                Style style = scheme.getStyle(i);
                if (style != null) {
                    int existing = style.font == null ? java.awt.Font.PLAIN : style.font.getStyle();
                    style.font = font.deriveFont(existing);
                }
            }
            textArea.setSyntaxScheme(scheme);
        }
        textArea.setFont(font);
        if (scroll != null) {
            scroll.getGutter().setLineNumberFont(UiTheme.MONO_SMALL);
        }
    }

    private void onMouseWheel(MouseWheelEvent event) {
        if ((event.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
            if (event.getPreciseWheelRotation() < 0) {
                UiTheme.increaseFontSize();
            } else {
                UiTheme.decreaseFontSize();
            }
            event.consume();
            return;
        }
        // A wheel listener on the text area steals events from RTextScrollPane.
        if (scroll != null) {
            scroll.dispatchEvent(new MouseWheelEvent(
                    scroll,
                    event.getID(),
                    event.getWhen(),
                    event.getModifiersEx(),
                    event.getX(),
                    event.getY(),
                    event.getClickCount(),
                    event.isPopupTrigger(),
                    event.getScrollType(),
                    event.getScrollAmount(),
                    event.getWheelRotation(),
                    event.getPreciseWheelRotation()));
        }
    }
}
