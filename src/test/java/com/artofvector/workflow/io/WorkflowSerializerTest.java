package com.artofvector.workflow.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        WorkflowNode first = NodeType.COMMAND.create();
        first.setPosition(40, 80);
        first.setProperty("command", "echo hello");
        WorkflowNode second = NodeType.COMMAND.create();
        second.setPosition(320, 80);
        second.setProperty("command", "echo {in}");
        graph.addNode(first);
        graph.addNode(second);
        first.setRunOrder(2);
        second.setRunOrder(1);
        graph.addConnection(new Connection(first.id(), "out", second.id(), "in"));

        WorkflowSerializer serializer = new WorkflowSerializer();
        String json = serializer.toJson(graph);
        WorkflowGraph restored = serializer.fromJson(json);

        assertEquals(2, restored.nodes().size());
        assertEquals(1, restored.connections().size());
        assertTrue(restored.find(first.id()).isPresent());
        assertEquals("echo hello", restored.find(first.id()).orElseThrow().property("command", ""));
        assertEquals(first.id(), restored.connections().get(0).fromNodeId());
        assertEquals(second.id(), restored.connections().get(0).toNodeId());
        assertEquals(2, restored.find(first.id()).orElseThrow().runOrder());
        assertEquals(1, restored.find(second.id()).orElseThrow().runOrder());
        assertTrue(restored.find(first.id()).orElseThrow().enabled());
    }

    @Test
    void roundTripPreservesDisabledNode() throws Exception {
        WorkflowGraph graph = new WorkflowGraph();
        WorkflowNode node = NodeType.COMMAND.create();
        node.setEnabled(false);
        graph.addNode(node);
        WorkflowGraph restored = new WorkflowSerializer().fromJson(new WorkflowSerializer().toJson(graph));
        assertFalse(restored.nodes().get(0).enabled());
    }

    @Test
    void unknownLegacyTypesLoadAsCommand() throws Exception {
        String json = """
                {
                  "nodes": [
                    {"id": "n1", "type": "NMAP", "title": "Nmap", "x": 10, "y": 20,
                     "properties": {"command": "nmap -sn 127.0.0.1"}}
                  ],
                  "connections": []
                }
                """;
        WorkflowGraph graph = new WorkflowSerializer().fromJson(json);
        assertEquals(1, graph.nodes().size());
        WorkflowNode node = graph.nodes().get(0);
        assertEquals("COMMAND", node.type());
        assertEquals("nmap -sn 127.0.0.1", node.property("command", ""));
        assertEquals(1, node.runOrder());
        assertTrue(node.enabled());
    }
}
