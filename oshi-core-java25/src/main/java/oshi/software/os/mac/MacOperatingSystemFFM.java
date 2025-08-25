/*
 * Copyright 2016-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.mac.MacSystemFunctions.getpid;
import static oshi.ffm.mac.MacSystemFunctions.proc_listpids;
import static oshi.ffm.mac.MacSystemFunctions.proc_pidinfo;
import static oshi.ffm.mac.MacSystemHeaders.INT_SIZE;
import static oshi.ffm.mac.MacSystemHeaders.PROC_ALL_PIDS;
import static oshi.ffm.mac.MacSystemHeaders.PROC_PIDTASKINFO;
import static oshi.ffm.mac.MacSystemStructs.PROC_TASK_INFO;
import static oshi.ffm.mac.MacSystemStructs.PTI_THREADNUM;
import static oshi.ffm.mac.MacSystemStructs.TIMEVAL;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.mac.Who;
import oshi.software.os.OSProcess;
import oshi.software.os.OSProcess.State;
import oshi.software.os.OSSession;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.mac.SysctlUtil;
import oshi.util.platform.mac.SysctlUtilFFM;
import oshi.util.tuples.Pair;

/**
 * macOS, previously Mac OS X and later OS X) is a series of proprietary graphical operating systems developed and
 * marketed by Apple Inc. since 2001. It is the primary operating system for Apple's Mac computers.
 */
@ThreadSafe
public class MacOperatingSystemFFM extends MacOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(MacOperatingSystemFFM.class);

    private static final long BOOTTIME;
    static {
        long bootTime = 0L;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment timeval = arena.allocate(TIMEVAL);
            if (SysctlUtilFFM.sysctl("kern.boottime", timeval)) {
                bootTime = timeval.get(JAVA_LONG, 0L);
            }
        } catch (Throwable e) {
            // do nothing, the bootTime == 0 conditional will handle
        }
        // Usually this works. If it doesn't, fall back to text parsing.
        // Boot time will be the first consecutive string of digits.
        if (bootTime == 0) {
            bootTime = ParseUtil.parseLongOrDefault(
                    ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""),
                    System.currentTimeMillis() / 1000);
        }
        BOOTTIME = bootTime;
    }

    public MacOperatingSystemFFM() {
        super();
    }

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        String family = this.major > 10 || (this.major == 10 && this.minor >= 12) ? "macOS"
                : System.getProperty("os.name");
        String codeName = parseCodeName();
        String buildNumber = SysctlUtil.sysctl("kern.osversion", "");
        return new Pair<>(family, new OSVersionInfo(this.osXVersion, codeName, buildNumber));
    }

    @Override
    public List<OSSession> getSessions() {
        return USE_WHO_COMMAND ? super.getSessions() : Who.queryUtxent();
    }

    @Override
    public List<OSProcess> queryAllProcesses() {
        try (Arena arena = Arena.ofConfined()) {
            // Calculate size needed, add a small buffer
            int numberOfProcesses = proc_listpids(PROC_ALL_PIDS, 0, MemorySegment.NULL, 0) / INT_SIZE;
            MemorySegment pidSegment = arena.allocate(JAVA_INT, numberOfProcesses + 10);
            numberOfProcesses = proc_listpids(PROC_ALL_PIDS, 0, pidSegment, numberOfProcesses * INT_SIZE) / INT_SIZE;
            // Use only the segment containing pids
            return Arrays.stream(pidSegment.asSlice(0, numberOfProcesses * INT_SIZE).toArray(ValueLayout.JAVA_INT))
                    .distinct().parallel().mapToObj(this::getProcess).filter(Objects::nonNull)
                    .filter(ProcessFiltering.VALID_PROCESS).toList();
        } catch (Throwable e) {
            LOG.warn("Failed to query processes", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public OSProcess getProcess(int pid) {
        OSProcess proc = new MacOSProcessFFM(pid, this.major, this.minor, this);
        return proc.getState().equals(State.INVALID) ? null : proc;
    }

    @Override
    public List<OSProcess> queryChildProcesses(int parentPid) {
        List<OSProcess> allProcs = queryAllProcesses();
        Set<Integer> descendantPids = getChildrenOrDescendants(allProcs, parentPid, false);
        return allProcs.stream().filter(p -> descendantPids.contains(p.getProcessID())).collect(Collectors.toList());
    }

    @Override
    public List<OSProcess> queryDescendantProcesses(int parentPid) {
        List<OSProcess> allProcs = queryAllProcesses();
        Set<Integer> descendantPids = getChildrenOrDescendants(allProcs, parentPid, true);
        return allProcs.stream().filter(p -> descendantPids.contains(p.getProcessID())).collect(Collectors.toList());
    }

    @Override
    public int getProcessId() {
        try {
            return getpid();
        } catch (Throwable e) {
            LOG.warn("Failed to get current pid", e.getMessage());
            return 0;
        }
    }

    @Override
    public int getProcessCount() {
        try {
            return proc_listpids(PROC_ALL_PIDS, 0, MemorySegment.NULL, 0) / INT_SIZE;
        } catch (Throwable e) {
            LOG.warn("Failed to query processes", e.getMessage());
            return 0;
        }
    }

    @Override
    public int getThreadCount() {
        try (Arena arena = Arena.ofConfined()) {
            // Calculate size needed, add a small buffer
            int numberOfProcesses = proc_listpids(PROC_ALL_PIDS, 0, MemorySegment.NULL, 0) / INT_SIZE;
            MemorySegment pidSegment = arena.allocate(JAVA_INT, numberOfProcesses + 10);
            numberOfProcesses = proc_listpids(PROC_ALL_PIDS, 0, pidSegment, numberOfProcesses * INT_SIZE) / INT_SIZE;
            // Use only the segment containing pids
            return Arrays.stream(pidSegment.asSlice(0, numberOfProcesses * INT_SIZE).toArray(ValueLayout.JAVA_INT))
                    .distinct().parallel().map(this::threadsPerProc).sum();
        } catch (Throwable e) {
            LOG.warn("Failed to query processes", e.getMessage());
            return 0;
        }
    }

    private int threadsPerProc(int pid) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buffer = arena.allocate(PROC_TASK_INFO);
            int result = proc_pidinfo(pid, PROC_PIDTASKINFO, 0L, buffer, (int) PROC_TASK_INFO.byteSize());
            if (result > 0) {
                return buffer.get(JAVA_INT, PROC_TASK_INFO.byteOffset(PTI_THREADNUM));
            }
            return 0;
        } catch (Throwable e) {
            // if this is a common warning for short-lived processes may need a lower log level
            LOG.warn("Failed to get threads for process {}:", pid, e.getMessage());
            return 0;
        }
    }

    @Override
    public long getSystemUptime() {
        return System.currentTimeMillis() / 1000 - BOOTTIME;
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }
}
