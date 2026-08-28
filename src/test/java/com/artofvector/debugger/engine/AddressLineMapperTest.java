package com.artofvector.debugger.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AddressLineMapperTest {

    @Test
    void mapsLineAndAddressBothWays() {
        AddressLineMapper mapper = new AddressLineMapper();
        mapper.map(10, 0x40100AL);
        assertEquals(0x40100AL, mapper.addressForLine(10).orElseThrow());
        assertEquals(10, mapper.lineForAddress(0x40100AL).orElseThrow());
    }

    @Test
    void floorsToNearestMappedAddress() {
        AddressLineMapper mapper = new AddressLineMapper();
        mapper.map(0, 0x401000L);
        mapper.map(1, 0x401004L);
        assertEquals(0, mapper.lineForAddress(0x401002L).orElseThrow());
        assertEquals(1, mapper.lineForAddress(0x401010L).orElseThrow());
    }

    @Test
    void identityMapFillsRange() {
        AddressLineMapper mapper = new AddressLineMapper();
        mapper.identityMap(0x1000, 8);
        assertEquals(8, mapper.size());
        assertTrue(mapper.addressForLine(3).isPresent());
    }
}
