package com.artofvector.workflow.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.artofvector.workflow.model.NodeContext;
import com.artofvector.workflow.model.NodeResult;
import com.artofvector.workflow.model.WorkflowNode;
import com.artofvector.workflow.nodes.NodeType;

class CommandLineTest {

    @Test
    void expandSubstitutesDollarIp() {
        NodeContext context = new NodeContext(null, null, java.util.Map.of("ip", "10.1.1.1"));
        assertEquals("nmap -sn 10.1.1.1", CommandLine.expand("nmap -sn $ip", context));
        assertEquals("nmap -sV 10.1.1.1", CommandLine.expand("nmap -sV {ip}", context));
        assertEquals("nmap -Pn 10.1.1.1", CommandLine.expand("nmap -Pn ${ip}", context));
    }

    @Test
    void expandLeavesUnrelatedTokens() {
        NodeContext context = new NodeContext(null, null, java.util.Map.of("ip", "10.1.1.1"));
        assertEquals("echo $ipaddr", CommandLine.expand("echo $ipaddr", context));
    }

    @Test
    void expandSubstitutesPreviousStdout() {
        NodeContext context = new NodeContext(null);
        context.putInput("in", "127.0.0.1");
        assertEquals("nmap -sn 127.0.0.1", CommandLine.expand("nmap -sn {in}", context));
        assertEquals("nmap -sV 127.0.0.1", CommandLine.expand("nmap -sV {stdout}", context));
    }

    @Test
    void commandNodeRunsEchoAndPassesStdout() throws Exception {
        WorkflowNode echo = NodeType.COMMAND.create();
        echo.setProperty("command", "echo pipeline-ok");
        NodeResult result = echo.execute(new NodeContext(null));
        assertTrue(result.success(), result.message());
        String stdout = String.valueOf(result.output("out")).trim();
        assertTrue(stdout.contains("pipeline-ok"), stdout);
    }

    @Test
    void nmapNodePrefixesBareArguments() {
        NodeContext context = new NodeContext(null);
        context.putInput("in", "127.0.0.1");
        assertEquals("nmap -sn 127.0.0.1", CommandLine.expand("nmap -sn {in}", context));
    }

    @Test
    void commandRunsInWorkingDirectory() throws Exception {
        Path dir = Files.createTempDirectory("aov-cwd");
        NodeContext context = new NodeContext(null, dir);
        WorkflowNode write = NodeType.COMMAND.create();
        write.setProperty("command", "echo cwd-ok > aov-marker.txt");
        NodeResult result = write.execute(context);
        assertTrue(result.success(), result.message());
        assertTrue(Files.exists(dir.resolve("aov-marker.txt")));
    }
}
