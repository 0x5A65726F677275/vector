package com.artofvector.debugger.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.TreeMap;

/**
 * Bidirectional map between editor source lines (0-based) and load addresses.
 */
public final class AddressLineMapper {

    private final Map<Integer, Long> lineToAddress = new HashMap<>();
    private final TreeMap<Long, Integer> addressToLine = new TreeMap<>();

    public synchronized void map(int line, long address) {
        lineToAddress.put(line, address);
        addressToLine.put(address, line);
    }

    public synchronized void clear() {
        lineToAddress.clear();
        addressToLine.clear();
    }

    public synchronized OptionalLong addressForLine(int line) {
        Long address = lineToAddress.get(line);
        return address == null ? OptionalLong.empty() : OptionalLong.of(address);
    }

    public synchronized OptionalInt lineForAddress(long address) {
        Integer exact = addressToLine.get(address);
        if (exact != null) {
            return OptionalInt.of(exact);
        }
        Map.Entry<Long, Integer> floor = addressToLine.floorEntry(address);
        return floor == null ? OptionalInt.empty() : OptionalInt.of(floor.getValue());
    }

    public synchronized int size() {
        return lineToAddress.size();
    }

    /**
     * Default identity mapping used by the simulated target: line N ↔ base + N.
     */
    public synchronized void identityMap(long base, int lineCount) {
        clear();
        for (int i = 0; i < lineCount; i++) {
            map(i, base + i);
        }
    }
}
