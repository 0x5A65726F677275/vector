package com.artofvector.workflow.model;

public final class Connection {

    private final String fromNodeId;
    private final String fromPortId;
    private final String toNodeId;
    private final String toPortId;

    public Connection(String fromNodeId, String fromPortId, String toNodeId, String toPortId) {
        this.fromNodeId = fromNodeId;
        this.fromPortId = fromPortId;
        this.toNodeId = toNodeId;
        this.toPortId = toPortId;
    }

    public String fromNodeId() {
        return fromNodeId;
    }

    public String fromPortId() {
        return fromPortId;
    }

    public String toNodeId() {
        return toNodeId;
    }

    public String toPortId() {
        return toPortId;
    }

    public boolean involves(String nodeId) {
        return fromNodeId.equals(nodeId) || toNodeId.equals(nodeId);
    }
}
