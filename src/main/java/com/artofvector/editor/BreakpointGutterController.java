package com.artofvector.editor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.GutterIconInfo;

import com.artofvector.debugger.engine.AddressLineMapper;
import com.artofvector.debugger.engine.DebugEvent;
import com.artofvector.debugger.engine.DebugEventListener;
import com.artofvector.debugger.engine.DebugSession;
import com.artofvector.log.AppLog;
import com.artofvector.ui.theme.UiTheme;

/**
 * Paints red breakpoint dots in the editor gutter and keeps them in sync with {@link DebugSession}.
 */
public final class BreakpointGutterController implements DebugEventListener {

    private final DebugSession session;
    private final AddressLineMapper mapper;
    private final Icon icon = createDot(UiTheme.BREAKPOINT);
    private EditorTab attached;
    private final Map<Integer, GutterIconInfo> iconsByLine = new HashMap<>();

    public BreakpointGutterController(DebugSession session, AddressLineMapper mapper) {
        this.session = session;
        this.mapper = mapper;
        session.addListener(this);
    }

    public void attach(EditorTab tab) {
        this.attached = tab;
        Gutter gutter = tab.gutter();
        gutter.setBookmarkingEnabled(true);
        gutter.setBookmarkIcon(icon);
        gutter.removeAllTrackingIcons();
        iconsByLine.clear();
        session.getBreakpoints().forEach(bp -> mapper.lineForAddress(bp.address()).ifPresent(this::showAtLine));
        tab.textArea().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && e.getX() < 24) {
                    toggleLine(tab.textArea().getCaretLineNumber());
                }
            }
        });
        tab.gutter().addPropertyChangeListener("bookmarking", evt -> {
            // Bookmark clicks are handled through toggleLine from icon row.
        });
        installGutterClick(tab);
    }

    public void toggleLine(int line) {
        mapper.addressForLine(line).ifPresentOrElse(address -> {
            try {
                if (session.hasBreakpoint(address)) {
                    session.removeBreakpoint(address);
                    hideAtLine(line);
                } else {
                    session.setBreakpoint(address);
                    showAtLine(line);
                }
            } catch (Exception e) {
                AppLog.error("Breakpoint toggle failed", e);
            }
        }, () -> AppLog.warn("No mapped address for source line " + (line + 1)));
    }

    @Override
    public void onDebugEvent(DebugEvent event) {
        if (event.type() == DebugEvent.Type.BREAKPOINT_HIT) {
            mapper.lineForAddress(event.address()).ifPresent(line -> {
                if (attached != null) {
                    attached.highlightLine(line);
                }
            });
        }
        if (event.type() == DebugEvent.Type.STEPPED) {
            mapper.lineForAddress(event.address()).ifPresent(line -> {
                if (attached != null) {
                    attached.highlightLine(line);
                }
            });
        }
    }

    private void installGutterClick(EditorTab tab) {
        tab.gutter().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                try {
                    int line = tab.textArea().getLineOfOffset(tab.textArea().viewToModel2D(e.getPoint()));
                    if (e.getX() < 18) {
                        toggleLine(line);
                    }
                } catch (Exception ignored) {
                    // Click mapping can fail if the gutter is scrolled mid-layout.
                }
            }
        });
    }

    private void showAtLine(int line) {
        if (attached == null || iconsByLine.containsKey(line)) {
            return;
        }
        try {
            GutterIconInfo info = attached.gutter().addLineTrackingIcon(line, icon, "Breakpoint");
            iconsByLine.put(line, info);
        } catch (Exception e) {
            AppLog.debug("Could not paint gutter icon: " + e.getMessage());
        }
    }

    private void hideAtLine(int line) {
        GutterIconInfo info = iconsByLine.remove(line);
        if (info != null && attached != null) {
            attached.gutter().removeTrackingIcon(info);
        }
    }

    private static Icon createDot(Color color) {
        int size = 12;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.fillOval(2, 2, size - 4, size - 4);
        g.dispose();
        return new ImageIcon(image);
    }
}
