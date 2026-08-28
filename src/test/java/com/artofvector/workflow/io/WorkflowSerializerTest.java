package com.artofvector.workflow.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.artofvector.workflow.model.Connection;
import com.artofvector.workflow.model.WorkflowGraph;
import com.artofvector.workflow.model.WorkflowNode;
import com.artofvector.workflow.nodes.NodeType;

class WorkflowSerializerTest {

    @Test
    void roundTripPreservesNodesAndWires() throws Exception {
        WorkflowGraph graph = new WorkflowGraph();
        WorkflowNode load = NodeType.LOAD_BINARY.create();
        load.setPosition(40, 80);
        load.setProperty("path", "/tmp/a.bin");
        WorkflowNode bp = NodeType.SET_BREAKPOINT.create();
        bp.setPosition(320, 80);
        graph.addNode(load);
        graph.addNode(bp);
        graph.addConnection(new Connection(load.id(), "out", bp.id(), "in"));

        WorkflowSerializer serializer = new WorkflowSerializer();
        String json = serializer.toJson(graph);
        WorkflowGraph restored = serializer.fromJson(json);

        assertEquals(2, restored.nodes().size());
        assertEquals(1, restored.connections().size());
        assertTrue(restored.find(load.id()).isPresent());
        assertEquals("/tmp/a.bin", restored.find(load.id()).orElseThrow().property("path", ""));
        assertEquals(load.id(), restored.connections().get(0).fromNodeId());
        assertEquals(bp.id(), restored.connections().get(0).toNodeId());
    }
}
