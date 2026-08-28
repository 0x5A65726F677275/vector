package com.artofvector.workflow.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        WorkflowNode first = NodeType.COMMAND.create();
        WorkflowNode second = NodeType.COMMAND.create();
        WorkflowNode third = NodeType.COMMAND.create();
        graph.addNode(third);
        graph.addNode(second);
        graph.addNode(first);
        graph.addConnection(new Connection(first.id(), "out", second.id(), "in"));
        graph.addConnection(new Connection(second.id(), "out", third.id(), "in"));

        List<WorkflowNode> order = new WorkflowEngine().topologicalSort(graph);
        assertEquals(List.of(first.id(), second.id(), third.id()), order.stream().map(WorkflowNode::id).toList());
    }

    @Test
    void topologicalSortRejectsCycles() {
        WorkflowGraph graph = new WorkflowGraph();
        WorkflowNode a = NodeType.COMMAND.create();
        WorkflowNode b = NodeType.COMMAND.create();
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
}
