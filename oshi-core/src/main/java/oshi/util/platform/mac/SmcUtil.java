/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.util.platform.mac;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.mac.IOKit.IOConnect;
import com.sun.jna.platform.mac.IOKit.IOService;
import com.sun.jna.platform.mac.IOKitUtil;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.jna.platform.mac.IOKit;
import oshi.jna.platform.mac.SystemB;
import oshi.util.ParseUtil;
import oshi.util.Util;

/**
 * Provides access to SMC calls on OS X
 */
public class SmcUtil {
    /**
     * Holds the return value of SMC version query.
     */
    @FieldOrder({ "major", "minor", "build", "reserved", "release" })
    public static
    class SMCKeyDataVers extends Structure {
        public byte major;
        public byte minor;
        public byte build;
        public byte[] reserved = new byte[1];
        public short release;

    }

    /**
     * Holds the return value of SMC pLimit query.
     */
    @FieldOrder({ "version", "length", "cpuPLimit", "gpuPLimit", "memPLimit" })
    public static
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
    public static
    class SMCKeyDataKeyInfo extends Structure {
        public int dataSize;
        public int dataType;
        public byte dataAttributes;
    }

    /**
     * Holds the return value of SMC query.
     */
    @FieldOrder({ "key", "vers", "pLimitData", "keyInfo", "result", "status", "data8", "data32", "bytes" })
    public static
    class SMCKeyData extends Structure {
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
    @FieldOrder({ "key", "dataSize", "dataType", "bytes" })
    public static
    class SMCVal extends Structure {
        public byte[] key = new byte[5];
        public int dataSize;
        public byte[] dataType = new byte[5];
        public byte[] bytes = new byte[32];
    }

    private static final Logger LOG = LoggerFactory.getLogger(SmcUtil.class);

    private static PointerByReference conn = new PointerByReference();

    /**
     * Map for caching info retrieved by a key necessary for subsequent calls.
     */
    private static Map<Integer, SmcUtil.SMCKeyDataKeyInfo> keyInfoCache = new ConcurrentHashMap<>();

    /**
     * Byte array used for matching return type
     */
    private static final byte[] DATATYPE_SP78 = ParseUtil.stringToByteArray("sp78", 5);
    private static final byte[] DATATYPE_FPE2 = ParseUtil.stringToByteArray("fpe2", 5);
    private static final byte[] DATATYPE_FLT = ParseUtil.stringToByteArray("flt ", 5);

    public static final String SMC_KEY_FAN_NUM = "FNum";

    public static final String SMC_KEY_FAN_SPEED = "F%dAc";

    public static final String SMC_KEY_CPU_TEMP = "TC0P";

    public static final String SMC_KEY_CPU_VOLTAGE = "VC0C";

    public static final byte SMC_CMD_READ_BYTES = 5;

    public static final byte SMC_CMD_READ_KEYINFO = 9;

    public static final int KERNEL_INDEX_SMC = 2;

    private SmcUtil() {
    }

    /**
     * Open a connection to SMC
     *
     * @return 0 if successful, nonzero if failure
     */
    public static int smcOpen() {
        IOService service = IOKitUtil.getMatchingService("AppleSMC");
        if (service == null) {
            LOG.error("Error: no SMC found");
            return 1;
        }
        SystemB.TaskPort taskSelf = oshi.jna.platform.mac.SystemB.INSTANCE.mach_task_self_ptr();
        int result = IOKit.INSTANCE.IOServiceOpen(service, taskSelf, 0, conn);
        service.release();
        if (result != 0) {
            if (LOG.isErrorEnabled()) {
                LOG.error(String.format("Error: IOServiceOpen() = 0x%08x", result));
            }
            return result;
        }
        // Delay to improve success of next query
        Util.sleep(5);
        return 0;
    }

    /**
     * Close connection to SMC
     *
     * @return 0 if successful, nonzero if failure
     */
    public static int smcClose() {
        IOConnect connect = new IOConnect(conn.getValue());
        return IOKit.INSTANCE.IOServiceClose(connect);
    }

