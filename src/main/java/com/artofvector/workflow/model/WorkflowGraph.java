package com.artofvector.workflow.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class WorkflowGraph {

    private final List<WorkflowNode> nodes = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();

    public List<WorkflowNode> nodes() {
        return nodes;
    }

    public List<Connection> connections() {
        return connections;
    }

    public void addNode(WorkflowNode node) {
        nodes.add(node);
    }

    public void removeNode(WorkflowNode node) {
        nodes.remove(node);
        connections.removeIf(c -> c.involves(node.id()));
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

    public void clear() {
        nodes.clear();
        connections.clear();
    }
}
