/*
 * MIT License
 *
 * Copyright (c) 2019-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.jna.platform.mac;

import com.sun.jna.Native; // NOSONAR squid:S1191
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.NativeLongByReference;

/**
 * The I/O Kit framework implements non-kernel access to I/O Kit objects
 * (drivers and nubs) through the device-interface mechanism.
 */
public interface IOKit extends com.sun.jna.platform.mac.IOKit {

    IOKit INSTANCE = Native.load("IOKit", IOKit.class);

    /*
     * Do not commit SMC structures to JNA
     */
    /**
     * Holds the return value of SMC version query.
     */
    @FieldOrder({ "major", "minor", "build", "reserved", "release" }) class SMCKeyDataVers extends Structure {
        public byte major;
        public byte minor;
        public byte build;
        public byte reserved;
        public short release;

    }

    /**
     * Holds the return value of SMC pLimit query.
     */
    @FieldOrder({ "version", "length", "cpuPLimit", "gpuPLimit", "memPLimit" }) class SMCKeyDataPLimitData extends Structure {
        public short version;
        public short length;
        public int cpuPLimit;
        public int gpuPLimit;
        public int memPLimit;
    }

    /**
     * Holds the return value of SMC KeyInfo query.
     */
    @FieldOrder({ "dataSize", "dataType", "dataAttributes" }) class SMCKeyDataKeyInfo extends Structure {
        public int dataSize;
        public int dataType;
        public byte dataAttributes;
    }

    /**
     * Holds the return value of SMC query.
     */
    @FieldOrder({ "key", "vers", "pLimitData", "keyInfo", "result", "status", "data8", "data32", "bytes" }) class SMCKeyData extends Structure {
        public int key;
        public SMCKeyDataVers vers;
        public SMCKeyDataPLimitData pLimitData;
        public SMCKeyDataKeyInfo keyInfo;
        public byte result;
        public byte status;
        public byte data8;
        public int data32;
        public byte[] bytes = new byte[32];
    }

    /**
     * Holds an SMC value
     */
    @FieldOrder({ "key", "dataSize", "dataType", "bytes" }) class SMCVal extends Structure {
        public byte[] key = new byte[5];
        public int dataSize;
        public byte[] dataType = new byte[5];
        public byte[] bytes = new byte[32];
    }

    /*
     * Beta/Non-API do not commit to JNA
     */
    int IOConnectCallStructMethod(IOConnect connection, int selector, Structure inputStructure,
            NativeLong structureInputSize, Structure outputStructure, NativeLongByReference structureOutputSize);
}
