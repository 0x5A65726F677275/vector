package com.artofvector.workflow.nodes;

import java.util.Map;

import com.artofvector.workflow.model.NodeContext;
import com.artofvector.workflow.model.NodeResult;
import com.artofvector.workflow.model.WorkflowNode;

public final class LogMessageNode extends WorkflowNode {

    public LogMessageNode(NodeType type) {
        super(type.name(), type.title(), type.accent());
        setProperty("message", "hello from workflow");
    }

    @Override
    public NodeResult execute(NodeContext context) {
        String message = property("message", "");
        Object incoming = context.input("out");
        if (incoming != null) {
            message = message + " | in=" + incoming;
        }
        context.log("[workflow] " + message);
        return NodeResult.ok(message, Map.of("message", message));
    }
}
