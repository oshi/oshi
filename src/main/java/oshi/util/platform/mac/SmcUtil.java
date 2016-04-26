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
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.util.platform.mac;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.ptr.IntByReference;

import oshi.jna.platform.mac.IOKit;
import oshi.jna.platform.mac.IOKit.IOConnect;
import oshi.jna.platform.mac.IOKit.MachPort;
import oshi.jna.platform.mac.IOKit.SMCKeyData;
import oshi.jna.platform.mac.IOKit.SMCKeyDataKeyInfo;
import oshi.jna.platform.mac.IOKit.SMCVal;
import oshi.jna.platform.mac.SystemB;
import oshi.util.Util;

/**
 * Provides access to SMC calls on OS X
 * 
 * @author widdis[at]gmail[dot]com
 */
public class SmcUtil {
    private static final Logger LOG = LoggerFactory.getLogger(SmcUtil.class);

    private static IOConnect conn = new IOConnect();
    // Cache the first key query
    private static Map<Integer, SMCKeyDataKeyInfo> keyInfoCache = new HashMap<Integer, SMCKeyDataKeyInfo>();

    /**
     * Open a connection to SMC
     * 
     * @return 0 if successful, nonzero if failure
     */
    public static int smcOpen() {
        int service = 0;
        MachPort masterPort = new MachPort();

        int result = IOKit.INSTANCE.IOMasterPort(0, masterPort);
        if (result != 0) {
            LOG.error(String.format("Error: IOMasterPort() = %08x", result));
            return result;
        }

        service = IOKit.INSTANCE.IOServiceGetMatchingService(masterPort.getValue(),
                IOKit.INSTANCE.IOServiceMatching("AppleSMC"));
        if (service == 0) {
            LOG.error("Error: no SMC found\n");
            return result;
        }

        result = IOKit.INSTANCE.IOServiceOpen(service, SystemB.INSTANCE.mach_task_self(), 0, conn);
        IOKit.INSTANCE.IOObjectRelease(service);
        if (result != 0) {
            LOG.error(String.format("Error: IOServiceOpen() = 0x%08x", result));
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
        return IOKit.INSTANCE.IOServiceClose(conn.getValue());
    }

    /**
     * Get a value from SMC which is in SP78 datatype (used for Temperature)
     * First bit is sign, next 7 bits are integer portion, last 8 bits are
     * fractional portion
     * 
     * @param key
     *            The key to retrieve
     * @param retries
     *            Number of times to retry the key
     * @return Double representing the value
     */
    public static double smcGetSp78(String key, int retries) {
        SMCVal val = new SMCVal();
        int result = smcReadKey(key, val, retries);
        if (result == 0) {
            if (val.dataSize > 0 && Arrays.equals(val.dataType, IOKit.DATATYPE_SP78)) {
                return val.bytes[0] + val.bytes[1] / 256d;
            }
        }
        // Read failed
        return 0d;
    }

    /**
     * Get a 32-bit integer value from SMC
     * 
     * @param key
     *            The key to retrieve
     * @param retries
     *            Number of times to retry the key
     * @return Int representing the value
     */
    public static int smcGetInt(String key, int retries) {
        SMCVal val = new SMCVal();
        int result = smcReadKey(key, val, retries);
        if (result == 0) {
            return strtoul(val.bytes, val.dataSize);
        }
        // Read failed
        return 0;
    }

    /**
     * Get a value from SMC which is in FPE2 datatype. First E (14) bits are
     * unsigned integer portion, Last 2 bits are fractional portion
     * 
     * @param key
     *            The key to retrieve
     * @param retries
     *            Number of times to retry the key
     * @return Float representing the value
     */
    public static float smcGetFpe2(String key, int retries) {
        SMCVal val = new SMCVal();
        int result = smcReadKey(key, val, retries);
        if (result == 0) {
            return strtof(val.bytes, val.dataSize, 2);
        }
        // Read failed
        return 0f;
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
    public static int smcGetKeyInfo(SMCKeyData inputStructure, SMCKeyData outputStructure) {
        if (keyInfoCache.containsKey(inputStructure.key)) {
            SMCKeyDataKeyInfo keyInfo = keyInfoCache.get(inputStructure.key);
            outputStructure.keyInfo.dataSize = keyInfo.dataSize;
            outputStructure.keyInfo.dataType = keyInfo.dataType;
            outputStructure.keyInfo.dataAttributes = keyInfo.dataAttributes;
        } else {
            int result = 0;
            inputStructure.data8 = IOKit.SMC_CMD_READ_KEYINFO;
            Util.sleep(4);
            result = smcCall(IOKit.KERNEL_INDEX_SMC, inputStructure, outputStructure);
            if (result != 0) {
                return result;
            }
            SMCKeyDataKeyInfo keyInfo = new SMCKeyDataKeyInfo();
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
    public static int smcReadKey(String key, SMCVal val, int retries) {
        SMCKeyData inputStructure = new SMCKeyData();
        SMCKeyData outputStructure = new SMCKeyData();

        inputStructure.key = strtoul(key, 4);
        int result;
        do {
            result = smcGetKeyInfo(inputStructure, outputStructure);
            if (result != 0) {
                continue;
            }

            val.dataSize = outputStructure.keyInfo.dataSize;
            val.dataType = new byte[5];
            for (int i = 3; i >= 0; i--) {
                val.dataType[i] = (byte) (outputStructure.keyInfo.dataType >> (8 * (3 - i)));
            }
            inputStructure.keyInfo.dataSize = val.dataSize;
            inputStructure.data8 = IOKit.SMC_CMD_READ_BYTES;

            Util.sleep(4);
            result = smcCall(IOKit.KERNEL_INDEX_SMC, inputStructure, outputStructure);
            if (result != 0) {
                continue;
            }
            break;
        } while (--retries > 0);
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
    public static int smcCall(int index, SMCKeyData inputStructure, SMCKeyData outputStructure) {
        int structureInputSize = inputStructure.size();
        IntByReference structureOutputSizePtr = new IntByReference(outputStructure.size());

        int result = IOKit.INSTANCE.IOConnectCallStructMethod(conn.getValue(), index, inputStructure,
                structureInputSize, outputStructure, structureOutputSizePtr);
        if (result != 0) {
            // This seems to be a common error, suppressing output
            // LOG.error(String.format("Error: IOConnectCallStructMethod() =
            // 0x%08x", result));
            return result;
        }
        // Success
        return result;
    }

    /**
     * Convert a string to an integer representation.
     * 
     * @param str
     *            A human readable string, up to 4 characters
     * @param size
     *            Size of the string
     * @return An integer representing the string where each character is
     *         treated as a byte in a 32-bit number
     */
    public static int strtoul(String str, int size) {
        return strtoul(str.getBytes(), size);
    }

    /**
     * Convert a byte array to its integer representation.
     * 
     * @param bytes
     *            An array of bytes no smaller than the size to be converted
     * @param size
     *            Number of bytes to convert to the integer. May not exceed 4.
     * @return An integer representing the byte array as a 32-bit number
     */
    public static int strtoul(byte[] bytes, int size) {
        if (size > 4) {
            throw new IllegalArgumentException("Can't convert more than 4 bytes to an int.");
        }
        if (size > bytes.length) {
            throw new IllegalArgumentException("Size can't be larger than array length.");
        }
        int total = 0;
        for (int i = 0; i < size; i++) {
            total += (bytes[i] & 0xff) << (size - 1 - i) * 8;
        }
        return total;
    }

    /**
     * Convert a byte array to its floating point representation.
     * 
     * @param bytes
     *            An array of bytes no smaller than the size to be converted
     * @param size
     *            Number of bytes to convert to the float. May not exceed 4.
     * @param e
     *            Number of bits representing the decimal, up to 8
     * @return A float; the integer portion representing the byte array as a
     *         32-bit number shifted by the bits specified in e; with the
     *         remaining bits used as a decimal
     */
    public static float strtof(byte[] bytes, int size, int e) {
        if (size > 4) {
            throw new IllegalArgumentException("Can't convert more than 4 bytes to a float.");
        }
        if (e > 8) {
            throw new IllegalArgumentException("Can't have more than 8 floating point bits.");
        }
        int total = 0;
        // Add up bits to left of fractional bits
        for (int i = 0; i < size; i++) {
            if (i == (size - 1))
                total += (bytes[i] & 0xff) >> e;
            else
                total += bytes[i] << (size - 1 - i) * (8 - e);
        }
        // Mask fractional bits and divide
        return total + (bytes[size - 1] & (1 << e - 1)) / (float) (1 << e);
    }
}