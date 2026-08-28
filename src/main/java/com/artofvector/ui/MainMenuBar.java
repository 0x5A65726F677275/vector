package com.artofvector.ui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import com.artofvector.Version;
import com.artofvector.editor.CodeEditorPanel;
import com.artofvector.log.AppLog;
import com.artofvector.ui.theme.UiIcons;
import com.artofvector.ui.theme.UiTheme;
import com.artofvector.workspace.Workspace;

public final class MainMenuBar extends JMenuBar {

    public MainMenuBar(Workspace workspace, CodeEditorPanel editor, Runnable clearConsole, Runnable exit) {
        setBackground(com.artofvector.ui.theme.UiTheme.BG_ELEVATED);
        add(fileMenu(workspace, editor, exit));
        add(editMenu(editor));
        add(viewMenu(clearConsole));
        add(helpMenu());
    }

    private JMenu fileMenu(Workspace workspace, CodeEditorPanel editor, Runnable exit) {
        JMenu menu = new JMenu("File");
        menu.add(item("Open File…", UiIcons.Glyph.OPEN,
                KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK), e -> {
            JFileChooser chooser = new JFileChooser();
            if (workspace.rootFolder() != null) {
                chooser.setCurrentDirectory(workspace.rootFolder().toFile());
            }
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                workspace.openFile(chooser.getSelectedFile().toPath());
            }
        }));
        menu.add(item("Open Folder…", UiIcons.Glyph.OPEN_FOLDER,
                KeyStroke.getKeyStroke(KeyEvent.VK_K, ActionEvent.CTRL_MASK), e ->
                workspace.chooseRootFolder(this)));
        menu.addSeparator();
        menu.add(item("Save", UiIcons.Glyph.SAVE,
                KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK), e -> {
            try {
                var tab = editor.currentTab();
                if (tab != null && tab.file() != null) {
                    tab.save();
                }
            } catch (Exception ex) {
                AppLog.error("Save failed", ex);
            }
        }));
        menu.addSeparator();
        menu.add(item("Close", UiIcons.Glyph.CLEAR,
                KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK), e -> editor.closeCurrent()));
        menu.add(item("Close All", UiIcons.Glyph.CLEAR, null, e -> editor.closeAll()));
        menu.addSeparator();
        menu.add(item("Exit", UiIcons.Glyph.STOP,
                KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK), e -> exit.run()));
        return menu;
    }

    private JMenu editMenu(CodeEditorPanel editor) {
        JMenu menu = new JMenu("Edit");
        menu.add(item("Undo", null, KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK), e ->
                editor.currentTextArea().ifPresent(org.fife.ui.rtextarea.RTextArea::undoLastAction)));
        return menu;
    }

    private JMenu viewMenu(Runnable clearConsole) {
        JMenu menu = new JMenu("View");
        menu.add(item("Increase Font Size", null,
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, ActionEvent.CTRL_MASK),
                e -> UiTheme.increaseFontSize()));
        menu.add(item("Decrease Font Size", null,
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ActionEvent.CTRL_MASK),
                e -> UiTheme.decreaseFontSize()));
        menu.add(item("Reset Font Size", null,
                KeyStroke.getKeyStroke(KeyEvent.VK_0, ActionEvent.CTRL_MASK),
                e -> UiTheme.resetFontSize()));
        menu.addSeparator();
        menu.add(item("Clear Console", UiIcons.Glyph.CLEAR, null, e -> clearConsole.run()));
        return menu;
    }

    private JMenu helpMenu() {
        JMenu menu = new JMenu("Help");
        menu.add(item("About", UiIcons.Glyph.APP, null, e -> AppLog.info(Version.NAME + " " + Version.NUMBER
                + " — editor, debugger, and workflow canvas.")));
        return menu;
    }

    private JMenuItem item(String text, UiIcons.Glyph glyph, KeyStroke stroke, Consumer<ActionEvent> handler) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(UiTheme.UI_FONT);
        if (glyph != null) {
            item.setIcon(UiIcons.of(glyph, 16));
        }
        if (stroke != null) {
            item.setAccelerator(stroke);
        }
        item.addActionListener(handler::accept);
        return item;
    }
}
