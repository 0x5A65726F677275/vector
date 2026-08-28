package com.artofvector.debugger.engine;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

import com.artofvector.debugger.disasm.Disassembler;
import com.artofvector.debugger.disasm.DisassemblerFactory;
import com.artofvector.debugger.disasm.Instruction;
import com.artofvector.debugger.engine.nativeapi.PtraceConstants;
import com.artofvector.debugger.engine.nativeapi.PtraceLib;
import com.artofvector.debugger.engine.nativeapi.UserRegsStruct;
import com.artofvector.log.AppLog;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * Linux ptrace-backed debug session. Attach, memory peek/poke, register access, INT3 breakpoints, step/continue.
 */
public final class LinuxPtraceDebugSession extends AbstractDebugSession {

    private final PtraceLib libc;
    private final Disassembler disassembler;
    private final AtomicBoolean waitLoop = new AtomicBoolean(false);
    private int pid;
    private Thread waiter;

    public LinuxPtraceDebugSession() {
        this.libc = PtraceLib.load();
        this.disassembler = DisassemblerFactory.create();
    }

    @Override
    public synchronized void attach(int pid) throws DebugException {
        if (pid <= 0) {
            throw new DebugException("A real process id is required for ptrace attach");
        }
        this.pid = pid;
        long rc = libc.ptrace(PtraceConstants.PTRACE_ATTACH, pid, Pointer.NULL, Pointer.NULL);
        if (rc < 0) {
            throw new DebugException("PTRACE_ATTACH failed, errno=" + Native.getLastError());
        }
        waitStopped("attach");
        refreshRegisters();
        state = State.STOPPED;
        AppLog.info("ptrace attached to pid " + pid + " at RIP=0x" + Long.toHexString(registers.rip()));
        fire(DebugEvent.of(DebugEvent.Type.ATTACHED, registers.rip(), "Attached to pid " + pid, registers));
    }

    @Override
    public synchronized void detach() throws DebugException {
        stopWaiter();
        if (pid != 0) {
            libc.ptrace(PtraceConstants.PTRACE_DETACH, pid, Pointer.NULL, Pointer.NULL);
        }
        state = State.DETACHED;
        fire(DebugEvent.of(DebugEvent.Type.DETACHED, 0, "Detached", registers));
        pid = 0;
    }

    @Override
    public synchronized void cont() throws DebugException {
        ensureAttached();
        long rc = libc.ptrace(PtraceConstants.PTRACE_CONT, pid, Pointer.NULL, Pointer.NULL);
        if (rc < 0) {
            throw new DebugException("PTRACE_CONT failed, errno=" + Native.getLastError());
        }
        state = State.RUNNING;
        fire(DebugEvent.of(DebugEvent.Type.CONTINUED, registers.rip(), "Continue", registers));
        startWaiter();
    }

    @Override
    public synchronized void pause() throws DebugException {
        ensureAttached();
        if (libc.kill(pid, PtraceConstants.SIGSTOP) != 0) {
            throw new DebugException("SIGSTOP failed, errno=" + Native.getLastError());
        }
    }

    @Override
    public synchronized void stop() throws DebugException {
        stopWaiter();
        if (pid != 0) {
            libc.kill(pid, PtraceConstants.SIGKILL);
            libc.ptrace(PtraceConstants.PTRACE_DETACH, pid, Pointer.NULL, Pointer.NULL);
        }
        state = State.DETACHED;
        pid = 0;
        fire(DebugEvent.of(DebugEvent.Type.STOPPED, 0, "Stopped", registers));
    }

    @Override
    public synchronized void stepInto() throws DebugException {
        ensureAttached();
        long rc = libc.ptrace(PtraceConstants.PTRACE_SINGLESTEP, pid, Pointer.NULL, Pointer.NULL);
        if (rc < 0) {
            throw new DebugException("PTRACE_SINGLESTEP failed, errno=" + Native.getLastError());
        }
        waitStopped("step");
        handleStop();
        fire(DebugEvent.of(DebugEvent.Type.STEPPED, registers.rip(), "Step into", registers));
    }

    @Override
    public synchronized void stepOver() throws DebugException {
        ensureAttached();
        byte[] window = readMemory(registers.rip(), 16);
        Instruction insn = disassembler.disassembleOne(window, registers.rip());
        if (insn != null && "call".equalsIgnoreCase(insn.mnemonic())) {
            long after = insn.address() + insn.size();
            setBreakpoint(after);
            cont();
            return;
        }
        stepInto();
    }

    @Override
    public synchronized void setBreakpoint(long address) throws DebugException {
        ensureAttached();
        if (breakpoints.containsKey(address)) {
            return;
        }
        byte original = readMemory(address, 1)[0];
        writeByte(address, int3());
        breakpoints.put(address, new Breakpoint(address, original, true));
        AppLog.info("Breakpoint set at 0x" + Long.toHexString(address));
        fire(DebugEvent.of(DebugEvent.Type.MEMORY_CHANGED, address, "breakpoint set", registers));
    }

    @Override
    public synchronized void removeBreakpoint(long address) throws DebugException {
        Breakpoint bp = breakpoints.remove(address);
        if (bp != null) {
            writeByte(address, bp.originalByte());
            fire(DebugEvent.of(DebugEvent.Type.MEMORY_CHANGED, address, "breakpoint removed", registers));
        }
    }

