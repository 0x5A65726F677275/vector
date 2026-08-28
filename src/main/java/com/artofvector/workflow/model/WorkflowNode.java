package com.artofvector.workflow.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A single workflow node. Subclasses implement {@link #execute(NodeContext)}.
 */
public abstract class WorkflowNode {

    public static final int WIDTH = 260;
    public static final int HEIGHT = 96;
    public static final int TOGGLE_WIDTH = 46;
    public static final int TOGGLE_HEIGHT = 18;

    private String id;
    private final String type;
    private String title;
    private int runOrder;
    private boolean enabled = true;
    private double x;
    private double y;
    private final List<Port> inputs = new ArrayList<>();
    private final List<Port> outputs = new ArrayList<>();
    private final Map<String, String> properties = new LinkedHashMap<>();
    private final Color accent;

    protected WorkflowNode(String type, String title, Color accent) {
        this(UUID.randomUUID().toString(), type, title, 80, 80, accent);
    }

    protected WorkflowNode(String id, String type, String title, double x, double y, Color accent) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.x = x;
        this.y = y;
        this.accent = accent;
        outputs.add(new Port("out", "out", Port.Direction.OUTPUT));
        inputs.add(new Port("in", "in", Port.Direction.INPUT));
    }

    public abstract NodeResult execute(NodeContext context);

    public String id() {
        return id;
    }

    public void restoreId(String id) {
        this.id = id;
    }

    public String type() {
        return type;
    }

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int runOrder() {
        return runOrder;
    }

    public void setRunOrder(int runOrder) {
        this.runOrder = Math.max(0, runOrder);
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void toggleEnabled() {
        this.enabled = !this.enabled;
    }

    public double enabledToggleX() {
        return x + WIDTH - TOGGLE_WIDTH - 8;
    }

    public double enabledToggleY() {
        return y + HEIGHT - TOGGLE_HEIGHT - 8;
    }

    public boolean hitEnabledToggle(double worldX, double worldY) {
        double tx = enabledToggleX();
        double ty = enabledToggleY();
        return worldX >= tx && worldX <= tx + TOGGLE_WIDTH
                && worldY >= ty && worldY <= ty + TOGGLE_HEIGHT;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void moveBy(double dx, double dy) {
        this.x += dx;
        this.y += dy;
    }

    public Color accent() {
        return accent;
    }

    public List<Port> inputs() {
        return inputs;
    }

    public List<Port> outputs() {
        return outputs;
    }

    public Map<String, String> properties() {
        return properties;
    }

    public String property(String key, String fallback) {
        return properties.getOrDefault(key, fallback);
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public boolean contains(double worldX, double worldY) {
        return worldX >= x && worldX <= x + WIDTH && worldY >= y && worldY <= y + HEIGHT;
    }

    public double inputPortX() {
        return x;
    }

    public double outputPortX() {
        return x + WIDTH;
    }

    public double portY(Port port) {
        List<Port> list = port.isInput() ? inputs : outputs;
        int index = list.indexOf(port);
        int count = Math.max(1, list.size());
        return y + HEIGHT * (index + 1) / (count + 1);
    }

    public Port hitPort(double worldX, double worldY, double radius) {
        for (Port port : inputs) {
            if (distance(worldX, worldY, inputPortX(), portY(port)) <= radius) {
                return port;
            }
        }
        for (Port port : outputs) {
            if (distance(worldX, worldY, outputPortX(), portY(port)) <= radius) {
                return port;
            }
        }
        return null;
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
