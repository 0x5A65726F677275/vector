package com.artofvector.workflow.nodes;

import java.util.LinkedHashMap;
import java.util.Map;

import com.artofvector.workflow.engine.CommandLine;
import com.artofvector.workflow.model.NodeContext;
import com.artofvector.workflow.model.NodeResult;
import com.artofvector.workflow.model.WorkflowNode;

/**
 * User-authored shell command. Previous node stdout is available as {@code {in}}.
 */
public final class RunCommandNode extends WorkflowNode {

    public RunCommandNode(NodeType type) {
        super(type.name(), type.title(), type.accent());
        setProperty("command", "nmap -sn $ip");
    }

    @Override
    public NodeResult execute(NodeContext context) {
        try {
            CommandLine.Result result = CommandLine.run(property("command", ""), context);
            Map<String, Object> outputs = new LinkedHashMap<>();
            outputs.put("out", result.stdout());
            outputs.put("stdout", result.stdout());
            outputs.put("stderr", result.stderr());
            outputs.put("exitCode", result.exitCode());
            if (result.exitCode() != 0) {
                return new NodeResult(false,
                        "exit " + result.exitCode() + (result.stderr().isBlank() ? "" : ": " + result.stderr()),
                        outputs);
            }
            return NodeResult.ok("exit 0", outputs);
        } catch (Exception e) {
            return NodeResult.fail(e.getMessage());
        }
    }
}
