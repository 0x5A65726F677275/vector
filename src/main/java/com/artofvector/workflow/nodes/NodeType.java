package com.artofvector.workflow.nodes;

import java.awt.Color;
import java.util.function.Function;

import com.artofvector.workflow.model.WorkflowNode;

public enum NodeType {
    NMAP("Nmap", new Color(0x3DCCC7), NmapNode::new),
    COMMAND("Command", new Color(0x58A6FF), RunCommandNode::new),
    LOAD_BINARY("Load Binary", new Color(0x388BFD), LoadBinaryNode::new),
    SET_BREAKPOINT("Set Breakpoint", new Color(0xF85149), SetBreakpointNode::new),
    RUN_TO_ADDRESS("Run to Address", new Color(0x3FB950), RunToAddressNode::new),
    STEP_INTO("Step Into", new Color(0x2A9D98), StepIntoNode::new),
    DUMP_MEMORY("Dump Memory", new Color(0xD29922), DumpMemoryNode::new),
    EXPORT_REPORT("Export Report", new Color(0xBC8CFF), ExportReportNode::new),
    LOG_MESSAGE("Log Message", new Color(0x8B9BAB), LogMessageNode::new);

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
        return NodeType.valueOf(name);
    }
}
