/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.mac.IOKitFunctions.IOConnectCallStructMethod;
import static oshi.ffm.mac.IOKitFunctions.IOServiceClose;
import static oshi.ffm.mac.IOKitFunctions.IOServiceOpen;
import static oshi.ffm.mac.MacSystem.SMC_BYTES;
import static oshi.ffm.mac.MacSystem.SMC_DATA8;
import static oshi.ffm.mac.MacSystem.SMC_DATA_ATTRIBUTES;
import static oshi.ffm.mac.MacSystem.SMC_DATA_SIZE;
import static oshi.ffm.mac.MacSystem.SMC_DATA_TYPE;
import static oshi.ffm.mac.MacSystem.SMC_KEY;
import static oshi.ffm.mac.MacSystem.SMC_KEY_DATA;
import static oshi.ffm.mac.MacSystem.SMC_KEY_INFO;
import static oshi.ffm.mac.MacSystem.SMC_VAL;
import static oshi.ffm.mac.MacSystem.SMC_VAL_BYTES;
import static oshi.ffm.mac.MacSystem.SMC_VAL_DATA_SIZE;
import static oshi.ffm.mac.MacSystem.SMC_VAL_DATA_TYPE;
import static oshi.ffm.mac.MacSystemFunctions.mach_task_self;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.mac.IOKit.IOService;
import oshi.util.ParseUtil;

/**
 * Provides access to SMC calls on macOS using FFM
 */
@ThreadSafe
public final class SmcUtilFFM {

    private static final Logger LOG = LoggerFactory.getLogger(SmcUtilFFM.class);

    // Cached key info: maps SMC key (int) to [dataSize(int), dataType(int), dataAttributes(byte)]
    private static final Map<Integer, int[]> KEY_INFO_CACHE = new ConcurrentHashMap<>();

    private static final byte[] DATATYPE_SP78 = ParseUtil.asciiStringToByteArray("sp78", 5);
    private static final byte[] DATATYPE_FPE2 = ParseUtil.asciiStringToByteArray("fpe2", 5);
    private static final byte[] DATATYPE_FLT = ParseUtil.asciiStringToByteArray("flt ", 5);

    // Absolute byte offsets of keyInfo sub-fields within an SMC_KEY_DATA segment
    private static final long KEY_INFO_DATA_SIZE_OFFSET = SMC_KEY_DATA.byteOffset(SMC_KEY_INFO, SMC_DATA_SIZE);
    private static final long KEY_INFO_DATA_TYPE_OFFSET = SMC_KEY_DATA.byteOffset(SMC_KEY_INFO, SMC_DATA_TYPE);
    private static final long KEY_INFO_DATA_ATTR_OFFSET = SMC_KEY_DATA.byteOffset(SMC_KEY_INFO, SMC_DATA_ATTRIBUTES);

    public static final String SMC_KEY_FAN_NUM = "FNum";
    public static final String SMC_KEY_FAN_SPEED = "F%dAc";
    public static final String SMC_KEY_CPU_TEMP = "TC0P";
    public static final String SMC_KEY_CPU_VOLTAGE = "VC0C";

    public static final String[] SMC_KEYS_CPU_TEMP_AS = { "Tp09", "Tp0T", "Tp01", "Tp05", "Tp0D" };
    public static final String[] SMC_KEYS_GPU_TEMP_AS = { "Tg05", "Tg0D", "Tg0f", "Tg0j" };
    public static final String SMC_KEY_CPU_VOLTAGE_AS = "VP0C";

    public static final byte SMC_CMD_READ_BYTES = 5;
    public static final byte SMC_CMD_READ_KEYINFO = 9;
    public static final int KERNEL_INDEX_SMC = 2;

    private SmcUtilFFM() {
    }

