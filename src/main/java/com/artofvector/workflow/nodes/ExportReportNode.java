package com.artofvector.workflow.nodes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.StringJoiner;

import com.artofvector.debugger.engine.Registers;
import com.artofvector.workflow.model.NodeContext;
import com.artofvector.workflow.model.NodeResult;
import com.artofvector.workflow.model.WorkflowNode;

public final class ExportReportNode extends WorkflowNode {

    public ExportReportNode(NodeType type) {
        super(type.name(), type.title(), type.accent());
        setProperty("path", "report.txt");
    }

    @Override
    public NodeResult execute(NodeContext context) {
        try {
            String path = property("path", "report.txt");
            StringJoiner body = new StringJoiner(System.lineSeparator());
            body.add("Art of Vector report");
            Registers regs = context.debug().session().getRegisters();
            if (regs != null) {
                body.add("RIP=" + String.format("0x%016x", regs.rip()));
                body.add("RSP=" + String.format("0x%016x", regs.rsp()));
            }
            Object hex = context.input("hex");
            if (hex != null) {
                body.add("Memory:");
                body.add(String.valueOf(hex));
            }
            Files.writeString(Path.of(path), body.toString());
            context.log("Export Report -> " + path);
            return NodeResult.ok("wrote " + path, Map.of("path", path));
        } catch (Exception e) {
            return NodeResult.fail("Export Report failed: " + e.getMessage());
        }
    }
}
