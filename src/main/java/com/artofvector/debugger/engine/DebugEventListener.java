package com.artofvector.debugger.engine;

@FunctionalInterface
public interface DebugEventListener {

    void onDebugEvent(DebugEvent event);
}
