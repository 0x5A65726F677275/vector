package com.artofvector.workflow.engine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.artofvector.debugger.DebugService;
import com.artofvector.log.AppLog;
import com.artofvector.workflow.model.Connection;
import com.artofvector.workflow.model.NodeContext;
import com.artofvector.workflow.model.NodeResult;
import com.artofvector.workflow.model.WorkflowGraph;
import com.artofvector.workflow.model.WorkflowNode;

/**
 * Kahn topological sort, then {@link WorkflowNode#execute(NodeContext)} in order.
 * Wires are hard dependencies. Among nodes that are ready at the same time,
 * {@link WorkflowNode#runOrder()} (then id) decides who runs first.
 * Each node receives the union of predecessor outputs as input.
 */
public final class WorkflowEngine {

    public List<WorkflowNode> topologicalSort(WorkflowGraph graph) {
        Map<String, WorkflowNode> byId = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();
        for (WorkflowNode node : graph.nodes()) {
            byId.put(node.id(), node);
            indegree.put(node.id(), 0);
            adj.put(node.id(), new ArrayList<>());
        }
        for (Connection connection : graph.connections()) {
            if (!indegree.containsKey(connection.fromNodeId()) || !indegree.containsKey(connection.toNodeId())) {
                continue;
            }
            adj.get(connection.fromNodeId()).add(connection.toNodeId());
            indegree.merge(connection.toNodeId(), 1, Integer::sum);
        }
        Comparator<String> byRunOrder = Comparator
                .comparingInt((String id) -> byId.get(id).runOrder())
                .thenComparing(id -> id);
        PriorityQueue<String> queue = new PriorityQueue<>(byRunOrder);
        indegree.forEach((id, degree) -> {
            if (degree == 0) {
                queue.add(id);
            }
        });
        List<WorkflowNode> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            String id = queue.remove();
            graph.find(id).ifPresent(order::add);
            for (String next : adj.getOrDefault(id, List.of())) {
                int degree = indegree.merge(next, -1, Integer::sum);
                if (degree == 0) {
                    queue.add(next);
                }
            }
        }
        if (order.size() != graph.nodes().size()) {
            throw new IllegalStateException("Workflow has a cycle");
        }
        return order;
    }

    public void execute(WorkflowGraph graph, DebugService debugService) {
        execute(graph, debugService, null);
    }

    public void execute(WorkflowGraph graph, DebugService debugService, Path workingDirectory) {
        List<WorkflowNode> order = topologicalSort(graph);
        Map<String, Map<String, Object>> outputs = new HashMap<>();
        long skipped = order.stream().filter(node -> !node.enabled()).count();
        AppLog.info("Running workflow (" + order.size() + " nodes"
                + (skipped == 0 ? "" : ", " + skipped + " off") + ")...");
        if (workingDirectory != null) {
            AppLog.info("Working folder: " + workingDirectory);
        }
        for (WorkflowNode node : order) {
            NodeContext context = new NodeContext(debugService, workingDirectory);
            for (Connection connection : graph.connections()) {
                if (!connection.toNodeId().equals(node.id())) {
                    continue;
                }
                Map<String, Object> from = outputs.getOrDefault(connection.fromNodeId(), Map.of());
                from.forEach(context::putInput);
                Object portValue = from.get(connection.fromPortId());
                if (portValue != null) {
                    context.putInput(connection.toPortId(), portValue);
                }
            }
            if (!node.enabled()) {
                outputs.put(node.id(), passthrough(context));
                AppLog.info("Node '" + node.title() + "' skipped (off)");
                continue;
            }
            NodeResult result = node.execute(context);
            if (!result.success()) {
                AppLog.error("Node '" + node.title() + "' failed: " + result.message());
                throw new IllegalStateException(result.message());
            }
            AppLog.info("Node '" + node.title() + "': " + result.message());
            outputs.put(node.id(), result.outputs());
        }
        AppLog.info("Workflow finished.");
    }

    private static Map<String, Object> passthrough(NodeContext context) {
        Map<String, Object> outputs = new LinkedHashMap<>(context.inputs());
        Object inbound = outputs.get("in");
        if (inbound == null) {
            inbound = outputs.get("out");
        }
        if (inbound == null) {
            inbound = outputs.get("stdout");
        }
        if (inbound != null) {
            outputs.putIfAbsent("out", inbound);
            outputs.putIfAbsent("stdout", inbound);
            outputs.putIfAbsent("in", inbound);
        }
        return outputs;
    }
}
