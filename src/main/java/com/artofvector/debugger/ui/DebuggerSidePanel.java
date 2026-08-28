package com.artofvector.debugger.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.artofvector.debugger.engine.DebugSession;
import com.artofvector.ui.theme.UiControls;
import com.artofvector.ui.theme.UiIcons;
import com.artofvector.ui.theme.UiTheme;

public final class DebuggerSidePanel extends JPanel {

    public DebuggerSidePanel(DebugSession session) {
        super(new BorderLayout());
        setBackground(UiTheme.BG_PANEL);
        setPreferredSize(new Dimension(280, 400));

        RegisterPanel registers = new RegisterPanel(session);
        BreakpointListPanel breakpoints = new BreakpointListPanel(session);

        JPanel top = titled("Registers", UiIcons.Glyph.DUMP, registers);
        JPanel bottom = titled("Breakpoints", UiIcons.Glyph.BREAKPOINT, breakpoints);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        split.setResizeWeight(0.65);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    private static JPanel titled(String title, UiIcons.Glyph glyph, JPanel content) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(UiTheme.BG_PANEL);
        wrap.add(UiControls.section(title, glyph), BorderLayout.NORTH);
        wrap.add(content, BorderLayout.CENTER);
        return wrap;
    }
}
