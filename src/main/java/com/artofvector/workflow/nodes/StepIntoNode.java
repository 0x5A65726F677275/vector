package com.artofvector.workflow.nodes;

import java.util.Map;

import com.artofvector.debugger.engine.DebugSession;
import com.artofvector.workflow.model.NodeContext;
import com.artofvector.workflow.model.NodeResult;
import com.artofvector.workflow.model.WorkflowNode;

public final class StepIntoNode extends WorkflowNode {

    public StepIntoNode(NodeType type) {
        super(type.name(), type.title(), type.accent());
    }

    @Override
    public NodeResult execute(NodeContext context) {
        try {
            DebugSession session = context.debug().session();
            if (!session.isAttached()) {
                session.attachSimulated();
            }
            session.stepInto();
            long rip = session.getRegisters().rip();
            context.log("Step Into -> RIP 0x" + Long.toHexString(rip));
            return NodeResult.ok("rip=0x" + Long.toHexString(rip), Map.of("address", rip));
        } catch (Exception e) {
            return NodeResult.fail("Step Into failed: " + e.getMessage());
        }
    }
}
