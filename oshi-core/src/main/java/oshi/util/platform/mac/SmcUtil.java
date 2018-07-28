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
package oshi.util.platform.mac;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.ptr.IntByReference; // NOSONAR

import oshi.jna.platform.mac.IOKit;
import oshi.jna.platform.mac.IOKit.IOConnect;
import oshi.jna.platform.mac.IOKit.SMCKeyData;
import oshi.jna.platform.mac.IOKit.SMCKeyDataKeyInfo;
import oshi.jna.platform.mac.IOKit.SMCVal;
import oshi.jna.platform.mac.SystemB;
import oshi.util.ParseUtil;
import oshi.util.Util;

/**
 * Provides access to SMC calls on OS X
 *
 * @author widdis[at]gmail[dot]com
 */
public class SmcUtil {
    private static final Logger LOG = LoggerFactory.getLogger(SmcUtil.class);

    private static IOConnect conn = new IOConnect();

    /**
     * Map for caching info retrieved by a key necessary for subsequent calls.
     */
    private static Map<Integer, SMCKeyDataKeyInfo> keyInfoCache = new ConcurrentHashMap<>();

    /**
     * Byte array used for matching return type
     */
    private static final byte[] DATATYPE_SP78 = ParseUtil.stringToByteArray("sp78", 5);

    private SmcUtil() {
    }

    /**
     * Open a connection to SMC
     *
     * @return 0 if successful, nonzero if failure
     */
    public static int smcOpen() {
        int service = IOKitUtil.getMatchingService("AppleSMC");
        if (service == 0) {
            LOG.error("Error: no SMC found");
            return 1;
        }

        int result = IOKit.INSTANCE.IOServiceOpen(service, SystemB.INSTANCE.mach_task_self(), 0, conn);
        IOKit.INSTANCE.IOObjectRelease(service);
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
        if (result == 0 && val.dataSize > 0 && Arrays.equals(val.dataType, DATATYPE_SP78)) {
            return val.bytes[0] + val.bytes[1] / 256d;
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
        SMCVal val = new SMCVal();
        int result = smcReadKey(key, val, retries);
        if (result == 0) {
            return ParseUtil.byteArrayToLong(val.bytes, val.dataSize);
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
            return ParseUtil.byteArrayToFloat(val.bytes, val.dataSize, 2);
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
            inputStructure.data8 = IOKit.SMC_CMD_READ_KEYINFO;
            Util.sleep(4);
            int result = smcCall(IOKit.KERNEL_INDEX_SMC, inputStructure, outputStructure);
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
            inputStructure.data8 = IOKit.SMC_CMD_READ_BYTES;

            Util.sleep(4);
            result = smcCall(IOKit.KERNEL_INDEX_SMC, inputStructure, outputStructure);
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
    public static int smcCall(int index, SMCKeyData inputStructure, SMCKeyData outputStructure) {
        return IOKit.INSTANCE.IOConnectCallStructMethod(conn.getValue(), index, inputStructure, inputStructure.size(),
                outputStructure, new IntByReference(outputStructure.size()));
    }
}