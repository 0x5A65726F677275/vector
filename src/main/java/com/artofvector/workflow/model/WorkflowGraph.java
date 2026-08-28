package com.artofvector.workflow.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class WorkflowGraph {

    private final List<WorkflowNode> nodes = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();
    private String ip = "127.0.0.1";

    public List<WorkflowNode> nodes() {
        return nodes;
    }

    public List<Connection> connections() {
        return connections;
    }

    public void addNode(WorkflowNode node) {
        if (node.runOrder() <= 0) {
            node.setRunOrder(nextOrder());
        }
        nodes.add(node);
    }

    public void removeNode(WorkflowNode node) {
        nodes.remove(node);
        connections.removeIf(c -> c.involves(node.id()));
        compactOrders();
    }

    public void addConnection(Connection connection) {
        if (connection.fromNodeId().equals(connection.toNodeId())) {
            return;
        }
        boolean duplicate = connections.stream().anyMatch(c ->
                c.fromNodeId().equals(connection.fromNodeId())
                        && c.fromPortId().equals(connection.fromPortId())
                        && c.toNodeId().equals(connection.toNodeId())
                        && c.toPortId().equals(connection.toPortId()));
        if (!duplicate) {
            connections.add(connection);
        }
    }

    public void removeConnection(Connection connection) {
        connections.remove(connection);
    }

    public Optional<WorkflowNode> find(String id) {
        return nodes.stream().filter(n -> n.id().equals(id)).findFirst();
    }

    public WorkflowNode hitNode(double x, double y) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            WorkflowNode node = nodes.get(i);
            if (node.contains(x, y) || node.hitPort(x, y, 10) != null) {
                return node;
            }
        }
        return null;
    }

    public List<WorkflowNode> nodesInRunOrder() {
        List<WorkflowNode> ordered = new ArrayList<>(nodes);
        ordered.sort(Comparator.comparingInt(WorkflowNode::runOrder).thenComparing(WorkflowNode::id));
        return ordered;
    }

    public void moveInRunOrder(WorkflowNode node, int delta) {
        List<WorkflowNode> ordered = nodesInRunOrder();
        int index = ordered.indexOf(node);
        int target = index + delta;
        if (index < 0 || target < 0 || target >= ordered.size()) {
            return;
        }
        ordered.remove(index);
        ordered.add(target, node);
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setRunOrder(i + 1);
        }
    }

    public void compactOrders() {
        List<WorkflowNode> ordered = nodesInRunOrder();
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setRunOrder(i + 1);
        }
    }

    public int nextOrder() {
        return nodes.stream().mapToInt(WorkflowNode::runOrder).max().orElse(0) + 1;
    }

    public String ip() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip == null ? "" : ip.strip();
    }

    public Map<String, String> variables() {
        return Map.of("ip", ip == null ? "" : ip);
    }

    public void clear() {
        nodes.clear();
        connections.clear();
    }
}
