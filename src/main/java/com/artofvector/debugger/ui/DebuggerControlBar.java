package com.artofvector.debugger.ui;

import javax.swing.JPanel;

import com.artofvector.debugger.engine.DebugException;
import com.artofvector.debugger.engine.DebugSession;
import com.artofvector.log.AppLog;
import com.artofvector.ui.theme.UiControls;
import com.artofvector.ui.theme.UiIcons;

public final class DebuggerControlBar extends JPanel {

    private final DebugSession session;

    public DebuggerControlBar(DebugSession session) {
        this.session = session;
        JPanel bar = UiControls.toolbar();
        setLayout(new java.awt.BorderLayout());
        setBackground(bar.getBackground());
        bar.add(UiControls.toolButton("Attach", UiIcons.Glyph.ATTACH, this::attach));
        bar.add(UiControls.primaryButton("Run", UiIcons.Glyph.PLAY, this::run));
        bar.add(UiControls.toolButton("Pause", UiIcons.Glyph.PAUSE, this::pause));
        bar.add(UiControls.toolButton("Step Into", UiIcons.Glyph.STEP_INTO, this::stepInto));
        bar.add(UiControls.toolButton("Step Over", UiIcons.Glyph.STEP_OVER, this::stepOver));
        bar.add(UiControls.dangerButton("Stop", UiIcons.Glyph.STOP, this::stop));
        add(bar, java.awt.BorderLayout.CENTER);
    }

    private void attach() {
        AttachProcessDialog.show(this).ifPresent(choice -> {
            try {
                if (choice.simulated()) {
                    session.attachSimulated();
                } else {
                    session.attach((int) choice.pid());
                }
            } catch (DebugException e) {
                AppLog.error("Attach failed", e);
                javax.swing.JOptionPane.showMessageDialog(
                        this, e.getMessage(), "Attach failed", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
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
        } catch (RuntimeException ex) {
            AppLog.error(label + " failed", ex);
        }
    }

    @FunctionalInterface
    private interface SessionAction {
        void run() throws DebugException;
    }
}