    /**
     * Open a connection to SMC.
     *
     * @return The connection segment if successful, null if failure. Caller must close with {@link #smcClose}.
     */
    public static MemorySegment smcOpen() {
        IOService smcService = IOKitUtilFFM.getMatchingService("AppleSMC");
        if (smcService == null) {
            LOG.error("Unable to locate AppleSMC service");
            return null;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment connPtr = arena.allocate(ADDRESS);
            int task = mach_task_self();
            int result = IOServiceOpen(smcService.segment(), task, 0, connPtr);
            if (result == 0) {
                MemorySegment conn = connPtr.get(ADDRESS, 0).reinterpret(Long.MAX_VALUE);
                if (conn.equals(MemorySegment.NULL)) {
                    LOG.error("IOServiceOpen returned null connect handle");
                    return null;
                }
                return conn;
            }
            LOG.error(
                    String.format(Locale.ROOT, "Unable to open connection to AppleSMC service. Error: 0x%08x", result));
            return null;
        } catch (Throwable e) {
            LOG.error("Exception opening SMC connection", e);
            return null;
        } finally {
            smcService.release();
        }
    }

    /**
     * Close connection to SMC.
     *
     * @param conn The connection segment returned by {@link #smcOpen}
     * @return 0 if successful, nonzero if failure
     */
    public static int smcClose(MemorySegment conn) {
        if (conn == null) {
            return -1;
        }
        try {
            return IOServiceClose(conn);
        } catch (Throwable e) {
            return -1;
        }
    }

