/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.bsd.disk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * Parses the BSD mount tables and {@code df -i} into the fields a file store needs.
 * <p>
 * Each format is parsed by its fixed parts rather than split on whitespace, so a mount point containing spaces keeps
 * them. None of these commands escapes a space: each prints the kernel's path as it is.
 */
@ThreadSafe
public final class MountTable {

    // A JSON string, its escapes left in place
    private static final String JSON_STRING = "\"([^\"\\\\]*+(?:\\\\.[^\"\\\\]*+)*+)\"";
    // One fstab entry of FreeBSD's mount -p --libxo json, whose fields putfsent emits in this order
    private static final Pattern LIBXO_ENTRY = Pattern
            .compile("\\{\\s*\"device\"\\s*:\\s*" + JSON_STRING + "\\s*,\\s*\"mntpoint\"\\s*:\\s*" + JSON_STRING
                    + "\\s*,\\s*\"fstype\"\\s*:\\s*" + JSON_STRING + "\\s*,\\s*\"opts\"\\s*:\\s*" + JSON_STRING);
    private static final Pattern TABS = Pattern.compile("\\t+");
    // NetBSD's mount: /dev/dk1 on / type ffs (noatime, local)
    private static final Pattern NETBSD_MOUNT = Pattern.compile("(.+?) on (.+) type (\\S+) (\\(.*\\))");
    // OpenBSD's mount -v, which adds the disk's DUID and partition where it has one:
    // /dev/sd0a (ed2d8ecd4d5492ee.a) on / type ffs (rw, local, ctime=Wed Oct 7 22:17:15 2026)
    private static final Pattern OPENBSD_MOUNT = Pattern
            .compile("(.+?)(?: \\(([0-9a-f]{16}\\.[a-p])\\))? on (.+) type (\\S+) (\\(.*\\))");
    // A df -i row: the volume, three block counts, the capacity, the inodes used and free, their percentage, and the
    // mount point, which runs to the end of the line. Avail can be negative once the reserve is in use, and a file
    // system without inodes prints - for its percentage
    private static final Pattern DF_INODES = Pattern
            .compile("(.+?)\\s+\\d+\\s+\\d+\\s+-?\\d+\\s+(?:\\d+%|-)\\s+(\\d+)\\s+(-?\\d+)\\s+(?:\\d+%|-)\\s+(.+)");

    private MountTable() {
    }

    /**
     * One mounted file system.
     */
    @Immutable
    public static final class Entry {
        private final String volume;
        private final String uuid;
        private final String path;
        private final String type;
        private final String options;

        Entry(String volume, String uuid, String path, String type, String options) {
            this.volume = volume;
            this.uuid = uuid;
            this.path = path;
            this.type = type;
            this.options = options;
        }

        /**
         * Gets the mounted device or other source.
         *
         * @return the volume
         */
        public String getVolume() {
            return volume;
        }

        /**
         * Gets the identifier the mount table gives the volume, if any.
         *
         * @return the identifier, or an empty string
         */
        public String getUuid() {
            return uuid;
        }

        /**
         * Gets the mount point.
         *
         * @return the mount point
         */
        public String getPath() {
            return path;
        }

        /**
         * Gets the file system type.
         *
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * Gets the mount options, as the mount table prints them.
         *
         * @return the options
         */
        public String getOptions() {
            return options;
        }
    }

    /**
     * Parses FreeBSD's {@code mount -p --libxo json}.
     * <p>
     * {@code mount -p} pads its fields with tabs, but after a field of 24 characters or more it separates it from the
     * next with a single space, which a space inside the field cannot be told apart from. The JSON form carries the
     * same fields unambiguously.
     *
     * @param json the lines of {@code mount -p --libxo json}
     * @return the mounted file systems, or an empty list where {@code mount} does not support libxo
     */
    public static List<Entry> parseFreeBsdLibxo(List<String> json) {
        List<Entry> entries = new ArrayList<>();
        Matcher m = LIBXO_ENTRY.matcher(String.join("\n", json));
        while (m.find()) {
            entries.add(new Entry(unescapeJson(m.group(1)), "", unescapeJson(m.group(2)), unescapeJson(m.group(3)),
                    unescapeJson(m.group(4))));
        }
        return entries;
    }

