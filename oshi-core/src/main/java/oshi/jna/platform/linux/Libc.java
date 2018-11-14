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
import java.util.Iterator;
import java.util.List;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

import oshi.jna.platform.unix.CLibrary;

/**
 * Linux C Library. This class should be considered non-API as it may be removed
 * if/when its code is incorporated into the JNA project.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface Libc extends CLibrary {

    Libc INSTANCE = Native.load("c", Libc.class);

    @FieldOrder({ "uptime", "loads", "totalram", "freeram", "sharedram", "bufferram", "totalswap", "freeswap", "procs",
            "totalhigh", "freehigh", "mem_unit", "_f" })
    class Sysinfo extends Structure {
        private static final int PADDING_SIZE = 20 - 2 * NativeLong.SIZE - 4;

        public NativeLong uptime; // Seconds since boot
        // 1, 5, and 15 minute load averages, divide by 2^16
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
        // Padding to 64 bytes
        public byte[] _f = new byte[PADDING_SIZE];

        /*
         * getFieldList and getFieldOrder are overridden because PADDING_SIZE
         * might be 0 - that is a GCC only extension and not supported by JNA
         * 
         * The dummy field at the end of the structure is just padding and so if
         * the field is the zero length array, it is stripped from the fields
         * and field order.
         */
        @Override
        protected List<Field> getFieldList() {
            List<Field> fields = new ArrayList<>(super.getFieldList());
            if (PADDING_SIZE == 0) {
                Iterator<Field> fieldIterator = fields.iterator();
                while (fieldIterator.hasNext()) {
                    Field field = fieldIterator.next();
                    if ("_f".equals(field.getName())) {
                        fieldIterator.remove();
                    }
                }
            }
            return fields;
        }

        @Override
        protected List<String> getFieldOrder() {
            List<String> fieldOrder = new ArrayList<>(super.getFieldOrder());
            if (PADDING_SIZE == 0) {
                fieldOrder.remove("_f");
            }
            return fieldOrder;
        }
    }

    @FieldOrder({ "fsBlockSize", "fsFragmentSize", "fsSizeInBlocks", "fsBlocksFree", "fsBlocksFreeUnpriv",
            "fsTotalInodeCount", "fsFreeInodeCount", "fsFreeInodeCountUnpriv", "fsId", "_fPad32bit", "fsMountFlags",
            "fsMaxFilenameLength", "_fSpare" })
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
        public int[] _fPad32bit = new int[PADDING_SIZE]; // 0 on 64-bit systems
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

        @Override
        protected List<String> getFieldOrder() {
            List<String> fieldOrder = new ArrayList<>(super.getFieldOrder());
            if (PADDING_SIZE == 0) {
                fieldOrder.remove("_fPad32bit");
            }
            return fieldOrder;
        }
    }

    /**
     * sysinfo() provides a simple way of getting overall system statistics.
     * This is more portable than reading /dev/kmem.
     * 
     * @param info
     *            A Sysinfo structure which will be populated
     * @return On success, zero is returned. On error, -1 is returned, and errno
     *         is set appropriately.
     */
    int sysinfo(Sysinfo info);

    /**
     * The function statvfs() returns information about a mounted filesystem.
     * 
     * @param path
     *            the pathname of any file within the mounted filesystem.
     * @param buf
     *            a pointer to a statvfs structure
     * @return On success, zero is returned. On error, -1 is returned, and errno
     *         is set appropriately.
     */
    int statvfs(String path, Statvfs buf);
}