    /**
     * Get a value from SMC which is in a floating point datatype (SP78, FPE2, FLT)
     *
     * @param key
     *            The key to retrieve
     * @param retries
     *            Number of times to retry the key
     * @return Double representing the value
     */
    public static double smcGetFloat(String key, int retries) {
        SmcUtil.SMCVal val = new SmcUtil.SMCVal();
        int result = smcReadKey(key, val, retries);
        if (result == 0 && val.dataSize > 0) {
            if (Arrays.equals(val.dataType, DATATYPE_SP78) && val.dataSize == 2) {
                // First bit is sign, next 7 bits are integer portion, last 8 bits are
                // fractional portion
                return val.bytes[0] + val.bytes[1] / 256d;
            } else if (Arrays.equals(val.dataType, DATATYPE_FPE2) && val.dataSize == 2) {
                // First E (14) bits are integer portion last 2 bits are fractional portion
                return ParseUtil.byteArrayToFloat(val.bytes, val.dataSize, 2);
            } else if (Arrays.equals(val.dataType, DATATYPE_FLT) && val.dataSize == 4) {
                // Standard 32-bit floating point
                return ByteBuffer.wrap(val.bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
            }
        }
        // Read failed
        return 0d;
    }

    /**
     * Get a 64-bit integer value from SMC
     *
     * @param key
     *            The key to retrieve
     * @param retries
     *            Number of times to retry the key
     * @return Long representing the value
     */
    public static long smcGetLong(String key, int retries) {
        SmcUtil.SMCVal val = new SmcUtil.SMCVal();
        int result = smcReadKey(key, val, retries);
        if (result == 0) {
            return ParseUtil.byteArrayToLong(val.bytes, val.dataSize);
        }
        // Read failed
        return 0;
    }

    /**
     * Get cached keyInfo if it exists, or generate new keyInfo
     *
     * @param inputStructure
     *            Key data input
     * @param outputStructure
     *            Key data output
     * @return 0 if successful, nonzero if failure
     */
    public static int smcGetKeyInfo(SmcUtil.SMCKeyData inputStructure, SmcUtil.SMCKeyData outputStructure) {
        if (keyInfoCache.containsKey(inputStructure.key)) {
            SmcUtil.SMCKeyDataKeyInfo keyInfo = keyInfoCache.get(inputStructure.key);
            outputStructure.keyInfo.dataSize = keyInfo.dataSize;
            outputStructure.keyInfo.dataType = keyInfo.dataType;
            outputStructure.keyInfo.dataAttributes = keyInfo.dataAttributes;
        } else {
            inputStructure.data8 = SmcUtil.SMC_CMD_READ_KEYINFO;
            Util.sleep(4);
            int result = smcCall(SmcUtil.KERNEL_INDEX_SMC, inputStructure, outputStructure);
            if (result != 0) {
                return result;
            }
            SmcUtil.SMCKeyDataKeyInfo keyInfo = new SmcUtil.SMCKeyDataKeyInfo();
            keyInfo.dataSize = outputStructure.keyInfo.dataSize;
            keyInfo.dataType = outputStructure.keyInfo.dataType;
            keyInfo.dataAttributes = outputStructure.keyInfo.dataAttributes;
            keyInfoCache.put(inputStructure.key, keyInfo);
        }
        return 0;
    }

    /**
     * Read a key from SMC
     *
     * @param key
     *            Key to read
     * @param val
     *            Structure to receive the result
     * @param retries
     *            Number of attempts to try
     * @return 0 if successful, nonzero if failure
     */
    public static int smcReadKey(String key, SmcUtil.SMCVal val, int retries) {
        SmcUtil.SMCKeyData inputStructure = new SmcUtil.SMCKeyData();
        SmcUtil.SMCKeyData outputStructure = new SmcUtil.SMCKeyData();

        inputStructure.key = (int) ParseUtil.strToLong(key, 4);
        int result;
        // These calls frequently result in kIOReturnIPCError so retry multiple
        // times with small delay
        int retry = 0;
        do {
            result = smcGetKeyInfo(inputStructure, outputStructure);
            if (result != 0) {
                continue;
            }

            val.dataSize = outputStructure.keyInfo.dataSize;
            val.dataType = ParseUtil.longToByteArray(outputStructure.keyInfo.dataType, 4, 5);

            inputStructure.keyInfo.dataSize = val.dataSize;
            inputStructure.data8 = SmcUtil.SMC_CMD_READ_BYTES;

            Util.sleep(4);
            result = smcCall(SmcUtil.KERNEL_INDEX_SMC, inputStructure, outputStructure);
            // If we got success, exit!
            if (result == 0) {
                break;
            }
        } while (++retry < retries);
        // If we errored out return code
        if (result != 0) {
            return result;
        }

        System.arraycopy(outputStructure.bytes, 0, val.bytes, 0, val.bytes.length);
        // Success
        return 0;
    }

    /**
     * Call SMC
     *
     * @param index
     *            Kernel index
     * @param inputStructure
     *            Key data input
     * @param outputStructure
     *            Key data output
     * @return 0 if successful, nonzero if failure
     */
    public static int smcCall(int index, SmcUtil.SMCKeyData inputStructure, SmcUtil.SMCKeyData outputStructure) {
        IOConnect connect = new IOConnect(conn.getValue());
        return IOKit.INSTANCE.IOConnectCallStructMethod(connect, index, inputStructure, inputStructure.size(),
                outputStructure, new IntByReference(outputStructure.size()));
    }
}