    /**
     * Parses {@code mount -p} on DragonFly, or on a FreeBSD whose {@code mount} has no libxo.
     * <p>
     * DragonFly prints {@code device<TAB>mount point<TAB>type options<TAB>dump pass}, one tab between each, so a mount
     * point keeps its spaces. FreeBSD pads every field with tabs instead, which leaves the type and options apart,
     * except that a field of 24 characters or more is followed by a single space: a long mount point then shares its
     * field with the type, which is split off at the last space. Any other line is split on whitespace, which cannot
     * keep a space in a mount point.
     *
     * @param lines the lines of {@code mount -p}
     * @return the mounted file systems
     */
    public static List<Entry> parseFstab(List<String> lines) {
        List<Entry> entries = new ArrayList<>();
        for (String line : lines) {
            String[] tabs = TABS.split(line.trim(), -1);
            // FreeBSD follows options of eight characters or more with a space, which is not a separator inside the
            // field
            int typeSpace = tabs.length == 4 ? tabs[2].trim().indexOf(' ') : -1;
            int pathSpace = tabs.length == 4 ? tabs[1].lastIndexOf(' ') : -1;
            if (typeSpace > 0) {
                // DragonFly: the type and options share a field
                entries.add(new Entry(tabs[0], "", tabs[1], tabs[2].substring(0, typeSpace),
                        tabs[2].substring(typeSpace + 1).trim()));
            } else if (pathSpace > 0) {
                // FreeBSD, a mount point of 24 characters or more: it shares a field with the type, which has no space
                entries.add(new Entry(tabs[0], "", tabs[1].substring(0, pathSpace), tabs[1].substring(pathSpace + 1),
                        tabs[2].trim()));
            } else if (tabs.length >= 5) {
                entries.add(new Entry(tabs[0], "", tabs[1], tabs[2], tabs[3].trim()));
            } else {
                String[] split = ParseUtil.whitespaces.split(line.trim(), -1);
                if (split.length >= 5) {
                    entries.add(new Entry(split[0], "", split[1], split[2], split[3]));
                }
            }
        }
        return entries;
    }

    /**
     * Parses NetBSD's {@code mount}.
     *
     * @param lines the lines of {@code mount}
     * @return the mounted file systems
     */
    public static List<Entry> parseNetBsd(List<String> lines) {
        List<Entry> entries = new ArrayList<>();
        for (String line : lines) {
            Matcher m = NETBSD_MOUNT.matcher(line.trim());
            if (m.matches()) {
                entries.add(new Entry(m.group(1), "", m.group(2), m.group(3), m.group(4)));
            }
        }
        return entries;
    }

    /**
     * Parses OpenBSD's {@code mount -v}. A file system on a disk is followed by its DUID and partition in parentheses,
     * which becomes the identifier without them; one with no disk, such as {@code tmpfs} or NFS, has none.
     *
     * @param lines the lines of {@code mount -v}
     * @return the mounted file systems
     */
    public static List<Entry> parseOpenBsdVerbose(List<String> lines) {
        List<Entry> entries = new ArrayList<>();
        for (String line : lines) {
            Matcher m = OPENBSD_MOUNT.matcher(line.trim());
            if (m.matches()) {
                String duid = m.group(2);
                entries.add(new Entry(m.group(1), duid == null ? "" : duid, m.group(3), m.group(4), m.group(5)));
            }
        }
        return entries;
    }

    /**
     * Parses {@code df -i} output into free and total inode counts keyed by mount point. The layout is the same on
     * FreeBSD, DragonFly, NetBSD and OpenBSD.
     * <p>
     * The mount point is the key because it is unique where the volume is not: two {@code tmpfs} mounts both name
     * {@code tmpfs} as their volume.
     *
     * @param df the lines of {@code df -i}
     * @return a pair of the free and the total inodes, each keyed by mount point
     */
    public static Pair<Map<String, Long>, Map<String, Long>> parseDfInodes(List<String> df) {
        Map<String, Long> free = new HashMap<>();
        Map<String, Long> total = new HashMap<>();
        for (String line : df) {
            Matcher m = DF_INODES.matcher(line.trim());
            if (m.matches()) {
                long used = ParseUtil.parseLongOrDefault(m.group(2), 0L);
                long ifree = ParseUtil.parseLongOrDefault(m.group(3), 0L);
                free.put(m.group(4), ifree);
                total.put(m.group(4), used + ifree);
            }
        }
        return new Pair<>(free, total);
    }

    private static String unescapeJson(String s) {
        if (s.indexOf('\\') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c != '\\' || i + 1 == s.length()) {
                sb.append(c);
                i++;
                continue;
            }
            // The escape and the character after it, plus four hex digits for \\u
            char e = s.charAt(i + 1);
            i += 2;
            switch (e) {
                case 'n':
                    sb.append('\n');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 'b':
                    sb.append('\b');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'u':
                    if (i + 4 <= s.length()) {
                        sb.append((char) ParseUtil.hexStringToInt(s.substring(i, i + 4), '?'));
                        i += 4;
                    } else {
                        sb.append(e);
                    }
                    break;
                default:
                    // \" \\ and \/ stand for the character itself
                    sb.append(e);
                    break;
            }
        }
        return sb.toString();
    }
}
