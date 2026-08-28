package com.artofvector.workflow.model;

import java.util.Map;

public final class NodeResult {

    private final boolean success;
    private final String message;
    private final Map<String, Object> outputs;

    public NodeResult(boolean success, String message, Map<String, Object> outputs) {
        this.success = success;
        this.message = message;
        this.outputs = outputs == null ? Map.of() : Map.copyOf(outputs);
    }

    public static NodeResult ok(String message, Map<String, Object> outputs) {
        return new NodeResult(true, message, outputs);
    }

    public static NodeResult ok(String message) {
        return ok(message, Map.of());
    }

    public static NodeResult fail(String message) {
        return new NodeResult(false, message, Map.of());
    }

    public boolean success() {
        return success;
    }

    public String message() {
        return message;
    }

    public Map<String, Object> outputs() {
        return outputs;
    }

    public Object output(String key) {
        return outputs.get(key);
    }
}
