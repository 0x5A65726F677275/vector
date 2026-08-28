package com.artofvector.debugger.engine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.artofvector.debugger.disasm.Disassembler;
import com.artofvector.debugger.disasm.FallbackDisassembler;
import com.artofvector.debugger.disasm.Instruction;
import com.artofvector.log.AppLog;

/**
 * In-process debug target so the workbench is usable on Windows and macOS.
 * A small x86-64 stub is loaded at {@link #BASE}.
 */
public final class SimulatedDebugSession extends AbstractDebugSession {

    public static final long BASE = 0x401000L;
    public static final long STACK_TOP = 0x7FFFFFFFE000L;

    private static final byte[] STUB = {
            0x55,                                         // push rbp
            0x48, (byte) 0x89, (byte) 0xE5,               // mov rbp, rsp
            0x48, (byte) 0x83, (byte) 0xEC, 0x20,         // sub rsp, 0x20
            (byte) 0x90,                                  // nop
            0x48, 0x31, (byte) 0xC0,                      // xor rax, rax
            0x48, (byte) 0x83, (byte) 0xC0, 0x01,         // add rax, 1
            (byte) 0x90,                                  // nop
            0x48, (byte) 0x89, (byte) 0xEC,               // mov rsp, rbp
            0x5D,                                         // pop rbp
            (byte) 0xC3                                   // ret
    };

    private final SparseMemory memory = new SparseMemory();
    private final Disassembler decoder = new FallbackDisassembler();
    private final AtomicBoolean runRequested = new AtomicBoolean(false);
    private Thread runThread;

    @Override
    public synchronized void attach(int pid) {
        memory.load(BASE, STUB);
        registers = initialRegisters(BASE);
        state = State.STOPPED;
        AppLog.info("Simulated target attached" + (pid > 0 ? " (pid hint " + pid + ")" : "")
                + " at 0x" + Long.toHexString(BASE));
        fire(DebugEvent.of(DebugEvent.Type.ATTACHED, BASE, "Simulated process attached", registers));
        fire(DebugEvent.of(DebugEvent.Type.REGISTERS_CHANGED, BASE, "registers", registers));
    }

    @Override
    public synchronized void detach() {
        stopRunLoop();
        state = State.DETACHED;
        fire(DebugEvent.of(DebugEvent.Type.DETACHED, 0, "Detached", registers));
    }

    @Override
    public void cont() throws DebugException {
        ensureAttached();
        if (state == State.RUNNING) {
            return;
        }
        state = State.RUNNING;
        fire(DebugEvent.of(DebugEvent.Type.CONTINUED, registers.rip(), "Continue", registers));
        runRequested.set(true);
        runThread = new Thread(this::runLoop, "sim-debug-run");
        runThread.setDaemon(true);
        runThread.start();
    }

    @Override
    public void pause() throws DebugException {
        ensureAttached();
        runRequested.set(false);
        state = State.STOPPED;
        fire(DebugEvent.of(DebugEvent.Type.PAUSED, registers.rip(), "Paused", registers));
    }

    @Override
    public synchronized void stop() {
        stopRunLoop();
        breakpoints.clear();
        state = State.DETACHED;
        fire(DebugEvent.of(DebugEvent.Type.STOPPED, 0, "Stopped", registers));
    }

    @Override
    public synchronized void stepInto() throws DebugException {
        ensureAttached();
        stepOnce(true);
    }

    @Override
    public synchronized void stepOver() throws DebugException {
        ensureAttached();
        Instruction insn = peekCurrent();
        if (insn != null && "call".equalsIgnoreCase(insn.mnemonic())) {
            long after = insn.address() + insn.size();
            setBreakpoint(after);
            cont();
            return;
        }
        stepOnce(true);
    }

    @Override
    public synchronized void setBreakpoint(long address) throws DebugException {
        ensureAttached();
        if (breakpoints.containsKey(address)) {
            return;
        }
        byte original = memory.readByte(address);
        memory.writeByte(address, int3());
        breakpoints.put(address, new Breakpoint(address, original, true));
        AppLog.info("Breakpoint set at 0x" + Long.toHexString(address));
        fire(DebugEvent.of(DebugEvent.Type.MEMORY_CHANGED, address, "breakpoint set", registers));
    }

    @Override
    public synchronized void removeBreakpoint(long address) throws DebugException {
        Breakpoint bp = breakpoints.remove(address);
        if (bp != null) {
            memory.writeByte(address, bp.originalByte());
            fire(DebugEvent.of(DebugEvent.Type.MEMORY_CHANGED, address, "breakpoint removed", registers));
        }
    }

