package com.artofvector.debugger.engine.nativeapi;

import java.util.LinkedHashMap;
import java.util.Map;

import com.artofvector.debugger.engine.Registers;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Linux x86-64 {@code user_regs_struct} as used by PTRACE_GETREGS / PTRACE_SETREGS.
 */
@Structure.FieldOrder({
        "r15", "r14", "r13", "r12", "rbp", "rbx", "r11", "r10",
        "r9", "r8", "rax", "rcx", "rdx", "rsi", "rdi", "orig_rax",
        "rip", "cs", "eflags", "rsp", "ss", "fs_base", "gs_base",
        "ds", "es", "fs", "gs"
})
public class UserRegsStruct extends Structure implements Structure.ByReference {

    public long r15;
    public long r14;
    public long r13;
    public long r12;
    public long rbp;
    public long rbx;
    public long r11;
    public long r10;
    public long r9;
    public long r8;
    public long rax;
    public long rcx;
    public long rdx;
    public long rsi;
    public long rdi;
    public long orig_rax;
    public long rip;
    public long cs;
    public long eflags;
    public long rsp;
    public long ss;
    public long fs_base;
    public long gs_base;
    public long ds;
    public long es;
    public long fs;
    public long gs;

    public UserRegsStruct() {
    }

    public UserRegsStruct(Pointer pointer) {
        super(pointer);
        read();
    }

    public Registers toRegisters() {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("rax", rax);
        map.put("rbx", rbx);
        map.put("rcx", rcx);
        map.put("rdx", rdx);
        map.put("rsi", rsi);
        map.put("rdi", rdi);
        map.put("rbp", rbp);
        map.put("rsp", rsp);
        map.put("r8", r8);
        map.put("r9", r9);
        map.put("r10", r10);
        map.put("r11", r11);
        map.put("r12", r12);
        map.put("r13", r13);
        map.put("r14", r14);
        map.put("r15", r15);
        map.put("rip", rip);
        map.put("eflags", eflags);
        map.put("cs", cs);
        map.put("ss", ss);
        map.put("ds", ds);
        map.put("es", es);
        map.put("fs", fs);
        map.put("gs", gs);
        map.put("orig_rax", orig_rax);
        return new Registers(map);
    }
}
