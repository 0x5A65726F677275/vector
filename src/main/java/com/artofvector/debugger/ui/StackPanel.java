package com.artofvector.debugger.ui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.artofvector.debugger.engine.DebugEvent;
import com.artofvector.debugger.engine.DebugEventListener;
import com.artofvector.debugger.engine.DebugSession;
import com.artofvector.debugger.engine.Registers;
import com.artofvector.ui.theme.UiTheme;

public final class StackPanel extends JPanel implements DebugEventListener {

    private final DebugSession session;
    private final DefaultTableModel model;

    public StackPanel(DebugSession session) {
        super(new BorderLayout());
        this.session = session;
        setBackground(UiTheme.BG_PANEL);

        model = new DefaultTableModel(new Object[]{"Address", "Qword", "ASCII"}, 0) {
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
    }

    public void refresh(Registers registers) {
        model.setRowCount(0);
        if (registers == null || !session.isAttached()) {
            return;
        }
        long rsp = registers.rsp();
        try {
            byte[] dump = session.readMemory(rsp, 8 * 16);
            for (int i = 0; i < dump.length; i += 8) {
                long qword = 0;
                StringBuilder ascii = new StringBuilder();
                for (int b = 0; b < 8 && i + b < dump.length; b++) {
                    int v = dump[i + b] & 0xff;
                    qword |= ((long) v) << (8 * b);
                    ascii.append(v >= 32 && v < 127 ? (char) v : '.');
                }
                model.addRow(new Object[]{
                        String.format("0x%016x", rsp + i),
                        String.format("0x%016x", qword),
                        ascii.toString()
                });
            }
        } catch (Exception e) {
            model.setRowCount(0);
            model.addRow(new Object[]{"-", e.getMessage(), ""});
        }
    }

    @Override
    public void onDebugEvent(DebugEvent event) {
        if (event.registers() != null) {
            refresh(event.registers());
        }
    }
}
