/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.windows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.win32.COM.COMInvoker;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.Guid.REFIID;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;

import oshi.driver.windows.DxgiAdapterInfo;

/**
 * Minimal JNA binding to {@code dxgi.dll} for enumerating display adapters and reading
 * {@code DXGI_ADAPTER_DESC.DedicatedVideoMemory}.
 *
 * <p>
 * DXGI is the authoritative Windows API for dedicated GPU memory. It is not subject to the 2 GiB cap that affects the
 * 32-bit registry value {@code HardwareInformation.MemorySize}.
 *
 * <p>
 * COM vtable layout used here:
 *
 * <pre>
 * IUnknown   (vtable slots 0-2):  QueryInterface, AddRef, Release
 * IDXGIObject (slots 3-6):        SetPrivateData, SetPrivateDataInterface, GetPrivateData, GetParent
 * IDXGIFactory (slots 7-13):      EnumAdapters, MakeWindowAssociation, GetWindowAssociation,
 *                                  CreateSwapChain, CreateSoftwareAdapter, (IDXGIFactory1 adds 2 more)
 * IDXGIAdapter (slots 3-5 on adapter): EnumOutputs, GetDesc, CheckInterfaceSupport
 * </pre>
 *
 * <p>
 * This class should be considered non-API as it may be removed if/when its code is incorporated into the JNA project.
 */
