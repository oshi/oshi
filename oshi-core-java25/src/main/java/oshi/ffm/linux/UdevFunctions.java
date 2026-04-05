/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.linux;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.ForeignFunctions;
import oshi.util.GlobalConfig;

/**
 * FFM bindings for the udev (userspace device management) library.
 * <p>
 * libudev provides APIs to introspect and enumerate devices on the local Linux system. All udev objects are opaque
 * {@link MemorySegment} handles. Callers must unref context, enumerate, and device handles when done.
 * <p>
 * Loading can be suppressed via the {@code oshi.os.linux.allowudev} configuration property.
 */
public final class UdevFunctions extends ForeignFunctions {

    private UdevFunctions() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(UdevFunctions.class);

    private static final boolean AVAILABLE;

    private static final MethodHandle udev_new;
    private static final MethodHandle udev_unref;
    private static final MethodHandle udev_enumerate_new;
    private static final MethodHandle udev_enumerate_unref;
    private static final MethodHandle udev_enumerate_add_match_subsystem;
    private static final MethodHandle udev_enumerate_scan_devices;
    private static final MethodHandle udev_enumerate_get_list_entry;
    private static final MethodHandle udev_list_entry_get_next;
    private static final MethodHandle udev_list_entry_get_name;
    private static final MethodHandle udev_device_new_from_syspath;
    private static final MethodHandle udev_device_unref;
    private static final MethodHandle udev_device_get_parent;
    private static final MethodHandle udev_device_get_parent_with_subsystem_devtype;
    private static final MethodHandle udev_device_get_syspath;
    private static final MethodHandle udev_device_get_sysname;
    private static final MethodHandle udev_device_get_devnode;
    private static final MethodHandle udev_device_get_devtype;
    private static final MethodHandle udev_device_get_subsystem;
    private static final MethodHandle udev_device_get_sysattr_value;
    private static final MethodHandle udev_device_get_property_value;

