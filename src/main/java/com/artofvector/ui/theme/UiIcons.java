package com.artofvector.ui.theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Crisp Java2D glyphs — no bitmap assets, so they stay sharp on Linux and HiDPI.
 */
public final class UiIcons {

    public enum Glyph {
        APP, FOLDER, FILE, JAVA, PYTHON, ASM, C, JSON, BINARY, MARKDOWN,
        PLAY, PAUSE, STOP, STEP_INTO, STEP_OVER, ATTACH,
        SAVE, OPEN, OPEN_FOLDER, NEW_FILE, RUN, CLEAR, LOAD, RESET,
        NMAP, COMMAND, BREAKPOINT, DUMP, REPORT, LOG, ADDRESS,
        EDITOR, DEBUGGER, WORKFLOW, CONSOLE, HEX, STACK
    }

    private static final Map<String, Icon> CACHE = new ConcurrentHashMap<>();

    private UiIcons() {
    }

    public static Icon of(Glyph glyph, int size) {
        return of(glyph, size, colorFor(glyph));
    }

    public static Icon of(Glyph glyph, int size, Color color) {
        String key = glyph.name() + ":" + size + ":" + color.getRGB();
        return CACHE.computeIfAbsent(key, k -> new ImageIcon(render(glyph, size, color)));
    }

    public static BufferedImage appImage(int size) {
        return render(Glyph.APP, size, UiTheme.ACCENT);
    }

