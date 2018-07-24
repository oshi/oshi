/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.jna.platform.windows;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

/**
 * Windows Cfgmgr32.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface Cfgmgr32 extends Library {
    Cfgmgr32 INSTANCE = Native.loadLibrary("Cfgmgr32", Cfgmgr32.class, W32APIOptions.DEFAULT_OPTIONS);

    final static int CR_SUCCESS = 0;
    final static int CR_BUFFER_SMALL = 0x0000001A;

    final static int CM_LOCATE_DEVNODE_NORMAL = 0;
    final static int CM_LOCATE_DEVNODE_PHANTOM = 1;
    final static int CM_LOCATE_DEVNODE_CANCELREMOVE = 2;
    final static int CM_LOCATE_DEVNODE_NOVALIDATION = 4;
    final static int CM_LOCATE_DEVNODE_BITS = 7;

    /**
     * The CM_Locate_DevNode function obtains a device instance handle to the
     * device node that is associated with a specified device instance ID on the
     * local machine.
     * 
     * @param pdnDevInst
     *            A pointer to a device instance handle that CM_Locate_DevNode
     *            retrieves. The retrieved handle is bound to the local machine.
     * @param pDeviceID
     *            A pointer to a NULL-terminated string representing a device
     *            instance ID. If this value is NULL, or if it points to a
     *            zero-length string, the function retrieves a device instance
     *            handle to the device at the root of the device tree. *
     * @param ulFlags
     *            A variable of ULONG type that supplies one of the following
     *            flag values that apply if the caller supplies a device
     *            instance identifier: CM_LOCATE_DEVNODE_NORMAL,
     *            CM_LOCATE_DEVNODE_PHANTOM, CM_LOCATE_DEVNODE_CANCELREMOVE, or
     *            CM_LOCATE_DEVNODE_NOVALIDATION
     * @return If the operation succeeds, CM_Locate_DevNode returns CR_SUCCESS.
     *         Otherwise, the function returns one of the CR_Xxx error codes
     *         that are defined in Cfgmgr32.h.
     * @see <A HREF=
     *      "https://docs.microsoft.com/en-us/windows/desktop/api/cfgmgr32/nf-cfgmgr32-cm_locate_devnodea">
     *      CM_Locate_DevNode</A>
     */
    int CM_Locate_DevNode(IntByReference pdnDevInst, String pDeviceID, int ulFlags);

    /**
     * The CM_Get_Parent function obtains a device instance handle to the parent
     * node of a specified device node (devnode) in the local machine's device
     * tree.
     * 
     * @param pdnDevInst
     *            Caller-supplied pointer to the device instance handle to the
     *            parent node that this function retrieves. The retrieved handle
     *            is bound to the local machine.
     * @param dnDevInst
     *            Caller-supplied device instance handle that is bound to the
     *            local machine.
     * @param ulFlags
     *            Not used, must be zero.
     * @return If the operation succeeds, the function returns CR_SUCCESS.
     *         Otherwise, it returns one of the CR_-prefixed error codes defined
     *         in Cfgmgr32.h.
     * @see <A HREF=
     *      "https://docs.microsoft.com/en-us/windows/desktop/api/cfgmgr32/nf-cfgmgr32-cm_get_parent">
     *      CM_Get_Parent</A>
     */
    int CM_Get_Parent(IntByReference pdnDevInst, int dnDevInst, int ulFlags);

    /**
     * The CM_Get_Child function is used to retrieve a device instance handle to
     * the first child node of a specified device node (devnode) in the local
     * machine's device tree.
     * 
     * @param pdnDevInst
     *            Caller-supplied pointer to the device instance handle to the
     *            child node that this function retrieves. The retrieved handle
     *            is bound to the local machine.
     * @param dnDevInst
     *            Caller-supplied device instance handle that is bound to the
     *            local machine.
     * @param ulFlags
     *            Not used, must be zero.
     * @return If the operation succeeds, the function returns CR_SUCCESS.
     *         Otherwise, it returns one of the CR_-prefixed error codes defined
     *         in Cfgmgr32.h.
     * @see <A HREF=
     *      "https://docs.microsoft.com/en-us/windows/desktop/api/cfgmgr32/nf-cfgmgr32-cm_get_child">
     *      CM_Get_Child</A>
     */
    int CM_Get_Child(IntByReference pdnDevInst, int dnDevInst, int ulFlags);

    /**
     * The CM_Get_Sibling function obtains a device instance handle to the next
     * sibling node of a specified device node (devnode) in the local machine's
     * device tree.
     * 
     * @param pdnDevInst
     *            Caller-supplied pointer to the device instance handle to the
     *            sibling node that this function retrieves. The retrieved
     *            handle is bound to the local machine.
     * @param dnDevInst
     *            Caller-supplied device instance handle that is bound to the
     *            local machine.
     * @param ulFlags
     *            Not used, must be zero.
     * @return If the operation succeeds, the function returns CR_SUCCESS.
     *         Otherwise, it returns one of the CR_-prefixed error codes defined
     *         in Cfgmgr32.h.
     * @see <A HREF=
     *      "https://docs.microsoft.com/en-us/windows/desktop/api/cfgmgr32/nf-cfgmgr32-cm_get_sibling">
     *      CM_Get_Sibling</A>
     */
    int CM_Get_Sibling(IntByReference pdnDevInst, int dnDevInst, int ulFlags);

    /**
     * The CM_Get_Device_ID function retrieves the device instance ID for a
     * specified device instance on the local machine.
     * 
     * @param devInst
     *            Caller-supplied device instance handle that is bound to the
     *            local machine.
     * @param Buffer
     *            Address of a buffer to receive a device instance ID string.
     *            The required buffer size can be obtained by calling
     *            CM_Get_Device_ID_Size, then incrementing the received value to
     *            allow room for the string's terminating NULL.
     * @param BufferLen
     *            Caller-supplied length, in characters, of the buffer specified
     *            by Buffer.
     * @param ulFlags
     *            Not used, must be zero.
     * @return If the operation succeeds, the function returns CR_SUCCESS.
     *         Otherwise, it returns one of the CR_-prefixed error codes defined
     *         in Cfgmgr32.h.
     * @see <A HREF=
     *      "https://docs.microsoft.com/en-us/windows/desktop/api/cfgmgr32/nf-cfgmgr32-cm_get_device_idw">
     *      CM_Get_Device_ID</A>
     */
    int CM_Get_Device_ID(int devInst, Pointer Buffer, int BufferLen, int ulFlags);

    /**
     * The CM_Get_Device_ID_Size function retrieves the buffer size required to
     * hold a device instance ID for a device instance on the local machine.
     * 
     * @param pulLen
     *            Receives a value representing the required buffer size, in
     *            characters.
     * @param dnDevInst
     *            Caller-supplied device instance handle that is bound to the
     *            local machine.
     * @param ulFlags
     *            Not used, must be zero.
     * @return If the operation succeeds, the function returns CR_SUCCESS.
     *         Otherwise, it returns one of the CR_-prefixed error codes defined
     *         in Cfgmgr32.h.
     * @see <A HREF=
     *      "https://docs.microsoft.com/en-us/windows/desktop/api/cfgmgr32/nf-cfgmgr32-cm_get_device_id_size">
     *      CM_Get_Device_ID_Size</A>
     */
    int CM_Get_Device_ID_Size(IntByReference pulLen, int dnDevInst, int ulFlags);
}

