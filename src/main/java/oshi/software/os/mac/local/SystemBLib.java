/**
 * Oshi (https://github.com/dblock/oshi)
 * 
 * Copyright (c) 2010 - 2015 The Oshi Project Team
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.mac.local;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * Memory and CPU stats from vm_stat and sysctl
 * 
 * @author widdis[at]gmail[dot]com
 */
public interface SystemBLib extends Library {
    // TODO: Submit these additions to the previously submitted JNA SystemB
    // class
    static int PROCESSOR_CPU_LOAD_INFO = 2;

    SystemBLib INSTANCE = (SystemBLib) Native.loadLibrary("System", SystemBLib.class);

    int host_processor_info(int machPort, int flavor, IntByReference procCount, PointerByReference procInfo,
            IntByReference procInfoCount);
}
