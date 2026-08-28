package com.artofvector.debugger.engine;

public record DebugEvent(
        Type type,
        long address,
        String message,
        Registers registers,
        Breakpoint breakpoint
) {

    public enum Type {
        ATTACHED,
        DETACHED,
        BREAKPOINT_HIT,
        STEPPED,
        CONTINUED,
        PAUSED,
        STOPPED,
        ERROR,
        REGISTERS_CHANGED,
        MEMORY_CHANGED
    }

    public static DebugEvent of(Type type, long address, String message, Registers registers) {
        return new DebugEvent(type, address, message, registers, null);
    }

    public static DebugEvent error(String message) {
        return new DebugEvent(Type.ERROR, 0, message, null, null);
    }
}
