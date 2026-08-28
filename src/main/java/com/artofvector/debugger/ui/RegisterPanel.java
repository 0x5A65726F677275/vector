package com.artofvector.debugger.ui;

import java.awt.BorderLayout;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.artofvector.debugger.engine.DebugEvent;
import com.artofvector.debugger.engine.DebugEventListener;
import com.artofvector.debugger.engine.DebugSession;
import com.artofvector.debugger.engine.Registers;
import com.artofvector.ui.theme.UiTheme;

public final class RegisterPanel extends JPanel implements DebugEventListener {

    private final DefaultTableModel model;
    private final JTable table;

    public RegisterPanel(DebugSession session) {
        super(new BorderLayout());
        setBackground(UiTheme.BG_PANEL);

        model = new DefaultTableModel(new Object[]{"Reg", "Hex", "Dec"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setFont(UiTheme.MONO_SMALL);
        table.setRowHeight(20);
        table.setBackground(UiTheme.BG_PANEL);
        table.setForeground(UiTheme.TEXT);
        table.setSelectionBackground(UiTheme.BG_SELECTED);
        table.setGridColor(UiTheme.BORDER);
        table.getTableHeader().setBackground(UiTheme.BG_ELEVATED);
        table.getTableHeader().setForeground(UiTheme.TEXT_MUTED);
        table.getTableHeader().setFont(UiTheme.UI_FONT);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        session.addListener(this);
        show(session.getRegisters());
    }

    public void show(Registers registers) {
        model.setRowCount(0);
        if (registers == null) {
            return;
        }
        for (Map.Entry<String, Long> entry : registers.asMap().entrySet()) {
            long value = entry.getValue();
            model.addRow(new Object[]{
                    entry.getKey(),
                    String.format("0x%016x", value),
                    Long.toUnsignedString(value)
            });
        }
    }

    @Override
    public void onDebugEvent(DebugEvent event) {
        if (event.registers() != null) {
            show(event.registers());
        }
    }
}
