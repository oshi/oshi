/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.mac;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
import oshi.util.GlobalConfig;
import oshi.util.ParseUtil;
import oshi.util.common.platform.mac.SmcKeyIndex;

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

    /** SMC key for the number of fans. */
    public static final String SMC_KEY_FAN_NUM = "FNum";
    /** SMC key format string for fan speed (use with fan index). */
    public static final String SMC_KEY_FAN_SPEED = "F%dAc";
    /** SMC key for CPU temperature (Intel). */
    public static final String SMC_KEY_CPU_TEMP = "TC0P";
    /** SMC key for CPU voltage (Intel). */
    public static final String SMC_KEY_CPU_VOLTAGE = "VC0C";

    /**
     * Apple Silicon CPU-die aggregate temperature keys, tried in order until one returns a plausible value. These are
     * computed by the SMC firmware and are chip-independent, unlike the per-core {@link #SMC_KEYS_CPU_TEMP_AS} keys
     * which vary between chips (and are entirely absent on some, e.g. the M3 Pro). {@code TCMb} is the CPU-die average
     * and {@code TCMz} is the CPU-die maximum; the average is preferred because it stays close to the single-sensor
     * value OSHI historically reported.
     */
    public static final List<String> SMC_KEYS_CPU_TEMP_AGGREGATE_AS = Collections
            .unmodifiableList(Arrays.asList("TCMb", "TCMz"));
    /** Apple Silicon CPU temperature keys, tried in order until one returns a positive value. */
    public static final List<String> SMC_KEYS_CPU_TEMP_AS = Collections
            .unmodifiableList(Arrays.asList("Tp09", "Tp0T", "Tp01", "Tp05", "Tp0D"));
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
    public static final List<String> SMC_KEYS_GPU_TEMP_AS = Collections.unmodifiableList(Arrays.asList("Tg05", "Tg0D",
            "Tg0f", "Tg0j", "Tg0C", "Tg04", "Tg0K", "Tg0L", "Tg0d", "Tg0e", "Tg1k", "Tg0X", "Tg0S", "Tg0y", "Tg0z"));

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
     * <p>
     * Only ever assigned an unmodifiable copy of a completed discovery, so the reference is safely published by the
     * volatile write and the list it points at is immutable; no further synchronization is needed on the read path.
     */
    private static volatile List<String> gpuTemperatureKeys; // NOSONAR squid:S3077 - published value is immutable
    /** SMC key for CPU voltage (Apple Silicon). */
    public static final String SMC_KEY_CPU_VOLTAGE_AS = "VP0C";

    /** SMC command to read bytes. */
    public static final byte SMC_CMD_READ_BYTES = 5;
    /** SMC command to read the key at an index in the key index. */
    public static final byte SMC_CMD_READ_INDEX = 8;
    /** SMC command to read key info. */
    public static final byte SMC_CMD_READ_KEYINFO = 9;
    /** Kernel index for SMC calls. */
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
                    LOG.error(String.format(Locale.ROOT, "Unable to open connection to AppleSMC service. Error: 0x%08x",
                            result));
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
                    // First bit is sign, next 7 bits are integer portion, last 8 bits are the
                    // (unsigned) fractional portion
                    return val.bytes[0] + (val.bytes[1] & 0xFF) / 256d;
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
    public static double smcGetFirstTemperature(IOConnect conn, List<String> keys) {
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
    public static double smcGetMaxTemperature(IOConnect conn, List<String> keys) {
        return SmcKeyIndex.maxPlausible(keys, key -> smcGetFloat(conn, key), SmcUtil::isPlausibleTemperature);
    }

    /**
     * Get the name of the key at an index in the SMC key index.
     *
     * @param conn  The connection
     * @param index The index, from 0 to the value of {@link #SMC_KEY_COUNT}
     * @return The four-character key name, or null if it could not be read
     */
    public static String smcReadKeyAtIndex(IOConnect conn, int index) {
        try (SMCKeyData input = new SMCKeyData(); SMCKeyData output = new SMCKeyData()) {
            input.data8 = SMC_CMD_READ_INDEX;
            input.data32 = index;
            if (smcCall(conn, KERNEL_INDEX_SMC, input, output) != 0) {
                return null;
            }
            byte[] keyBytes = ParseUtil.longToByteArray(output.key, 4, 4);
            StringBuilder sb = new StringBuilder(4);
            for (byte b : keyBytes) {
                sb.append((char) (b & 0xFF));
            }
            return sb.toString();
        }
    }

    /**
     * Get the SMC data type of a key, e.g. {@code flt} or {@code sp78}.
     *
     * @param conn The connection
     * @param key  The key to query
     * @return The data type, or an empty string if it could not be read
     */
    public static String smcGetDataType(IOConnect conn, String key) {
        try (SMCVal val = new SMCVal()) {
            if (smcReadKey(conn, key, val) != 0 || val.dataType == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder(4);
            for (byte b : val.dataType) {
                if (b == 0) {
                    break;
                }
                sb.append((char) b);
            }
            return sb.toString().trim();
        }
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
        IOConnect conn = smcOpen();
        if (conn == null) {
            return null; // NOSONAR squid:S1168 - null means "could not read", which the caller must not cache
        }
        try {
            int keyCount = (int) smcGetLong(conn, SMC_KEY_COUNT);
            // No data-type filter: smcGetFloat already decodes every temperature encoding the SMC uses (flt, sp78,
            // fpe2) and returns 0 for anything else, which the plausibility floor then rejects. Filtering on "flt"
            // here would cost a second read per key and would wrongly exclude a sensor reported as sp78.
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
