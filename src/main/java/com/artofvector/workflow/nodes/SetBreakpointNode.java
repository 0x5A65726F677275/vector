package com.artofvector.workflow.nodes;

import java.util.Map;

import com.artofvector.workflow.model.NodeContext;
import com.artofvector.workflow.model.NodeResult;
import com.artofvector.workflow.model.WorkflowNode;

public final class SetBreakpointNode extends WorkflowNode {

    public SetBreakpointNode(NodeType type) {
        super(type.name(), type.title(), type.accent());
        setProperty("address", "0x401000");
    }

    @Override
    public NodeResult execute(NodeContext context) {
        try {
            Object incoming = context.input("address");
            long address = incoming instanceof Number n
                    ? n.longValue()
                    : LoadBinaryNode.parseHex(property("address", "0x401000"));
            context.debug().session().setBreakpoint(address);
            context.log("Set Breakpoint at 0x" + Long.toHexString(address));
            return NodeResult.ok("breakpoint 0x" + Long.toHexString(address), Map.of("address", address));
        } catch (Exception e) {
            return NodeResult.fail("Set Breakpoint failed: " + e.getMessage());
        }
    }
}