    @Override
    public synchronized byte[] readMemory(long address, int size) {
        return memory.read(address, size);
    }

    @Override
    public synchronized void writeMemory(long address, byte[] data) {
        memory.write(address, data);
        fire(DebugEvent.of(DebugEvent.Type.MEMORY_CHANGED, address, "memory write", registers));
    }

    public byte[] stubImage() {
        return STUB.clone();
    }

    private void runLoop() {
        try {
            while (runRequested.get() && state == State.RUNNING) {
                boolean hit = stepOnce(false);
                if (hit) {
                    break;
                }
                if (registers.rip() < BASE || registers.rip() >= BASE + STUB.length) {
                    state = State.STOPPED;
                    fire(DebugEvent.of(DebugEvent.Type.STOPPED, registers.rip(), "Fell off stub", registers));
                    break;
                }
            }
        } catch (DebugException e) {
            state = State.STOPPED;
            fire(DebugEvent.error(e.getMessage()));
        }
    }

    private synchronized boolean stepOnce(boolean notifyStep) throws DebugException {
        ensureAttached();
        long rip = registers.rip();
        byte current = memory.readByte(rip);
        Breakpoint bp = breakpoints.get(rip);
        if (current == int3() && bp != null) {
            memory.writeByte(rip, bp.originalByte());
            state = State.STOPPED;
            runRequested.set(false);
            fire(new DebugEvent(DebugEvent.Type.BREAKPOINT_HIT, rip, "Breakpoint hit", registers, bp));
            return true;
        }
        Instruction insn = peekCurrent();
        int size = insn == null ? 1 : Math.max(1, insn.size());
        applySideEffects(insn);
        registers = withRip(registers.rip() + size);
        if (notifyStep) {
            state = State.STOPPED;
            fire(DebugEvent.of(DebugEvent.Type.STEPPED, registers.rip(), "Step", registers));
            fire(DebugEvent.of(DebugEvent.Type.REGISTERS_CHANGED, registers.rip(), "registers", registers));
        }
        return false;
    }

    private Instruction peekCurrent() {
        byte[] window = memory.read(registers.rip(), 16);
        List<Instruction> decoded = decoder.disassemble(window, registers.rip());
        return decoded.isEmpty() ? null : decoded.get(0);
    }

    private void applySideEffects(Instruction insn) {
        if (insn == null) {
            return;
        }
        Map<String, Long> map = new LinkedHashMap<>(registers.asMap());
        if ("xor".equals(insn.mnemonic()) && insn.operands().contains("rax")) {
            map.put("rax", 0L);
        }
        if ("add".equals(insn.mnemonic()) && insn.operands().startsWith("rax")) {
            map.put("rax", map.getOrDefault("rax", 0L) + 1);
        }
        if ("push".equals(insn.mnemonic())) {
            map.put("rsp", map.getOrDefault("rsp", STACK_TOP) - 8);
        }
        if ("pop".equals(insn.mnemonic())) {
            map.put("rsp", map.getOrDefault("rsp", STACK_TOP) + 8);
        }
        if ("sub".equals(insn.mnemonic()) && insn.operands().startsWith("rsp")) {
            map.put("rsp", map.getOrDefault("rsp", STACK_TOP) - 0x20);
        }
        registers = new Registers(map);
    }

    private Registers withRip(long rip) {
        Map<String, Long> map = new LinkedHashMap<>(registers.asMap());
        map.put("rip", rip);
        return new Registers(map);
    }

    private static Registers initialRegisters(long rip) {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("rax", 0L);
        map.put("rbx", 0L);
        map.put("rcx", 0L);
        map.put("rdx", 0L);
        map.put("rsi", 0L);
        map.put("rdi", 0L);
        map.put("rbp", STACK_TOP);
        map.put("rsp", STACK_TOP);
        map.put("r8", 0L);
        map.put("r9", 0L);
        map.put("r10", 0L);
        map.put("r11", 0L);
        map.put("r12", 0L);
        map.put("r13", 0L);
        map.put("r14", 0L);
        map.put("r15", 0L);
        map.put("rip", rip);
        map.put("eflags", 0x202L);
        return new Registers(map);
    }

    private void stopRunLoop() {
        runRequested.set(false);
        if (runThread != null) {
            try {
                runThread.join(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            runThread = null;
        }
    }

    private void ensureAttached() throws DebugException {
        if (state == State.DETACHED) {
            throw new DebugException("Not attached");
        }
    }
}
