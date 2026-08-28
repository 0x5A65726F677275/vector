package com.artofvector.workflow.nodes;

import java.util.LinkedHashMap;
import java.util.Map;

import com.artofvector.workflow.engine.CommandLine;
import com.artofvector.workflow.model.NodeContext;
import com.artofvector.workflow.model.NodeResult;
import com.artofvector.workflow.model.WorkflowNode;

/**
 * User-authored nmap command. Double-click the node to edit.
 * Previous node stdout substitutes {@code {in}} (e.g. {@code nmap -sV {in}}).
 */
public final class NmapNode extends WorkflowNode {

    public NmapNode(NodeType type) {
        super(type.name(), type.title(), type.accent());
        setProperty("command", "nmap");
    }

    @Override
    public NodeResult execute(NodeContext context) {
        String written = property("command", "nmap").trim();
        if (written.isBlank()) {
            return NodeResult.fail("Write an nmap command (double-click this node)");
        }
        String command = written.toLowerCase().startsWith("nmap") ? written : "nmap " + written;
        try {
            CommandLine.Result result = CommandLine.run(command, context);
            Map<String, Object> outputs = new LinkedHashMap<>();
            outputs.put("out", result.stdout());
            outputs.put("stdout", result.stdout());
            outputs.put("stderr", result.stderr());
            outputs.put("exitCode", result.exitCode());
            if (result.exitCode() != 0) {
                String detail = result.stderr().isBlank() ? result.stdout() : result.stderr();
                return new NodeResult(false, "nmap exit " + result.exitCode()
                        + (detail.isBlank() ? "" : ": " + abbreviate(detail)), outputs);
            }
            return NodeResult.ok("nmap exit 0", outputs);
        } catch (Exception e) {
            return NodeResult.fail("nmap failed: " + e.getMessage());
        }
    }

    private static String abbreviate(String text) {
        String oneLine = text.replace('\n', ' ').trim();
        return oneLine.length() > 160 ? oneLine.substring(0, 157) + "..." : oneLine;
    }
}
