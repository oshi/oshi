/*
 * Copyright 2016-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.mac;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.mac.MacSystemHeaders.INT_SIZE;
import static oshi.ffm.mac.MacSystemHeaders.PROC_ALL_PIDS;
import static oshi.ffm.mac.MacSystemHeaders.PROC_PIDTASKINFO;
import static oshi.ffm.mac.MacSystemImpl.getpid;
import static oshi.ffm.mac.MacSystemImpl.proc_listpids;
import static oshi.ffm.mac.MacSystemImpl.proc_pidinfo;
import static oshi.ffm.mac.MacSystemStructs.PROC_TASK_INFO;
import static oshi.ffm.mac.MacSystemStructs.PTI_THREADNUM;
import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;
import static oshi.util.Memoizer.installedAppsExpiration;

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.mac.Who;
import oshi.driver.mac.WindowInfo;
import oshi.jna.Struct.CloseableTimeval;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.ApplicationInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSDesktopWindow;
import oshi.software.os.OSProcess;
import oshi.software.os.OSProcess.State;
import oshi.software.os.OSService;
import oshi.software.os.OSSession;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.platform.mac.SysctlUtil;
import oshi.util.tuples.Pair;

/**
 * macOS, previously Mac OS X and later OS X) is a series of proprietary graphical operating systems developed and
 * marketed by Apple Inc. since 2001. It is the primary operating system for Apple's Mac computers.
 */
@ThreadSafe
public class MacOperatingSystemFFM extends AbstractOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(MacOperatingSystem.class);

    public static final String MACOS_VERSIONS_PROPERTIES = "oshi.macos.versions.properties";

    private static final String SYSTEM_LIBRARY_LAUNCH_AGENTS = "/System/Library/LaunchAgents";
    private static final String SYSTEM_LIBRARY_LAUNCH_DAEMONS = "/System/Library/LaunchDaemons";

    private final String osXVersion;
    private final int major;
    private final int minor;
    private final Supplier<List<ApplicationInfo>> installedAppsSupplier = Memoizer
            .memoize(MacInstalledApps::queryInstalledApps, installedAppsExpiration());

    private static final long BOOTTIME;
    static {
        try (CloseableTimeval tv = new CloseableTimeval()) {
            if (!SysctlUtil.sysctl("kern.boottime", tv) || tv.tv_sec.longValue() == 0L) {
                // Usually this works. If it doesn't, fall back to text parsing.
                // Boot time will be the first consecutive string of digits.
                BOOTTIME = ParseUtil.parseLongOrDefault(
                        ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""),
                        System.currentTimeMillis() / 1000);
            } else {
                // tv now points to a 64-bit timeval structure for boot time.
                // First 4 bytes are seconds, second 4 bytes are microseconds
                // (we ignore)
                BOOTTIME = tv.tv_sec.longValue();
            }
        }
    }

    public MacOperatingSystemFFM() {
        String version = System.getProperty("os.version");
        int verMajor = ParseUtil.getFirstIntValue(version);
        int verMinor = ParseUtil.getNthIntValue(version, 2);
        // Big Sur (11.x) may return 10.16
        if (verMajor == 10 && verMinor > 15) {
            String swVers = ExecutingCommand.getFirstAnswer("sw_vers -productVersion");
            if (!swVers.isEmpty()) {
                version = swVers;
            }
            verMajor = ParseUtil.getFirstIntValue(version);
            verMinor = ParseUtil.getNthIntValue(version, 2);
        }
        this.osXVersion = version;
        this.major = verMajor;
        this.minor = verMinor;
    }

    @Override
    public String queryManufacturer() {
        return "Apple";
    }

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        String family = this.major > 10 || (this.major == 10 && this.minor >= 12) ? "macOS"
                : System.getProperty("os.name");
        String codeName = parseCodeName();
        String buildNumber = SysctlUtil.sysctl("kern.osversion", "");
        return new Pair<>(family, new OSVersionInfo(this.osXVersion, codeName, buildNumber));
    }

    private String parseCodeName() {
        Properties verProps = FileUtil.readPropertiesFromFilename(MACOS_VERSIONS_PROPERTIES);
        String codeName = null;
        if (this.major > 10) {
            codeName = verProps.getProperty(Integer.toString(this.major));
        } else if (this.major == 10) {
            codeName = verProps.getProperty(this.major + "." + this.minor);
        }
        if (Util.isBlank(codeName)) {
            LOG.warn("Unable to parse version {}.{} to a codename.", this.major, this.minor);
        }
        return codeName;
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness == 64 || (this.major == 10 && this.minor > 6)) {
            return 64;
        }
        return ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("getconf LONG_BIT"), 32);
    }

    @Override
    public FileSystem getFileSystem() {
        return new MacFileSystem();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new MacInternetProtocolStats(isElevated());
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
    public int getThreadId() {
        OSThread thread = getCurrentThread();
        if (thread == null) {
            return 0;
        }
        return thread.getThreadId();
    }

    @Override
    public OSThread getCurrentThread() {
        // Get oldest thread
        return getCurrentProcess().getThreadDetails().stream().sorted(Comparator.comparingLong(OSThread::getStartTime))
                .findFirst().orElse(new MacOSThread(getProcessId()));
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

    @Override
    public NetworkParams getNetworkParams() {
        return new MacNetworkParams();
    }

    @Override
    public List<OSService> getServices() {
        // Get running services
        List<OSService> services = new ArrayList<>();
        Set<String> running = new HashSet<>();
        for (OSProcess p : getChildProcesses(1, ProcessFiltering.ALL_PROCESSES, ProcessSorting.PID_ASC, 0)) {
            OSService s = new OSService(p.getName(), p.getProcessID(), RUNNING);
            services.add(s);
            running.add(p.getName());
        }
        // Get Directories for stopped services
        ArrayList<File> files = new ArrayList<>();
        File dir = new File(SYSTEM_LIBRARY_LAUNCH_AGENTS);
        if (dir.exists() && dir.isDirectory()) {
            files.addAll(Arrays.asList(dir.listFiles((f, name) -> name.toLowerCase(Locale.ROOT).endsWith(".plist"))));
        } else {
            LOG.error("Directory: /System/Library/LaunchAgents does not exist");
        }
        dir = new File(SYSTEM_LIBRARY_LAUNCH_DAEMONS);
        if (dir.exists() && dir.isDirectory()) {
            files.addAll(Arrays.asList(dir.listFiles((f, name) -> name.toLowerCase(Locale.ROOT).endsWith(".plist"))));
        } else {
            LOG.error("Directory: /System/Library/LaunchDaemons does not exist");
        }
        for (File f : files) {
            // remove .plist extension
            String name = f.getName().substring(0, f.getName().length() - 6);
            int index = name.lastIndexOf('.');
            String shortName = (index < 0 || index > name.length() - 2) ? name : name.substring(index + 1);
            if (!running.contains(name) && !running.contains(shortName)) {
                OSService s = new OSService(name, 0, STOPPED);
                services.add(s);
            }
        }
        return services;
    }

    @Override
    public List<OSDesktopWindow> getDesktopWindows(boolean visibleOnly) {
        return WindowInfo.queryDesktopWindows(visibleOnly);
    }

    @Override
    public List<ApplicationInfo> getInstalledApplications() {
        return installedAppsSupplier.get();
    }
}
