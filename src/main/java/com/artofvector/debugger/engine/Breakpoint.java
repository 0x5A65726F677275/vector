package com.artofvector.debugger.engine;

public record Breakpoint(long address, byte originalByte, boolean enabled) {

    public Breakpoint withEnabled(boolean enabled) {
        return new Breakpoint(address, originalByte, enabled);
    }
}
