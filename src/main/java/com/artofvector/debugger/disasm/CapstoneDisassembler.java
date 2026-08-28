package com.artofvector.debugger.disasm;

import java.util.ArrayList;
import java.util.List;

import com.artofvector.debugger.disasm.capstone.CapstoneLib;
import com.artofvector.debugger.disasm.capstone.CsInsn;
import com.artofvector.log.AppLog;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * Capstone-backed disassembler. The native {@code capstone} library must be on the JNA search path.
 */
public final class CapstoneDisassembler implements Disassembler, AutoCloseable {

    private final CapstoneLib lib;
    private final LongByReference handle = new LongByReference();
    private boolean open;

    public CapstoneDisassembler() {
        this.lib = CapstoneLib.load();
        int err = lib.cs_open(CapstoneLib.CS_ARCH_X86, CapstoneLib.CS_MODE_64, handle);
        if (err != 0) {
            throw new IllegalStateException("cs_open failed: " + lib.cs_strerror(err));
        }
        open = true;
        AppLog.info("Capstone engine opened (x86-64).");
    }

    @Override
    public synchronized List<Instruction> disassemble(byte[] code, long address) {
        if (!open || code == null || code.length == 0) {
            return List.of();
        }
        PointerByReference insnRef = new PointerByReference();
        long count = lib.cs_disasm(handle.getValue(), code, code.length, address, 0, insnRef);
        if (count <= 0) {
            return List.of();
        }
        List<Instruction> out = new ArrayList<>((int) count);
        Pointer base = insnRef.getValue();
        CsInsn probe = new CsInsn();
        int stride = probe.size();
        for (int i = 0; i < count; i++) {
            CsInsn insn = new CsInsn(base.share((long) i * stride));
            out.add(new Instruction(insn.address, insn.codeBytes(), insn.mnemonicString(), insn.opString()));
        }
        lib.cs_free(base, count);
        return out;
    }

    @Override
    public synchronized void close() {
        if (open) {
            lib.cs_close(handle);
            open = false;
        }
    }
}
