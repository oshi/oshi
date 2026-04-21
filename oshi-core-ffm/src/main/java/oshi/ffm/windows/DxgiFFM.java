/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.driver.common.windows.gpu.DxgiAdapterInfo;
import oshi.ffm.ForeignFunctions;

/**
 * FFM binding to {@code dxgi.dll} for enumerating display adapters via COM vtable calls.
 */
public final class DxgiFFM extends ForeignFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(DxgiFFM.class);

    private static final int DXGI_ERROR_NOT_FOUND = 0x887A0002;

    // DXGI_ADAPTER_DESC layout (x64):
    // Description: WCHAR[128] = 256 bytes, VendorId/DeviceId/SubSysId/Revision: 4*4=16 bytes,
    // DedicatedVideoMemory/DedicatedSystemMemory/SharedSystemMemory: 3*8=24 bytes (SIZE_T on x64),
    // AdapterLuid: 8 bytes (DWORD LowPart + LONG HighPart)
    private static final long DESC_SIZE = 304;
    private static final long DESC_VENDOR_ID_OFFSET = 256;
    private static final long DESC_DEVICE_ID_OFFSET = 260;
    private static final long DESC_DEDICATED_VIDEO_MEMORY_OFFSET = 272;
    private static final long DESC_LUID_LOW_OFFSET = 296;
    private static final long DESC_LUID_HIGH_OFFSET = 300;

    // IID_IDXGIFactory {7B7166EC-21C7-44AE-B21A-C9AE321AE369}
    private static final byte[] IID_IDXGI_FACTORY = { (byte) 0xEC, 0x66, 0x71, 0x7B, (byte) 0xC7, 0x21, (byte) 0xAE,
            0x44, (byte) 0xB2, 0x1A, (byte) 0xC9, (byte) 0xAE, 0x32, 0x1A, (byte) 0xE3, 0x69 };

    private static final boolean AVAILABLE;
    private static final MethodHandle CREATE_DXGI_FACTORY;

    static {
        boolean available = false;
        MethodHandle hCreate = null;
        try {
            SymbolLookup dxgi = SymbolLookup.libraryLookup("dxgi", Arena.global());
            MemorySegment sym = dxgi.findOrThrow("CreateDXGIFactory");
            hCreate = Linker.nativeLinker().downcallHandle(sym, FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            available = true;
        } catch (Throwable t) {
            LOG.debug("dxgi.dll not available via FFM: {}", t.getMessage());
        }
        AVAILABLE = available;
        CREATE_DXGI_FACTORY = hCreate;
    }

    private DxgiFFM() {
    }

    /**
     * Enumerates all DXGI display adapters.
     *
     * @return list of {@link DxgiAdapterInfo}, one per adapter; empty if DXGI is unavailable
     */
    public static List<DxgiAdapterInfo> queryAdapters() {
        if (!AVAILABLE) {
            return Collections.emptyList();
        }
        try (Arena arena = Arena.ofConfined()) {
            // Write IID to memory
            MemorySegment riid = arena.allocate(16);
            riid.copyFrom(MemorySegment.ofArray(IID_IDXGI_FACTORY));

            // CreateDXGIFactory(riid, &ppFactory)
            MemorySegment ppFactory = arena.allocate(ADDRESS);
            int hr = (int) CREATE_DXGI_FACTORY.invokeExact(riid, ppFactory);
            if (hr < 0) {
                LOG.debug("CreateDXGIFactory failed: 0x{}", Integer.toHexString(hr));
                return Collections.emptyList();
            }

            MemorySegment factory = ppFactory.get(ADDRESS, 0).reinterpret(Long.MAX_VALUE, arena, null);
            List<DxgiAdapterInfo> result = new ArrayList<>();
            try {
                for (int i = 0;; i++) {
                    MemorySegment ppAdapter = arena.allocate(ADDRESS);
                    int enumHr = vtableCallEnumAdapters(factory, i, ppAdapter, arena);
                    if (enumHr == DXGI_ERROR_NOT_FOUND) {
                        break;
                    }
                    if (enumHr < 0) {
                        LOG.debug("IDXGIFactory::EnumAdapters({}) failed: 0x{}", i, Integer.toHexString(enumHr));
                        break;
                    }

                    MemorySegment adapter = ppAdapter.get(ADDRESS, 0).reinterpret(Long.MAX_VALUE, arena, null);
                    try {
                        MemorySegment desc = arena.allocate(DESC_SIZE);
                        int descHr = vtableCallGetDesc(adapter, desc, arena);
                        if (descHr >= 0) {
                            String name = readDescriptionWchars(desc);
                            int vendorId = desc.get(JAVA_INT, DESC_VENDOR_ID_OFFSET);
                            int deviceId = desc.get(JAVA_INT, DESC_DEVICE_ID_OFFSET);
                            long vram = desc.get(JAVA_LONG, DESC_DEDICATED_VIDEO_MEMORY_OFFSET);
                            int luidLow = desc.get(JAVA_INT, DESC_LUID_LOW_OFFSET);
                            int luidHigh = desc.get(JAVA_INT, DESC_LUID_HIGH_OFFSET);
                            result.add(new DxgiAdapterInfo(name, vendorId, deviceId, vram, luidLow, luidHigh));
                            LOG.debug("DXGI adapter {}: '{}' vendor=0x{} device=0x{} vram={} luid=0x{}_0x{}", i, name,
                                    Integer.toHexString(vendorId), Integer.toHexString(deviceId), vram,
                                    Integer.toHexString(luidHigh), Integer.toHexString(luidLow));
                        }
                    } finally {
                        vtableRelease(adapter, arena);
                    }
                }
            } finally {
                vtableRelease(factory, arena);
            }
            return Collections.unmodifiableList(result);
        } catch (Throwable t) {
            LOG.debug("DXGI enumeration failed: {}", t.getMessage());
            return Collections.emptyList();
        }
    }

    // IDXGIFactory::EnumAdapters is vtable slot 7
    private static int vtableCallEnumAdapters(MemorySegment factory, int index, MemorySegment ppAdapter, Arena arena)
            throws Throwable {
        MemorySegment vtable = factory.get(ADDRESS, 0).reinterpret(8 * 32L, arena, null);
        MemorySegment fn = vtable.get(ADDRESS, 7 * ADDRESS.byteSize());
        MethodHandle mh = Linker.nativeLinker().downcallHandle(fn,
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
        return (int) mh.invokeExact(factory, index, ppAdapter);
    }

    // IDXGIAdapter::GetDesc is vtable slot 8
    private static int vtableCallGetDesc(MemorySegment adapter, MemorySegment desc, Arena arena) throws Throwable {
        MemorySegment vtable = adapter.get(ADDRESS, 0).reinterpret(8 * 32L, arena, null);
        MemorySegment fn = vtable.get(ADDRESS, 8 * ADDRESS.byteSize());
        MethodHandle mh = Linker.nativeLinker().downcallHandle(fn, FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
        return (int) mh.invokeExact(adapter, desc);
    }

    // IUnknown::Release is vtable slot 2
    private static void vtableRelease(MemorySegment obj, Arena arena) throws Throwable {
        MemorySegment vtable = obj.get(ADDRESS, 0).reinterpret(8 * 32L, arena, null);
        MemorySegment fn = vtable.get(ADDRESS, 2 * ADDRESS.byteSize());
        MethodHandle mh = Linker.nativeLinker().downcallHandle(fn, FunctionDescriptor.of(JAVA_INT, ADDRESS));
        mh.invokeExact(obj);
    }

    private static String readDescriptionWchars(MemorySegment desc) {
        // Description is WCHAR[128] at offset 0
        int len = 0;
        while (len < 128 && desc.get(JAVA_CHAR, len * 2L) != 0) {
            len++;
        }
        char[] chars = new char[len];
        for (int i = 0; i < len; i++) {
            chars[i] = desc.get(JAVA_CHAR, i * 2L);
        }
        return new String(chars);
    }
}
