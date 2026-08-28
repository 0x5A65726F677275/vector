package com.artofvector.workflow.model;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.artofvector.debugger.DebugService;
import com.artofvector.log.AppLog;

public final class NodeContext {

    private final DebugService debugService;
    private final Path workingDirectory;
    private final Map<String, Object> inputs = new HashMap<>();
    private final Map<String, Object> extras = new HashMap<>();

    public NodeContext(DebugService debugService) {
        this(debugService, null);
    }

    public NodeContext(DebugService debugService, Path workingDirectory) {
        this.debugService = debugService;
        this.workingDirectory = workingDirectory;
    }

    public DebugService debug() {
        return debugService;
    }

    public Path workingDirectory() {
        return workingDirectory;
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
