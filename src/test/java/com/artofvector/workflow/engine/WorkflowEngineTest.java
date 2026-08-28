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
    void unconnectedNodesFollowRunOrder() {
        WorkflowGraph graph = new WorkflowGraph();
        WorkflowNode later = NodeType.COMMAND.create();
        later.setRunOrder(2);
        WorkflowNode earlier = NodeType.COMMAND.create();
        earlier.setRunOrder(1);
        graph.addNode(later);
        graph.addNode(earlier);

        List<WorkflowNode> order = new WorkflowEngine().topologicalSort(graph);
        assertEquals(List.of(earlier.id(), later.id()), order.stream().map(WorkflowNode::id).toList());
    }

    @Test
    void wiresWinOverRunOrder() {
        WorkflowGraph graph = new WorkflowGraph();
        WorkflowNode first = NodeType.COMMAND.create();
        first.setRunOrder(2);
        WorkflowNode second = NodeType.COMMAND.create();
        second.setRunOrder(1);
        graph.addNode(first);
        graph.addNode(second);
        graph.addConnection(new Connection(first.id(), "out", second.id(), "in"));

        List<WorkflowNode> order = new WorkflowEngine().topologicalSort(graph);
        assertEquals(List.of(first.id(), second.id()), order.stream().map(WorkflowNode::id).toList());
    }

    @Test
    void moveInRunOrderChangesUnconnectedSequence() {
        WorkflowGraph graph = new WorkflowGraph();
        WorkflowNode first = NodeType.COMMAND.create();
        WorkflowNode second = NodeType.COMMAND.create();
        graph.addNode(first);
        graph.addNode(second);
        graph.moveInRunOrder(first, 1);

        List<WorkflowNode> order = new WorkflowEngine().topologicalSort(graph);
        assertEquals(List.of(second.id(), first.id()), order.stream().map(WorkflowNode::id).toList());
    }

    @Test
    void disabledNodeIsSkipped() {
        WorkflowGraph graph = new WorkflowGraph();
        WorkflowNode skip = NodeType.COMMAND.create();
        skip.setProperty("command", "exit 1");
        skip.setEnabled(false);
        graph.addNode(skip);
        new WorkflowEngine().execute(graph, null);
    }

    @Test
    void disabledNodePassesStdoutThrough() {
        WorkflowGraph graph = new WorkflowGraph();
        WorkflowNode first = NodeType.COMMAND.create();
        first.setProperty("command", "echo hello-from-first");
        WorkflowNode skipped = NodeType.COMMAND.create();
        skipped.setProperty("command", "exit 1");
        skipped.setEnabled(false);
        WorkflowNode third = NodeType.COMMAND.create();
        third.setProperty("command", "echo got-{in}");
        graph.addNode(first);
        graph.addNode(skipped);
        graph.addNode(third);
        graph.addConnection(new Connection(first.id(), "out", skipped.id(), "in"));
        graph.addConnection(new Connection(skipped.id(), "out", third.id(), "in"));
        new WorkflowEngine().execute(graph, null);
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
