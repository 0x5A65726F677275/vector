package com.artofvector.debugger.engine;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;

/**
 * Fan-out for debugger observers. UI listeners are invoked on the EDT.
 */
public final class DebugEventSupport {

    private final List<DebugEventListener> listeners = new CopyOnWriteArrayList<>();

    public void add(DebugEventListener listener) {
        listeners.add(listener);
    }

    public void remove(DebugEventListener listener) {
        listeners.remove(listener);
    }

    public void fire(DebugEvent event) {
        Runnable emit = () -> {
            for (DebugEventListener listener : listeners) {
                listener.onDebugEvent(event);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            emit.run();
        } else {
            SwingUtilities.invokeLater(emit);
        }
    }
}
