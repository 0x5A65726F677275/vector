package com.artofvector.workflow.ui;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.artofvector.debugger.DebugService;
import com.artofvector.log.AppLog;
import com.artofvector.ui.WorkbenchModule;
import com.artofvector.ui.theme.UiControls;
import com.artofvector.ui.theme.UiIcons;
import com.artofvector.ui.theme.UiTheme;
import com.artofvector.workflow.canvas.WorkflowCanvas;
import com.artofvector.workflow.engine.WorkflowEngine;
import com.artofvector.workflow.io.WorkflowSerializer;
import com.artofvector.workflow.model.WorkflowGraph;
import com.artofvector.workflow.nodes.NodeType;
import com.artofvector.workflow.palette.NodePalette;
import com.artofvector.workspace.Workspace;

public final class WorkflowPanel extends JPanel implements WorkbenchModule {

    private final DebugService debugService;
    private final Workspace workspace;
    private final WorkflowGraph graph = new WorkflowGraph();
    private final WorkflowCanvas canvas = new WorkflowCanvas(graph);
    private final WorkflowEngine engine = new WorkflowEngine();
    private final WorkflowSerializer serializer = new WorkflowSerializer();

    public WorkflowPanel(DebugService debugService, Workspace workspace) {
        super(new BorderLayout());
        this.debugService = debugService;
        this.workspace = workspace;
        setBackground(UiTheme.BG_PANEL);

        canvas.setTransferHandler(new DropHandler());

        JPanel toolbar = UiControls.toolbar();
        toolbar.add(UiControls.primaryButton("Run", UiIcons.Glyph.PLAY, this::run));
        toolbar.add(UiControls.toolButton("Save", UiIcons.Glyph.SAVE, this::save));
        toolbar.add(UiControls.toolButton("Load", UiIcons.Glyph.LOAD, this::load));
        toolbar.add(UiControls.toolButton("Clear", UiIcons.Glyph.CLEAR, this::clear));
        toolbar.add(UiControls.toolButton("Reset View", UiIcons.Glyph.RESET, canvas::resetView));
        javax.swing.JLabel hint = new javax.swing.JLabel("  Drag Command → double-click to write the command → Run");
        hint.setForeground(UiTheme.TEXT_MUTED);
        hint.setFont(UiTheme.UI_FONT);
        toolbar.add(hint);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new NodePalette(), canvas);
        split.setDividerLocation(200);
        split.setBorder(null);

        add(toolbar, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    @Override
    public String tabTitle() {
        return "Workflow";
    }

    @Override
    public javax.swing.Icon tabIcon() {
        return UiIcons.of(UiIcons.Glyph.WORKFLOW, 16);
    }

    @Override
    public JPanel component() {
        return this;
    }

    private void run() {
        Thread worker = new Thread(() -> {
            try {
                engine.execute(graph, debugService, workspace.rootFolder());
            } catch (Exception e) {
                AppLog.error("Workflow failed", e);
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, e.getMessage(), "Workflow failed", JOptionPane.ERROR_MESSAGE));
            }
        }, "workflow-run");
        worker.setDaemon(true);
        worker.start();
    }

    private void save() {
        JFileChooser chooser = chooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().endsWith(".json")) {
                file = new File(file.getParentFile(), file.getName() + ".json");
            }
            try {
                serializer.save(graph, file.toPath());
                AppLog.info("Workflow saved to " + file);
            } catch (Exception e) {
                AppLog.error("Save workflow failed", e);
            }
        }
    }

    private void load() {
        JFileChooser chooser = chooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                WorkflowGraph loaded = serializer.load(chooser.getSelectedFile().toPath());
                graph.clear();
                loaded.nodes().forEach(graph::addNode);
                loaded.connections().forEach(graph::addConnection);
                canvas.repaint();
                AppLog.info("Workflow loaded from " + chooser.getSelectedFile());
            } catch (Exception e) {
                AppLog.error("Load workflow failed", e);
            }
        }
    }

    private void clear() {
        graph.clear();
        canvas.repaint();
    }

    private JFileChooser chooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Workflow JSON", "json"));
        if (workspace.rootFolder() != null) {
            chooser.setCurrentDirectory(workspace.rootFolder().toFile());
        }
        return chooser;
    }

    private final class DropHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            try {
                String name = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                NodeType type = NodeType.valueOf(name);
                Point drop = support.getDropLocation().getDropPoint();
                Point onCanvas = SwingUtilities.convertPoint(
                        (JComponent) support.getComponent(), drop, canvas);
                canvas.dropNode(type.create(), onCanvas);
                return true;
            } catch (Exception e) {
                AppLog.warn("Drop failed: " + e.getMessage());
                return false;
            }
        }
    }
}
