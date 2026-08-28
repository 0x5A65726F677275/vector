package com.artofvector.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.artofvector.ui.theme.UiControls;
import com.artofvector.ui.theme.UiIcons;
import com.artofvector.ui.theme.UiTheme;

/**
 * Filesystem tree used as the workbench's left navigation pane.
 */
public final class FileTreePanel extends JPanel {

    private final JTree tree;
    private final DefaultTreeModel model;
    private final JLabel folderLabel = new JLabel("No folder open");
    private Consumer<Path> openHandler = path -> {
    };
    private Runnable folderChooser = () -> {
    };

    public FileTreePanel() {
        super(new BorderLayout());
        setBackground(UiTheme.BG_PANEL);

        JPanel toolbar = UiControls.toolbar();
        toolbar.add(UiControls.toolButton("Open Folder", UiIcons.Glyph.OPEN_FOLDER, () -> folderChooser.run()));
        folderLabel.setForeground(UiTheme.TEXT_MUTED);
        folderLabel.setFont(UiTheme.UI_FONT);
        folderLabel.setIcon(UiIcons.of(UiIcons.Glyph.FOLDER, 14));
        folderLabel.setBorder(new EmptyBorder(0, 8, 0, 4));
        toolbar.add(folderLabel);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("No folder open");
        model = new DefaultTreeModel(root);
        tree = new JTree(model);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setBackground(UiTheme.BG_PANEL);
        tree.setForeground(UiTheme.TEXT);
        tree.setFont(UiTheme.UI_FONT);
        tree.setRowHeight(Math.max(22, UiTheme.UI_FONT.getSize() + 8));
        tree.setCellRenderer(new FileRenderer());

        tree.addTreeSelectionListener(this::onSelect);
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Path path = selectedPath();
                    if (path != null && path.toFile().isFile()) {
                        openHandler.accept(path);
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tree);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UiTheme.BG_PANEL);
        add(toolbar, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    public void setOpenHandler(Consumer<Path> openHandler) {
        this.openHandler = openHandler;
    }

    public void setFolderChooser(Runnable folderChooser) {
        this.folderChooser = folderChooser;
    }

    public void setRoot(Path folder) {
        if (folder == null) {
            folderLabel.setText("No folder open");
            folderLabel.setIcon(UiIcons.of(UiIcons.Glyph.FOLDER, 14, UiTheme.TEXT_DIM));
            folderLabel.setToolTipText(null);
            model.setRoot(new DefaultMutableTreeNode("No folder open"));
            return;
        }
        folderLabel.setText(folder.getFileName() == null ? folder.toString() : folder.getFileName().toString());
        folderLabel.setIcon(UiIcons.of(UiIcons.Glyph.FOLDER, 14));
        folderLabel.setToolTipText(folder.toAbsolutePath().toString());
        FileNode root = new FileNode(folder.toFile());
        loadChildren(root);
        model.setRoot(root);
        tree.expandPath(new TreePath(root.getPath()));
    }

    public Path selectedPath() {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            return null;
        }
        Object node = path.getLastPathComponent();
        if (node instanceof FileNode fileNode) {
            return fileNode.file.toPath();
        }
        return null;
    }

    private void onSelect(TreeSelectionEvent event) {
        TreePath path = event.getPath();
        if (path == null) {
            return;
        }
        Object node = path.getLastPathComponent();
        if (node instanceof FileNode fileNode && fileNode.getChildCount() == 0 && fileNode.file.isDirectory()) {
            loadChildren(fileNode);
            model.reload(fileNode);
        }
    }

    private void loadChildren(FileNode node) {
        node.removeAllChildren();
        File[] children = node.file.listFiles();
        if (children == null) {
            return;
        }
        java.util.Arrays.sort(children, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) {
                return -1;
            }
            if (!a.isDirectory() && b.isDirectory()) {
                return 1;
            }
            return a.getName().compareToIgnoreCase(b.getName());
        });
        for (File child : children) {
            if (child.isHidden()) {
                continue;
            }
            FileNode childNode = new FileNode(child);
            node.add(childNode);
        }
    }

    private static final class FileNode extends DefaultMutableTreeNode {
        private final File file;

        private FileNode(File file) {
            super(file.getName());
            this.file = file;
        }
    }

    private static final class FileRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(
                JTree tree, Object value, boolean selected, boolean expanded,
                boolean leaf, int row, boolean hasFocus
        ) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            setBackgroundNonSelectionColor(UiTheme.BG_PANEL);
            setBackgroundSelectionColor(UiTheme.BG_SELECTED);
            setTextNonSelectionColor(UiTheme.TEXT);
            setTextSelectionColor(UiTheme.TEXT);
            setBorderSelectionColor(UiTheme.BG_SELECTED);
            setFont(tree.getFont());
            if (value instanceof FileNode fileNode) {
                setIcon(UiIcons.forFile(fileNode.file));
                setClosedIcon(getIcon());
                setOpenIcon(getIcon());
                setLeafIcon(getIcon());
                setText(fileNode.file.getName());
            } else {
                setIcon(UiIcons.of(UiIcons.Glyph.FOLDER, 16, UiTheme.TEXT_DIM));
            }
            return this;
        }
    }
}
