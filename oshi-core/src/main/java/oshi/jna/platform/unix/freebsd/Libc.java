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
package oshi.jna.platform.unix.freebsd;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Native;
import com.sun.jna.Structure;

import oshi.jna.platform.unix.CLibrary;

/**
 * C library. This class should be considered non-API as it may be removed
 * if/when its code is incorporated into the JNA project.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface Libc extends CLibrary {
    Libc INSTANCE = Native.loadLibrary("libc", Libc.class);

    /*
     * Data size
     */
    int UINT64_SIZE = Native.getNativeSize(long.class);
    int INT_SIZE = Native.getNativeSize(int.class);

    /*
     * CPU state indices
     */
    int CPUSTATES = 5;
    int CP_USER = 0;
    int CP_NICE = 1;
    int CP_SYS = 2;
    int CP_INTR = 3;
    int CP_IDLE = 4;

    class CpTime extends Structure {
        public long[] cpu_ticks = new long[CPUSTATES];

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "cpu_ticks" });
        }
    }
}
