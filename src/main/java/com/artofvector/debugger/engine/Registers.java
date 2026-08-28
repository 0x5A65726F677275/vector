package com.artofvector.debugger.engine;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Snapshot of the general-purpose register file (x86-64 names, with EIP alias for RIP).
 */
public final class Registers {

    private final Map<String, Long> values;

    public Registers(Map<String, Long> values) {
        this.values = new LinkedHashMap<>(values);
    }

    public static Registers empty() {
        return new Registers(Map.of());
    }

    public long get(String name) {
        Long value = values.get(name.toLowerCase());
        return value == null ? 0L : value;
    }

    public long rip() {
        if (values.containsKey("rip")) {
            return values.get("rip");
        }
        return get("eip");
    }

    public long rsp() {
        if (values.containsKey("rsp")) {
            return values.get("rsp");
        }
        return get("esp");
    }

    public Map<String, Long> asMap() {
        return new LinkedHashMap<>(values);
    }
}
