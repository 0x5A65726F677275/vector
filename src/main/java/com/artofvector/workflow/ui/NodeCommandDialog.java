package com.artofvector.workflow.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import com.artofvector.ui.theme.UiTheme;
import com.artofvector.workflow.model.WorkflowNode;

/**
 * Double-click editor for a node's command (or other properties).
 */
public final class NodeCommandDialog extends JDialog {

    private boolean accepted;

    private NodeCommandDialog(Frame owner, WorkflowNode node) {
        super(owner, node.title(), true);
        setBackground(UiTheme.BG_PANEL);
        getContentPane().setBackground(UiTheme.BG_PANEL);

        JLabel hint = new JLabel("<html>Write the command to run. Connected order is respected. "
                + "Use <b>{in}</b> for the previous node's stdout.</html>");
        hint.setForeground(UiTheme.TEXT_MUTED);
        hint.setFont(UiTheme.UI_FONT);
        hint.setBorder(new EmptyBorder(0, 0, 8, 0));

        JTextArea area = new JTextArea(propertyValue(node), 6, 48);
        area.setFont(UiTheme.MONO_FONT);
        area.setBackground(UiTheme.BG_INPUT);
        area.setForeground(UiTheme.TEXT);
        area.setCaretColor(UiTheme.TEXT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(UiTheme.panelBorder());

        JButton ok = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        ok.setFont(UiTheme.UI_FONT);
        cancel.setFont(UiTheme.UI_FONT);
        ok.addActionListener(e -> {
            String key = node.properties().containsKey("command") ? "command" : firstKey(node);
            if (key != null) {
                node.setProperty(key, area.getText());
            }
            accepted = true;
            dispose();
        });
        cancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(ok);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(UiTheme.BG_PANEL);
        body.setBorder(new EmptyBorder(12, 14, 12, 14));
        body.add(hint, BorderLayout.NORTH);
        body.add(scroll, BorderLayout.CENTER);
        body.add(buttons, BorderLayout.SOUTH);

        setContentPane(body);
        setPreferredSize(new Dimension(560, 280));
        pack();
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(ok);
    }

    public static boolean edit(Frame owner, WorkflowNode node) {
        NodeCommandDialog dialog = new NodeCommandDialog(owner, node);
        dialog.setVisible(true);
        return dialog.accepted;
    }

    private static String propertyValue(WorkflowNode node) {
        if (node.properties().containsKey("command")) {
            return node.property("command", "");
        }
        if (node.properties().isEmpty()) {
            return "";
        }
        String key = node.properties().keySet().iterator().next();
        return node.property(key, "");
    }

    private static String firstKey(WorkflowNode node) {
        return node.properties().isEmpty() ? null : node.properties().keySet().iterator().next();
    }
}
