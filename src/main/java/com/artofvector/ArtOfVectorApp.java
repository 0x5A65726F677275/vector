package com.artofvector;

import java.nio.file.Path;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.artofvector.debugger.DebugService;
import com.artofvector.debugger.ui.DebuggerPanel;
import com.artofvector.editor.CodeEditorPanel;
import com.artofvector.editor.FileTreePanel;
import com.artofvector.log.AppLog;
import com.artofvector.ui.ConsolePanel;
import com.artofvector.ui.MainWindow;
import com.artofvector.ui.WorkbenchModule;
import com.artofvector.ui.theme.UiTheme;
import com.artofvector.workflow.ui.WorkflowPanel;
import com.artofvector.workspace.Workspace;

public final class ArtOfVectorApp {

    private ArtOfVectorApp() {
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        UiTheme.install();
        SwingUtilities.invokeLater(ArtOfVectorApp::start);
    }

    private static void start() {
        try {
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            Workspace workspace = new Workspace();
            DebugService debugService = new DebugService();
            ConsolePanel console = new ConsolePanel();
            CodeEditorPanel editor = new CodeEditorPanel(workspace);
            FileTreePanel fileTree = new FileTreePanel();

            List<WorkbenchModule> modules = List.of(
                    editor,
                    new DebuggerPanel(debugService),
                    new WorkflowPanel(debugService)
            );

            Path home = Path.of(System.getProperty("user.dir"));
            workspace.setRootFolder(home);

            MainWindow window = new MainWindow(workspace, debugService, modules, console, editor, fileTree);
            window.show();
        } catch (Exception e) {
            AppLog.error("Failed to start " + Version.NAME, e);
            e.printStackTrace();
        }
    }
}
