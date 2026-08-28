package com.artofvector.workflow.palette;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;

import com.artofvector.ui.theme.UiTheme;
import com.artofvector.workflow.nodes.NodeType;

public final class NodePalette extends JPanel {

    public static final DataFlavor NODE_TYPE_FLAVOR = new DataFlavor(String.class, "NodeType");

    public NodePalette() {
        super(new BorderLayout());
        setBackground(UiTheme.BG_PANEL);
        setPreferredSize(new Dimension(200, 400));

        JLabel title = new JLabel("Nodes");
        title.setForeground(UiTheme.TEXT_MUTED);
        title.setFont(UiTheme.UI_FONT_BOLD);
        title.setBorder(new EmptyBorder(8, 12, 6, 12));

        DefaultListModel<NodeType> model = new DefaultListModel<>();
        for (NodeType type : NodeType.values()) {
            model.addElement(type);
        }
        JList<NodeType> list = new JList<>(model);
        list.setBackground(UiTheme.BG_PANEL);
        list.setForeground(UiTheme.TEXT);
        list.setFont(UiTheme.UI_FONT);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new Renderer());
        list.setDragEnabled(true);
        list.setTransferHandler(new TransferHandler() {
            @Override
            protected Transferable createTransferable(JComponent c) {
                NodeType type = list.getSelectedValue();
                return type == null ? null : new StringSelection(type.name());
            }

            @Override
            public int getSourceActions(JComponent c) {
                return COPY;
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        add(title, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    private static final class Renderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus
        ) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof NodeType type) {
                label.setText(type.title());
                label.setBorder(new EmptyBorder(8, 12, 8, 12));
                label.setForeground(UiTheme.TEXT);
                if (isSelected) {
                    label.setBackground(UiTheme.BG_SELECTED);
                } else {
                    label.setBackground(UiTheme.BG_PANEL);
                }
                label.setOpaque(true);
                label.setIcon(new ColorDot(type.accent()));
            }
            return label;
        }
    }

    private static final class ColorDot implements javax.swing.Icon {
        private final Color color;

        private ColorDot(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, java.awt.Graphics g, int x, int y) {
            g.setColor(color);
            g.fillOval(x, y + 2, 10, 10);
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 14;
        }
    }
}
