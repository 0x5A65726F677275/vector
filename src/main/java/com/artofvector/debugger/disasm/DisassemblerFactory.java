package com.artofvector.debugger.disasm;

import com.artofvector.log.AppLog;

public final class DisassemblerFactory {

    private DisassemblerFactory() {
    }

    public static Disassembler create() {
        try {
            return new CapstoneDisassembler();
        } catch (UnsatisfiedLinkError | Exception e) {
            AppLog.warn("Capstone native library not available (" + e.getMessage()
                    + "). Using fallback decoder. Install libcapstone for full disassembly.");
            return new FallbackDisassembler();
        }
    }
}
