package com.artofvector.debugger.ui;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.artofvector.debugger.engine.DebugException;
import com.artofvector.debugger.engine.DebugSession;
import com.artofvector.log.AppLog;
import com.artofvector.ui.theme.UiTheme;

public final class DebuggerControlBar extends JPanel {

    private final DebugSession session;

    public DebuggerControlBar(DebugSession session) {
        super(new FlowLayout(FlowLayout.LEFT, 8, 6));
        this.session = session;
        setBackground(UiTheme.BG_ELEVATED);

        add(button("Attach", this::attach));
        add(button("Run", this::run));
        add(button("Pause", this::pause));
        add(button("Step Into", this::stepInto));
        add(button("Step Over", this::stepOver));
        add(button("Stop", this::stop));
    }

    private JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(UiTheme.UI_FONT);
        button.setBackground(UiTheme.BG_HOVER);
        button.setForeground(UiTheme.TEXT);
        button.setFocusPainted(false);
        button.addActionListener(e -> {
            try {
                action.run();
            } catch (RuntimeException ex) {
                AppLog.error(text + " failed", ex);
            }
        });
        return button;
    }

    private void attach() {
        String input = JOptionPane.showInputDialog(
                this,
                "Process id to attach (leave empty to use the simulated target):",
                "Attach",
                JOptionPane.QUESTION_MESSAGE
        );
        if (input == null) {
            return;
        }
        try {
            String trimmed = input.trim();
            if (trimmed.isEmpty() || trimmed.equals("0")) {
                session.attachSimulated();
            } else {
                session.attach(Integer.parseInt(trimmed));
            }
        } catch (NumberFormatException e) {
            AppLog.warn("PID must be an integer");
        } catch (DebugException e) {
            AppLog.error("Attach failed", e);
            JOptionPane.showMessageDialog(this, e.getMessage(), "Attach failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void run() {
        wrap("Run", session::cont);
    }

    private void pause() {
        wrap("Pause", session::pause);
    }

    private void stepInto() {
        wrap("Step Into", session::stepInto);
    }

    private void stepOver() {
        wrap("Step Over", session::stepOver);
    }

    private void stop() {
        wrap("Stop", session::stop);
    }

    private void wrap(String label, SessionAction action) {
        try {
            action.run();
        } catch (DebugException e) {
            AppLog.error(label + " failed", e);
        }
    }

    @FunctionalInterface
    private interface SessionAction {
        void run() throws DebugException;
    }
}
