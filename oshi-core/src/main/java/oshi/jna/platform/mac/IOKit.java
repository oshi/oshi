/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.mac;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.NativeLongByReference;

import oshi.util.Util;

/**
 * The I/O Kit framework implements non-kernel access to I/O Kit objects (drivers and nubs) through the device-interface
 * mechanism.
 */
public interface IOKit extends com.sun.jna.platform.mac.IOKit {

    IOKit INSTANCE = Native.load("IOKit", IOKit.class);

    /*
     * Do not commit SMC structures to JNA
     */
    /**
     * Holds the return value of SMC version query.
     */
    @FieldOrder({ "major", "minor", "build", "reserved", "release" })
    class SMCKeyDataVers extends Structure {
        public byte major;
        public byte minor;
        public byte build;
        public byte reserved;
        public short release;
    }

    /**
     * Holds the return value of SMC pLimit query.
     */
    @FieldOrder({ "version", "length", "cpuPLimit", "gpuPLimit", "memPLimit" })
    class SMCKeyDataPLimitData extends Structure {
        public short version;
        public short length;
        public int cpuPLimit;
        public int gpuPLimit;
        public int memPLimit;
    }

    /**
     * Holds the return value of SMC KeyInfo query.
     */
    @FieldOrder({ "dataSize", "dataType", "dataAttributes" })
    class SMCKeyDataKeyInfo extends Structure {
        public int dataSize;
        public int dataType;
        public byte dataAttributes;
    }

    /**
     * Holds the return value of SMC query.
     */
    @FieldOrder({ "key", "vers", "pLimitData", "keyInfo", "result", "status", "data8", "data32", "bytes" })
    class SMCKeyData extends Structure implements AutoCloseable {
        public int key;
        public SMCKeyDataVers vers;
        public SMCKeyDataPLimitData pLimitData;
        public SMCKeyDataKeyInfo keyInfo;
        public byte result;
        public byte status;
        public byte data8;
        public int data32;
        public byte[] bytes = new byte[32];

        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    /**
     * Holds an SMC value
     */
    @FieldOrder({ "key", "dataSize", "dataType", "bytes" })
    class SMCVal extends Structure implements AutoCloseable {
        public byte[] key = new byte[5];
        public int dataSize;
        public byte[] dataType = new byte[5];
        public byte[] bytes = new byte[32];

        @Override
        public void close() {
            Util.freeMemory(getPointer());
        }
    }

    /*
     * Beta/Non-API do not commit to JNA
     */
    int IOConnectCallStructMethod(IOConnect connection, int selector, Structure inputStructure,
            NativeLong structureInputSize, Structure outputStructure, NativeLongByReference structureOutputSize);
}
