package com.artofvector.workflow.io;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.artofvector.workflow.model.Connection;
import com.artofvector.workflow.model.WorkflowGraph;
import com.artofvector.workflow.model.WorkflowNode;
import com.artofvector.workflow.nodes.NodeType;

public final class WorkflowSerializer {

    private final ObjectMapper mapper;

    public WorkflowSerializer() {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void save(WorkflowGraph graph, Path path) throws IOException {
        mapper.writeValue(path.toFile(), toDocument(graph));
    }

    public WorkflowGraph load(Path path) throws IOException {
        WorkflowDocument document = mapper.readValue(path.toFile(), WorkflowDocument.class);
        return fromDocument(document);
    }

    public String toJson(WorkflowGraph graph) throws IOException {
        return mapper.writeValueAsString(toDocument(graph));
    }

    public WorkflowGraph fromJson(String json) throws IOException {
        return fromDocument(mapper.readValue(json, WorkflowDocument.class));
    }

    private WorkflowDocument toDocument(WorkflowGraph graph) {
        WorkflowDocument document = new WorkflowDocument();
        for (WorkflowNode node : graph.nodes()) {
            NodeDocument nd = new NodeDocument();
            nd.id = node.id();
            nd.type = node.type();
            nd.title = node.title();
            nd.order = node.runOrder();
            nd.enabled = node.enabled();
            nd.x = node.x();
            nd.y = node.y();
            nd.properties = node.properties();
            document.nodes.add(nd);
        }
        document.ip = graph.ip();
        for (Connection connection : graph.connections()) {
            ConnectionDocument cd = new ConnectionDocument();
            cd.fromNode = connection.fromNodeId();
            cd.fromPort = connection.fromPortId();
            cd.toNode = connection.toNodeId();
            cd.toPort = connection.toPortId();
            document.connections.add(cd);
        }
        return document;
    }

    private WorkflowGraph fromDocument(WorkflowDocument document) {
        WorkflowGraph graph = new WorkflowGraph();
        if (document.nodes != null) {
            for (NodeDocument nd : document.nodes) {
                NodeType type = NodeType.fromName(nd.type);
                WorkflowNode node = type.create();
                node.setTitle(nd.title == null ? type.title() : nd.title);
                node.setPosition(nd.x, nd.y);
                if (nd.properties != null) {
                    nd.properties.forEach(node::setProperty);
                }
                if (nd.id != null && !nd.id.isBlank()) {
                    node.restoreId(nd.id);
                }
                if (nd.order > 0) {
                    node.setRunOrder(nd.order);
                }
                node.setEnabled(nd.enabled);
                graph.addNode(node);
            }
        }
        if (document.ip != null) {
            graph.setIp(document.ip);
        }
        if (document.connections != null) {
            for (ConnectionDocument cd : document.connections) {
                graph.addConnection(new Connection(cd.fromNode, cd.fromPort, cd.toNode, cd.toPort));
            }
        }
        return graph;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class WorkflowDocument {
        public List<NodeDocument> nodes = new ArrayList<>();
        public List<ConnectionDocument> connections = new ArrayList<>();
        public String ip;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class NodeDocument {
        public String id;
        public String type;
        public String title;
        public int order;
        public boolean enabled = true;
        public double x;
        public double y;
        public Map<String, String> properties;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class ConnectionDocument {
        public String fromNode;
        public String fromPort;
        public String toNode;
        public String toPort;
    }
}