    @Override
    public synchronized byte[] readMemory(long address, int size) throws DebugException {
        ensureAttached();
        byte[] out = new byte[size];
        int i = 0;
        while (i < size) {
            long aligned = (address + i) & ~7L;
            long word = peek(aligned);
            byte[] chunk = longToBytes(word);
            int offset = (int) ((address + i) & 7);
            while (offset < 8 && i < size) {
                out[i++] = chunk[offset++];
            }
        }
        return out;
    }

    @Override
    public synchronized void writeMemory(long address, byte[] data) throws DebugException {
        ensureAttached();
        for (int i = 0; i < data.length; i++) {
            writeByte(address + i, data[i]);
        }
        fire(DebugEvent.of(DebugEvent.Type.MEMORY_CHANGED, address, "memory write", registers));
    }

    private void handleStop() throws DebugException {
        refreshRegisters();
        state = State.STOPPED;
        long rip = registers.rip();
        long trapAddr = rip > 0 ? rip - 1 : rip;
        Breakpoint bp = breakpoints.get(trapAddr);
        if (bp == null) {
            bp = breakpoints.get(rip);
        }
        if (bp != null) {
            writeByte(bp.address(), bp.originalByte());
            setRip(bp.address());
            refreshRegisters();
            fire(new DebugEvent(DebugEvent.Type.BREAKPOINT_HIT, bp.address(), "Breakpoint hit", registers, bp));
        }
        fire(DebugEvent.of(DebugEvent.Type.REGISTERS_CHANGED, registers.rip(), "registers", registers));
    }

    private void startWaiter() {
        stopWaiter();
        waitLoop.set(true);
        waiter = new Thread(() -> {
            try {
                waitStopped("cont");
                if (waitLoop.get()) {
                    handleStop();
                }
            } catch (DebugException e) {
                fire(DebugEvent.error(e.getMessage()));
            }
        }, "ptrace-wait");
        waiter.setDaemon(true);
        waiter.start();
    }

    private void stopWaiter() {
        waitLoop.set(false);
        if (waiter != null) {
            waiter.interrupt();
            waiter = null;
        }
    }

    private void waitStopped(String why) throws DebugException {
        IntByReference status = new IntByReference();
        int rc = libc.waitpid(pid, status, 0);
        if (rc < 0) {
            throw new DebugException("waitpid after " + why + " failed, errno=" + Native.getLastError());
        }
        int st = status.getValue();
        if (PtraceConstants.waitExited(st)) {
            state = State.DETACHED;
            fire(DebugEvent.of(DebugEvent.Type.STOPPED, 0, "Process exited", registers));
            throw new DebugException("Process exited");
        }
    }

    private void refreshRegisters() throws DebugException {
        UserRegsStruct regs = new UserRegsStruct();
        long rc = libc.ptrace(PtraceConstants.PTRACE_GETREGS, pid, Pointer.NULL, regs.getPointer());
        if (rc < 0) {
            throw new DebugException("PTRACE_GETREGS failed, errno=" + Native.getLastError());
        }
        regs.read();
        registers = regs.toRegisters();
    }

    private void setRip(long rip) throws DebugException {
        UserRegsStruct regs = new UserRegsStruct();
        long rc = libc.ptrace(PtraceConstants.PTRACE_GETREGS, pid, Pointer.NULL, regs.getPointer());
        if (rc < 0) {
            throw new DebugException("PTRACE_GETREGS failed, errno=" + Native.getLastError());
        }
        regs.read();
        regs.rip = rip;
        regs.write();
        rc = libc.ptrace(PtraceConstants.PTRACE_SETREGS, pid, Pointer.NULL, regs.getPointer());
        if (rc < 0) {
            throw new DebugException("PTRACE_SETREGS failed, errno=" + Native.getLastError());
        }
    }

    private long peek(long address) throws DebugException {
        Native.setLastError(0);
        long word = libc.ptrace(PtraceConstants.PTRACE_PEEKTEXT, pid, new Pointer(address), Pointer.NULL);
        int errno = Native.getLastError();
        if (word == -1L && errno != 0) {
            throw new DebugException("PTRACE_PEEKTEXT failed at 0x" + Long.toHexString(address) + ", errno=" + errno);
        }
        return word;
    }

    private void poke(long address, long word) throws DebugException {
        long rc = libc.ptrace(PtraceConstants.PTRACE_POKETEXT, pid, new Pointer(address), new Pointer(word));
        if (rc < 0) {
            throw new DebugException("PTRACE_POKETEXT failed at 0x" + Long.toHexString(address)
                    + ", errno=" + Native.getLastError());
        }
    }

    private void writeByte(long address, byte value) throws DebugException {
        long aligned = address & ~7L;
        int offset = (int) (address & 7);
        long word = peek(aligned);
        byte[] bytes = longToBytes(word);
        bytes[offset] = value;
        poke(aligned, bytesToLong(bytes));
    }

    private static byte[] longToBytes(long word) {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(word).array();
    }

    private static long bytesToLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    private void ensureAttached() throws DebugException {
        if (state == State.DETACHED || pid == 0) {
            throw new DebugException("Not attached");
        }
    }
}
