package com.artofvector.debugger.ui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import com.artofvector.debugger.DebugService;
import com.artofvector.ui.WorkbenchModule;
import com.artofvector.ui.theme.UiIcons;
import com.artofvector.ui.theme.UiTheme;

public final class DebuggerPanel extends JPanel implements WorkbenchModule {

    private final DisassemblyPanel disassembly;
    private final HexView hexView;
    private final StackPanel stack;

    public DebuggerPanel(DebugService service) {
        super(new BorderLayout());
        setBackground(UiTheme.BG_PANEL);

        disassembly = new DisassemblyPanel(service.session(), service.disassembler());
        hexView = new HexView(service.session());
        stack = new StackPanel(service.session());

        JTabbedPane bottom = new JTabbedPane();
        bottom.setFont(UiTheme.UI_FONT);
        bottom.addTab("Hex", UiIcons.of(UiIcons.Glyph.HEX, 14), hexView);
        bottom.addTab("Stack", UiIcons.of(UiIcons.Glyph.STACK, 14), stack);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, disassembly, bottom);
        split.setResizeWeight(0.62);
        split.setBorder(null);

        add(new DebuggerControlBar(service.session()), BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    public DisassemblyPanel disassembly() {
        return disassembly;
    }

    @Override
    public String tabTitle() {
        return "Debugger";
    }

    @Override
    public javax.swing.Icon tabIcon() {
        return UiIcons.of(UiIcons.Glyph.DEBUGGER, 16);
    }

    @Override
    public JPanel component() {
        return this;
    }
}
