package com.artofvector.workflow.model;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.artofvector.debugger.DebugService;
import com.artofvector.log.AppLog;

public final class NodeContext {

    private final DebugService debugService;
    private final Path workingDirectory;
    private final Map<String, String> variables;
    private final Map<String, Object> inputs = new HashMap<>();
    private final Map<String, Object> extras = new HashMap<>();

    public NodeContext(DebugService debugService) {
        this(debugService, null, Map.of());
    }

    public NodeContext(DebugService debugService, Path workingDirectory) {
        this(debugService, workingDirectory, Map.of());
    }

    public NodeContext(DebugService debugService, Path workingDirectory, Map<String, String> variables) {
        this.debugService = debugService;
        this.workingDirectory = workingDirectory;
        this.variables = variables == null || variables.isEmpty()
                ? Map.of()
                : Map.copyOf(variables);
    }

    public DebugService debug() {
        return debugService;
    }

    public Path workingDirectory() {
        return workingDirectory;
    }

    public String variable(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String value = variables.get(name);
        if (value != null) {
            return value;
        }
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue() == null ? "" : entry.getValue();
            }
        }
        return "";
    }

    public Map<String, String> variables() {
        return variables;
    }

    public void putInput(String key, Object value) {
        inputs.put(key, value);
    }

    public Object input(String key) {
        return inputs.get(key);
    }

    public String inputString(String key, String fallback) {
        Object value = inputs.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    public Map<String, Object> inputs() {
        return inputs;
    }

    public void put(String key, Object value) {
        extras.put(key, value);
    }

    public Object get(String key) {
        return extras.get(key);
    }

    public void log(String message) {
        AppLog.info(message);
    }
}
