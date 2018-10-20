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
package oshi.jna.platform.linux;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

import oshi.jna.platform.unix.CLibrary;

/**
 * Linux C Library. This class should be considered non-API as it may be removed
 * if/when its code is incorporated into the JNA project.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface Libc extends CLibrary {

    Libc INSTANCE = Native.load("c", Libc.class);

    class Sysinfo extends Structure {
        public NativeLong uptime; // Seconds since boot
        // 1, 5, and 15 minute load averages
        public NativeLong[] loads = new NativeLong[3];
        public NativeLong totalram; // Total usable main memory size
        public NativeLong freeram; // Available memory size
        public NativeLong sharedram; // Amount of shared memory
        public NativeLong bufferram; // Memory used by buffers
        public NativeLong totalswap; // Total swap space size
        public NativeLong freeswap; // swap space still available
        public short procs; // Number of current processes
        public NativeLong totalhigh; // Total high memory size
        public NativeLong freehigh; // Available high memory size
        public int mem_unit; // Memory unit size in bytes
        public byte[] _f = new byte[8]; // Won't be written for 64-bit systems

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "uptime", "loads", "totalram", "freeram", "sharedram", "bufferram",
                    "totalswap", "freeswap", "procs", "totalhigh", "freehigh", "mem_unit", "_f" });
        }
    }

    class Statvfs extends Structure {
        private static final int PADDING_SIZE = 8 / NativeLong.SIZE - 1;

        public NativeLong fsBlockSize;
        public NativeLong fsFragmentSize;
        public NativeLong fsSizeInBlocks;
        public NativeLong fsBlocksFree;
        public NativeLong fsBlocksFreeUnpriv;
        public NativeLong fsTotalInodeCount;
        public NativeLong fsFreeInodeCount;
        public NativeLong fsFreeInodeCountUnpriv;
        public NativeLong fsId;
        public int[] _fPad32bit = new int[PADDING_SIZE];  // Won't be written for 64-bit systems
        public NativeLong fsMountFlags;
        public NativeLong fsMaxFilenameLength;
        public int[] _fSpare = new int[6];

        @Override
        protected List<Field> getFieldList() {
            List<Field> fields = new ArrayList<Field>(super.getFieldList());
            if (PADDING_SIZE == 0) {
                Iterator<Field> fieldIterator = fields.iterator();
                while (fieldIterator.hasNext()) {
                    Field field = fieldIterator.next();
                    if ("_fPad32bit".equals(field.getName())) {
                        fieldIterator.remove();
                    }
                }
            }
            return fields;
        }

        protected List<String> getFieldOrder() {
            if (PADDING_SIZE == 0) {
                return Arrays.asList(new String[]{"fsBlockSize", "fsFragmentSize", "fsSizeInBlocks", "fsBlocksFree",
                        "fsBlocksFreeUnpriv", "fsTotalInodeCount", "fsFreeInodeCount", "fsFreeInodeCountUnpriv",
                        "fsId", "fsMountFlags", "fsMaxFilenameLength", "_fSpare"});
            } else {
                return Arrays.asList(new String[]{"fsBlockSize", "fsFragmentSize", "fsSizeInBlocks", "fsBlocksFree",
                        "fsBlocksFreeUnpriv", "fsTotalInodeCount", "fsFreeInodeCount", "fsFreeInodeCountUnpriv",
                        "fsId", "_fPad32bit", "fsMountFlags", "fsMaxFilenameLength", "_fSpare"});
            }
        }

    }

    int sysinfo(Sysinfo info);

    int statvfs(String path, Statvfs buf);

}
