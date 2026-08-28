package com.artofvector.debugger.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.artofvector.debugger.engine.nativeapi.PtraceConstants;

/**
 * Shared breakpoint bookkeeping for ptrace and simulated sessions.
 */
public abstract class AbstractDebugSession implements DebugSession {

    protected final DebugEventSupport events = new DebugEventSupport();
    protected final Map<Long, Breakpoint> breakpoints = new ConcurrentHashMap<>();
    protected volatile State state = State.DETACHED;
    protected volatile Registers registers = Registers.empty();

    @Override
    public void addListener(DebugEventListener listener) {
        events.add(listener);
    }

    @Override
    public void removeListener(DebugEventListener listener) {
        events.remove(listener);
    }

    @Override
    public boolean hasBreakpoint(long address) {
        return breakpoints.containsKey(address);
    }

    @Override
    public Collection<Breakpoint> getBreakpoints() {
        return Collections.unmodifiableCollection(breakpoints.values());
    }

    @Override
    public Registers getRegisters() {
        return registers;
    }

    @Override
    public boolean isAttached() {
        return state != State.DETACHED;
    }

    @Override
    public State getState() {
        return state;
    }

    protected byte int3() {
        return PtraceConstants.INT3;
    }

    protected void fire(DebugEvent event) {
        events.fire(event);
    }
}
