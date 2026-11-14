/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.aix;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.common.AbstractLogicalVolumeGroup;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * An AIX Logical Volume Manager volume group, read with {@code lsvg} and {@code lspv}.
 */
@Immutable
public final class AixLogicalVolumeGroup extends AbstractLogicalVolumeGroup {

    private AixLogicalVolumeGroup(String name, Map<String, Set<String>> lvMap, Set<String> pvSet) {
        super(name, lvMap, pvSet);
    }

    /**
     * Gets the active volume groups. None of the commands needs privilege.
     *
     * @return the volume groups that are varied on
     */
    public static List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        return queryLogicalVolumeGroups(ExecutingCommand::runNative);
    }

    /**
     * Reads the active volume groups through the given command runner. Package-private for testing.
     * <p>
     * {@code lsvg -o} names the volume groups that are varied on, and {@code lsvg -p} and {@code lsvg -l}, given all of
     * them at once, list each one's physical and logical volumes. {@code -L} keeps those from waiting on a volume
     * group's lock, which another LVM command may hold for as long as it runs, at the cost of a listing that may be
     * incomplete while that volume group is being changed. Neither says which physical volumes hold a logical volume,
     * so in a volume group of more than one, {@code lspv -l} lists the logical volumes on each physical volume; in a
     * volume group of one, every logical volume is on it. Physical volumes are named as {@code getDiskStores()} names
     * them, as in {@code hdisk0}.
     *
     * @param run runs a command and returns the lines it prints
     * @return the volume groups that are varied on
     */
    static List<LogicalVolumeGroup> queryLogicalVolumeGroups(Function<String, List<String>> run) {
        List<String> vgs = new ArrayList<>();
        for (String line : run.apply("lsvg -o")) {
            String vg = line.trim();
            if (!vg.isEmpty()) {
                vgs.add(vg);
            }
        }
        if (vgs.isEmpty()) {
            return new ArrayList<>();
        }
        String vgList = String.join(" ", vgs);
        Map<String, Set<String>> pvsByVg = parseSections(run.apply("lsvg -L -p " + vgList), "PV_NAME");
        Map<String, Set<String>> lvsByVg = parseSections(run.apply("lsvg -L -l " + vgList), "LV NAME");
        List<LogicalVolumeGroup> groups = new ArrayList<>();
        for (String vg : vgs) {
            Set<String> pvs = pvsByVg.getOrDefault(vg, new LinkedHashSet<>());
            Map<String, Set<String>> lvMap = new LinkedHashMap<>();
            for (String lv : lvsByVg.getOrDefault(vg, new LinkedHashSet<>())) {
                lvMap.put(lv, pvs.size() == 1 ? new LinkedHashSet<>(pvs) : new LinkedHashSet<>());
            }
            if (pvs.size() > 1) {
                for (String pv : pvs) {
                    for (Set<String> lvs : parseSections(run.apply("lspv -l " + pv), "LV NAME").values()) {
                        for (String lv : lvs) {
                            Set<String> onPvs = lvMap.get(lv);
                            if (onPvs != null) {
                                onPvs.add(pv);
                            }
                        }
                    }
                }
            }
            groups.add(new AixLogicalVolumeGroup(vg, lvMap, pvs));
        }
        return groups;
    }

    /**
     * Parses the output of {@code lsvg -p}, {@code lsvg -l} or {@code lspv -l}, which print a {@code name:} line for
     * each volume group or physical volume asked about, then a heading row, then one row per volume whose first column
     * is its name.
     *
     * @param lines   the lines the command printed
     * @param heading the start of the heading row, which is skipped
     * @return the names in the first column, keyed by the section they appear under
     */
    static Map<String, Set<String>> parseSections(List<String> lines, String heading) {
        Map<String, Set<String>> sections = new LinkedHashMap<>();
        Set<String> current = null;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.endsWith(":") && !trimmed.contains(" ")) {
                current = sections.computeIfAbsent(trimmed.substring(0, trimmed.length() - 1),
                        k -> new LinkedHashSet<>());
            } else if (current != null && !isHeading(trimmed, heading)) {
                current.add(ParseUtil.whitespaces.split(trimmed, 2)[0]);
            }
        }
        return sections;
    }

    /**
     * Whether a row is the column heading: one that starts with the heading's words followed by whitespace, so a volume
     * whose name merely begins with them, such as {@code PV_NAMEdata}, is not mistaken for it.
     *
     * @param row     a trimmed row
     * @param heading the heading's first words
     * @return whether the row is the heading
     */
    private static boolean isHeading(String row, String heading) {
        return row.startsWith(heading)
                && (row.length() == heading.length() || Character.isWhitespace(row.charAt(heading.length())));
    }
}
