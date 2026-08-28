package com.artofvector.debugger.engine;

import java.util.Arrays;
import java.util.TreeMap;

/**
 * Sparse 4 KiB-paged virtual memory used by the simulated debug target.
 */
public final class SparseMemory {

    private static final int PAGE = 4096;
    private final TreeMap<Long, byte[]> pages = new TreeMap<>();

    public synchronized byte readByte(long address) {
        byte[] page = pages.get(pageOf(address));
        if (page == null) {
            return 0;
        }
        return page[offset(address)];
    }

    public synchronized void writeByte(long address, byte value) {
        byte[] page = pages.computeIfAbsent(pageOf(address), key -> new byte[PAGE]);
        page[offset(address)] = value;
    }

    public synchronized byte[] read(long address, int size) {
        byte[] out = new byte[size];
        for (int i = 0; i < size; i++) {
            out[i] = readByte(address + i);
        }
        return out;
    }

    public synchronized void write(long address, byte[] data) {
        for (int i = 0; i < data.length; i++) {
            writeByte(address + i, data[i]);
        }
    }

    public synchronized void load(long address, byte[] image) {
        write(address, Arrays.copyOf(image, image.length));
    }

    private static long pageOf(long address) {
        return address & ~((long) PAGE - 1);
    }

    private static int offset(long address) {
        return (int) (address & (PAGE - 1));
    }
}
