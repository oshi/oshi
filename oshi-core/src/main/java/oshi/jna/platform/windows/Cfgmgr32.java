/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.jna.platform.windows;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.win32.W32APIOptions;

/**
 * Windows Cfgmgr32. This class should be considered non-API as it may be
 * removed if/when its code is incorporated into the JNA project.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface Cfgmgr32 extends Library {
    Cfgmgr32 INSTANCE = (Cfgmgr32) Native.loadLibrary("Cfgmgr32", Cfgmgr32.class, W32APIOptions.DEFAULT_OPTIONS);

    int CM_Get_Parent(IntByReference pdnDevInst, int dnDevInst, int ulFlags);

    int CM_Get_Child(IntByReference pdnDevInst, int dnDevInst, int ulFlags);

    int CM_Get_Sibling(IntByReference pdnDevInst, int dnDevInst, int ulFlags);

    int CM_Get_Device_ID(int devInst, char[] Buffer, int BufferLen, int ulFlags);

    int CM_Get_Device_ID_Size(NativeLongByReference pulLen, int dnDevInst, int ulFlags);
}
