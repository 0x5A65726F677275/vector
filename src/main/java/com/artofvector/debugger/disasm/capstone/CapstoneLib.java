package com.artofvector.debugger.disasm.capstone;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * JNA binding for the Capstone C API ({@code cs_open}, {@code cs_disasm}, {@code cs_free}, {@code cs_close}).
 */
public interface CapstoneLib extends Library {

    int CS_ARCH_X86 = 3;
    int CS_MODE_64 = 1 << 3;
    int CS_MODE_32 = 1 << 2;

    static CapstoneLib load() {
        return Native.load("capstone", CapstoneLib.class);
    }

    int cs_open(int arch, int mode, LongByReference handle);

    long cs_disasm(long handle, byte[] code, long codeSize, long address, long count, PointerByReference insn);

    void cs_free(Pointer insn, long count);

    int cs_close(LongByReference handle);

    String cs_strerror(int code);
}