    /**
     * Get a value from SMC which is in a floating point datatype (SP78, FPE2, FLT)
     *
     * @param conn The connection
     * @param key  The key to retrieve
     * @return Double representing the value
     */
    public static double smcGetFloat(MemorySegment conn, String key) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment val = arena.allocate(SMC_VAL);
            int result = smcReadKey(conn, key, val, arena);
            if (result == 0) {
                int dataSize = val.get(JAVA_INT, SMC_VAL.byteOffset(SMC_VAL_DATA_SIZE));
                if (dataSize > 0) {
                    byte[] dataType = readByteArray(val, SMC_VAL.byteOffset(SMC_VAL_DATA_TYPE), 5);
                    byte[] bytes = readByteArray(val, SMC_VAL.byteOffset(SMC_VAL_BYTES), dataSize);
                    if (Arrays.equals(dataType, DATATYPE_SP78) && dataSize == 2) {
                        return bytes[0] + (bytes[1] & 0xFF) / 256d;
                    } else if (Arrays.equals(dataType, DATATYPE_FPE2) && dataSize == 2) {
                        return ParseUtil.byteArrayToFloat(bytes, dataSize, 2);
                    } else if (Arrays.equals(dataType, DATATYPE_FLT) && dataSize == 4) {
                        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    }
                }
            }
        } catch (Throwable e) {
            LOG.warn("Failed to read SMC float key {}: {}", key, e.getMessage());
        }
        return 0d;
    }

    /**
     * Get the first positive value from a list of SMC keys.
     *
     * @param conn The connection
     * @param keys The keys to try in order
     * @return The first value greater than 0, or 0 if all keys fail
     */
    public static double smcGetFirstFloat(MemorySegment conn, String... keys) {
        for (String key : keys) {
            double val = smcGetFloat(conn, key);
            if (val > 0d) {
                return val;
            }
        }
        return 0d;
    }

    /**
     * Get a 64-bit integer value from SMC
     *
     * @param conn The connection
     * @param key  The key to retrieve
     * @return Long representing the value
     */
    public static long smcGetLong(MemorySegment conn, String key) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment val = arena.allocate(SMC_VAL);
            int result = smcReadKey(conn, key, val, arena);
            if (result == 0) {
                int dataSize = val.get(JAVA_INT, SMC_VAL.byteOffset(SMC_VAL_DATA_SIZE));
                byte[] bytes = readByteArray(val, SMC_VAL.byteOffset(SMC_VAL_BYTES), dataSize);
                return ParseUtil.byteArrayToLong(bytes, dataSize);
            }
        } catch (Throwable e) {
            LOG.warn("Failed to read SMC long key {}: {}", key, e.getMessage());
        }
        return 0L;
    }

    private static int smcReadKey(MemorySegment conn, String key, MemorySegment val, Arena arena) throws Throwable {
        MemorySegment input = arena.allocate(SMC_KEY_DATA);
        MemorySegment output = arena.allocate(SMC_KEY_DATA);

        input.set(JAVA_INT, SMC_KEY_DATA.byteOffset(SMC_KEY), (int) ParseUtil.strToLong(key, 4));

        int result = smcGetKeyInfo(conn, input, output, arena);
        if (result == 0) {
            int dataSize = output.get(JAVA_INT, KEY_INFO_DATA_SIZE_OFFSET);
            int dataType = output.get(JAVA_INT, KEY_INFO_DATA_TYPE_OFFSET);

            val.set(JAVA_INT, SMC_VAL.byteOffset(SMC_VAL_DATA_SIZE), dataSize);
            byte[] typeBytes = ParseUtil.longToByteArray(dataType, 4, 5);
            writeByteArray(val, SMC_VAL.byteOffset(SMC_VAL_DATA_TYPE), typeBytes);

            // Reset input and re-populate for the actual read
            input.fill((byte) 0);
            input.set(JAVA_INT, SMC_KEY_DATA.byteOffset(SMC_KEY), (int) ParseUtil.strToLong(key, 4));
            input.set(JAVA_INT, KEY_INFO_DATA_SIZE_OFFSET, dataSize);
            input.set(JAVA_BYTE, SMC_KEY_DATA.byteOffset(SMC_DATA8), SMC_CMD_READ_BYTES);

            result = smcCall(conn, KERNEL_INDEX_SMC, input, output);
            if (result == 0) {
                byte[] outBytes = readByteArray(output, SMC_KEY_DATA.byteOffset(SMC_BYTES), 32);
                writeByteArray(val, SMC_VAL.byteOffset(SMC_VAL_BYTES), outBytes);
            }
        }
        return result;
    }

    private static int smcGetKeyInfo(MemorySegment conn, MemorySegment input, MemorySegment output, Arena arena)
            throws Throwable {
        int key = input.get(JAVA_INT, SMC_KEY_DATA.byteOffset(SMC_KEY));
        int[] cached = KEY_INFO_CACHE.get(key);
        if (cached != null) {
            output.set(JAVA_INT, KEY_INFO_DATA_SIZE_OFFSET, cached[0]);
            output.set(JAVA_INT, KEY_INFO_DATA_TYPE_OFFSET, cached[1]);
            output.set(JAVA_BYTE, KEY_INFO_DATA_ATTR_OFFSET, (byte) cached[2]);
            return 0;
        }
        input.set(JAVA_BYTE, SMC_KEY_DATA.byteOffset(SMC_DATA8), SMC_CMD_READ_KEYINFO);
        int result = smcCall(conn, KERNEL_INDEX_SMC, input, output);
        if (result == 0) {
            int dataSize = output.get(JAVA_INT, KEY_INFO_DATA_SIZE_OFFSET);
            int dataType = output.get(JAVA_INT, KEY_INFO_DATA_TYPE_OFFSET);
            byte dataAttr = output.get(JAVA_BYTE, KEY_INFO_DATA_ATTR_OFFSET);
            KEY_INFO_CACHE.put(key, new int[] { dataSize, dataType, dataAttr & 0xFF });
        }
        return result;
    }

    private static int smcCall(MemorySegment conn, int index, MemorySegment input, MemorySegment output)
            throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outputSize = arena.allocate(JAVA_LONG);
            outputSize.set(JAVA_LONG, 0, SMC_KEY_DATA.byteSize());
            return IOConnectCallStructMethod(conn, index, input, SMC_KEY_DATA.byteSize(), output, outputSize);
        }
    }

    private static byte[] readByteArray(MemorySegment seg, long offset, int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = seg.get(JAVA_BYTE, offset + i);
        }
        return result;
    }

    private static void writeByteArray(MemorySegment seg, long offset, byte[] data) {
        for (int i = 0; i < data.length; i++) {
            seg.set(JAVA_BYTE, offset + i, data[i]);
        }
    }
}
