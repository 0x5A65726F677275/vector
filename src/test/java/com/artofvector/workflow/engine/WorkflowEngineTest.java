package com.artofvector.workflow.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.artofvector.workflow.model.Connection;
import com.artofvector.workflow.model.WorkflowGraph;
import com.artofvector.workflow.model.WorkflowNode;
import com.artofvector.workflow.nodes.NodeType;

class WorkflowEngineTest {

    @Test
    void topologicalSortOrdersDependencies() {
        WorkflowGraph graph = new WorkflowGraph();
        WorkflowNode load = NodeType.LOAD_BINARY.create();
        WorkflowNode bp = NodeType.SET_BREAKPOINT.create();
        WorkflowNode dump = NodeType.DUMP_MEMORY.create();
        graph.addNode(dump);
        graph.addNode(bp);
        graph.addNode(load);
        graph.addConnection(new Connection(load.id(), "out", bp.id(), "in"));
        graph.addConnection(new Connection(bp.id(), "out", dump.id(), "in"));

        List<WorkflowNode> order = new WorkflowEngine().topologicalSort(graph);
        assertEquals(List.of(load.id(), bp.id(), dump.id()), order.stream().map(WorkflowNode::id).toList());
    }

    @Test
    void topologicalSortRejectsCycles() {
        WorkflowGraph graph = new WorkflowGraph();
        WorkflowNode a = NodeType.LOG_MESSAGE.create();
        WorkflowNode b = NodeType.LOG_MESSAGE.create();
        graph.addNode(a);
        graph.addNode(b);
        graph.addConnection(new Connection(a.id(), "out", b.id(), "in"));
        graph.addConnection(new Connection(b.id(), "out", a.id(), "in"));
        assertThrows(IllegalStateException.class, () -> new WorkflowEngine().topologicalSort(graph));
    }

    @Test
    void chainedCommandsPassStdoutInConnectionOrder() {
        WorkflowGraph graph = new WorkflowGraph();
        WorkflowNode first = NodeType.COMMAND.create();
        first.setProperty("command", "echo 127.0.0.1");
        WorkflowNode second = NodeType.COMMAND.create();
        second.setProperty("command", "echo scanned-{in}");
        graph.addNode(second);
        graph.addNode(first);
        graph.addConnection(new Connection(first.id(), "out", second.id(), "in"));

        new WorkflowEngine().execute(graph, null);
    }

    @Test
    void logNodeSucceeds() {
        WorkflowNode node = NodeType.LOG_MESSAGE.create();
        var result = node.execute(new com.artofvector.workflow.model.NodeContext(null));
        assertTrue(result.success());
    }
}
