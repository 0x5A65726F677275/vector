package com.artofvector.debugger.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.artofvector.debugger.disasm.Disassembler;
import com.artofvector.debugger.disasm.Instruction;
import com.artofvector.debugger.engine.DebugEvent;
import com.artofvector.debugger.engine.DebugEventListener;
import com.artofvector.debugger.engine.DebugSession;
import com.artofvector.ui.theme.UiTheme;

public final class DisassemblyPanel extends JPanel implements DebugEventListener {

    private final DebugSession session;
    private final Disassembler disassembler;
    private final DefaultTableModel model;
    private final JTable table;
    private long currentRip;

    public DisassemblyPanel(DebugSession session, Disassembler disassembler) {
        super(new BorderLayout());
        this.session = session;
        this.disassembler = disassembler;
        setBackground(UiTheme.BG_PANEL);

        model = new DefaultTableModel(new Object[]{"Address", "Bytes", "Instruction"}, 0) {
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
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.getTableHeader().setBackground(UiTheme.BG_ELEVATED);
        table.getTableHeader().setForeground(UiTheme.TEXT_MUTED);
        table.getTableHeader().setFont(UiTheme.UI_FONT);
        table.setDefaultRenderer(Object.class, new RipRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        session.addListener(this);
    }

    public void refreshFrom(long address, int bytes) {
        try {
            byte[] data = session.readMemory(address, bytes);
            List<Instruction> insns = disassembler.disassemble(data, address);
            model.setRowCount(0);
            for (Instruction insn : insns) {
                model.addRow(new Object[]{
                        String.format("0x%016x", insn.address()),
                        toHex(insn.bytes()),
                        insn.text()
                });
            }
            highlightRip(session.getRegisters() == null ? address : session.getRegisters().rip());
        } catch (Exception e) {
            model.setRowCount(0);
            model.addRow(new Object[]{"-", "-", e.getMessage()});
        }
    }

    @Override
    public void onDebugEvent(DebugEvent event) {
        switch (event.type()) {
            case ATTACHED, STEPPED, BREAKPOINT_HIT, PAUSED, REGISTERS_CHANGED, MEMORY_CHANGED -> {
                long rip = event.registers() == null ? currentRip : event.registers().rip();
                currentRip = rip;
                refreshFrom(Math.max(0, rip - 32), 128);
            }
            default -> {
            }
        }
    }

    private void highlightRip(long rip) {
        currentRip = rip;
        String needle = String.format("0x%016x", rip);
        for (int i = 0; i < model.getRowCount(); i++) {
            if (needle.equals(model.getValueAt(i, 0))) {
                table.getSelectionModel().setSelectionInterval(i, i);
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
                break;
            }
        }
        table.repaint();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x ", b));
        }
        return sb.toString().trim();
    }

    private final class RipRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column
        ) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(UiTheme.MONO_SMALL);
            String addr = String.valueOf(model.getValueAt(row, 0));
            boolean current = addr.equals(String.format("0x%016x", currentRip));
            if (isSelected) {
                c.setBackground(UiTheme.BG_SELECTED);
                c.setForeground(UiTheme.TEXT);
            } else if (current) {
                c.setBackground(UiTheme.RIP_HIGHLIGHT);
                c.setForeground(UiTheme.ACCENT);
            } else {
                c.setBackground(UiTheme.BG_PANEL);
                c.setForeground(column == 2 ? UiTheme.TEXT : UiTheme.TEXT_MUTED);
            }
            return c;
        }
    }
}
