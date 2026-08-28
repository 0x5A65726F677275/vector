package com.artofvector.debugger.engine;

public class DebugException extends Exception {

    public DebugException(String message) {
        super(message);
    }

    public DebugException(String message, Throwable cause) {
        super(message, cause);
    }
}
