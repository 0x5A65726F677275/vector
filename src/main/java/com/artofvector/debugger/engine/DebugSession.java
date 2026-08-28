package com.artofvector.debugger.engine;

import java.util.Collection;
import java.util.OptionalLong;

public interface DebugSession {

    enum State {
        DETACHED, STOPPED, RUNNING
    }

    void addListener(DebugEventListener listener);

    void removeListener(DebugEventListener listener);

    void attach(int pid) throws DebugException;

    /**
     * Simulated sessions ignore the pid and load a canned target. Real ptrace sessions attach by pid.
     */
    default void attachSimulated() throws DebugException {
        attach(0);
    }

    void detach() throws DebugException;

    void cont() throws DebugException;

    void pause() throws DebugException;

    void stop() throws DebugException;

    void stepInto() throws DebugException;

    void stepOver() throws DebugException;

    void setBreakpoint(long address) throws DebugException;

    void removeBreakpoint(long address) throws DebugException;

    boolean hasBreakpoint(long address);

    Collection<Breakpoint> getBreakpoints();

    Registers getRegisters();

    byte[] readMemory(long address, int size) throws DebugException;

    void writeMemory(long address, byte[] data) throws DebugException;

    boolean isAttached();

    State getState();

    default OptionalLong instructionPointer() {
        Registers regs = getRegisters();
        return regs == null ? OptionalLong.empty() : OptionalLong.of(regs.rip());
    }
}
