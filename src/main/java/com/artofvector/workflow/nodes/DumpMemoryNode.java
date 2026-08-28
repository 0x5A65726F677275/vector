package com.artofvector.workflow.nodes;

import java.util.Map;

import com.artofvector.workflow.model.NodeContext;
import com.artofvector.workflow.model.NodeResult;
import com.artofvector.workflow.model.WorkflowNode;

public final class DumpMemoryNode extends WorkflowNode {

    public DumpMemoryNode(NodeType type) {
        super(type.name(), type.title(), type.accent());
        setProperty("address", "0x401000");
        setProperty("size", "64");
    }

    @Override
    public NodeResult execute(NodeContext context) {
        try {
            Object incoming = context.input("address");
            long address = incoming instanceof Number n
                    ? n.longValue()
                    : LoadBinaryNode.parseHex(property("address", "0x401000"));
            int size = Integer.parseInt(property("size", "64"));
            byte[] data = context.debug().session().readMemory(address, size);
            StringBuilder hex = new StringBuilder();
            for (byte b : data) {
                hex.append(String.format("%02x ", b));
            }
            context.log("Dump Memory 0x" + Long.toHexString(address) + " (" + size + "): " + hex);
            return NodeResult.ok("dumped " + size + " bytes", Map.of(
                    "address", address,
                    "bytes", data,
                    "hex", hex.toString().trim()
            ));
        } catch (Exception e) {
            return NodeResult.fail("Dump Memory failed: " + e.getMessage());
        }
    }
}
