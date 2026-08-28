package com.artofvector.editor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import com.artofvector.log.AppLog;
import com.artofvector.ui.WorkbenchModule;
import com.artofvector.ui.theme.UiTheme;
import com.artofvector.workspace.Workspace;

public final class CodeEditorPanel extends JPanel implements WorkbenchModule {

    private final JTabbedPane tabs = new JTabbedPane();
    private final Workspace workspace;
    private final Map<JPanel, EditorTab> tabMap = new HashMap<>();
    private BreakpointGutterController gutterController;

    public CodeEditorPanel(Workspace workspace) {
        super(new BorderLayout());
        this.workspace = workspace;
        setBackground(UiTheme.BG_PANEL);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        toolbar.setBackground(UiTheme.BG_ELEVATED);
        toolbar.add(button("Open", this::openFileDialog));
        toolbar.add(button("Save", this::saveCurrent));
        toolbar.add(button("Save As", this::saveCurrentAs));
        toolbar.add(button("New", this::newUntitled));

        tabs.setBackground(UiTheme.BG_PANEL);
        tabs.setForeground(UiTheme.TEXT);
        tabs.setFont(UiTheme.UI_FONT);

        add(toolbar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        workspace.addFileOpenListener(this::openFile);
        newUntitled();
    }

    public void setGutterController(BreakpointGutterController gutterController) {
        this.gutterController = gutterController;
        EditorTab current = currentTab();
        if (gutterController != null && current != null) {
            gutterController.attach(current);
        }
    }

    public EditorTab currentTab() {
        int index = tabs.getSelectedIndex();
        if (index < 0) {
            return null;
        }
        JPanel wrapper = (JPanel) tabs.getComponentAt(index);
        return tabMap.get(wrapper);
    }

    public Optional<RSyntaxTextArea> currentTextArea() {
        EditorTab tab = currentTab();
        return tab == null ? Optional.empty() : Optional.of(tab.textArea());
    }

    public void openFile(Path path) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            JPanel wrapper = (JPanel) tabs.getComponentAt(i);
            EditorTab existing = tabMap.get(wrapper);
            if (existing != null && path.equals(existing.file())) {
                tabs.setSelectedIndex(i);
                return;
            }
        }
        addTab(new EditorTab(path));
        SwingUtilities.invokeLater(() -> {
            int last = tabs.getTabCount() - 1;
            if (last >= 0) {
                tabs.setSelectedIndex(last);
            }
        });
    }

    public void highlightSourceLine(int line) {
        EditorTab tab = currentTab();
        if (tab != null) {
            tab.highlightLine(line);
        }
    }

    @Override
    public String tabTitle() {
        return "Editor";
    }

    @Override
    public JPanel component() {
        return this;
    }

    private void newUntitled() {
        addTab(EditorTab.untitled());
    }

    private void addTab(EditorTab tab) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UiTheme.BG_PANEL);
        wrapper.add(tab.component(), BorderLayout.CENTER);
        tabMap.put(wrapper, tab);
        tabs.addTab(tab.title(), wrapper);
        tabs.setSelectedComponent(wrapper);
        tab.setDirtyListener(() -> refreshTitle(wrapper, tab));
        if (gutterController != null) {
            gutterController.attach(tab);
        }
    }

    private void refreshTitle(JPanel wrapper, EditorTab tab) {
        int index = tabs.indexOfComponent(wrapper);
        if (index >= 0) {
            tabs.setTitleAt(index, tab.title());
        }
    }

    private void openFileDialog() {
        JFileChooser chooser = chooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path path = chooser.getSelectedFile().toPath();
            workspace.openFile(path);
        }
    }

    private void saveCurrent() {
        EditorTab tab = currentTab();
        if (tab == null) {
            return;
        }
        try {
            if (tab.file() == null) {
                saveCurrentAs();
                return;
            }
            tab.save();
            refreshTitle((JPanel) tabs.getSelectedComponent(), tab);
        } catch (IOException e) {
            AppLog.error("Save failed", e);
            JOptionPane.showMessageDialog(this, e.getMessage(), "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveCurrentAs() {
        EditorTab tab = currentTab();
        if (tab == null) {
            return;
        }
        JFileChooser chooser = chooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path path = chooser.getSelectedFile().toPath();
            tab.setFile(path);
            try {
                tab.save();
                refreshTitle((JPanel) tabs.getSelectedComponent(), tab);
            } catch (IOException e) {
                AppLog.error("Save failed", e);
                JOptionPane.showMessageDialog(this, e.getMessage(), "Save failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public boolean closeCurrent() {
        EditorTab tab = currentTab();
        if (tab == null) {
            return true;
        }
        if (!tab.confirmClose()) {
            return false;
        }
        JPanel wrapper = (JPanel) tabs.getSelectedComponent();
        tabMap.remove(wrapper);
        tabs.remove(wrapper);
        if (tabs.getTabCount() == 0) {
            newUntitled();
        }
        return true;
    }

    public boolean closeAll() {
        while (tabs.getTabCount() > 0) {
            tabs.setSelectedIndex(0);
            EditorTab tab = currentTab();
            if (tab != null && !tab.confirmClose()) {
                return false;
            }
            JPanel wrapper = (JPanel) tabs.getComponentAt(0);
            tabMap.remove(wrapper);
            tabs.remove(0);
        }
        return true;
    }

    private JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(UiTheme.UI_FONT);
        button.setBackground(UiTheme.BG_HOVER);
        button.setForeground(UiTheme.TEXT);
        button.setFocusPainted(false);
        button.addActionListener(e -> action.run());
        return button;
    }

    private JFileChooser chooser() {
        JFileChooser chooser = new JFileChooser();
        if (workspace.rootFolder() != null) {
            chooser.setCurrentDirectory(workspace.rootFolder().toFile());
        } else {
            chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        }
        return chooser;
    }
}
