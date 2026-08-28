package com.artofvector.workflow.nodes;

import java.awt.Color;
import java.util.function.Function;

import com.artofvector.workflow.model.WorkflowNode;

public enum NodeType {
    COMMAND("Command", new Color(0x58A6FF), RunCommandNode::new);

    private final String title;
    private final Color accent;
    private final Function<NodeType, WorkflowNode> factory;

    NodeType(String title, Color accent, Function<NodeType, WorkflowNode> factory) {
        this.title = title;
        this.accent = accent;
        this.factory = factory;
    }

    public String title() {
        return title;
    }

    public Color accent() {
        return accent;
    }

    public WorkflowNode create() {
        return factory.apply(this);
    }

    public static NodeType fromName(String name) {
        if (name == null || name.isBlank()) {
            return COMMAND;
        }
        try {
            return NodeType.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return COMMAND;
        }
    }
}
