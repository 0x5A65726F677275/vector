package com.artofvector.debugger;

import com.artofvector.debugger.disasm.Disassembler;
import com.artofvector.debugger.disasm.DisassemblerFactory;
import com.artofvector.debugger.engine.AddressLineMapper;
import com.artofvector.debugger.engine.DebugSession;
import com.artofvector.debugger.engine.DebugSessionFactory;
import com.artofvector.debugger.engine.SimulatedDebugSession;

/**
 * Facade shared by the debugger UI and workflow nodes. Keeps backend construction in one place.
 */
public final class DebugService {

    private final DebugSession session;
    private final AddressLineMapper mapper = new AddressLineMapper();
    private final Disassembler disassembler = DisassemblerFactory.create();

    public DebugService() {
        this.session = DebugSessionFactory.create();
        mapper.identityMap(SimulatedDebugSession.BASE, 64);
    }

    public DebugSession session() {
        return session;
    }

    public AddressLineMapper mapper() {
        return mapper;
    }

    public Disassembler disassembler() {
        return disassembler;
    }
}
