/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

/**
 * Utility to read process resource limits from {@code /proc/<pid>/limits}, which Linux and the BSDs with a mounted
 * procfs both provide in the same format.
 */
@ThreadSafe
public final class ProcLimits {

    private ProcLimits() {
    }

    /**
     * Queries {@code /proc/<pid>/limits} for a process's open file limit.
     *
     * @param processId the process ID
     * @param index     {@code 1} for the soft limit, {@code 2} for the hard limit
     * @return the limit, or {@code -1} if procfs is not mounted, the row is absent, or that field is unlimited
     */
    public static long queryOpenFileLimit(long processId, int index) {
        final String limitsPath = String.format(Locale.ROOT, "/proc/%d/limits", processId);
        if (!Files.exists(Paths.get(limitsPath))) {
            return -1; // not supported
        }
        return parseOpenFileLimit(FileUtil.readFile(limitsPath), index);
    }

    /**
     * Parses the open file limit out of {@code /proc/<pid>/limits} content. Split from the file read so it can be
     * tested against fixture data on any operating system.
     * <p>
     * A field reading {@code unlimited} contributes no digits, so it is absent from the split and yields {@code -1} for
     * that index alone. The other field is still returned: a process whose hard limit is unlimited normally still has a
     * real soft limit.
     *
     * @param lines the lines of {@code /proc/<pid>/limits}
     * @param index {@code 1} for the soft limit, {@code 2} for the hard limit
     * @return the limit, or {@code -1} if there is no {@code Max open files} row or that field is unlimited
     */
    public static long parseOpenFileLimit(List<String> lines, int index) {
        final Optional<String> maxOpenFilesLine = lines.stream().filter(line -> line.startsWith("Max open files"))
                .findFirst();
        if (!maxOpenFilesLine.isPresent()) {
            return -1;
        }
        // Split all non-digits away -> ["", "{soft-limit}", "{hard-limit}", ""]
        final String[] split = ParseUtil.notDigits.split(maxOpenFilesLine.get(), -1);
        // Element 0 is the empty string left of the label, so a usable index is 1 or 2
        if (index < 1 || split.length <= index) {
            return -1;
        }
        return ParseUtil.parseLongOrDefault(split[index], -1);
    }
}
