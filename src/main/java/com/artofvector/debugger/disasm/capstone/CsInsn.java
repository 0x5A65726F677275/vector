package com.artofvector.debugger.disasm.capstone;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

@Structure.FieldOrder({"id", "address", "size", "bytes", "mnemonic", "opStr", "detail"})
public class CsInsn extends Structure {

    public int id;
    public long address;
    public short size;
    public byte[] bytes = new byte[24];
    public byte[] mnemonic = new byte[32];
    public byte[] opStr = new byte[160];
    public Pointer detail;

    public CsInsn() {
    }

    public CsInsn(Pointer pointer) {
        super(pointer);
        read();
    }

    public String mnemonicString() {
        return NativeString.of(mnemonic);
    }

    public String opString() {
        return NativeString.of(opStr);
    }

    public byte[] codeBytes() {
        int len = size & 0xffff;
        if (len <= 0 || len > bytes.length) {
            len = Math.min(16, bytes.length);
        }
        byte[] out = new byte[len];
        System.arraycopy(bytes, 0, out, 0, len);
        return out;
    }

    private static final class NativeString {
        private NativeString() {
        }

        static String of(byte[] raw) {
            int end = 0;
            while (end < raw.length && raw[end] != 0) {
                end++;
            }
            return new String(raw, 0, end, java.nio.charset.StandardCharsets.US_ASCII);
        }
    }
}
