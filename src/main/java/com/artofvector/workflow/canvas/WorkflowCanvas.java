package com.artofvector.workflow.canvas;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.artofvector.ui.theme.UiIcons;
import com.artofvector.ui.theme.UiTheme;
import com.artofvector.workflow.model.Connection;
import com.artofvector.workflow.model.Port;
import com.artofvector.workflow.model.WorkflowGraph;
import com.artofvector.workflow.model.WorkflowNode;

/**
 * n8n-style node canvas: dotted grid, draggable nodes, bezier wires, wheel-zoom, middle-button pan.
 */
public final class WorkflowCanvas extends JPanel {

    private static final double MIN_SCALE = 0.25;
    private static final double MAX_SCALE = 3.0;
    private static final int GRID = 24;
    private static final int PORT_R = 6;

    private final WorkflowGraph graph;
    private double scale = 1.0;
    private double translateX = 40;
    private double translateY = 40;

    private WorkflowNode draggingNode;
    private Point lastMouse;
    private boolean panning;
    private Port dragPort;
    private WorkflowNode dragPortNode;
    private Point2D.Double dragPortWorld;

    private WorkflowNode selected;
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    public WorkflowCanvas(WorkflowGraph graph) {
        this.graph = graph;
        setBackground(UiTheme.BG_ROOT);
        setFocusable(true);
        setPreferredSize(new Dimension(1200, 800));

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                lastMouse = e.getPoint();
                Point2D world = screenToWorld(e.getPoint());
                if (SwingUtilities.isMiddleMouseButton(e) || e.isControlDown() && SwingUtilities.isLeftMouseButton(e)) {
                    panning = true;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    return;
                }
                WorkflowNode hit = graph.hitNode(world.getX(), world.getY());
                if (hit != null && SwingUtilities.isLeftMouseButton(e)
                        && hit.hitEnabledToggle(world.getX(), world.getY())) {
                    hit.toggleEnabled();
                    select(hit);
                    notifyChanged();
                    repaint();
                    return;
                }
                if (hit != null) {
                    Port port = hit.hitPort(world.getX(), world.getY(), PORT_R + 4);
                    if (port != null && port.isOutput() && SwingUtilities.isLeftMouseButton(e)) {
                        dragPort = port;
                        dragPortNode = hit;
                        dragPortWorld = new Point2D.Double(world.getX(), world.getY());
                        select(hit);
                        return;
                    }
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        draggingNode = hit;
                        select(hit);
                        graph.nodes().remove(hit);
                        graph.nodes().add(hit);
                    }
                } else {
                    select(null);
                }
                if (e.getClickCount() == 2 && hit != null) {
                    editProperties(hit);
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragPort != null && dragPortNode != null) {
                    Point2D world = screenToWorld(e.getPoint());
                    WorkflowNode target = graph.hitNode(world.getX(), world.getY());
                    if (target != null && target != dragPortNode) {
                        Port in = target.hitPort(world.getX(), world.getY(), PORT_R + 8);
                        if (in != null && in.isInput()) {
                            graph.addConnection(new Connection(
                                    dragPortNode.id(), dragPort.id(), target.id(), in.id()));
                            notifyChanged();
                        } else if (target.inputs().size() == 1) {
                            graph.addConnection(new Connection(
                                    dragPortNode.id(), dragPort.id(),
                                    target.id(), target.inputs().get(0).id()));
                            notifyChanged();
                        }
                    }
                }
                draggingNode = null;
                dragPort = null;
                dragPortNode = null;
                panning = false;
                setCursor(Cursor.getDefaultCursor());
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point now = e.getPoint();
                if (panning && lastMouse != null) {
                    translateX += now.x - lastMouse.x;
                    translateY += now.y - lastMouse.y;
                } else if (draggingNode != null && lastMouse != null) {
                    Point2D prev = screenToWorld(lastMouse);
                    Point2D cur = screenToWorld(now);
                    draggingNode.moveBy(cur.getX() - prev.getX(), cur.getY() - prev.getY());
                } else if (dragPort != null) {
                    Point2D world = screenToWorld(now);
                    dragPortWorld = new Point2D.Double(world.getX(), world.getY());
                }
                lastMouse = now;
                repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double old = scale;
                double factor = e.getPreciseWheelRotation() < 0 ? 1.1 : 1 / 1.1;
                scale = Math.min(MAX_SCALE, Math.max(MIN_SCALE, scale * factor));
                Point p = e.getPoint();
                translateX = p.x - (p.x - translateX) * (scale / old);
                translateY = p.y - (p.y - translateY) * (scale / old);
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);

        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "delete");
        getActionMap().put("delete", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (selected != null) {
                    graph.removeNode(selected);
                    selected = null;
                    notifyChanged();
                    repaint();
                }
            }
        });
    }

    public WorkflowGraph graph() {
        return graph;
    }

    public WorkflowNode selected() {
        return selected;
    }

    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    public void select(WorkflowNode node) {
        select(node, true);
    }

    public void select(WorkflowNode node, boolean notify) {
        if (this.selected == node) {
            return;
        }
        this.selected = node;
        repaint();
        if (notify) {
            notifyChanged();
        }
    }

    public void notifyChanged() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }

    public void dropNode(WorkflowNode node, Point screen) {
        Point2D world = screenToWorld(screen);
        node.setPosition(world.getX() - (WorkflowNode.WIDTH / 2.0), world.getY() - (WorkflowNode.HEIGHT / 2.0));
        graph.addNode(node);
        selected = node;
        notifyChanged();
        repaint();
    }

    public void resetView() {
        scale = 1.0;
        translateX = 40;
        translateY = 40;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        paintGrid(g2);

        AffineTransform transform = new AffineTransform();
        transform.translate(translateX, translateY);
        transform.scale(scale, scale);
        g2.transform(transform);

        for (Connection connection : graph.connections()) {
            graph.find(connection.fromNodeId()).ifPresent(from ->
                    graph.find(connection.toNodeId()).ifPresent(to -> {
                        Port fromPort = from.outputs().stream()
                                .filter(p -> p.id().equals(connection.fromPortId()))
                                .findFirst().orElse(null);
                        Port toPort = to.inputs().stream()
                                .filter(p -> p.id().equals(connection.toPortId()))
                                .findFirst().orElse(null);
                        if (fromPort != null && toPort != null) {
                            paintBezier(g2, from.outputPortX(), from.portY(fromPort),
                                    to.inputPortX(), to.portY(toPort), UiTheme.ACCENT_DIM);
                        }
                    }));
        }

        if (dragPort != null && dragPortNode != null && dragPortWorld != null) {
            paintBezier(g2,
                    dragPortNode.outputPortX(), dragPortNode.portY(dragPort),
                    dragPortWorld.x, dragPortWorld.y,
                    UiTheme.ACCENT);
        }

        for (WorkflowNode node : graph.nodes()) {
            paintNode(g2, node, node == selected);
        }
        g2.dispose();
    }

    private void paintGrid(Graphics2D g2) {
        g2.setColor(UiTheme.BG_ROOT);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(UiTheme.GRID);
        double step = GRID * scale;
        double originX = translateX % step;
        double originY = translateY % step;
        for (double x = originX; x < getWidth(); x += step) {
            for (double y = originY; y < getHeight(); y += step) {
                g2.fillRect((int) x, (int) y, 2, 2);
            }
        }
    }

    private void paintBezier(Graphics2D g2, double x1, double y1, double x2, double y2, Color color) {
        double dx = Math.max(40, Math.abs(x2 - x1) * 0.5);
        Path2D path = new Path2D.Double();
        path.moveTo(x1, y1);
        path.curveTo(x1 + dx, y1, x2 - dx, y2, x2, y2);
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
        g2.draw(path);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(color);
        g2.draw(path);
    }

    private void paintNode(Graphics2D g2, WorkflowNode node, boolean selected) {
        RoundRectangle2D body = new RoundRectangle2D.Double(node.x(), node.y(),
                WorkflowNode.WIDTH, WorkflowNode.HEIGHT, 14, 14);

        Composite previous = g2.getComposite();
        if (!node.enabled()) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
        }

        g2.setColor(new Color(0, 0, 0, 50));
        g2.fill(new RoundRectangle2D.Double(node.x() + 2, node.y() + 3,
                WorkflowNode.WIDTH, WorkflowNode.HEIGHT, 14, 14));

        g2.setColor(UiTheme.BG_ELEVATED);
        g2.fill(body);
        g2.setColor(selected ? node.accent() : UiTheme.BORDER);
        g2.setStroke(new BasicStroke(selected ? 2f : 1f));
        g2.draw(body);

        g2.setColor(node.accent());
        g2.fillRoundRect((int) node.x(), (int) node.y(), 5, WorkflowNode.HEIGHT, 8, 8);

        int iconBox = 28;
        int iconX = (int) node.x() + 16;
        int iconY = (int) node.y() + 16;
        g2.setColor(new Color(node.accent().getRed(), node.accent().getGreen(), node.accent().getBlue(), 36));
        g2.fill(new RoundRectangle2D.Double(iconX, iconY, iconBox, iconBox, 8, 8));
        UiIcons.of(UiIcons.forNodeType(node.type()), 18, node.accent())
                .paintIcon(this, g2, iconX + 5, iconY + 5);

        float textX = iconX + iconBox + 10;
        int badgePad = 22;
        int textMax = (int) (node.x() + WorkflowNode.WIDTH - 14 - textX - badgePad);
        java.awt.Shape previousClip = g2.getClip();
        g2.clip(new RoundRectangle2D.Double(node.x() + 8, node.y() + 4,
                WorkflowNode.WIDTH - 16 - badgePad, WorkflowNode.HEIGHT - 30, 10, 10));

        g2.setFont(UiTheme.UI_FONT_BOLD);
        g2.setColor(UiTheme.TEXT);
        g2.drawString(ellipsize(g2, node.title(), textMax), textX, (float) node.y() + 32);
        g2.setFont(UiTheme.UI_FONT);
        g2.setColor(UiTheme.TEXT_MUTED);
        g2.drawString(ellipsize(g2, subtitleOf(node), textMax), textX, (float) node.y() + 52);
        g2.setClip(previousClip);

        paintOrderBadge(g2, node);

        for (Port port : node.inputs()) {
            paintPort(g2, node.inputPortX(), node.portY(port), true);
        }
        for (Port port : node.outputs()) {
            paintPort(g2, node.outputPortX(), node.portY(port), false);
        }

        g2.setComposite(previous);
        paintEnabledToggle(g2, node);
    }

    private void paintOrderBadge(Graphics2D g2, WorkflowNode node) {
        String label = String.valueOf(Math.max(1, node.runOrder()));
        int size = 20;
        int bx = (int) node.x() + WorkflowNode.WIDTH - size - 8;
        int by = (int) node.y() + 8;
        g2.setColor(selected == node ? node.accent() : UiTheme.BG_HOVER);
        g2.fill(new Ellipse2D.Double(bx, by, size, size));
        g2.setColor(selected == node ? UiTheme.BG_ROOT : UiTheme.TEXT_MUTED);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new Ellipse2D.Double(bx, by, size, size));
        g2.setFont(UiTheme.UI_FONT_BOLD.deriveFont(10.5f));
        java.awt.FontMetrics metrics = g2.getFontMetrics();
        int tx = bx + (size - metrics.stringWidth(label)) / 2;
        int ty = by + (size - metrics.getHeight()) / 2 + metrics.getAscent();
        g2.setColor(selected == node ? UiTheme.BG_ROOT : UiTheme.TEXT);
        g2.drawString(label, tx, ty);
    }

    private void paintEnabledToggle(Graphics2D g2, WorkflowNode node) {
        double tx = node.enabledToggleX();
        double ty = node.enabledToggleY();
        RoundRectangle2D pill = new RoundRectangle2D.Double(
                tx, ty, WorkflowNode.TOGGLE_WIDTH, WorkflowNode.TOGGLE_HEIGHT, 9, 9);
        boolean on = node.enabled();
        g2.setColor(on ? UiTheme.SUCCESS : UiTheme.BG_HOVER);
        g2.fill(pill);
        g2.setColor(on ? UiTheme.SUCCESS : UiTheme.BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(pill);
        String label = on ? "On" : "Off";
        g2.setFont(UiTheme.UI_FONT_BOLD.deriveFont(10.5f));
        g2.setColor(on ? Color.WHITE : UiTheme.TEXT_MUTED);
        java.awt.FontMetrics metrics = g2.getFontMetrics();
        float lx = (float) (tx + (WorkflowNode.TOGGLE_WIDTH - metrics.stringWidth(label)) / 2.0);
        float ly = (float) (ty + (WorkflowNode.TOGGLE_HEIGHT - metrics.getHeight()) / 2.0 + metrics.getAscent());
        g2.drawString(label, lx, ly);
    }

    private void paintPort(Graphics2D g2, double x, double y, boolean input) {
        Ellipse2D dot = new Ellipse2D.Double(x - PORT_R, y - PORT_R, PORT_R * 2, PORT_R * 2);
        g2.setColor(input ? UiTheme.WARNING : UiTheme.ACCENT);
        g2.fill(dot);
        g2.setColor(UiTheme.BG_ROOT);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(dot);
    }

    private static String subtitleOf(WorkflowNode node) {
        String subtitle = node.property("command", "");
        if (subtitle.isBlank()) {
            subtitle = node.properties().isEmpty()
                    ? ("COMMAND".equals(node.type()) ? "node" : node.type().toLowerCase().replace('_', ' '))
                    : node.properties().values().stream().findFirst().orElse(node.type());
        }
        return subtitle.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').strip();
    }

    private static String ellipsize(Graphics2D g2, String text, int maxWidth) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (maxWidth <= 0) {
            return "…";
        }
        java.awt.FontMetrics metrics = g2.getFontMetrics();
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int budget = maxWidth - metrics.stringWidth(ellipsis);
        if (budget <= 0) {
            return ellipsis;
        }
        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (metrics.stringWidth(text.substring(0, mid)) <= budget) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return text.substring(0, low) + ellipsis;
    }

    private Point2D screenToWorld(Point screen) {
        return new Point2D.Double((screen.x - translateX) / scale, (screen.y - translateY) / scale);
    }

    private void editProperties(WorkflowNode node) {
        if (node.properties().containsKey("command")) {
            java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
            java.awt.Frame frame = window instanceof java.awt.Frame f ? f : null;
            com.artofvector.workflow.ui.NodeCommandDialog.edit(frame, node);
            notifyChanged();
            repaint();
            return;
        }
        if (node.properties().isEmpty()) {
            String title = JOptionPane.showInputDialog(this, "Node title", node.title());
            if (title != null && !title.isBlank()) {
                node.setTitle(title);
                notifyChanged();
            }
            return;
        }
        String key = node.properties().keySet().iterator().next();
        String value = JOptionPane.showInputDialog(this, key + " for " + node.title(), node.property(key, ""));
        if (value != null) {
            node.setProperty(key, value);
        }
        if (node.properties().size() > 1) {
            node.properties().keySet().stream().skip(1).forEach(extra -> {
                String next = JOptionPane.showInputDialog(this, extra + " for " + node.title(), node.property(extra, ""));
                if (next != null) {
                    node.setProperty(extra, next);
                }
            });
        }
        notifyChanged();
        repaint();
    }
}
