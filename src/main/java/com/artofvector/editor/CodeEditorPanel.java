package com.artofvector.editor;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import com.artofvector.log.AppLog;
import com.artofvector.ui.WorkbenchModule;
import com.artofvector.ui.theme.UiControls;
import com.artofvector.ui.theme.UiIcons;
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

        JPanel toolbar = UiControls.toolbar();
        toolbar.add(UiControls.toolButton("Open", UiIcons.Glyph.OPEN, this::openFileDialog));
        toolbar.add(UiControls.toolButton("Open Folder", UiIcons.Glyph.OPEN_FOLDER, () -> workspace.chooseRootFolder(this)));
        toolbar.add(UiControls.toolButton("Save", UiIcons.Glyph.SAVE, this::saveCurrent));
        toolbar.add(UiControls.toolButton("Save As", UiIcons.Glyph.SAVE, this::saveCurrentAs));
        toolbar.add(UiControls.toolButton("New", UiIcons.Glyph.NEW_FILE, this::newUntitled));

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

    public void applyFonts() {
        tabs.setFont(UiTheme.UI_FONT);
        for (EditorTab tab : tabMap.values()) {
            tab.applyFont();
        }
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
    public javax.swing.Icon tabIcon() {
        return UiIcons.of(UiIcons.Glyph.EDITOR, 16);
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