    static {
        boolean available = false;
        MethodHandle hNew = null;
        MethodHandle hUnref = null;
        MethodHandle hEnumNew = null;
        MethodHandle hEnumUnref = null;
        MethodHandle hEnumAddMatch = null;
        MethodHandle hEnumScan = null;
        MethodHandle hEnumGetList = null;
        MethodHandle hListGetNext = null;
        MethodHandle hListGetName = null;
        MethodHandle hDevNewFromSyspath = null;
        MethodHandle hDevUnref = null;
        MethodHandle hDevGetParent = null;
        MethodHandle hDevGetParentSubDev = null;
        MethodHandle hDevGetSyspath = null;
        MethodHandle hDevGetSysname = null;
        MethodHandle hDevGetDevnode = null;
        MethodHandle hDevGetDevtype = null;
        MethodHandle hDevGetSubsystem = null;
        MethodHandle hDevGetSysattr = null;
        MethodHandle hDevGetProperty = null;
        try {
            if (!GlobalConfig.get(GlobalConfig.OSHI_OS_LINUX_ALLOWUDEV, true)) {
                LOG.info("Loading of udev not allowed by configuration. Some features may not work.");
            } else {
                SymbolLookup lib = loadUdevLibrary();
                hNew = LINKER.downcallHandle(lib.findOrThrow("udev_new"), FunctionDescriptor.of(ADDRESS));
                hUnref = LINKER.downcallHandle(lib.findOrThrow("udev_unref"), FunctionDescriptor.ofVoid(ADDRESS));
                hEnumNew = LINKER.downcallHandle(lib.findOrThrow("udev_enumerate_new"),
                        FunctionDescriptor.of(ADDRESS, ADDRESS));
                hEnumUnref = LINKER.downcallHandle(lib.findOrThrow("udev_enumerate_unref"),
                        FunctionDescriptor.ofVoid(ADDRESS));
                hEnumAddMatch = LINKER.downcallHandle(lib.findOrThrow("udev_enumerate_add_match_subsystem"),
                        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
                hEnumScan = LINKER.downcallHandle(lib.findOrThrow("udev_enumerate_scan_devices"),
                        FunctionDescriptor.of(JAVA_INT, ADDRESS));
                hEnumGetList = LINKER.downcallHandle(lib.findOrThrow("udev_enumerate_get_list_entry"),
                        FunctionDescriptor.of(ADDRESS, ADDRESS));
                hListGetNext = LINKER.downcallHandle(lib.findOrThrow("udev_list_entry_get_next"),
                        FunctionDescriptor.of(ADDRESS, ADDRESS));
                hListGetName = LINKER.downcallHandle(lib.findOrThrow("udev_list_entry_get_name"),
                        FunctionDescriptor.of(ADDRESS, ADDRESS));
                hDevNewFromSyspath = LINKER.downcallHandle(lib.findOrThrow("udev_device_new_from_syspath"),
                        FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
                hDevUnref = LINKER.downcallHandle(lib.findOrThrow("udev_device_unref"),
                        FunctionDescriptor.ofVoid(ADDRESS));
                hDevGetParent = LINKER.downcallHandle(lib.findOrThrow("udev_device_get_parent"),
                        FunctionDescriptor.of(ADDRESS, ADDRESS));
                hDevGetParentSubDev = LINKER.downcallHandle(
                        lib.findOrThrow("udev_device_get_parent_with_subsystem_devtype"),
                        FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));
                hDevGetSyspath = LINKER.downcallHandle(lib.findOrThrow("udev_device_get_syspath"),
                        FunctionDescriptor.of(ADDRESS, ADDRESS));
                hDevGetSysname = LINKER.downcallHandle(lib.findOrThrow("udev_device_get_sysname"),
                        FunctionDescriptor.of(ADDRESS, ADDRESS));
                hDevGetDevnode = LINKER.downcallHandle(lib.findOrThrow("udev_device_get_devnode"),
                        FunctionDescriptor.of(ADDRESS, ADDRESS));
                hDevGetDevtype = LINKER.downcallHandle(lib.findOrThrow("udev_device_get_devtype"),
                        FunctionDescriptor.of(ADDRESS, ADDRESS));
                hDevGetSubsystem = LINKER.downcallHandle(lib.findOrThrow("udev_device_get_subsystem"),
                        FunctionDescriptor.of(ADDRESS, ADDRESS));
                hDevGetSysattr = LINKER.downcallHandle(lib.findOrThrow("udev_device_get_sysattr_value"),
                        FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
                hDevGetProperty = LINKER.downcallHandle(lib.findOrThrow("udev_device_get_property_value"),
                        FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
                available = true;
            }
        } catch (Throwable e) {
            LOG.warn("Did not find udev library in operating system. Some features may not work. {}", e.toString());
        }
        AVAILABLE = available;
        udev_new = hNew;
        udev_unref = hUnref;
        udev_enumerate_new = hEnumNew;
        udev_enumerate_unref = hEnumUnref;
        udev_enumerate_add_match_subsystem = hEnumAddMatch;
        udev_enumerate_scan_devices = hEnumScan;
        udev_enumerate_get_list_entry = hEnumGetList;
        udev_list_entry_get_next = hListGetNext;
        udev_list_entry_get_name = hListGetName;
        udev_device_new_from_syspath = hDevNewFromSyspath;
        udev_device_unref = hDevUnref;
        udev_device_get_parent = hDevGetParent;
        udev_device_get_parent_with_subsystem_devtype = hDevGetParentSubDev;
        udev_device_get_syspath = hDevGetSyspath;
        udev_device_get_sysname = hDevGetSysname;
        udev_device_get_devnode = hDevGetDevnode;
        udev_device_get_devtype = hDevGetDevtype;
        udev_device_get_subsystem = hDevGetSubsystem;
        udev_device_get_sysattr_value = hDevGetSysattr;
        udev_device_get_property_value = hDevGetProperty;
    }

    /**
     * Attempts to load libudev by trying the unversioned name first, then known versioned sonames. The unversioned
     * {@code libudev.so} is only present when the development package is installed; the versioned names are present on
     * all runtime installations.
     *
     * @return the {@link SymbolLookup} for libudev
     * @throws IllegalArgumentException if none of the candidate names can be loaded
     */
    private static SymbolLookup loadUdevLibrary() {
        for (String name : new String[] { "udev", "libudev.so.1", "libudev.so.0" }) {
            try {
                return name.startsWith("lib") ? SymbolLookup.libraryLookup(name, LIBRARY_ARENA) : libraryLookup(name);
            } catch (IllegalArgumentException ignored) {
                // try next candidate name
            }
        }
        throw new IllegalArgumentException("Cannot open library: libudev (tried udev, libudev.so.1, libudev.so.0)");
    }

    /**
     * Returns whether libudev was successfully loaded and all symbols bound.
     *
     * @return {@code true} if libudev is available
     */
    public static boolean isAvailable() {
        return AVAILABLE;
    }

    // ---- Context ----

    /**
     * Allocates a new udev context. The caller must call {@link #udev_unref} when done.
     *
     * @return opaque udev context handle, or {@link MemorySegment#NULL} on failure
     * @throws Throwable if the native call fails
     */
    public static MemorySegment udev_new() throws Throwable {
        return (MemorySegment) udev_new.invokeExact();
    }

    /**
     * Drops a reference to a udev context. Once the reference count hits 0, the context is destroyed.
     *
     * @param udev the udev context handle
     * @throws Throwable if the native call fails
     */
    public static void udev_unref(MemorySegment udev) throws Throwable {
        udev_unref.invokeExact(udev);
    }

    // ---- Enumerate ----

    /**
     * Creates a new udev enumerate object. The caller must call {@link #udev_enumerate_unref} when done.
     *
     * @param udev the udev context handle
     * @return opaque enumerate handle, or {@link MemorySegment#NULL} on failure
     * @throws Throwable if the native call fails
     */
    public static MemorySegment udev_enumerate_new(MemorySegment udev) throws Throwable {
        return (MemorySegment) udev_enumerate_new.invokeExact(udev);
    }

    /**
     * Drops a reference to a udev enumerate object.
     *
     * @param enumerate the enumerate handle
     * @throws Throwable if the native call fails
     */
    public static void udev_enumerate_unref(MemorySegment enumerate) throws Throwable {
        udev_enumerate_unref.invokeExact(enumerate);
    }

    /**
     * Adds a subsystem filter to the enumerate object.
     *
     * @param enumerate the enumerate handle
     * @param subsystem the subsystem name as a native string segment
     * @return 0 or greater on success
     * @throws Throwable if the native call fails
     */
    public static int udev_enumerate_add_match_subsystem(MemorySegment enumerate, MemorySegment subsystem)
            throws Throwable {
        return (int) udev_enumerate_add_match_subsystem.invokeExact(enumerate, subsystem);
    }

    /**
     * Scans {@code /sys} for devices matching the enumerate filters.
     *
     * @param enumerate the enumerate handle
     * @return 0 or greater on success
     * @throws Throwable if the native call fails
     */
    public static int udev_enumerate_scan_devices(MemorySegment enumerate) throws Throwable {
        return (int) udev_enumerate_scan_devices.invokeExact(enumerate);
    }

    /**
     * Gets the first list entry from an enumerate object.
     *
     * @param enumerate the enumerate handle
     * @return the first list entry handle, or {@link MemorySegment#NULL} if empty
     * @throws Throwable if the native call fails
     */
    public static MemorySegment udev_enumerate_get_list_entry(MemorySegment enumerate) throws Throwable {
        return (MemorySegment) udev_enumerate_get_list_entry.invokeExact(enumerate);
    }

    // ---- List entry ----

    /**
     * Gets the next entry in a list.
     *
     * @param entry the current list entry handle
     * @return the next list entry handle, or {@link MemorySegment#NULL} if none
     * @throws Throwable if the native call fails
     */
    public static MemorySegment udev_list_entry_get_next(MemorySegment entry) throws Throwable {
        return (MemorySegment) udev_list_entry_get_next.invokeExact(entry);
    }

    /**
     * Gets the name (syspath) of a list entry.
     *
     * @param entry the list entry handle
     * @return a native pointer to the name string, or {@link MemorySegment#NULL} on failure
     * @throws Throwable if the native call fails
     */
    public static MemorySegment udev_list_entry_get_name(MemorySegment entry) throws Throwable {
        return (MemorySegment) udev_list_entry_get_name.invokeExact(entry);
    }

    // ---- Device ----

    /**
     * Creates a udev device from a syspath. The caller must call {@link #udev_device_unref} when done.
     *
     * @param udev    the udev context handle
     * @param syspath native string segment for the {@code /sys} path
     * @return opaque device handle, or {@link MemorySegment#NULL} on failure
     * @throws Throwable if the native call fails
     */
    public static MemorySegment udev_device_new_from_syspath(MemorySegment udev, MemorySegment syspath)
            throws Throwable {
        return (MemorySegment) udev_device_new_from_syspath.invokeExact(udev, syspath);
    }

    /**
     * Drops a reference to a udev device.
     *
     * @param device the device handle
     * @throws Throwable if the native call fails
     */
    public static void udev_device_unref(MemorySegment device) throws Throwable {
        udev_device_unref.invokeExact(device);
    }

    /**
     * Gets the parent device. The returned handle is owned by the child; do not unref it separately.
     *
     * @param device the device handle
     * @return the parent device handle, or {@link MemorySegment#NULL} if none
     * @throws Throwable if the native call fails
     */
    public static MemorySegment udev_device_get_parent(MemorySegment device) throws Throwable {
        return (MemorySegment) udev_device_get_parent.invokeExact(device);
    }

    /**
     * Gets the first parent device matching a subsystem and devtype. The returned handle is owned by the child; do not
     * unref it separately.
     *
     * @param device    the device handle
     * @param subsystem native string segment for the subsystem to match
     * @param devtype   native string segment for the devtype to match
     * @return the matching parent device handle, or {@link MemorySegment#NULL} if none
     * @throws Throwable if the native call fails
     */
    public static MemorySegment udev_device_get_parent_with_subsystem_devtype(MemorySegment device,
            MemorySegment subsystem, MemorySegment devtype) throws Throwable {
        return (MemorySegment) udev_device_get_parent_with_subsystem_devtype.invokeExact(device, subsystem, devtype);
    }

    /**
     * Gets the syspath of a device.
     *
     * @param device the device handle
     * @return a native pointer to the syspath string, or {@link MemorySegment#NULL} on failure
     * @throws Throwable if the native call fails
     */
    public static MemorySegment udev_device_get_syspath(MemorySegment device) throws Throwable {
        return (MemorySegment) udev_device_get_syspath.invokeExact(device);
    }

    /**
     * Gets the sysname of a device.
     *
     * @param device the device handle
     * @return a native pointer to the sysname string, or {@link MemorySegment#NULL} on failure
     * @throws Throwable if the native call fails
     */
    public static MemorySegment udev_device_get_sysname(MemorySegment device) throws Throwable {
        return (MemorySegment) udev_device_get_sysname.invokeExact(device);
    }

    /**
     * Gets the devnode of a device (e.g. {@code /dev/sda}).
     *
     * @param device the device handle
     * @return a native pointer to the devnode string, or {@link MemorySegment#NULL} on failure
     * @throws Throwable if the native call fails
     */
    public static MemorySegment udev_device_get_devnode(MemorySegment device) throws Throwable {
        return (MemorySegment) udev_device_get_devnode.invokeExact(device);
    }

    /**
     * Gets the devtype of a device (e.g. {@code "disk"}, {@code "partition"}, {@code "usb_device"}).
     *
     * @param device the device handle
     * @return a native pointer to the devtype string, or {@link MemorySegment#NULL} on failure
     * @throws Throwable if the native call fails
     */
    public static MemorySegment udev_device_get_devtype(MemorySegment device) throws Throwable {
        return (MemorySegment) udev_device_get_devtype.invokeExact(device);
    }

    /**
     * Gets the subsystem of a device (e.g. {@code "block"}, {@code "net"}, {@code "usb"}).
     *
     * @param device the device handle
     * @return a native pointer to the subsystem string, or {@link MemorySegment#NULL} on failure
     * @throws Throwable if the native call fails
     */
    public static MemorySegment udev_device_get_subsystem(MemorySegment device) throws Throwable {
        return (MemorySegment) udev_device_get_subsystem.invokeExact(device);
    }

    /**
     * Retrieves a sysfs attribute value from a device.
     *
     * @param device  the device handle
     * @param sysattr native string segment for the attribute name
     * @return a native pointer to the attribute value string, or {@link MemorySegment#NULL} on failure
     * @throws Throwable if the native call fails
     */
    public static MemorySegment udev_device_get_sysattr_value(MemorySegment device, MemorySegment sysattr)
            throws Throwable {
        return (MemorySegment) udev_device_get_sysattr_value.invokeExact(device, sysattr);
    }

    /**
     * Retrieves a udev property value from a device.
     *
     * @param device the device handle
     * @param key    native string segment for the property key
     * @return a native pointer to the property value string, or {@link MemorySegment#NULL} on failure
     * @throws Throwable if the native call fails
     */
    public static MemorySegment udev_device_get_property_value(MemorySegment device, MemorySegment key)
            throws Throwable {
        return (MemorySegment) udev_device_get_property_value.invokeExact(device, key);
    }

    // ---- Convenience helpers ----

    /**
     * Reads a udev string result (syspath, sysattr, property, etc.) into a Java String, using the provided arena for
     * the reinterpret scope.
     *
     * @param ptr   the native pointer returned by a udev getter
     * @param arena the arena to scope the reinterpreted segment to
     * @return the Java string, or {@code null} if the pointer is null or {@link MemorySegment#NULL}
     */
    public static String getString(MemorySegment ptr, Arena arena) {
        return getStringFromNativePointer(ptr, arena);
    }

    /**
     * Allocates a native UTF-8 string in the given arena and calls
     * {@link #udev_enumerate_add_match_subsystem(MemorySegment, MemorySegment)}.
     *
     * @param enumerate the enumerate handle
     * @param subsystem the subsystem name
     * @param arena     the arena to allocate the string in
     * @return 0 or greater on success
     * @throws Throwable if the native call fails
     */
    public static int addMatchSubsystem(MemorySegment enumerate, String subsystem, Arena arena) throws Throwable {
        return udev_enumerate_add_match_subsystem(enumerate, arena.allocateFrom(subsystem));
    }

    /**
     * Allocates a native UTF-8 string in the given arena and calls
     * {@link #udev_device_new_from_syspath(MemorySegment, MemorySegment)}.
     *
     * @param udev    the udev context handle
     * @param syspath the syspath string
     * @param arena   the arena to allocate the string in
     * @return opaque device handle, or {@link MemorySegment#NULL} on failure
     * @throws Throwable if the native call fails
     */
    public static MemorySegment deviceNewFromSyspath(MemorySegment udev, String syspath, Arena arena) throws Throwable {
        return udev_device_new_from_syspath(udev, arena.allocateFrom(syspath));
    }

    /**
     * Allocates native UTF-8 strings in the given arena and calls
     * {@link #udev_device_get_sysattr_value(MemorySegment, MemorySegment)}.
     *
     * @param device  the device handle
     * @param sysattr the attribute name
     * @param arena   the arena to allocate the string in
     * @return the attribute value, or {@code null} if not found
     * @throws Throwable if the native call fails
     */
    public static String getSysattrValue(MemorySegment device, String sysattr, Arena arena) throws Throwable {
        return getString(udev_device_get_sysattr_value(device, arena.allocateFrom(sysattr)), arena);
    }

    /**
     * Allocates a native UTF-8 string in the given arena and calls
     * {@link #udev_device_get_property_value(MemorySegment, MemorySegment)}.
     *
     * @param device the device handle
     * @param key    the property key
     * @param arena  the arena to allocate the string in
     * @return the property value, or {@code null} if not found
     * @throws Throwable if the native call fails
     */
    public static String getPropertyValue(MemorySegment device, String key, Arena arena) throws Throwable {
        return getString(udev_device_get_property_value(device, arena.allocateFrom(key)), arena);
    }

    /**
     * Allocates native UTF-8 strings in the given arena and calls
     * {@link #udev_device_get_parent_with_subsystem_devtype(MemorySegment, MemorySegment, MemorySegment)}.
     *
     * @param device    the device handle
     * @param subsystem the subsystem to match
     * @param devtype   the devtype to match
     * @param arena     the arena to allocate strings in
     * @return the matching parent device handle, or {@link MemorySegment#NULL} if none
     * @throws Throwable if the native call fails
     */
    public static MemorySegment getParentWithSubsystemDevtype(MemorySegment device, String subsystem, String devtype,
            Arena arena) throws Throwable {
        return udev_device_get_parent_with_subsystem_devtype(device, arena.allocateFrom(subsystem),
                arena.allocateFrom(devtype));
    }
}