    public static Icon forFile(File file) {
        if (file == null) {
            return of(Glyph.FILE, 16);
        }
        if (file.isDirectory()) {
            return of(Glyph.FOLDER, 16);
        }
        String name = file.getName().toLowerCase();
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot + 1) : "";
        return switch (ext) {
            case "java" -> of(Glyph.JAVA, 16);
            case "py" -> of(Glyph.PYTHON, 16);
            case "c", "h", "cpp", "cc", "cxx", "hpp" -> of(Glyph.C, 16);
            case "asm", "s", "nasm", "inc" -> of(Glyph.ASM, 16);
            case "json" -> of(Glyph.JSON, 16);
            case "md", "txt" -> of(Glyph.MARKDOWN, 16);
            case "exe", "elf", "bin", "so", "dll", "o" -> of(Glyph.BINARY, 16);
            default -> of(Glyph.FILE, 16);
        };
    }

    public static Glyph forNodeType(String type) {
        return Glyph.COMMAND;
    }

    public static Color colorFor(Glyph glyph) {
        return switch (glyph) {
            case APP, NMAP, ASM, RUN, PLAY -> UiTheme.ACCENT;
            case FOLDER, OPEN_FOLDER -> UiTheme.WARNING;
            case JAVA -> new Color(0xE8873A);
            case PYTHON -> new Color(0x4B8BBE);
            case C, COMMAND, STEP_OVER, EDITOR, HEX -> UiTheme.CURRENT;
            case JSON, REPORT, WORKFLOW -> new Color(0xBC8CFF);
            case BINARY, DUMP, STACK -> new Color(0x79C0FF);
            case BREAKPOINT, STOP, CLEAR -> UiTheme.DANGER;
            case ADDRESS, LOAD -> UiTheme.SUCCESS;
            case PAUSE, ATTACH, LOG, MARKDOWN, FILE, OPEN, SAVE, NEW_FILE,
                    RESET, CONSOLE, DEBUGGER, STEP_INTO -> UiTheme.TEXT_MUTED;
            default -> UiTheme.TEXT;
        };
    }

    private static BufferedImage render(Glyph glyph, int size, Color color) {
        int s = Math.max(12, size);
        BufferedImage image = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(color);
        float sw = Math.max(1.5f, s * 0.08f);
        g.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float u = s / 24f;
        paint(g, glyph, u, color, sw);
        g.dispose();
        return image;
    }

    private static void paint(Graphics2D g, Glyph glyph, float u, Color color, float stroke) {
        switch (glyph) {
            case APP -> paintApp(g, u);
            case FOLDER, OPEN_FOLDER -> paintFolder(g, u, glyph == Glyph.OPEN_FOLDER);
            case FILE, MARKDOWN, JAVA, PYTHON, C, JSON, ASM -> paintFile(g, u, glyph);
            case BINARY -> paintBinary(g, u);
            case PLAY, RUN -> paintPlay(g, u);
            case PAUSE -> paintPause(g, u);
            case STOP -> paintStop(g, u);
            case STEP_INTO -> paintStepInto(g, u);
            case STEP_OVER -> paintStepOver(g, u);
            case ATTACH -> paintAttach(g, u);
            case SAVE -> paintSave(g, u);
            case OPEN -> paintOpen(g, u);
            case NEW_FILE -> paintNewFile(g, u);
            case CLEAR -> paintClear(g, u);
            case LOAD -> paintLoad(g, u);
            case RESET -> paintReset(g, u);
            case NMAP -> paintNmap(g, u);
            case COMMAND -> paintCommand(g, u);
            case BREAKPOINT -> paintBreakpoint(g, u);
            case DUMP, HEX -> paintDump(g, u);
            case REPORT -> paintReport(g, u);
            case LOG, CONSOLE -> paintConsole(g, u);
            case ADDRESS -> paintAddress(g, u);
            case EDITOR -> paintEditor(g, u);
            case DEBUGGER -> paintBug(g, u);
            case WORKFLOW -> paintWorkflow(g, u);
            case STACK -> paintStack(g, u);
        }
    }

    private static void paintApp(Graphics2D g, float u) {
        g.setColor(UiTheme.BG_ELEVATED);
        g.fill(new RoundRectangle2D.Float(1 * u, 1 * u, 22 * u, 22 * u, 7 * u, 7 * u));
        g.setColor(UiTheme.ACCENT);
        g.fill(new RoundRectangle2D.Float(2.2f * u, 2.2f * u, 19.6f * u, 19.6f * u, 6 * u, 6 * u));
        g.setColor(Color.WHITE);
        Path2D v = new Path2D.Float();
        v.moveTo(7 * u, 15.5f * u);
        v.lineTo(12 * u, 8 * u);
        v.lineTo(17 * u, 15.5f * u);
        g.setStroke(new BasicStroke(2.2f * u, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(v);
        g.setStroke(new BasicStroke(1.8f * u, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(12 * u, 8 * u, 17.5f * u, 8 * u));
        g.draw(new Line2D.Float(17.5f * u, 8 * u, 17.5f * u, 11.5f * u));
    }

    private static void paintFolder(Graphics2D g, float u, boolean open) {
        Path2D tab = new Path2D.Float();
        tab.moveTo(3 * u, 8 * u);
        tab.lineTo(3 * u, 6.5f * u);
        tab.quadTo(3 * u, 5 * u, 4.5f * u, 5 * u);
        tab.lineTo(10 * u, 5 * u);
        tab.lineTo(12 * u, 7.5f * u);
        tab.lineTo(19.5f * u, 7.5f * u);
        tab.quadTo(21 * u, 7.5f * u, 21 * u, 9 * u);
        tab.lineTo(21 * u, 18 * u);
        tab.quadTo(21 * u, 19.5f * u, 19.5f * u, 19.5f * u);
        tab.lineTo(4.5f * u, 19.5f * u);
        tab.quadTo(3 * u, 19.5f * u, 3 * u, 18 * u);
        tab.closePath();
        Color fill = new Color(UiTheme.WARNING.getRed(), UiTheme.WARNING.getGreen(), UiTheme.WARNING.getBlue(), 70);
        g.setColor(fill);
        g.fill(tab);
        g.setColor(UiTheme.WARNING);
        g.draw(tab);
        if (open) {
            g.draw(new Line2D.Float(8 * u, 12 * u, 16 * u, 12 * u));
        }
    }

    private static void paintFile(Graphics2D g, float u, Glyph glyph) {
        Path2D page = new Path2D.Float();
        page.moveTo(7 * u, 3.5f * u);
        page.lineTo(14 * u, 3.5f * u);
        page.lineTo(18.5f * u, 8 * u);
        page.lineTo(18.5f * u, 20.5f * u);
        page.quadTo(18.5f * u, 21.5f * u, 17.5f * u, 21.5f * u);
        page.lineTo(6.5f * u, 21.5f * u);
        page.quadTo(5.5f * u, 21.5f * u, 5.5f * u, 20.5f * u);
        page.lineTo(5.5f * u, 4.5f * u);
        page.quadTo(5.5f * u, 3.5f * u, 7 * u, 3.5f * u);
        page.closePath();
        Color badge = colorFor(glyph);
        g.setColor(new Color(badge.getRed(), badge.getGreen(), badge.getBlue(), 50));
        g.fill(page);
        g.setColor(badge);
        g.draw(page);
        g.draw(new Line2D.Float(14 * u, 3.5f * u, 14 * u, 8 * u));
        g.draw(new Line2D.Float(14 * u, 8 * u, 18.5f * u, 8 * u));
        g.setStroke(new BasicStroke(1.4f * u, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(8.5f * u, 12 * u, 15.5f * u, 12 * u));
        g.draw(new Line2D.Float(8.5f * u, 15.5f * u, 15.5f * u, 15.5f * u));
        g.draw(new Line2D.Float(8.5f * u, 18.5f * u, 13 * u, 18.5f * u));
    }

    private static void paintBinary(Graphics2D g, float u) {
        g.draw(new RoundRectangle2D.Float(4 * u, 5 * u, 16 * u, 14 * u, 4 * u, 4 * u));
        g.setFont(g.getFont().deriveFont(6.5f * u));
        g.drawString("0x", 7.2f * u, 14.5f * u);
    }

    private static void paintPlay(Graphics2D g, float u) {
        Path2D p = new Path2D.Float();
        p.moveTo(8 * u, 5.5f * u);
        p.lineTo(19 * u, 12 * u);
        p.lineTo(8 * u, 18.5f * u);
        p.closePath();
        g.fill(p);
    }

    private static void paintPause(Graphics2D g, float u) {
        g.fill(new RoundRectangle2D.Float(7 * u, 5.5f * u, 3.6f * u, 13 * u, 2 * u, 2 * u));
        g.fill(new RoundRectangle2D.Float(13.4f * u, 5.5f * u, 3.6f * u, 13 * u, 2 * u, 2 * u));
    }

    private static void paintStop(Graphics2D g, float u) {
        g.fill(new RoundRectangle2D.Float(6.5f * u, 6.5f * u, 11 * u, 11 * u, 3 * u, 3 * u));
    }

    private static void paintStepInto(Graphics2D g, float u) {
        g.draw(new Line2D.Float(12 * u, 4 * u, 12 * u, 14 * u));
        Path2D arrow = new Path2D.Float();
        arrow.moveTo(8.5f * u, 12 * u);
        arrow.lineTo(12 * u, 16.5f * u);
        arrow.lineTo(15.5f * u, 12 * u);
        g.draw(arrow);
        g.draw(new Line2D.Float(6 * u, 19.5f * u, 18 * u, 19.5f * u));
    }

    private static void paintStepOver(Graphics2D g, float u) {
        g.draw(new Arc2D.Float(6 * u, 7 * u, 12 * u, 12 * u, 200, 140, Arc2D.OPEN));
        Path2D arrow = new Path2D.Float();
        arrow.moveTo(15.5f * u, 7 * u);
        arrow.lineTo(19 * u, 9.5f * u);
        arrow.lineTo(15 * u, 12 * u);
        g.draw(arrow);
        g.draw(new Line2D.Float(6 * u, 19.5f * u, 18 * u, 19.5f * u));
    }

    private static void paintAttach(Graphics2D g, float u) {
        g.draw(new Ellipse2D.Float(4.5f * u, 4.5f * u, 7.5f * u, 7.5f * u));
        g.draw(new Ellipse2D.Float(12 * u, 12 * u, 7.5f * u, 7.5f * u));
        g.draw(new Line2D.Float(10.2f * u, 10.2f * u, 13.8f * u, 13.8f * u));
    }

    private static void paintSave(Graphics2D g, float u) {
        Path2D p = new Path2D.Float();
        p.moveTo(5 * u, 5 * u);
        p.lineTo(16 * u, 5 * u);
        p.lineTo(19.5f * u, 8.5f * u);
        p.lineTo(19.5f * u, 19.5f * u);
        p.lineTo(5 * u, 19.5f * u);
        p.closePath();
        g.draw(p);
        g.draw(new RoundRectangle2D.Float(8 * u, 5 * u, 7 * u, 5 * u, 1.5f * u, 1.5f * u));
        g.draw(new Line2D.Float(8 * u, 19.5f * u, 8 * u, 13 * u));
        g.draw(new Line2D.Float(16 * u, 19.5f * u, 16 * u, 13 * u));
        g.draw(new Line2D.Float(8 * u, 13 * u, 16 * u, 13 * u));
    }

    private static void paintOpen(Graphics2D g, float u) {
        g.draw(new RoundRectangle2D.Float(4.5f * u, 7 * u, 12 * u, 12 * u, 3 * u, 3 * u));
        g.draw(new Line2D.Float(12 * u, 4.5f * u, 19.5f * u, 4.5f * u));
        g.draw(new Line2D.Float(19.5f * u, 4.5f * u, 19.5f * u, 12 * u));
        g.draw(new Line2D.Float(19.5f * u, 4.5f * u, 13 * u, 11 * u));
    }

    private static void paintNewFile(Graphics2D g, float u) {
        paintFile(g, u, Glyph.FILE);
        g.fill(new Ellipse2D.Float(14.5f * u, 14.5f * u, 7 * u, 7 * u));
        g.setColor(UiTheme.BG_PANEL);
        g.setStroke(new BasicStroke(1.6f * u, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(18 * u, 16.2f * u, 18 * u, 20 * u));
        g.draw(new Line2D.Float(16.2f * u, 18.1f * u, 19.8f * u, 18.1f * u));
    }

    private static void paintClear(Graphics2D g, float u) {
        g.draw(new Line2D.Float(7 * u, 7 * u, 17 * u, 17 * u));
        g.draw(new Line2D.Float(17 * u, 7 * u, 7 * u, 17 * u));
    }

    private static void paintLoad(Graphics2D g, float u) {
        g.draw(new Line2D.Float(12 * u, 4.5f * u, 12 * u, 14 * u));
        Path2D arrow = new Path2D.Float();
        arrow.moveTo(8 * u, 11 * u);
        arrow.lineTo(12 * u, 15.5f * u);
        arrow.lineTo(16 * u, 11 * u);
        g.draw(arrow);
        g.draw(new Line2D.Float(5.5f * u, 19.5f * u, 18.5f * u, 19.5f * u));
        g.draw(new Line2D.Float(5.5f * u, 16.5f * u, 5.5f * u, 19.5f * u));
        g.draw(new Line2D.Float(18.5f * u, 16.5f * u, 18.5f * u, 19.5f * u));
    }

    private static void paintReset(Graphics2D g, float u) {
        g.draw(new Arc2D.Float(5 * u, 5 * u, 14 * u, 14 * u, 40, 260, Arc2D.OPEN));
        Path2D arrow = new Path2D.Float();
        arrow.moveTo(16.5f * u, 4.5f * u);
        arrow.lineTo(19.5f * u, 7.5f * u);
        arrow.lineTo(15.2f * u, 8.8f * u);
        g.draw(arrow);
    }

    private static void paintNmap(Graphics2D g, float u) {
        g.draw(new Ellipse2D.Float(4 * u, 4 * u, 16 * u, 16 * u));
        g.draw(new Ellipse2D.Float(7.5f * u, 7.5f * u, 9 * u, 9 * u));
        g.fill(new Ellipse2D.Float(11 * u, 11 * u, 2.4f * u, 2.4f * u));
        g.draw(new Line2D.Float(12.2f * u, 12.2f * u, 18.5f * u, 6.5f * u));
    }

    private static void paintCommand(Graphics2D g, float u) {
        g.draw(new RoundRectangle2D.Float(3.5f * u, 5 * u, 17 * u, 14 * u, 3.5f * u, 3.5f * u));
        Path2D chev = new Path2D.Float();
        chev.moveTo(7 * u, 9.5f * u);
        chev.lineTo(10.5f * u, 12 * u);
        chev.lineTo(7 * u, 14.5f * u);
        g.draw(chev);
        g.draw(new Line2D.Float(12.5f * u, 15 * u, 17 * u, 15 * u));
    }

    private static void paintBreakpoint(Graphics2D g, float u) {
        g.fill(new Ellipse2D.Float(6 * u, 6 * u, 12 * u, 12 * u));
        g.setColor(new Color(255, 255, 255, 70));
        g.fill(new Ellipse2D.Float(8.5f * u, 8 * u, 5 * u, 4 * u));
    }

    private static void paintDump(Graphics2D g, float u) {
        g.draw(new RoundRectangle2D.Float(5 * u, 4.5f * u, 14 * u, 4.2f * u, 2 * u, 2 * u));
        g.draw(new RoundRectangle2D.Float(5 * u, 10 * u, 14 * u, 4.2f * u, 2 * u, 2 * u));
        g.draw(new RoundRectangle2D.Float(5 * u, 15.5f * u, 14 * u, 4.2f * u, 2 * u, 2 * u));
    }

    private static void paintReport(Graphics2D g, float u) {
        g.draw(new RoundRectangle2D.Float(6 * u, 4 * u, 12 * u, 16.5f * u, 2.5f * u, 2.5f * u));
        g.draw(new Line2D.Float(9 * u, 8 * u, 15 * u, 8 * u));
        g.draw(new Line2D.Float(9 * u, 11.5f * u, 15 * u, 11.5f * u));
        g.draw(new Line2D.Float(9 * u, 15 * u, 13 * u, 15 * u));
    }

    private static void paintConsole(Graphics2D g, float u) {
        g.draw(new RoundRectangle2D.Float(3.5f * u, 5 * u, 17 * u, 14 * u, 3 * u, 3 * u));
        g.draw(new Line2D.Float(3.5f * u, 8.5f * u, 20.5f * u, 8.5f * u));
        g.fill(new Ellipse2D.Float(5.5f * u, 6.1f * u, 1.6f * u, 1.6f * u));
        g.fill(new Ellipse2D.Float(8 * u, 6.1f * u, 1.6f * u, 1.6f * u));
        g.draw(new Line2D.Float(6.5f * u, 12 * u, 10 * u, 14.5f * u));
        g.draw(new Line2D.Float(6.5f * u, 17 * u, 10 * u, 14.5f * u));
    }

    private static void paintAddress(Graphics2D g, float u) {
        g.draw(new Line2D.Float(5 * u, 19 * u, 19 * u, 5 * u));
        Path2D head = new Path2D.Float();
        head.moveTo(13.5f * u, 5.2f * u);
        head.lineTo(19 * u, 5 * u);
        head.lineTo(18.8f * u, 10.5f * u);
        g.draw(head);
    }

    private static void paintEditor(Graphics2D g, float u) {
        Path2D left = new Path2D.Float();
        left.moveTo(10 * u, 6 * u);
        left.lineTo(6 * u, 12 * u);
        left.lineTo(10 * u, 18 * u);
        g.draw(left);
        Path2D right = new Path2D.Float();
        right.moveTo(14 * u, 6 * u);
        right.lineTo(18 * u, 12 * u);
        right.lineTo(14 * u, 18 * u);
        g.draw(right);
    }

    private static void paintBug(Graphics2D g, float u) {
        g.draw(new Ellipse2D.Float(8 * u, 5 * u, 8 * u, 7 * u));
        g.draw(new RoundRectangle2D.Float(7.5f * u, 11 * u, 9 * u, 8 * u, 4 * u, 4 * u));
        g.draw(new Line2D.Float(7.5f * u, 14 * u, 4.5f * u, 12 * u));
        g.draw(new Line2D.Float(16.5f * u, 14 * u, 19.5f * u, 12 * u));
        g.draw(new Line2D.Float(7.5f * u, 17 * u, 4.5f * u, 19 * u));
        g.draw(new Line2D.Float(16.5f * u, 17 * u, 19.5f * u, 19 * u));
        g.draw(new Line2D.Float(12 * u, 5 * u, 12 * u, 3.5f * u));
    }

    private static void paintWorkflow(Graphics2D g, float u) {
        g.draw(new RoundRectangle2D.Float(4 * u, 4 * u, 7 * u, 5.5f * u, 2 * u, 2 * u));
        g.draw(new RoundRectangle2D.Float(13 * u, 9.5f * u, 7 * u, 5.5f * u, 2 * u, 2 * u));
        g.draw(new RoundRectangle2D.Float(4 * u, 15.5f * u, 7 * u, 5.5f * u, 2 * u, 2 * u));
        g.draw(new Line2D.Float(11 * u, 6.8f * u, 13 * u, 12 * u));
        g.draw(new Line2D.Float(11 * u, 18.2f * u, 13 * u, 12.5f * u));
    }

    private static void paintStack(Graphics2D g, float u) {
        g.draw(new RoundRectangle2D.Float(6 * u, 4 * u, 12 * u, 5 * u, 2 * u, 2 * u));
        g.draw(new RoundRectangle2D.Float(5 * u, 9.5f * u, 14 * u, 5 * u, 2 * u, 2 * u));
        g.draw(new RoundRectangle2D.Float(4 * u, 15 * u, 16 * u, 5 * u, 2 * u, 2 * u));
    }

    public static final class ColoredIcon implements Icon {
        private final Icon icon;

        public ColoredIcon(Glyph glyph, int size, Color color) {
            this.icon = of(glyph, size, color);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            icon.paintIcon(c, g, x, y);
        }

        @Override
        public int getIconWidth() {
            return icon.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return icon.getIconHeight();
        }
    }
}
