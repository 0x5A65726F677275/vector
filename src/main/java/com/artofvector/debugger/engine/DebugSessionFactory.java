package com.artofvector.debugger.engine;

import com.artofvector.log.AppLog;
import com.sun.jna.Platform;

public final class DebugSessionFactory {

    private DebugSessionFactory() {
    }

    public static DebugSession create() {
        if (Platform.isLinux()) {
            try {
                return new LinuxPtraceDebugSession();
            } catch (UnsatisfiedLinkError | Exception e) {
                AppLog.warn("Could not load ptrace backend: " + e.getMessage() + ". Falling back to simulation.");
            }
        } else {
            AppLog.info("ptrace is Linux-only on this host (" + System.getProperty("os.name")
                    + "). Using simulated debug session.");
        }
        return new SimulatedDebugSession();
    }
}
