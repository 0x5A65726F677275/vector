package com.artofvector.debugger.ui;

import java.awt.BorderLayout;
import java.util.Collection;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.artofvector.debugger.engine.Breakpoint;
import com.artofvector.debugger.engine.DebugEvent;
import com.artofvector.debugger.engine.DebugEventListener;
import com.artofvector.debugger.engine.DebugSession;
import com.artofvector.ui.theme.UiTheme;

public final class BreakpointListPanel extends JPanel implements DebugEventListener {

    private final DebugSession session;
    private final DefaultTableModel model;

    public BreakpointListPanel(DebugSession session) {
        super(new BorderLayout());
        this.session = session;
        setBackground(UiTheme.BG_PANEL);

        model = new DefaultTableModel(new Object[]{"Address", "Orig", "On"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setFont(UiTheme.MONO_SMALL);
        table.setRowHeight(20);
        table.setBackground(UiTheme.BG_PANEL);
        table.setForeground(UiTheme.TEXT);
        table.setSelectionBackground(UiTheme.BG_SELECTED);
        table.setGridColor(UiTheme.BORDER);
        table.getTableHeader().setBackground(UiTheme.BG_ELEVATED);
        table.getTableHeader().setForeground(UiTheme.TEXT_MUTED);

        add(new JScrollPane(table), BorderLayout.CENTER);
        session.addListener(this);
        reload();
    }

    public void reload() {
        model.setRowCount(0);
        Collection<Breakpoint> bps = session.getBreakpoints();
        for (Breakpoint bp : bps) {
            model.addRow(new Object[]{
                    String.format("0x%016x", bp.address()),
                    String.format("%02x", bp.originalByte() & 0xff),
                    bp.enabled() ? "yes" : "no"
            });
        }
    }

    @Override
    public void onDebugEvent(DebugEvent event) {
        switch (event.type()) {
            case MEMORY_CHANGED, BREAKPOINT_HIT, ATTACHED, DETACHED, STOPPED -> reload();
            default -> {
            }
        }
    }
}
