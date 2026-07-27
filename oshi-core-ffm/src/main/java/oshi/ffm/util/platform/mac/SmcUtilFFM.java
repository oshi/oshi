/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.mac;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.ForeignFunctions.callInArenaDoubleOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaIntOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaLongOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.ffm.platform.mac.IOKitFunctions.IOConnectCallStructMethod;
import static oshi.ffm.platform.mac.IOKitFunctions.IOServiceClose;
import static oshi.ffm.platform.mac.IOKitFunctions.IOServiceOpen;
import static oshi.ffm.platform.mac.MacSystem.SMC_BYTES;
import static oshi.ffm.platform.mac.MacSystem.SMC_DATA32;
import static oshi.ffm.platform.mac.MacSystem.SMC_DATA8;
import static oshi.ffm.platform.mac.MacSystem.SMC_DATA_ATTRIBUTES;
import static oshi.ffm.platform.mac.MacSystem.SMC_DATA_SIZE;
import static oshi.ffm.platform.mac.MacSystem.SMC_DATA_TYPE;
import static oshi.ffm.platform.mac.MacSystem.SMC_KEY;
import static oshi.ffm.platform.mac.MacSystem.SMC_KEY_DATA;
import static oshi.ffm.platform.mac.MacSystem.SMC_KEY_INFO;
import static oshi.ffm.platform.mac.MacSystem.SMC_VAL;
import static oshi.ffm.platform.mac.MacSystem.SMC_VAL_BYTES;
import static oshi.ffm.platform.mac.MacSystem.SMC_VAL_DATA_SIZE;
import static oshi.ffm.platform.mac.MacSystem.SMC_VAL_DATA_TYPE;
import static oshi.ffm.platform.mac.MacSystemFunctions.mach_task_self;
import static oshi.util.ExceptionUtil.getIntOrDefault;
import static oshi.util.LogLevel.DEBUG;
import static oshi.util.LogLevel.ERROR;
import static oshi.util.LogLevel.WARN;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.mac.IOKit.IOService;
import oshi.util.GlobalConfig;
import oshi.util.ParseUtil;
import oshi.util.common.platform.mac.SmcKeyIndex;

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

    /** SMC key for the number of fans. */
    public static final String SMC_KEY_FAN_NUM = "FNum";
    /** SMC key format for fan speed (use with String.format). */
    public static final String SMC_KEY_FAN_SPEED = "F%dAc";
    /** SMC key for CPU proximity temperature. */
    public static final String SMC_KEY_CPU_TEMP = "TC0P";
    /** SMC key for CPU voltage. */
    public static final String SMC_KEY_CPU_VOLTAGE = "VC0C";

    /**
     * Apple Silicon CPU-die aggregate temperature keys, tried in order until one returns a plausible value. These are
     * computed by the SMC firmware and are chip-independent, unlike the per-core {@link #SMC_KEYS_CPU_TEMP_AS} keys
     * which vary between chips (and are entirely absent on some, e.g. the M3 Pro). {@code TCMb} is the CPU-die average
     * and {@code TCMz} is the CPU-die maximum; the average is preferred because it stays close to the single-sensor
     * value OSHI historically reported.
     */
    public static final List<String> SMC_KEYS_CPU_TEMP_AGGREGATE_AS = List.of("TCMb", "TCMz");
    /** SMC keys for Apple Silicon CPU temperature sensors. */
    public static final List<String> SMC_KEYS_CPU_TEMP_AS = List.of("Tp09", "Tp0T", "Tp01", "Tp05", "Tp0D");
    /**
     * Fallback Apple Silicon GPU temperature keys, used only when runtime discovery cannot complete. The hottest
     * plausible reading among them is reported.
     * <p>
     * GPU sensor keys are chip-specific, so no fixed list is complete: on an M2 Max only {@code Tg0f} and {@code Tg0j}
     * of the four originally shipped exist, and six of that machine's sensors appear in no published key table at all.
     * {@link #getGpuTemperatureKeys()} therefore discovers them from the SMC instead; this list is breadth-first
     * insurance for the case where that fails. It leads with the four keys OSHI read before discovery existed, so the
     * fallback can never report less than the previous implementation, followed by the keys most commonly present
     * across published M1 through M5 and A18 sensor dumps.
     */
    public static final List<String> SMC_KEYS_GPU_TEMP_AS = List.of("Tg05", "Tg0D", "Tg0f", "Tg0j", "Tg0C", "Tg04",
            "Tg0K", "Tg0L", "Tg0d", "Tg0e", "Tg1k", "Tg0X", "Tg0S", "Tg0y", "Tg0z");

    /** SMC key whose value is the number of keys in the key index. */
    public static final String SMC_KEY_COUNT = "#KEY";

    /** The prefix shared by Apple Silicon GPU cluster temperature keys. */
    private static final String GPU_KEY_PREFIX = "Tg";

    /** Guards {@link #gpuTemperatureKeys}. */
    private static final Object GPU_KEY_LOCK = new Object();

    /**
     * Discovered GPU temperature keys, or null if discovery has not yet completed successfully. Deliberately not a
     * {@link oshi.util.Memoizer}: that would cache a failed discovery permanently, and a transient failure to open the
     * SMC would then disable GPU temperature for the lifetime of the JVM.
     */
    private static volatile List<String> gpuTemperatureKeys;
    /** SMC key for Apple Silicon CPU voltage. */
    public static final String SMC_KEY_CPU_VOLTAGE_AS = "VP0C";

    /** SMC command to read bytes. */
    public static final byte SMC_CMD_READ_BYTES = 5;
    /** SMC command to read the key at an index in the key index. */
    public static final byte SMC_CMD_READ_INDEX = 8;
    /** SMC command to read key info. */
    public static final byte SMC_CMD_READ_KEYINFO = 9;
    /** Kernel index for SMC. */
    public static final int KERNEL_INDEX_SMC = 2;

    private SmcUtilFFM() {
    }

    /**
     * Open a connection to SMC.
     *
     * @return The io_connect_t port (nonzero) if successful, 0 if failure. Caller must close with {@link #smcClose}.
     */
    public static int smcOpen() {
        IOService smcService = IOKitUtilFFM.getMatchingService("AppleSMC");
        if (smcService == null) {
            LOG.error("Unable to locate AppleSMC service");
            return 0;
        }
        try {
            return callInArenaIntOrDefault(arena -> {
                MemorySegment connPtr = arena.allocate(JAVA_INT);
                int task = mach_task_self();
                int result = IOServiceOpen(smcService.segment(), task, 0, connPtr);
                if (result == 0) {
                    int conn = connPtr.get(JAVA_INT, 0);
                    if (conn == 0) {
                        LOG.error("IOServiceOpen returned null connect handle");
                        return 0;
                    }
                    return conn;
                }
                String hex = String.format(Locale.ROOT, "0x%08x", result);
                LOG.error("Unable to open connection to AppleSMC service. Error: {}", hex);
                return 0;
            }, LOG, ERROR, "Exception opening SMC connection", 0);
        } finally {
            smcService.release();
        }
    }

    /**
     * Close connection to SMC.
     *
     * @param conn The io_connect_t port returned by {@link #smcOpen}
     * @return 0 if successful, nonzero if failure
     */
    public static int smcClose(int conn) {
        return getIntOrDefault(() -> IOServiceClose(conn), -1);
    }

    /**
     * Get a value from SMC which is in a floating point datatype (SP78, FPE2, FLT)
     *
     * @param conn The connection
     * @param key  The key to retrieve
     * @return Double representing the value
     */
    public static double smcGetFloat(int conn, String key) {
        return callInArenaDoubleOrDefault(arena -> {
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
            return 0d;
        }, 0d, LOG, WARN, "Failed to read SMC float key {}", key);
    }

    /**
     * The lowest reading accepted as a genuine temperature, in degrees Celsius.
     * <p>
     * Apple Silicon power-gates a CPU core cluster when it is idle, and the SMC then reports a fixed sentinel for each
     * die sensor in that cluster instead of a reading. Observed sentinels are 6.7 and 4.633 on an M2 Max (Tp01/Tp09 and
     * Tp05/Tp0D), up to 8.425 across all of that machine's sensors, and -4.0 through 5.2 in the
     * <a href="https://github.com/dkorunic/iSMC">iSMC</a> sample reports, which independently describe them as
     * "firmware sentinels from inactive sensor slots". They are below room ambient and so cannot be real die
     * temperatures, but most are positive, so a simple {@code > 0} test accepts them.
     * <p>
     * Across those reports, covering M1 through M5, A18 and Intel T2 machines, no genuine sensor read between 8.425 and
     * 21, so this floor sits in an empty band with margin on both sides. It is a plausibility guard, not a hardware
     * specification.
     */
    public static final double MIN_PLAUSIBLE_TEMPERATURE = 15d;

    /**
     * Tests whether a reading is plausible as a temperature, rejecting the sentinel an idle-gated sensor reports.
     *
     * @param celsius the reading to test, in degrees Celsius
     * @return true if the reading is at least {@link #MIN_PLAUSIBLE_TEMPERATURE}
     */
    public static boolean isPlausibleTemperature(double celsius) {
        return celsius >= MIN_PLAUSIBLE_TEMPERATURE;
    }

    /**
     * Get the first plausible temperature from a list of SMC keys, skipping sensors that are reporting a parked value
     * because their core cluster is idle. See {@link #MIN_PLAUSIBLE_TEMPERATURE}.
     *
     * @param conn The connection
     * @param keys The keys to try in order
     * @return The first reading at or above {@link #MIN_PLAUSIBLE_TEMPERATURE}, or 0 if no key returned one
     */
    public static double smcGetFirstTemperature(int conn, List<String> keys) {
        for (String key : keys) {
            double val = smcGetFloat(conn, key);
            if (isPlausibleTemperature(val)) {
                return val;
            }
            if (val != 0d) {
                LOG.debug("Ignoring implausible temperature {} from SMC key {}; the sensor is likely idle-gated.", val,
                        key);
            }
        }
        return 0d;
    }

    /**
     * Get the highest plausible temperature among a list of SMC keys.
     * <p>
     * Plausibility is applied per read rather than when the keys were discovered, because whether a key exists is a
     * property of the hardware while whether it currently reports a usable value is not.
     * <p>
     * Clusters expose their sensors in pairs at different locations on the die, offset by a few degrees and tracking
     * each other as load changes; verified on an M2 Max where a pair moved 44.8/51.4 to 56.1/62.6 under load and fell
     * together afterwards. They are not an instantaneous/peak-hold pair, so taking the maximum consistently reports the
     * hotter location rather than a latched peak.
     *
     * @param conn The connection
     * @param keys The keys to read
     * @return The highest reading at or above {@link #MIN_PLAUSIBLE_TEMPERATURE}, or 0 if none were plausible
     */
    public static double smcGetMaxTemperature(int conn, List<String> keys) {
        return SmcKeyIndex.maxPlausible(keys, key -> smcGetFloat(conn, key), SmcUtilFFM::isPlausibleTemperature);
    }

    /**
     * Get the name of the key at an index in the SMC key index.
     *
     * @param conn  The connection
     * @param index The index, from 0 to the value of {@link #SMC_KEY_COUNT}
     * @return The four-character key name, or null if it could not be read
     */
    public static String smcReadKeyAtIndex(int conn, int index) {
        return callInArenaOrDefault(arena -> {
            MemorySegment input = arena.allocate(SMC_KEY_DATA);
            MemorySegment output = arena.allocate(SMC_KEY_DATA);
            input.set(JAVA_BYTE, SMC_KEY_DATA.byteOffset(SMC_DATA8), SMC_CMD_READ_INDEX);
            input.set(JAVA_INT, SMC_KEY_DATA.byteOffset(SMC_DATA32), index);
            if (smcCall(conn, KERNEL_INDEX_SMC, input, output) != 0) {
                return null;
            }
            int key = output.get(JAVA_INT, SMC_KEY_DATA.byteOffset(SMC_KEY));
            byte[] keyBytes = ParseUtil.longToByteArray(key, 4, 4);
            StringBuilder sb = new StringBuilder(4);
            for (byte b : keyBytes) {
                sb.append((char) (b & 0xFF));
            }
            return sb.toString();
        }, null, LOG, DEBUG, "Failed to read SMC key at index {}", index);
    }

    /**
     * Get the SMC data type of a key, e.g. {@code flt} or {@code sp78}.
     *
     * @param conn The connection
     * @param key  The key to query
     * @return The data type, or an empty string if it could not be read
     */
    public static String smcGetDataType(int conn, String key) {
        return callInArenaOrDefault(arena -> {
            MemorySegment val = arena.allocate(SMC_VAL);
            if (smcReadKey(conn, key, val, arena) != 0) {
                return "";
            }
            byte[] type = readByteArray(val, SMC_VAL.byteOffset(SMC_VAL_DATA_TYPE), 5);
            StringBuilder sb = new StringBuilder(4);
            for (byte b : type) {
                if (b == 0) {
                    break;
                }
                sb.append((char) b);
            }
            return sb.toString().trim();
        }, "", LOG, DEBUG, "Failed to read SMC data type for key {}", key);
    }

    /**
     * Get the Apple Silicon GPU temperature keys for this machine, discovering them from the SMC on first use and
     * caching the result.
     * <p>
     * Discovery binary searches the sorted key index for the {@code Tg} block and keeps the keys matching the GPU
     * naming convention. It does not filter by plausibility: an idle sensor reports a sentinel below ambient, so
     * filtering here would let one unlucky first call cache an empty set and disable GPU temperature permanently.
     * Plausibility is applied per read by {@link #smcGetMaxTemperature}.
     * <p>
     * Can be overridden with the {@link GlobalConfig#OSHI_OS_MAC_SENSORS_GPUTEMPERATURE_KEYS} configuration property,
     * which bypasses discovery entirely. Falls back to {@link #SMC_KEYS_GPU_TEMP_AS} if discovery cannot complete,
     * without caching, so a later call retries.
     *
     * @return The keys to read for GPU temperature, never null
     */
    public static List<String> getGpuTemperatureKeys() {
        List<String> keys = gpuTemperatureKeys;
        if (keys != null) {
            return keys;
        }
        synchronized (GPU_KEY_LOCK) {
            if (gpuTemperatureKeys != null) {
                return gpuTemperatureKeys;
            }
            // Read the config lazily rather than at class initialization, so GlobalConfig.set() still takes effect.
            List<String> configured = SmcKeyIndex
                    .parseConfiguredKeys(GlobalConfig.get(GlobalConfig.OSHI_OS_MAC_SENSORS_GPUTEMPERATURE_KEYS, ""));
            if (!configured.isEmpty()) {
                LOG.debug("Using configured GPU temperature keys {}", configured);
                gpuTemperatureKeys = configured;
                return configured;
            }
            List<String> discovered = discoverGpuTemperatureKeys();
            if (discovered == null) {
                LOG.debug("GPU temperature key discovery did not complete; using the fallback list this time.");
                return SMC_KEYS_GPU_TEMP_AS;
            }
            LOG.debug("Discovered {} GPU temperature keys: {}", discovered.size(), discovered);
            gpuTemperatureKeys = discovered;
            return discovered;
        }
    }

    /**
     * @return the discovered keys, or null if the SMC key index could not be read
     */
    private static List<String> discoverGpuTemperatureKeys() {
        int conn = smcOpen();
        if (conn == 0) {
            return null;
        }
        try {
            int keyCount = (int) smcGetLong(conn, SMC_KEY_COUNT);
            // No data-type filter: smcGetFloat already decodes every temperature encoding the SMC uses (flt, sp78,
            // fpe2) and returns 0 for anything else, which the plausibility floor then rejects.
            return SmcKeyIndex.findKeys(keyCount, i -> smcReadKeyAtIndex(conn, i), GPU_KEY_PREFIX,
                    SmcKeyIndex::isGpuTemperatureKey);
        } finally {
            smcClose(conn);
        }
    }

    /**
     * Get a 64-bit integer value from SMC
     *
     * @param conn The connection
     * @param key  The key to retrieve
     * @return Long representing the value
     */
    public static long smcGetLong(int conn, String key) {
        return callInArenaLongOrDefault(arena -> {
            MemorySegment val = arena.allocate(SMC_VAL);
            int result = smcReadKey(conn, key, val, arena);
            if (result == 0) {
                int dataSize = val.get(JAVA_INT, SMC_VAL.byteOffset(SMC_VAL_DATA_SIZE));
                byte[] bytes = readByteArray(val, SMC_VAL.byteOffset(SMC_VAL_BYTES), dataSize);
                return ParseUtil.byteArrayToLong(bytes, dataSize);
            }
            return 0L;
        }, 0L, LOG, WARN, "Failed to read SMC long key {}", key);
    }

    private static int smcReadKey(int conn, String key, MemorySegment val, Arena arena) throws Throwable {
        MemorySegment input = arena.allocate(SMC_KEY_DATA);
        MemorySegment output = arena.allocate(SMC_KEY_DATA);

        input.set(JAVA_INT, SMC_KEY_DATA.byteOffset(SMC_KEY), (int) ParseUtil.strToLong(key, 4));

        int result = smcGetKeyInfo(conn, input, output);
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

    private static int smcGetKeyInfo(int conn, MemorySegment input, MemorySegment output) throws Throwable {
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

    private static int smcCall(int conn, int index, MemorySegment input, MemorySegment output) throws Throwable {
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
