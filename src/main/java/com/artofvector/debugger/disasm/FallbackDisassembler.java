package com.artofvector.debugger.disasm;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal x86-64 decoder used when the Capstone native library is not installed.
 */
public final class FallbackDisassembler implements Disassembler {

    @Override
    public List<Instruction> disassemble(byte[] code, long address) {
        List<Instruction> out = new ArrayList<>();
        int i = 0;
        while (i < code.length) {
            Decoded decoded = decodeOne(code, i);
            byte[] bytes = java.util.Arrays.copyOfRange(code, i, i + decoded.length);
            out.add(new Instruction(address + i, bytes, decoded.mnemonic, decoded.operands));
            i += decoded.length;
        }
        return out;
    }

    private static Decoded decodeOne(byte[] code, int offset) {
        int b = code[offset] & 0xff;
        boolean rexW = false;
        int pos = offset;
        if (b >= 0x40 && b <= 0x4f) {
            rexW = (b & 0x8) != 0;
            pos++;
            if (pos >= code.length) {
                return new Decoded(1, "db", hex(b));
            }
            b = code[pos] & 0xff;
        }
        int lengthPrefix = pos - offset;
        return switch (b) {
            case 0x90 -> new Decoded(lengthPrefix + 1, "nop", "");
            case 0xCC -> new Decoded(lengthPrefix + 1, "int3", "");
            case 0xC3 -> new Decoded(lengthPrefix + 1, "ret", "");
            case 0x55 -> new Decoded(lengthPrefix + 1, "push", "rbp");
            case 0x5D -> new Decoded(lengthPrefix + 1, "pop", "rbp");
            case 0x50 -> new Decoded(lengthPrefix + 1, "push", "rax");
            case 0x58 -> new Decoded(lengthPrefix + 1, "pop", "rax");
            case 0xC9 -> new Decoded(lengthPrefix + 1, "leave", "");
            case 0x89 -> decodeMov(code, pos, lengthPrefix, rexW);
            case 0x83 -> decodeAluImm8(code, pos, lengthPrefix);
            case 0x31 -> decodeXor(code, pos, lengthPrefix, rexW);
            case 0xE8 -> decodeRel(code, pos, lengthPrefix, 4, "call");
            case 0xE9 -> decodeRel(code, pos, lengthPrefix, 4, "jmp");
            case 0xEB -> decodeRel(code, pos, lengthPrefix, 1, "jmp");
            default -> new Decoded(lengthPrefix + 1, "db", hex(b));
        };
    }

    private static Decoded decodeMov(byte[] code, int pos, int prefix, boolean rexW) {
        if (pos + 1 >= code.length) {
            return new Decoded(prefix + 1, "db", hex(code[pos] & 0xff));
        }
        int modrm = code[pos + 1] & 0xff;
        if (modrm == 0xE5) {
            return new Decoded(prefix + 2, "mov", rexW ? "rbp, rsp" : "ebp, esp");
        }
        if (modrm == 0xEC) {
            return new Decoded(prefix + 2, "mov", rexW ? "rsp, rbp" : "esp, ebp");
        }
        return new Decoded(prefix + 2, "mov", "r/m, r");
    }

    private static Decoded decodeAluImm8(byte[] code, int pos, int prefix) {
        if (pos + 2 >= code.length) {
            return new Decoded(prefix + 1, "db", hex(code[pos] & 0xff));
        }
        int modrm = code[pos + 1] & 0xff;
        int imm = code[pos + 2];
        String dest = (modrm == 0xEC) ? "rsp" : (modrm == 0xC0 ? "rax" : "r/m");
        int op = (modrm >> 3) & 7;
        String mnemonic = switch (op) {
            case 0 -> "add";
            case 4 -> "and";
            case 5 -> "sub";
            default -> "alu";
        };
        return new Decoded(prefix + 3, mnemonic, dest + ", " + imm);
    }

    private static Decoded decodeXor(byte[] code, int pos, int prefix, boolean rexW) {
        if (pos + 1 >= code.length) {
            return new Decoded(prefix + 1, "db", hex(code[pos] & 0xff));
        }
        int modrm = code[pos + 1] & 0xff;
        if (modrm == 0xC0) {
            return new Decoded(prefix + 2, "xor", rexW ? "rax, rax" : "eax, eax");
        }
        return new Decoded(prefix + 2, "xor", "r/m, r");
    }

    private static Decoded decodeRel(byte[] code, int pos, int prefix, int immSize, String mnemonic) {
        if (pos + immSize >= code.length) {
            return new Decoded(prefix + 1, "db", hex(code[pos] & 0xff));
        }
        return new Decoded(prefix + 1 + immSize, mnemonic, "rel");
    }

    private static String hex(int value) {
        return String.format("0x%02x", value);
    }

    private record Decoded(int length, String mnemonic, String operands) {
    }
}
