package com.artofvector.ui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.function.Consumer;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import com.artofvector.Version;
import com.artofvector.editor.CodeEditorPanel;
import com.artofvector.log.AppLog;
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
        menu.add(item("Open File…", KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK), e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                workspace.openFile(chooser.getSelectedFile().toPath());
            }
        }));
        menu.add(item("Open Folder…", KeyStroke.getKeyStroke(KeyEvent.VK_K, ActionEvent.CTRL_MASK), e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                Path folder = chooser.getSelectedFile().toPath();
                workspace.setRootFolder(folder);
                AppLog.info("Workspace folder: " + folder);
            }
        }));
        menu.addSeparator();
        menu.add(item("Save", KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK), e -> {
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
        menu.add(item("Exit", KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK), e -> exit.run()));
        return menu;
    }

    private JMenu editMenu(CodeEditorPanel editor) {
        JMenu menu = new JMenu("Edit");
        menu.add(item("Undo", KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK), e ->
                editor.currentTextArea().ifPresent(org.fife.ui.rtextarea.RTextArea::undoLastAction)));
        return menu;
    }

    private JMenu viewMenu(Runnable clearConsole) {
        JMenu menu = new JMenu("View");
        menu.add(item("Clear Console", null, e -> clearConsole.run()));
        return menu;
    }

    private JMenu helpMenu() {
        JMenu menu = new JMenu("Help");
        menu.add(item("About", null, e -> AppLog.info(Version.NAME + " " + Version.NUMBER
                + " — editor, debugger, and workflow canvas.")));
        return menu;
    }

    private JMenuItem item(String text, KeyStroke stroke, Consumer<ActionEvent> handler) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(com.artofvector.ui.theme.UiTheme.UI_FONT);
        if (stroke != null) {
            item.setAccelerator(stroke);
        }
        item.addActionListener(handler::accept);
        return item;
    }
}
