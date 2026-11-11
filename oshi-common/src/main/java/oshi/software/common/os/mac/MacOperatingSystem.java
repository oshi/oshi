/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.mac;

import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;
import static oshi.util.Memoizer.installedAppsExpiration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.ApplicationInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OSSession;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.tuples.Pair;

/**
 * macOS, previously Mac OS X and later OS X) is a series of proprietary graphical operating systems developed and
 * marketed by Apple Inc. since 2001. It is the primary operating system for Apple's Mac computers.
 */
@ThreadSafe
public abstract class MacOperatingSystem extends AbstractOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(MacOperatingSystem.class);

    /** Configuration key for macOS version properties file. */
    public static final String MACOS_VERSIONS_PROPERTIES = "oshi.macos.versions.properties";

    private static final String SYSTEM_LIBRARY_LAUNCH_AGENTS = "/System/Library/LaunchAgents";
    private static final String SYSTEM_LIBRARY_LAUNCH_DAEMONS = "/System/Library/LaunchDaemons";
    private static final String LIBRARY_LAUNCH_AGENTS = "/Library/LaunchAgents";
    private static final String LIBRARY_LAUNCH_DAEMONS = "/Library/LaunchDaemons";

    // CHECKSTYLE:OFF VisibilityModifier - protected final, set by the constructor and read by both backends
    /** Maximum number of processes. */
    protected final int maxProc;

    /** macOS version string. */
    protected final String osXVersion;
    /** Major version number. */
    protected final int major;
    /** Minor version number. */
    protected final int minor;
    // CHECKSTYLE:ON VisibilityModifier

    private final Supplier<List<ApplicationInfo>> installedAppsSupplier = Memoizer
            .memoize(MacInstalledApps::queryInstalledApps, installedAppsExpiration());

    /**
     * Creates a MacOperatingSystem.
     *
     * @param maxProc the maximum number of processes
     */
    protected MacOperatingSystem(int maxProc) {
        String version = System.getProperty("os.version");
        int verMajor = ParseUtil.getFirstIntValue(version);
        int verMinor = ParseUtil.getNthIntValue(version, 2);
        // Big Sur (11.x) may return 10.16
        if (verMajor == 10 && verMinor > 15) {
            String swVers = ExecutingCommand.getFirstAnswer("sw_vers -productVersion");
            version = resolveVersion(version, swVers);
            verMajor = ParseUtil.getFirstIntValue(version);
            verMinor = ParseUtil.getNthIntValue(version, 2);
        }
        this.osXVersion = version;
        this.major = verMajor;
        this.minor = verMinor;
        this.maxProc = maxProc;
    }

    static String resolveVersion(String osVersion, String swVersOutput) {
        if (!swVersOutput.isEmpty()) {
            return swVersOutput;
        }
        return osVersion;
    }

    @Override
    public String queryManufacturer() {
        return "Apple";
    }

    /**
     * Parses the macOS code name from the version.
     *
     * @return the code name
     */
    protected @Nullable String parseCodeName() {
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
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        String family = this.major > 10 || (this.major == 10 && this.minor >= 12) ? "macOS"
                : System.getProperty("os.name");
        String codeName = parseCodeName();
        String buildNumber = querySysctl("kern.osversion", "");
        return new Pair<>(family, new OSVersionInfo(this.osXVersion, codeName, buildNumber));
    }

    @Override
    public List<OSSession> getSessions() {
        return USE_WHO_COMMAND ? super.getSessions() : queryWhoSessions();
    }

    /**
     * Reads a string-valued sysctl by name.
     *
     * @param name the sysctl name (e.g. {@code "kern.osversion"})
     * @param def  the default value if the sysctl can't be read
     * @return the sysctl value, or {@code def}
     */
    protected abstract String querySysctl(String name, String def);

    /**
     * Reads the login sessions natively via {@code getutxent}.
     *
     * @return the sessions
     */
    protected abstract List<OSSession> queryWhoSessions();

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness == 64 || (this.major == 10 && this.minor > 6)) {
            return 64;
        }
        return ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("getconf LONG_BIT"), 32);
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
    public long getSystemUptime() {
        return System.currentTimeMillis() / 1000 - getSystemBootTime();
    }

    @Override
    public List<OSService> getServices() {
        // Jobs launchd has loaded, with a pid when running: the system domain, then the current user's
        Map<String, OSService> services = new LinkedHashMap<>();
        for (OSService s : parseLaunchctlPrint(ExecutingCommand.runNative("launchctl print system"))) {
            services.putIfAbsent(s.getName(), s);
        }
        for (OSService s : parseLaunchctlList(ExecutingCommand.runNative("launchctl list"))) {
            services.putIfAbsent(s.getName(), s);
        }
        if (services.isEmpty()) {
            // launchctl unavailable: fall back to launchd's children, named by process
            for (OSProcess p : getChildProcesses(1, ProcessFiltering.ALL_PROCESSES, ProcessSorting.PID_ASC, 0)) {
                services.putIfAbsent(p.getName(), new OSService(p.getName(), p.getProcessID(), RUNNING));
            }
        }
        // Installed jobs launchd has not loaded, named by their plist
        String home = System.getProperty("user.home", "");
        for (String dir : Arrays.asList(SYSTEM_LIBRARY_LAUNCH_AGENTS, SYSTEM_LIBRARY_LAUNCH_DAEMONS,
                LIBRARY_LAUNCH_AGENTS, LIBRARY_LAUNCH_DAEMONS, home + LIBRARY_LAUNCH_AGENTS)) {
            File[] plists = new File(dir).listFiles((f, name) -> name.toLowerCase(Locale.ROOT).endsWith(".plist"));
            if (plists != null) {
                for (File f : plists) {
                    String label = f.getName().substring(0, f.getName().length() - 6);
                    services.putIfAbsent(label, new OSService(label, 0, STOPPED));
                }
            }
        }
        return new ArrayList<>(services.values());
    }

    /**
     * Parses the services block of {@code launchctl print <domain>}, whose rows are a pid (0 when not running), the
     * last exit status, and the job label.
     *
     * @param lines the command output
     * @return the loaded jobs
     */
    static List<OSService> parseLaunchctlPrint(List<String> lines) {
        List<OSService> services = new ArrayList<>();
        boolean inServices = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!inServices) {
                inServices = "services = {".equals(trimmed);
            } else if ("}".equals(trimmed)) {
                break;
            } else {
                addLaunchdJob(services, ParseUtil.whitespaces.split(trimmed));
            }
        }
        return services;
    }

    /**
     * Parses {@code launchctl list}, whose rows are a pid ({@code -} when not running), the last exit status, and the
     * job label, after a header row.
     *
     * @param lines the command output
     * @return the loaded jobs
     */
    static List<OSService> parseLaunchctlList(List<String> lines) {
        List<OSService> services = new ArrayList<>();
        for (String line : lines) {
            addLaunchdJob(services, ParseUtil.whitespaces.split(line.trim()));
        }
        return services;
    }

    private static void addLaunchdJob(List<OSService> services, String[] row) {
        // Skip the header and the per-instance jobs launchd creates for each GUI application
        if (row.length != 3 || "Label".equals(row[2]) || row[2].startsWith("application.")) {
            return;
        }
        int pid = ParseUtil.parseIntOrDefault(row[0], 0);
        services.add(new OSService(row[2], Math.max(pid, 0), pid > 0 ? RUNNING : STOPPED));
    }

    @Override
    public List<ApplicationInfo> getInstalledApplications() {
        return installedAppsSupplier.get();
    }
}
