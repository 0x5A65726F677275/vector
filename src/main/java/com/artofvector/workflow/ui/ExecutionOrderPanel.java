package com.artofvector.workflow.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import com.artofvector.ui.theme.UiTheme;
import com.artofvector.workflow.canvas.WorkflowCanvas;
import com.artofvector.workflow.model.WorkflowGraph;
import com.artofvector.workflow.model.WorkflowNode;

/**
 * Lets the user set which unconnected (or simultaneously ready) node runs first.
 * Wires remain hard dependencies and always win over this list.
 */
public final class ExecutionOrderPanel extends JPanel {

    private final WorkflowGraph graph;
    private final WorkflowCanvas canvas;
    private final DefaultListModel<WorkflowNode> model = new DefaultListModel<>();
    private final JList<WorkflowNode> list = new JList<>(model);
    private boolean syncing;

    public ExecutionOrderPanel(WorkflowGraph graph, WorkflowCanvas canvas) {
        super(new BorderLayout());
        this.graph = graph;
        this.canvas = canvas;
        setBackground(UiTheme.BG_PANEL);
        setPreferredSize(new Dimension(200, 280));

        JLabel title = new JLabel("Run order");
        title.setForeground(UiTheme.TEXT_MUTED);
        title.setFont(UiTheme.UI_FONT_BOLD);
        title.setBorder(new EmptyBorder(10, 12, 2, 12));

        JLabel hint = new JLabel("<html>Wires still run first.<br>Use ↑ ↓ for the rest.</html>");
        hint.setForeground(UiTheme.TEXT_DIM);
        hint.setFont(UiTheme.UI_FONT);
        hint.setBorder(new EmptyBorder(0, 12, 8, 12));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.NORTH);
        header.add(hint, BorderLayout.CENTER);

        list.setBackground(UiTheme.BG_PANEL);
        list.setForeground(UiTheme.TEXT);
        list.setFont(UiTheme.UI_FONT);
        list.setFixedCellHeight(36);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new Renderer());
        list.addListSelectionListener(e -> {
            if (syncing || e.getValueIsAdjusting()) {
                return;
            }
            WorkflowNode node = list.getSelectedValue();
            if (node != null) {
                canvas.select(node, false);
            }
        });

        JButton up = moveButton("↑", -1);
        JButton down = moveButton("↓", 1);
        JButton onOff = new JButton("On / Off");
        onOff.setFont(UiTheme.UI_FONT_BOLD);
        onOff.setBackground(UiTheme.BG_HOVER);
        onOff.setForeground(UiTheme.TEXT);
        onOff.setFocusPainted(false);
        onOff.setBorderPainted(false);
        onOff.setOpaque(true);
        onOff.addActionListener(e -> toggleSelected());
        JPanel buttons = new JPanel(new GridLayout(1, 3, 6, 0));
        buttons.setOpaque(false);
        buttons.setBorder(new EmptyBorder(8, 10, 10, 10));
        buttons.add(up);
        buttons.add(down);
        buttons.add(onOff);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UiTheme.BG_PANEL);

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        canvas.addChangeListener(this::refresh);
        refresh();
    }

    public void refresh() {
        WorkflowNode selected = canvas.selected();
        syncing = true;
        try {
            model.clear();
            for (WorkflowNode node : graph.nodesInRunOrder()) {
                model.addElement(node);
            }
            if (selected != null) {
                list.setSelectedValue(selected, true);
            } else {
                list.clearSelection();
            }
        } finally {
            syncing = false;
        }
    }

    private void move(int delta) {
        WorkflowNode node = list.getSelectedValue();
        if (node == null) {
            node = canvas.selected();
        }
        if (node == null) {
            return;
        }
        graph.moveInRunOrder(node, delta);
        canvas.repaint();
        canvas.notifyChanged();
        syncing = true;
        try {
            list.setSelectedValue(node, true);
        } finally {
            syncing = false;
        }
    }

    private void toggleSelected() {
        WorkflowNode node = list.getSelectedValue();
        if (node == null) {
            node = canvas.selected();
        }
        if (node == null) {
            return;
        }
        node.toggleEnabled();
        canvas.repaint();
        canvas.notifyChanged();
        syncing = true;
        try {
            list.setSelectedValue(node, true);
        } finally {
            syncing = false;
        }
    }

    private JButton moveButton(String label, int delta) {
        JButton button = new JButton(label);
        button.setFont(UiTheme.UI_FONT_BOLD);
        button.setBackground(UiTheme.BG_HOVER);
        button.setForeground(UiTheme.TEXT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.addActionListener(e -> move(delta));
        return button;
    }

    private static final class Renderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus
        ) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof WorkflowNode node) {
                label.setText(node.runOrder() + "  " + node.title() + (node.enabled() ? "" : "  off"));
                label.setBorder(new EmptyBorder(6, 12, 6, 12));
                label.setForeground(node.enabled() ? UiTheme.TEXT : UiTheme.TEXT_DIM);
                label.setBackground(isSelected ? UiTheme.BG_SELECTED : UiTheme.BG_PANEL);
                label.setOpaque(true);
            }
            return label;
        }
    }
}
