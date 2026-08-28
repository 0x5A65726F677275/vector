package com.artofvector.debugger.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;

import com.artofvector.debugger.engine.DebugSession;
import com.artofvector.ui.theme.UiTheme;

public final class DebuggerSidePanel extends JPanel {

    public DebuggerSidePanel(DebugSession session) {
        super(new BorderLayout());
        setBackground(UiTheme.BG_PANEL);
        setPreferredSize(new Dimension(280, 400));

        RegisterPanel registers = new RegisterPanel(session);
        BreakpointListPanel breakpoints = new BreakpointListPanel(session);

        JPanel top = titled("Registers", registers);
        JPanel bottom = titled("Breakpoints", breakpoints);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        split.setResizeWeight(0.65);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    private static JPanel titled(String title, JPanel content) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(UiTheme.BG_PANEL);
        JLabel label = new JLabel(title);
        label.setForeground(UiTheme.TEXT_MUTED);
        label.setFont(UiTheme.UI_FONT_BOLD);
        label.setBorder(new EmptyBorder(6, 10, 4, 10));
        wrap.add(label, BorderLayout.NORTH);
        wrap.add(content, BorderLayout.CENTER);
        return wrap;
    }
}
