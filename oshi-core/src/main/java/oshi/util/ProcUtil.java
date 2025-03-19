package oshi.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class ProcUtil {

    private ProcUtil() {
    }

    /**
     * Parses /proc files with a given structure consisting of a keyed header line followed by a
     * keyed value line. {@code /proc/net/netstat} and {@code /proc/net/snmp} are specific examples
     * of this. The returned map is of the form {key: {stat: value, stat: value, ...}}. An example
     * of the file structure is:
     * <pre>
     *     TcpExt: SyncookiesSent SyncookiesRecv SyncookiesFailed ...
     *     TcpExt: 0 4 0 ...
     *     IpExt: InNoRoutes InTruncatedPkts InMcastPkts OutMcastPkts ...
     *     IpExt: 55 0 27786 1435 ...
     *     MPTcpExt: MPCapableSYNRX MPCapableSYNTX MPCapableSYNACKRX ...
     *     MPTcpExt: 0 0 0 ...
     * </pre>
     * Which would produce a mapping structure like:
     * <pre>
     *     {
     *         "TcpExt": {"SyncookiesSent":0, "SyncookiesRecv":4, "SyncookiesFailed":0, ... }
     *         "IpExt": {"InNoRoutes":55, "InTruncatedPkts":27786, "InMcastPkts":1435, ... }
     *         "MPTcpExt": {"MPCapableSYNACKRX":0, "MPCapableSYNTX":0, "MPCapableSYNACKRX":0, ... }
     *     }
     * </pre>
     *
     * @param procFile the file to process
     * @param keys an optional array of keys to return. If none are given, all found keys are returned
     * @return a map of keys to stats
     */
    public static Map<String, Map<String, Long>> getMapFromHeaderProc(String procFile, String... keys) {
        Map<String, Map<String, Long>> result = new HashMap<>();

        List<String> lines = FileUtil.readFile(procFile);
        String previousKey = null;

        for (String line : lines) {
            String[] parts = line.split("\\s+");
            String key = parts[0].substring(0, parts[0].length() - 1);

            if (keys.length > 0 && Arrays.binarySearch(keys, key) < 0) {
                continue;
            }

            if (key.equals(previousKey)) {
                Map<String, Long> data = result.get(key);
                if (data != null) {
                    int idx = 1;
                    for (String stat : data.keySet()) {
                        data.put(stat, ParseUtil.parseLongOrDefault(parts[idx], 0));
                        idx++;
                    }
                }
            } else {
                // Use a LinkedHashMap to preserve the insertion order
                Map<String, Long> data = new LinkedHashMap<>();
                for (int i = 1; i < parts.length; i++) {
                    data.put(parts[i], 0L);
                }
                result.put(key, data);
            }

            previousKey = key;
        }

        return result;
    }

}
