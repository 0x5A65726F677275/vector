package com.artofvector.debugger.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.artofvector.debugger.engine.DebugEvent;
import com.artofvector.debugger.engine.DebugEventListener;
import com.artofvector.debugger.engine.DebugSession;
import com.artofvector.log.AppLog;
import com.artofvector.ui.theme.UiControls;
import com.artofvector.ui.theme.UiIcons;
import com.artofvector.ui.theme.UiTheme;

/**
 * Hex + ASCII memory dump, painted with Java2D so columns stay aligned under zoomed fonts.
 */
public final class HexView extends JPanel implements DebugEventListener {

    private final DebugSession session;
    private final JTextField addressField = new JTextField("0x401000", 12);
    private final DumpCanvas canvas = new DumpCanvas();
    private long baseAddress = 0x401000;
    private byte[] data = new byte[256];

    public HexView(DebugSession session) {
        super(new BorderLayout());
        this.session = session;
        setBackground(UiTheme.BG_PANEL);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(UiTheme.BG_ELEVATED);
        JLabel label = new JLabel("Address", UiIcons.of(UiIcons.Glyph.HEX, 14), JLabel.LEFT);
        label.setIconTextGap(8);
        label.setForeground(UiTheme.TEXT_MUTED);
        label.setFont(UiTheme.UI_FONT);
        JButton go = UiControls.toolButton("Dump", UiIcons.Glyph.DUMP, this::dumpFromField);
        bar.add(label);
        bar.add(addressField);
        bar.add(go);

        JScrollPane scroll = new JScrollPane(canvas);
        scroll.getViewport().setBackground(UiTheme.BG_INPUT);
        scroll.setBorder(null);

        add(bar, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        session.addListener(this);
    }

    public void dump(long address, int size) {
        baseAddress = address;
        addressField.setText(String.format("0x%x", address));
        try {
            data = session.readMemory(address, size);
        } catch (Exception e) {
            data = new byte[0];
            AppLog.warn("Hex dump failed: " + e.getMessage());
        }
        canvas.revalidate();
        canvas.repaint();
    }

    @Override
    public void onDebugEvent(DebugEvent event) {
        switch (event.type()) {
            case ATTACHED, MEMORY_CHANGED, STEPPED, BREAKPOINT_HIT ->
                    SwingUtilities.invokeLater(() -> dump(baseAddress, Math.max(256, data.length)));
            default -> {
            }
        }
    }

    private void dumpFromField() {
        try {
            String text = addressField.getText().trim();
            long address = text.startsWith("0x") || text.startsWith("0X")
                    ? Long.parseUnsignedLong(text.substring(2), 16)
                    : Long.parseUnsignedLong(text);
            dump(address, 256);
        } catch (NumberFormatException e) {
            AppLog.warn("Invalid dump address: " + addressField.getText());
        }
    }

    private final class DumpCanvas extends JPanel {
        private DumpCanvas() {
            setBackground(UiTheme.BG_INPUT);
            setFont(UiTheme.MONO_SMALL);
        }

        @Override
        public java.awt.Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(UiTheme.MONO_SMALL);
            int charW = Math.max(8, fm.charWidth('0'));
            int rows = Math.max(1, (data.length + 15) / 16);
            int width = 8 + fm.stringWidth("0000000000000000") + charW * 2
                    + charW * 3 * 16 + charW * 2 + charW * 16 + 24;
            int rowH = fm.getHeight() + 4;
            return new java.awt.Dimension(width, 8 + rows * rowH);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(UiTheme.MONO_SMALL);
            FontMetrics fm = g2.getFontMetrics();
            int charW = Math.max(8, fm.charWidth('0'));
            int y = fm.getAscent() + 4;
            int rowH = fm.getHeight() + 4;
            int addrW = fm.stringWidth("0000000000000000");
            int hexStart = 8 + addrW + charW * 2;
            int asciiStart = hexStart + charW * 3 * 16 + charW * 2;
            for (int i = 0; i < data.length; i += 16) {
                g2.setColor(UiTheme.TEXT_DIM);
                g2.drawString(String.format("%016x", baseAddress + i), 8, y);

                int hexX = hexStart;
                for (int b = 0; b < 16 && i + b < data.length; b++) {
                    if (b == 8) {
                        hexX += charW;
                    }
                    g2.setColor(UiTheme.TEXT);
                    g2.drawString(String.format("%02x", data[i + b] & 0xff), hexX, y);
                    hexX += charW * 3;
                }

                StringBuilder ascii = new StringBuilder();
                for (int b = 0; b < 16 && i + b < data.length; b++) {
                    int v = data[i + b] & 0xff;
                    ascii.append(v >= 32 && v < 127 ? (char) v : '.');
                }
                g2.setColor(UiTheme.ACCENT_DIM);
                g2.drawString(ascii.toString(), asciiStart, y);
                y += rowH;
            }
            g2.dispose();
        }
    }
}
