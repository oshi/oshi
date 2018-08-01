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

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinBase.SYSTEMTIME;
import com.sun.jna.platform.win32.WinNT.LARGE_INTEGER;

/**
 * Various performance counters structures and definitions
 * 
 * @author Daniel Widdis
 * @see <A HREF=
 *      "https://msdn.microsoft.com/en-us/library/windows/desktop/aa373093(v=vs.85).aspx">
 *      Performance Counters Structures</A>
 */
public interface WinPerf extends com.sun.jna.platform.win32.WinPerf {
    /**
     * Describes the performance data block that you queried
     * 
     * @see <A HREF=
     *      "https://msdn.microsoft.com/en-us/library/windows/desktop/aa373157(v=vs.85).aspx">PERF_DATA_BLOCK</A>
     */
    class PERF_DATA_BLOCK extends Structure {
        public char[] Signature = new char[4];
        public int LittleEndian;
        public int Version;
        public int Revision;
        public int TotalByteLength;
        public int HeaderLength;
        public int NumObjectTypes;
        public int DefaultObject;
        public SYSTEMTIME SystemTime = new SYSTEMTIME();
        public LARGE_INTEGER PerfTime = new LARGE_INTEGER();
        public LARGE_INTEGER PerfFreq = new LARGE_INTEGER();
        public LARGE_INTEGER PerfTime100nSec = new LARGE_INTEGER();
        public int SystemNameLength;
        public int SystemNameOffset;

        public PERF_DATA_BLOCK() {
            super();
        }

        public PERF_DATA_BLOCK(Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "Signature", "LittleEndian", "Version", "Revision", "TotalByteLength",
                    "HeaderLength", "NumObjectTypes", "DefaultObject", "SystemTime", "PerfTime", "PerfFreq",
                    "PerfTime100nSec", "SystemNameLength", "SystemNameOffset" });
        }
    }

    /**
     * Describes an instance of a performance object
     * 
     * @see <A HREF=
     *      "https://msdn.microsoft.com/en-us/library/windows/desktop/aa373159(v=vs.85).aspx">PERF_INSTANCE_DEFINITION</A>
     */
    class PERF_INSTANCE_DEFINITION extends Structure {
        public int ByteLength;
        public int ParentObjectTitleIndex;
        public int ParentObjectInstance;
        public int UniqueID;
        public int NameOffset;
        public int NameLength;

        public PERF_INSTANCE_DEFINITION() {
            super();
        }

        public PERF_INSTANCE_DEFINITION(Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "ByteLength", "ParentObjectTitleIndex", "ParentObjectInstance",
                    "UniqueID", "NameOffset", "NameLength" });
        }
    }

    /**
     * Describes object-specific performance information, for example, the
     * number of instances of the object and the number of counters that the
     * object defines.
     * 
     * @see <A HREF=
     *      "https://msdn.microsoft.com/en-us/library/windows/desktop/aa373160(v=vs.85).aspx">
     *      PERF_OBJECT_TYPE</A>
     */
    class PERF_OBJECT_TYPE extends Structure {
        public int TotalByteLength;
        public int DefinitionLength;
        public int HeaderLength;
        public int ObjectNameTitleIndex;
        public int ObjectNameTitle; // always 32 bit
        public int ObjectHelpTitleIndex;
        public int ObjectHelpTitle; // always 32 bit
        public int DetailLevel;
        public int NumCounters;
        public int DefaultCounter;
        public int NumInstances;
        public int CodePage;
        public LARGE_INTEGER PerfTime;
        public LARGE_INTEGER PerfFreq;

        public PERF_OBJECT_TYPE() {
            super();
        }

        public PERF_OBJECT_TYPE(Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "TotalByteLength", "DefinitionLength", "HeaderLength",
                    "ObjectNameTitleIndex", "ObjectNameTitle", "ObjectHelpTitleIndex", "ObjectHelpTitle", "DetailLevel",
                    "NumCounters", "DefaultCounter", "NumInstances", "CodePage", "PerfTime", "PerfFreq" });
        }
    }

    /**
     * Describes a performance counter.
     * 
     * @see <A HREF=
     *      "https://msdn.microsoft.com/en-us/library/windows/desktop/aa373150(v=vs.85).aspx">
     *      PERF_COUNTER_DEFINITION</A>
     */
    class PERF_COUNTER_DEFINITION extends Structure {
        public int ByteLength;
        public int CounterNameTitleIndex;
        public int CounterNameTitle; // always 32 bit
        public int CounterHelpTitleIndex;
        public int CounterHelpTitle; // always 32 bit
        public int DefaultScale;
        public int DetailLevel;
        public int CounterType;
        public int CounterSize;
        public int CounterOffset;

        public PERF_COUNTER_DEFINITION() {
            super();
        }

        public PERF_COUNTER_DEFINITION(Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "ByteLength", "CounterNameTitleIndex", "CounterNameTitle",
                    "CounterHelpTitleIndex", "CounterHelpTitle", "DefaultScale", "DetailLevel", "CounterType",
                    "CounterSize", "CounterOffset" });
        }
    }

    /**
     * Describes the block of memory that contains the raw performance counter
     * data for an object's counters.
     * 
     * @see <A HREF=
     *      "https://msdn.microsoft.com/en-us/library/windows/desktop/aa373147(v=vs.85).aspx">
     *      PERF_COUNTER_BLOCK</A>
     */
    class PERF_COUNTER_BLOCK extends Structure {
        public int ByteLength;

        public PERF_COUNTER_BLOCK() {
            super();
        }

        public PERF_COUNTER_BLOCK(Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "ByteLength" });
        }
    }
}
