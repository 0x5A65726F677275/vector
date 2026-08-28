package com.artofvector.debugger.engine.nativeapi;

/**
 * Linux ptrace request codes and related constants (x86-64).
 */
public final class PtraceConstants {

    public static final int PTRACE_TRACEME = 0;
    public static final int PTRACE_PEEKTEXT = 1;
    public static final int PTRACE_PEEKDATA = 2;
    public static final int PTRACE_PEEKUSER = 3;
    public static final int PTRACE_POKETEXT = 4;
    public static final int PTRACE_POKEDATA = 5;
    public static final int PTRACE_POKEUSER = 6;
    public static final int PTRACE_CONT = 7;
    public static final int PTRACE_KILL = 8;
    public static final int PTRACE_SINGLESTEP = 9;
    public static final int PTRACE_GETREGS = 12;
    public static final int PTRACE_SETREGS = 13;
    public static final int PTRACE_ATTACH = 16;
    public static final int PTRACE_DETACH = 17;
    public static final int PTRACE_SYSCALL = 24;
    public static final int PTRACE_INTERRUPT = 0x4207;

    public static final int SIGTRAP = 5;
    public static final int SIGSTOP = 19;
    public static final int SIGKILL = 9;

    public static final byte INT3 = (byte) 0xCC;

    private PtraceConstants() {
    }

    public static boolean waitStopped(int status) {
        return (status & 0xff) == 0x7f;
    }

    public static int waitStopSignal(int status) {
        return (status >> 8) & 0xff;
    }

    public static boolean waitExited(int status) {
        return (status & 0x7f) == 0;
    }
}
