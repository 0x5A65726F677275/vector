package com.artofvector.workflow.nodes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.artofvector.debugger.engine.SimulatedDebugSession;
import com.artofvector.workflow.model.NodeContext;
import com.artofvector.workflow.model.NodeResult;
import com.artofvector.workflow.model.WorkflowNode;

public final class LoadBinaryNode extends WorkflowNode {

    public LoadBinaryNode(NodeType type) {
        super(type.name(), type.title(), type.accent());
        setProperty("path", "");
        setProperty("address", "0x401000");
    }

    @Override
    public NodeResult execute(NodeContext context) {
        String path = property("path", "");
        long address = parseHex(property("address", "0x401000"));
        try {
            byte[] bytes;
            if (path.isBlank()) {
                bytes = context.debug().session() instanceof SimulatedDebugSession sim
                        ? sim.stubImage()
                        : context.debug().session().readMemory(address, 64);
                context.log("Load Binary: using in-session image at 0x" + Long.toHexString(address));
            } else {
                bytes = Files.readAllBytes(Path.of(path));
                context.debug().session().writeMemory(address, bytes);
                context.log("Load Binary: wrote " + bytes.length + " bytes from " + path
                        + " to 0x" + Long.toHexString(address));
            }
            return NodeResult.ok("loaded " + bytes.length + " bytes", Map.of(
                    "bytes", bytes,
                    "address", address,
                    "size", bytes.length
            ));
        } catch (Exception e) {
            return NodeResult.fail("Load Binary failed: " + e.getMessage());
        }
    }

    static long parseHex(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
            return Long.parseUnsignedLong(trimmed.substring(2), 16);
        }
        return Long.parseUnsignedLong(trimmed);
    }
}
