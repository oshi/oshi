package oshi.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * @param keys an optional array of keys to return in the outer map. If none are given, all
     *             found keys are returned.
     * @return a map of keys to stats
     */
    public static Map<String, Map<String, Long>> parseNestedStatistics(String procFile, String... keys) {
        Map<String, Map<String, Long>> result = new HashMap<>();
        Set<String> keysSet = new HashSet<>(Arrays.asList(keys));

        List<String> lines = FileUtil.readFile(procFile);
        String previousKey = null;
        String[] statNames = null;

        for (String line : lines) {
            String[] parts = ParseUtil.whitespaces.split(line);
            if (parts.length == 0) {
                continue;
            }
            String key = parts[0].substring(0, parts[0].length() - 1);

            if (!keysSet.isEmpty() && !keysSet.contains(key)) {
                continue;
            }

            if (key.equals(previousKey)) {
                if (parts.length == statNames.length) {
                    Map<String, Long> stats = new HashMap<>(parts.length - 1);
                    for (int i = 1; i < parts.length; i++) {
                        stats.put(statNames[i], ParseUtil.parseLongOrDefault(parts[i], 0));
                    }
                    result.put(key, stats);
                }
            } else {
                statNames = parts;
            }

            previousKey = key;
        }

        return result;
    }

    /**
     * Parses /proc files formatted as "statistic  (long)value" to produce a simple mapping. An
     * example would be /proc/net/snmp6. The file format would look like:
     * <pre>
     *    Ip6InReceives             8026
     *    Ip6InHdrErrors            0
     *    Icmp6InMsgs               2
     *    Icmp6InErrors             0
     *    Icmp6OutMsgs              424
     *    Udp6IgnoredMulti          5
     *    Udp6MemErrors             1
     *    UdpLite6InDatagrams       37
     *    UdpLite6NoPorts           1
     * </pre>
     * Which would produce a mapping structure like:
     * <pre>
     *     {
     *         "Ip6InReceives":8026,
     *         "Ip6InHdrErrors":0,
     *         "Icmp6InMsgs":2,
     *         "Icmp6InErrors":0,
     *         ...
     *     }
     * </pre>
     *
     * @param procFile the file to process
     * @param separator a regex specifying the separator between statistic and value. For
     *                  whitespace use {@code "\\s+"}.
     * @return a map statistics and associated values
     */
    public static Map<String, Long> parseStatistics(String procFile, String separator) {
        Map<String, Long> result = new HashMap<>();
        List<String> lines = FileUtil.readFile(procFile, false);
        for (String line : lines) {
            String[] parts = line.split(separator);
            if (parts.length == 2) {
                result.put(parts[0], ParseUtil.parseLongOrDefault(parts[1], 0));
            }
        }

        return result;
    }
}
