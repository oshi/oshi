/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FFM bindings for CfgMgr32.dll (Configuration Manager) functions.
 */
public final class Cfgmgr32FFM extends WindowsForeignFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(Cfgmgr32FFM.class);

    private static final SymbolLookup CFGMGR32 = lib("CfgMgr32");

    public static final int CR_SUCCESS = 0;

    // Device registry property constants
    public static final int CM_DRP_DEVICEDESC = 0x00000001;
    public static final int CM_DRP_SERVICE = 0x00000005;
    public static final int CM_DRP_CLASS = 0x00000008;
    public static final int CM_DRP_MFG = 0x0000000C;
    public static final int CM_DRP_FRIENDLYNAME = 0x0000000D;

    private static final MethodHandle CM_Get_Child = downcall(CFGMGR32, "CM_Get_Child", JAVA_INT, ADDRESS, JAVA_INT,
            JAVA_INT);

    /**
     * Gets the first child of a device instance.
     *
     * @param pdnDevInst receives the child device instance handle
     * @param dnDevInst  the parent device instance
     * @param ulFlags    must be 0
     * @return CR_SUCCESS or error code
     */
    public static int CM_Get_Child(MemorySegment pdnDevInst, int dnDevInst, int ulFlags) throws Throwable {
        return (int) CM_Get_Child.invokeExact(pdnDevInst, dnDevInst, ulFlags);
    }

    private static final MethodHandle CM_Get_Sibling = downcall(CFGMGR32, "CM_Get_Sibling", JAVA_INT, ADDRESS, JAVA_INT,
            JAVA_INT);

    /**
     * Gets the next sibling of a device instance.
     *
     * @param pdnDevInst receives the sibling device instance handle
     * @param dnDevInst  the device instance
     * @param ulFlags    must be 0
     * @return CR_SUCCESS or error code
     */
    public static int CM_Get_Sibling(MemorySegment pdnDevInst, int dnDevInst, int ulFlags) throws Throwable {
        return (int) CM_Get_Sibling.invokeExact(pdnDevInst, dnDevInst, ulFlags);
    }

    private static final MethodHandle CM_Get_Device_IDW = downcall(CFGMGR32, "CM_Get_Device_IDW", JAVA_INT, JAVA_INT,
            ADDRESS, JAVA_INT, JAVA_INT);

    /**
     * Gets the device instance ID string.
     *
     * @param dnDevInst the device instance
     * @param buffer    receives the device ID string (wide chars)
     * @param bufferLen length of buffer in characters
     * @param ulFlags   must be 0
     * @return CR_SUCCESS or error code
     */
    public static int CM_Get_Device_ID(int dnDevInst, MemorySegment buffer, int bufferLen, int ulFlags)
            throws Throwable {
        return (int) CM_Get_Device_IDW.invokeExact(dnDevInst, buffer, bufferLen, ulFlags);
    }

    private static final MethodHandle CM_Get_DevNode_Registry_PropertyW = downcall(CFGMGR32,
            "CM_Get_DevNode_Registry_PropertyW", JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT);

    /**
     * Gets a device instance registry property.
     *
     * @param dnDevInst      the device instance
     * @param ulProperty     the property to retrieve (CM_DRP_*)
     * @param pulRegDataType receives the registry data type (may be NULL)
     * @param buffer         receives the property value
     * @param pulLength      on input, buffer size in bytes; on output, bytes written
     * @param ulFlags        must be 0
     * @return CR_SUCCESS or error code
     */
    public static int CM_Get_DevNode_Registry_Property(int dnDevInst, int ulProperty, MemorySegment pulRegDataType,
            MemorySegment buffer, MemorySegment pulLength, int ulFlags) throws Throwable {
        return (int) CM_Get_DevNode_Registry_PropertyW.invokeExact(dnDevInst, ulProperty, pulRegDataType, buffer,
                pulLength, ulFlags);
    }

    /**
     * Convenience method to get a device ID as a String.
     *
     * @param dnDevInst the device instance
     * @param arena     arena for allocation
     * @return the device ID string, or empty string on failure
     */
    public static String getDeviceId(int dnDevInst, Arena arena) {
        try {
            MemorySegment buf = arena.allocate(520); // MAX_DEVICE_ID_LEN = 200 chars * 2 = 400 bytes; 520 provides
                                                     // margin
            if (CM_Get_Device_ID(dnDevInst, buf, 260, 0) == CR_SUCCESS) {
                return readWideString(buf);
            }
        } catch (Throwable t) {
            LOG.debug("CM_Get_Device_ID failed for node {}", dnDevInst);
        }
        return "";
    }

    /**
     * Convenience method to get a device node registry property as a String.
     *
     * @param dnDevInst  the device instance
     * @param ulProperty the property constant (CM_DRP_*)
     * @param buf        pre-allocated buffer (at least MAX_PATH * 2 bytes)
     * @param sizeSeg    pre-allocated int segment for size
     * @return the property value string, or empty string on failure
     */
    public static String getDevNodeProperty(int dnDevInst, int ulProperty, MemorySegment buf, MemorySegment sizeSeg) {
        try {
            buf.fill((byte) 0);
            sizeSeg.set(JAVA_INT, 0, (int) buf.byteSize());
            if (CM_Get_DevNode_Registry_Property(dnDevInst, ulProperty, MemorySegment.NULL, buf, sizeSeg,
                    0) == CR_SUCCESS) {
                return readWideString(buf);
            }
        } catch (Throwable t) {
            LOG.debug("CM_Get_DevNode_Registry_Property failed for node {} property {}", dnDevInst, ulProperty);
        }
        return "";
    }
}