public final class WindowsDxgi {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsDxgi.class);

    // IID for IDXGIFactory {7B7166EC-21C7-44AE-B21A-C9AE321AE369}
    private static final com.sun.jna.platform.win32.Guid.IID IID_IDXGI_FACTORY = new com.sun.jna.platform.win32.Guid.IID(
            "{7B7166EC-21C7-44AE-B21A-C9AE321AE369}");

    // DXGI_ERROR_NOT_FOUND — returned by EnumAdapters when index is out of range
    private static final int DXGI_ERROR_NOT_FOUND = 0x887A0002;

    private WindowsDxgi() {
    }

    // -------------------------------------------------------------------------
    // Native function entry point
    // -------------------------------------------------------------------------

    /**
     * Minimal binding to {@code dxgi.dll} just to call {@code CreateDXGIFactory}.
     */
    private interface DxgiLib extends StdCallLibrary {
        DxgiLib INSTANCE = Native.load("dxgi", DxgiLib.class);

        /**
         * Creates a DXGI factory object.
         *
         * @param riid      IID of the factory interface to create (IDXGIFactory)
         * @param ppFactory receives the factory pointer
         * @return HRESULT
         */
        int CreateDXGIFactory(REFIID riid, PointerByReference ppFactory);
    }

    private static final boolean DXGI_AVAILABLE;
    static {
        boolean available = false;
        try {
            @SuppressWarnings("unused")
            DxgiLib lib = DxgiLib.INSTANCE;
            available = true;
        } catch (UnsatisfiedLinkError e) {
            LOG.debug("dxgi.dll not available: {}", e.getMessage());
        } catch (NoClassDefFoundError e) {
            LOG.debug("JNA DxgiLib class failed to load: {}", e.getMessage());
        }
        DXGI_AVAILABLE = available;
    }

    // -------------------------------------------------------------------------
    // DXGI_ADAPTER_DESC structure
    // -------------------------------------------------------------------------

    /**
     * Maps to the Windows {@code DXGI_ADAPTER_DESC} structure.
     *
     * <p>
     * Layout (x64):
     * <ul>
     * <li>Description: WCHAR[128] = 256 bytes</li>
     * <li>VendorId: UINT = 4 bytes</li>
     * <li>DeviceId: UINT = 4 bytes</li>
     * <li>SubSysId: UINT = 4 bytes</li>
     * <li>Revision: UINT = 4 bytes</li>
     * <li>DedicatedVideoMemory: SIZE_T = 8 bytes (x64)</li>
     * <li>DedicatedSystemMemory: SIZE_T = 8 bytes</li>
     * <li>SharedSystemMemory: SIZE_T = 8 bytes</li>
     * <li>AdapterLuid: LUID = 8 bytes</li>
     * </ul>
     */
    @FieldOrder({ "Description", "VendorId", "DeviceId", "SubSysId", "Revision", "DedicatedVideoMemory",
            "DedicatedSystemMemory", "SharedSystemMemory", "AdapterLuidLowPart", "AdapterLuidHighPart" })
    public static class DXGI_ADAPTER_DESC extends Structure {
        public char[] Description = new char[128];
        public int VendorId;
        public int DeviceId;
        public int SubSysId;
        public int Revision;
        // SIZE_T is pointer-sized; use long (8 bytes on x64, 4 bytes on x86)
        public com.sun.jna.platform.win32.BaseTSD.SIZE_T DedicatedVideoMemory;
        public com.sun.jna.platform.win32.BaseTSD.SIZE_T DedicatedSystemMemory;
        public com.sun.jna.platform.win32.BaseTSD.SIZE_T SharedSystemMemory;
        public int AdapterLuidLowPart;
        public int AdapterLuidHighPart;
    }

    // -------------------------------------------------------------------------
    // IDXGIAdapter vtable wrapper
    // -------------------------------------------------------------------------

    /**
     * Wraps an {@code IDXGIAdapter} COM pointer for vtable-based invocation.
     *
     * <p>
     * IDXGIAdapter vtable (inherits IUnknown + IDXGIObject):
     *
     * <pre>
     * slot 0  QueryInterface  (IUnknown)
     * slot 1  AddRef          (IUnknown)
     * slot 2  Release         (IUnknown)
     * slot 3  SetPrivateData  (IDXGIObject)
     * slot 4  SetPrivateDataInterface (IDXGIObject)
     * slot 5  GetPrivateData  (IDXGIObject)
     * slot 6  GetParent       (IDXGIObject)
     * slot 7  EnumOutputs     (IDXGIAdapter)
     * slot 8  GetDesc         (IDXGIAdapter)
     * slot 9  CheckInterfaceSupport (IDXGIAdapter)
     * </pre>
     */
    private static final class DxgiAdapter extends COMInvoker {

        DxgiAdapter(Pointer p) {
            setPointer(p);
        }

        HRESULT GetDesc(DXGI_ADAPTER_DESC desc) {
            return (HRESULT) _invokeNativeObject(8, new Object[] { getPointer(), desc.getPointer() }, HRESULT.class);
        }

        int Release() {
            return _invokeNativeInt(2, new Object[] { getPointer() });
        }
    }

    // -------------------------------------------------------------------------
    // IDXGIFactory vtable wrapper
    // -------------------------------------------------------------------------

    /**
     * Wraps an {@code IDXGIFactory} COM pointer for vtable-based invocation.
     *
     * <p>
     * IDXGIFactory vtable (inherits IUnknown + IDXGIObject):
     *
     * <pre>
     * slot 0  QueryInterface  (IUnknown)
     * slot 1  AddRef          (IUnknown)
     * slot 2  Release         (IUnknown)
     * slot 3  SetPrivateData  (IDXGIObject)
     * slot 4  SetPrivateDataInterface (IDXGIObject)
     * slot 5  GetPrivateData  (IDXGIObject)
     * slot 6  GetParent       (IDXGIObject)
     * slot 7  EnumAdapters    (IDXGIFactory)
     * </pre>
     */
    private static final class DxgiFactory extends COMInvoker {

        DxgiFactory(Pointer p) {
            setPointer(p);
        }

        HRESULT EnumAdapters(int index, PointerByReference ppAdapter) {
            return (HRESULT) _invokeNativeObject(7, new Object[] { getPointer(), index, ppAdapter.getPointer() },
                    HRESULT.class);
        }

        int Release() {
            return _invokeNativeInt(2, new Object[] { getPointer() });
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Enumerates all DXGI display adapters and returns their identity and dedicated video memory.
     *
     * <p>
     * Fails gracefully: if {@code dxgi.dll} is unavailable or any COM call fails, returns an empty list so callers can
     * fall back to registry-based detection.
     *
     * @return list of {@link DxgiAdapterInfo}, one per adapter; empty if DXGI is unavailable
     */
    public static List<DxgiAdapterInfo> queryAdapters() {
        if (!DXGI_AVAILABLE) {
            return Collections.emptyList();
        }
        PointerByReference ppFactory = new PointerByReference();
        REFIID riid = new REFIID(IID_IDXGI_FACTORY);

        int hr = DxgiLib.INSTANCE.CreateDXGIFactory(riid, ppFactory);

        if (COMUtils.FAILED(new HRESULT(hr))) {
            LOG.debug("CreateDXGIFactory failed: 0x{}", Integer.toHexString(hr));
            return Collections.emptyList();
        }

        DxgiFactory factory = new DxgiFactory(ppFactory.getValue());
        List<DxgiAdapterInfo> result = new ArrayList<>();
        try {
            for (int i = 0;; i++) {
                PointerByReference ppAdapter = new PointerByReference();
                // EnumAdapters takes a PointerByReference but the vtable slot expects the raw
                // pointer-to-pointer; pass the Pointer directly via the PointerByReference value.
                HRESULT enumHr = factory.EnumAdapters(i, ppAdapter);
                if (enumHr.intValue() == DXGI_ERROR_NOT_FOUND) {
                    break;
                }
                if (COMUtils.FAILED(enumHr)) {
                    LOG.debug("IDXGIFactory::EnumAdapters({}) failed: 0x{}", i, Integer.toHexString(enumHr.intValue()));
                    break;
                }

                DxgiAdapter adapter = new DxgiAdapter(ppAdapter.getValue());
                try {
                    DXGI_ADAPTER_DESC desc = new DXGI_ADAPTER_DESC();
                    HRESULT descHr = adapter.GetDesc(desc);
                    if (COMUtils.SUCCEEDED(descHr)) {
                        desc.read();
                        String name = Native.toString(desc.Description);
                        long vram = desc.DedicatedVideoMemory.longValue();
                        result.add(new DxgiAdapterInfo(name, desc.VendorId, desc.DeviceId, vram));
                        LOG.debug("DXGI adapter {}: '{}' vendor=0x{} device=0x{} vram={}", i, name,
                                Integer.toHexString(desc.VendorId), Integer.toHexString(desc.DeviceId), vram);
                    } else {
                        LOG.debug("IDXGIAdapter::GetDesc({}) failed: 0x{}", i, Integer.toHexString(descHr.intValue()));
                    }
                } finally {
                    adapter.Release();
                }
            }
        } finally {
            factory.Release();
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Finds the best-matching DXGI adapter for a given vendor ID, device ID, and adapter name.
     *
     * <p>
     * Matching priority:
     * <ol>
     * <li>Vendor ID + Device ID (exact, both non-zero)</li>
     * <li>Normalized name match (case-insensitive, ignoring {@code (R)}, {@code (TM)}, extra spaces)</li>
     * </ol>
     *
     * <p>
     * If multiple adapters share the same vendor+device ID (e.g. multi-GPU), the first one is returned. If no confident
     * match is found, returns {@code null}.
     *
     * @param adapters    list from {@link #queryAdapters()}
     * @param vendorId    PCI vendor ID parsed from the registry key (0 if unknown)
     * @param deviceId    PCI device ID parsed from the registry key (0 if unknown)
     * @param adapterName adapter name from the registry {@code DriverDesc} value
     * @return best-matching adapter, or {@code null}
     */
    public static DxgiAdapterInfo findMatch(List<DxgiAdapterInfo> adapters, int vendorId, int deviceId,
            String adapterName) {
        // Priority 1: vendor + device ID
        if (vendorId != 0 && deviceId != 0) {
            for (DxgiAdapterInfo a : adapters) {
                if (a.getVendorId() == vendorId && a.getDeviceId() == deviceId) {
                    return a;
                }
            }
        }
        // Priority 2: normalized name
        if (adapterName != null && !adapterName.isEmpty()) {
            String norm = normalizeName(adapterName);
            for (DxgiAdapterInfo a : adapters) {
                if (normalizeName(a.getDescription()).equals(norm)) {
                    return a;
                }
            }
        }
        return null;
    }

    /**
     * Converts a registry value (REG_QWORD as Long, REG_DWORD as Integer, or REG_BINARY as byte[]) to a VRAM size in
     * bytes. REG_BINARY is interpreted as little-endian.
     *
     * <p>
     * Exposed as public for testing.
     *
     * @param value the registry value object
     * @return the VRAM size in bytes, or 0 if the value type is unrecognised
     */
    public static long registryValueToVram(Object value) {
        if (value instanceof Long) {
            return (long) value;
        } else if (value instanceof Integer) {
            return Integer.toUnsignedLong((int) value);
        } else if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            // Import is not available here; inline the little-endian conversion
            long total = 0L;
            int size = Math.min(bytes.length, 8);
            for (int i = 0; i < size; i++) {
                total = total << 8 | bytes[size - i - 1] & 0xff;
            }
            return total;
        }
        return 0L;
    }

    /**
     * Normalizes an adapter name for fuzzy matching: lower-case, strips {@code (R)}/{@code (TM)}, collapses whitespace.
     *
     * @param name the raw adapter name, may be {@code null}
     * @return normalized name, never {@code null}
     */
    public static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(java.util.Locale.ROOT).replace("(r)", "").replace("(tm)", "").replaceAll("\\s+", " ")
                .trim();
    }
}
