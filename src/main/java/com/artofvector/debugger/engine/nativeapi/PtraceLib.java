package com.artofvector.debugger.engine.nativeapi;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * JNA binding to libc ptrace / waitpid. Only loaded on Linux.
 */
public interface PtraceLib extends Library {

    static PtraceLib load() {
        if (!Platform.isLinux()) {
            throw new IllegalStateException("ptrace is only available on Linux");
        }
        return Native.load("c", PtraceLib.class);
    }

    long ptrace(int request, int pid, Pointer addr, Pointer data);

    int waitpid(int pid, IntByReference status, int options);

    int kill(int pid, int sig);
}
