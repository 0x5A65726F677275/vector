package com.artofvector.debugger.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SimulatedDebugSessionTest {

    @Test
    void attachStepAndBreakpoint() throws Exception {
        SimulatedDebugSession session = new SimulatedDebugSession();
        session.attach(0);
        assertTrue(session.isAttached());
        long start = session.getRegisters().rip();
        assertEquals(SimulatedDebugSession.BASE, start);

        session.setBreakpoint(start);
        assertTrue(session.hasBreakpoint(start));
        session.stepInto();
        assertEquals(DebugSession.State.STOPPED, session.getState());

        byte[] dump = session.readMemory(SimulatedDebugSession.BASE, 4);
        assertEquals(4, dump.length);
        session.stop();
        assertEquals(DebugSession.State.DETACHED, session.getState());
    }
}
