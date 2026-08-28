package com.artofvector.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;

import com.artofvector.Version;
import com.artofvector.debugger.DebugService;
import com.artofvector.debugger.engine.DebugEvent;
import com.artofvector.debugger.ui.DebuggerSidePanel;
import com.artofvector.editor.BreakpointGutterController;
import com.artofvector.editor.CodeEditorPanel;
import com.artofvector.editor.FileTreePanel;
import com.artofvector.log.AppLog;
import com.artofvector.ui.theme.UiTheme;
import com.artofvector.workspace.Workspace;

public final class MainWindow {

    private final JFrame frame;
    private final CodeEditorPanel editor;
    private final StatusBar statusBar = new StatusBar();

    public MainWindow(
            Workspace workspace,
            DebugService debugService,
            List<WorkbenchModule> modules,
            ConsolePanel console,
            CodeEditorPanel editor,
            FileTreePanel fileTree
    ) {
        this.editor = editor;
        frame = new JFrame(Version.NAME + " " + Version.NUMBER);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1100, 720));
        frame.getContentPane().setBackground(UiTheme.BG_ROOT);

        editor.setGutterController(new BreakpointGutterController(debugService.session(), debugService.mapper()));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UiTheme.UI_FONT);
        tabs.setBackground(UiTheme.BG_ROOT);
        for (WorkbenchModule module : modules) {
            tabs.addTab(module.tabTitle(), module.component());
        }
        tabs.addChangeListener((ChangeEvent e) -> {
            int index = tabs.getSelectedIndex();
            if (index >= 0 && index < modules.size()) {
                modules.get(index).onActivated();
                statusBar.setLeft(modules.get(index).tabTitle());
            }
        });

        workspace.addFileOpenListener(path -> tabs.setSelectedComponent(editor));

        debugService.session().addListener((DebugEvent event) -> {
            switch (event.type()) {
                case ATTACHED -> statusBar.setRight("attached @ 0x" + Long.toHexString(event.address()));
                case BREAKPOINT_HIT -> statusBar.setRight("bp 0x" + Long.toHexString(event.address()));
                case STEPPED -> statusBar.setRight("rip 0x" + Long.toHexString(event.address()));
                case DETACHED, STOPPED -> statusBar.setRight("idle");
                default -> {
                }
            }
        });

        fileTree.setOpenHandler(workspace::openFile);
        workspace.addFolderListener(fileTree::setRoot);

        DebuggerSidePanel side = new DebuggerSidePanel(debugService.session());

        JSplitPane centerRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabs, side);
        centerRight.setResizeWeight(0.82);
        centerRight.setBorder(null);

        JSplitPane leftCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, fileTree, centerRight);
        leftCenter.setDividerLocation(240);
        leftCenter.setBorder(null);

        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, leftCenter, console);
        vertical.setResizeWeight(0.82);
        vertical.setBorder(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiTheme.BG_ROOT);
        root.add(vertical, BorderLayout.CENTER);
        root.add(statusBar, BorderLayout.SOUTH);

        frame.setJMenuBar(new MainMenuBar(workspace, editor, console::clear, this::requestClose));
        frame.setContentPane(root);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                requestClose();
            }
        });

        statusBar.setLeft("Editor");
        statusBar.setRight("idle");
        AppLog.info(Version.NAME + " " + Version.NUMBER + " started.");
    }

    public void show() {
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void requestClose() {
        if (editor.closeAll()) {
            frame.dispose();
            System.exit(0);
        }
    }
}
