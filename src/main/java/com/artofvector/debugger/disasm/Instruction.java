package com.artofvector.debugger.disasm;

public record Instruction(long address, byte[] bytes, String mnemonic, String operands) {

    public int size() {
        return bytes == null ? 0 : bytes.length;
    }

    public String text() {
        if (operands == null || operands.isBlank()) {
            return mnemonic;
        }
        return mnemonic + " " + operands;
    }
}
