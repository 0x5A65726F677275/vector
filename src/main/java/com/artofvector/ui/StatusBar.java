package com.artofvector.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.artofvector.ui.theme.UiIcons;
import com.artofvector.ui.theme.UiTheme;

public final class StatusBar extends JPanel {

    private final JLabel left = new JLabel("Ready");
    private final JLabel folder = new JLabel("");
    private final JLabel right = new JLabel("");

    public StatusBar() {
        super(new BorderLayout());
        setBackground(UiTheme.BG_ELEVATED);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                new EmptyBorder(5, 12, 5, 12)));

        left.setForeground(UiTheme.TEXT_MUTED);
        left.setFont(UiTheme.UI_FONT);
        left.setIcon(UiIcons.of(UiIcons.Glyph.EDITOR, 14));
        folder.setForeground(UiTheme.TEXT);
        folder.setFont(UiTheme.UI_FONT);
        folder.setIcon(UiIcons.of(UiIcons.Glyph.FOLDER, 14));
        right.setForeground(UiTheme.ACCENT);
        right.setFont(UiTheme.UI_FONT);

        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightWrap.setOpaque(false);
        rightWrap.add(right);

        add(left, BorderLayout.WEST);
        add(folder, BorderLayout.CENTER);
        add(rightWrap, BorderLayout.EAST);
    }

    public void setLeft(String text) {
        left.setText(text);
        if ("Debugger".equals(text)) {
            left.setIcon(UiIcons.of(UiIcons.Glyph.DEBUGGER, 14));
        } else if ("Workflow".equals(text)) {
            left.setIcon(UiIcons.of(UiIcons.Glyph.WORKFLOW, 14));
        } else {
            left.setIcon(UiIcons.of(UiIcons.Glyph.EDITOR, 14));
        }
    }

    public void setFolder(String text) {
        folder.setText(text == null || text.isBlank() ? "" : "  " + text);
        folder.setToolTipText(text);
    }

    public void setRight(String text) {
        right.setText(text);
    }
}
