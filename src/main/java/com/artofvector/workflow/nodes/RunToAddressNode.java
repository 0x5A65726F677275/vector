package com.artofvector.workflow.nodes;

import java.util.Map;

import com.artofvector.debugger.engine.DebugSession;
import com.artofvector.workflow.model.NodeContext;
import com.artofvector.workflow.model.NodeResult;
import com.artofvector.workflow.model.WorkflowNode;

public final class RunToAddressNode extends WorkflowNode {

    public RunToAddressNode(NodeType type) {
        super(type.name(), type.title(), type.accent());
        setProperty("address", "0x401010");
    }

    @Override
    public NodeResult execute(NodeContext context) {
        try {
            DebugSession session = context.debug().session();
            if (!session.isAttached()) {
                session.attachSimulated();
            }
            Object incoming = context.input("address");
            long address = incoming instanceof Number n
                    ? n.longValue()
                    : LoadBinaryNode.parseHex(property("address", "0x401010"));
            session.setBreakpoint(address);
            session.cont();
            context.log("Run to 0x" + Long.toHexString(address));
            return NodeResult.ok("running to 0x" + Long.toHexString(address), Map.of("address", address));
        } catch (Exception e) {
            return NodeResult.fail("Run to Address failed: " + e.getMessage());
        }
    }
}
