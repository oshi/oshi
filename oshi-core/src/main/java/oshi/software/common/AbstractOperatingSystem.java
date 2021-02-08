/**
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.common;

import static oshi.software.os.OperatingSystem.ProcessFiltering.ALL_PROCESSES;
import static oshi.software.os.OperatingSystem.ProcessSorting.NO_SORTING;
import static oshi.util.Memoizer.memoize;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.sun.jna.Platform; // NOSONAR squid:S1191

import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.GlobalConfig;
import oshi.util.tuples.Pair;

/**
 * Common methods for OperatingSystem implementations
 */
public abstract class AbstractOperatingSystem implements OperatingSystem {

    public static final String OSHI_OS_UNIX_WHOCOMMAND = "oshi.os.unix.whoCommand";
    protected static final boolean USE_WHO_COMMAND = GlobalConfig.get(OSHI_OS_UNIX_WHOCOMMAND, false);

    private final Supplier<String> manufacturer = memoize(this::queryManufacturer);
    private final Supplier<Pair<String, OSVersionInfo>> familyVersionInfo = memoize(this::queryFamilyVersionInfo);
    private final Supplier<Integer> bitness = memoize(this::queryPlatformBitness);

    @Override
    public String getManufacturer() {
        return manufacturer.get();
    }

    protected abstract String queryManufacturer();

    @Override
    public String getFamily() {
        return familyVersionInfo.get().getA();
    }

    @Override
    public OSVersionInfo getVersionInfo() {
        return familyVersionInfo.get().getB();
    }

    protected abstract Pair<String, OSVersionInfo> queryFamilyVersionInfo();

    @Override
    public int getBitness() {
        return bitness.get();
    }

    private int queryPlatformBitness() {
        if (Platform.is64Bit()) {
            return 64;
        }
        // Initialize based on JVM Bitness. Individual OS implementations will test
        // if 32-bit JVM running on 64-bit OS
        int jvmBitness = System.getProperty("os.arch").indexOf("64") != -1 ? 64 : 32;
        return queryBitness(jvmBitness);
    }

    /**
     * Backup OS-specific query to determine bitness if previous checks fail
     *
     * @param jvmBitness
     *            The bitness of the JVM
     * @return The operating system bitness
     */
    protected abstract int queryBitness(int jvmBitness);

    @Override
    public List<OSProcess> getProcesses(Predicate<OSProcess> filter, Comparator<OSProcess> sort, int limit) {
        return queryAllProcesses().stream().filter(filter == null ? ALL_PROCESSES : filter)
                .sorted(sort == null ? NO_SORTING : sort).limit(limit > 0 ? limit : Long.MAX_VALUE)
                .collect(Collectors.toList());
    }

    protected abstract List<OSProcess> queryAllProcesses();

    @Override
    public List<OSProcess> getChildProcesses(int parentPid, Predicate<OSProcess> filter, Comparator<OSProcess> sort,
            int limit) {
        return queryChildProcesses(parentPid).stream().filter(filter == null ? ALL_PROCESSES : filter)
                .sorted(sort == null ? NO_SORTING : sort).limit(limit > 0 ? limit : Long.MAX_VALUE)
                .collect(Collectors.toList());
    }

    protected abstract List<OSProcess> queryChildProcesses(int parentPid);

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getManufacturer()).append(' ').append(getFamily()).append(' ').append(getVersionInfo());
        return sb.toString();
    }
}
