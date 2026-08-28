package com.artofvector.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.artofvector.ui.theme.UiTheme;

public final class StatusBar extends JPanel {

    private final JLabel left = new JLabel("Ready");
    private final JLabel right = new JLabel("");

    public StatusBar() {
        super(new BorderLayout());
        setBackground(UiTheme.BG_ELEVATED);
        setBorder(new EmptyBorder(4, 10, 4, 10));
        setPreferredSize(new Dimension(100, 26));

        left.setForeground(UiTheme.TEXT_MUTED);
        left.setFont(UiTheme.UI_FONT);
        right.setForeground(UiTheme.ACCENT);
        right.setFont(UiTheme.UI_FONT);

        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightWrap.setOpaque(false);
        rightWrap.add(right);

        add(left, BorderLayout.WEST);
        add(rightWrap, BorderLayout.EAST);
    }

    public void setLeft(String text) {
        left.setText(text);
    }

    public void setRight(String text) {
        right.setText(text);
    }
}
