/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.mac;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.NativeLong;
import com.sun.jna.platform.mac.IOKit.IOConnect;
import com.sun.jna.platform.mac.IOKit.IOService;
import com.sun.jna.platform.mac.IOKitUtil;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseableNativeLongByReference;
import oshi.jna.ByRef.CloseablePointerByReference;
import oshi.jna.platform.mac.IOKit;
import oshi.jna.platform.mac.IOKit.SMCKeyData;
import oshi.jna.platform.mac.IOKit.SMCKeyDataKeyInfo;
import oshi.jna.platform.mac.IOKit.SMCVal;
import oshi.jna.platform.mac.SystemB;
import oshi.util.ParseUtil;

/**
 * Provides access to SMC calls on macOS
 */
@ThreadSafe
public final class SmcUtil {

    private static final Logger LOG = LoggerFactory.getLogger(SmcUtil.class);

    private static final IOKit IO = IOKit.INSTANCE;

    /**
     * Thread-safe map for caching info retrieved by a key necessary for subsequent calls.
     */
    private static Map<Integer, SMCKeyDataKeyInfo> keyInfoCache = new ConcurrentHashMap<>();

    /**
     * Byte array used for matching return type
     */
    private static final byte[] DATATYPE_SP78 = ParseUtil.asciiStringToByteArray("sp78", 5);
    private static final byte[] DATATYPE_FPE2 = ParseUtil.asciiStringToByteArray("fpe2", 5);
    private static final byte[] DATATYPE_FLT = ParseUtil.asciiStringToByteArray("flt ", 5);

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
     * Open a connection to SMC.
     *
     * @return The connection if successful, null if failure
     */
    public static IOConnect smcOpen() {
        IOService smcService = IOKitUtil.getMatchingService("AppleSMC");
        if (smcService != null) {
            try (CloseablePointerByReference connPtr = new CloseablePointerByReference()) {
                int result = IO.IOServiceOpen(smcService, SystemB.INSTANCE.mach_task_self(), 0, connPtr);
                if (result == 0) {
                    return new IOConnect(connPtr.getValue());
                } else if (LOG.isErrorEnabled()) {
                    LOG.error(String.format("Unable to open connection to AppleSMC service. Error: 0x%08x", result));
                }
            } finally {
                smcService.release();
            }
        } else {
            LOG.error("Unable to locate AppleSMC service");
        }
        return null;
    }

    /**
     * Close connection to SMC.
     *
     * @param conn The connection
     *
     * @return 0 if successful, nonzero if failure
     */
    public static int smcClose(IOConnect conn) {
        return IO.IOServiceClose(conn);
    }

    /**
     * Get a value from SMC which is in a floating point datatype (SP78, FPE2, FLT)
     *
     * @param conn The connection
     * @param key  The key to retrieve
     * @return Double representing the value
     */
    public static double smcGetFloat(IOConnect conn, String key) {
        try (SMCVal val = new SMCVal()) {
            int result = smcReadKey(conn, key, val);
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
        }
        // Read failed
        return 0d;
    }

    /**
     * Get a 64-bit integer value from SMC
     *
     * @param conn The connection
     * @param key  The key to retrieve
     * @return Long representing the value
     */
    public static long smcGetLong(IOConnect conn, String key) {
        try (SMCVal val = new SMCVal()) {
            int result = smcReadKey(conn, key, val);
            if (result == 0) {
                return ParseUtil.byteArrayToLong(val.bytes, val.dataSize);
            }
        }
        // Read failed
        return 0;
    }

    /**
     * Get cached keyInfo if it exists, or generate new keyInfo
     *
     * @param conn            The connection
     * @param inputStructure  Key data input
     * @param outputStructure Key data output
     * @return 0 if successful, nonzero if failure
     */
    public static int smcGetKeyInfo(IOConnect conn, SMCKeyData inputStructure, SMCKeyData outputStructure) {
        if (keyInfoCache.containsKey(inputStructure.key)) {
            SMCKeyDataKeyInfo keyInfo = keyInfoCache.get(inputStructure.key);
            outputStructure.keyInfo.dataSize = keyInfo.dataSize;
            outputStructure.keyInfo.dataType = keyInfo.dataType;
            outputStructure.keyInfo.dataAttributes = keyInfo.dataAttributes;
        } else {
            inputStructure.data8 = SMC_CMD_READ_KEYINFO;
            int result = smcCall(conn, KERNEL_INDEX_SMC, inputStructure, outputStructure);
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
     * @param conn The connection
     * @param key  Key to read
     * @param val  Structure to receive the result
     * @return 0 if successful, nonzero if failure
     */
    public static int smcReadKey(IOConnect conn, String key, SMCVal val) {
        try (SMCKeyData inputStructure = new SMCKeyData(); SMCKeyData outputStructure = new SMCKeyData()) {
            inputStructure.key = (int) ParseUtil.strToLong(key, 4);
            int result = smcGetKeyInfo(conn, inputStructure, outputStructure);
            if (result == 0) {
                val.dataSize = outputStructure.keyInfo.dataSize;
                val.dataType = ParseUtil.longToByteArray(outputStructure.keyInfo.dataType, 4, 5);

                inputStructure.keyInfo.dataSize = val.dataSize;
                inputStructure.data8 = SMC_CMD_READ_BYTES;

                result = smcCall(conn, KERNEL_INDEX_SMC, inputStructure, outputStructure);
                if (result == 0) {
                    System.arraycopy(outputStructure.bytes, 0, val.bytes, 0, val.bytes.length);
                    return 0;
                }
            }
            return result;
        }
    }

    /**
     * Call SMC
     *
     * @param conn            The connection
     * @param index           Kernel index
     * @param inputStructure  Key data input
     * @param outputStructure Key data output
     * @return 0 if successful, nonzero if failure
     */
    public static int smcCall(IOConnect conn, int index, SMCKeyData inputStructure, SMCKeyData outputStructure) {
        try (CloseableNativeLongByReference size = new CloseableNativeLongByReference(
                new NativeLong(outputStructure.size()))) {
            return IO.IOConnectCallStructMethod(conn, index, inputStructure, new NativeLong(inputStructure.size()),
                    outputStructure, size);
        }
    }
}
