package com.artofvector.debugger.disasm;

import java.util.List;

public interface Disassembler {

    List<Instruction> disassemble(byte[] code, long address);

    default Instruction disassembleOne(byte[] code, long address) {
        List<Instruction> list = disassemble(code, address);
        return list.isEmpty() ? null : list.get(0);
    }
}
