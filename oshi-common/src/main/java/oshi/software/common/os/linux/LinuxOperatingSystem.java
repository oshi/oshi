/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;
import static oshi.util.Memoizer.installedAppsExpiration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.ApplicationInfo;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.OSProcess;
import oshi.software.os.OSProcess.State;
import oshi.software.os.OSService;
import oshi.software.os.OSThread;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.driver.linux.proc.CpuStat;
import oshi.util.driver.linux.proc.ProcessStat;
import oshi.util.driver.linux.proc.UpTime;
import oshi.util.linux.ProcPath;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Linux is a family of open source Unix-like operating systems based on the Linux kernel, an operating system kernel
 * first released on September 17, 1991, by Linus Torvalds. Linux is typically packaged in a Linux distribution.
 */
@ThreadSafe
public abstract class LinuxOperatingSystem extends AbstractOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxOperatingSystem.class);

    private static final String OS_RELEASE_LOG = "os-release: {}";
    private static final String LSB_RELEASE_A_LOG = "lsb_release -a: {}";
    private static final String LSB_RELEASE_LOG = "lsb-release: {}";
    private static final String RELEASE_DELIM = " release ";
    private static final String DOUBLE_QUOTES = "(?:(?:^\")|(?:\"$))";
    private static final String FILENAME_PROPERTIES = "oshi.linux.filename.properties";

    private final Supplier<List<ApplicationInfo>> installedAppsSupplier = Memoizer
            .memoize(LinuxInstalledApps::queryInstalledApps, installedAppsExpiration());

    /**
     * OS Name for manufacturer
     */
    private static final String OS_NAME = ExecutingCommand.getFirstAnswer("uname -o");

    private static final long BOOTTIME;
    static {
        long tempBT = CpuStat.getBootTime();
        // If above fails, current time minus uptime.
        if (tempBT == 0) {
            tempBT = System.currentTimeMillis() / 1000L - (long) UpTime.getSystemUptimeSeconds();
        }
        BOOTTIME = tempBT;
    }

    // PPID is 4th numeric value in proc pid stat; subtract 1 for 0-index
    private static final int[] PPID_INDEX = { 3 };

    /**
     * <p>
     * Constructor for LinuxOperatingSystem.
     * </p>
     */
    protected LinuxOperatingSystem() {
        super.getVersionInfo();
    }

    @Override
    public String queryManufacturer() {
        return OS_NAME;
    }

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        Triplet<String, String, String> familyVersionCodename = queryFamilyVersionCodenameFromReleaseFiles();
        String buildNumber = null;
        List<String> procVersion = FileUtil.readFile(ProcPath.VERSION);
        if (!procVersion.isEmpty()) {
            String[] split = ParseUtil.whitespaces.split(procVersion.get(0));
            for (String s : split) {
                if (!"Linux".equals(s) && !"version".equals(s)) {
                    buildNumber = s;
                    break;
                }
            }
        }
        OSVersionInfo versionInfo = new OSVersionInfo(familyVersionCodename.getB(), familyVersionCodename.getC(),
                buildNumber);
        return new Pair<>(familyVersionCodename.getA(), versionInfo);
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness < 64 && !ExecutingCommand.getFirstAnswer("uname -m").contains("64")) {
            return jvmBitness;
        }
        return 64;
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new LinuxInternetProtocolStats();
    }

    @Override
    public List<OSProcess> queryAllProcesses() {
        return queryChildProcesses(-1);
    }

    @Override
    public List<OSProcess> queryChildProcesses(int parentPid) {
        File[] pidFiles = ProcessStat.getPidFiles();
        if (parentPid >= 0) {
            return queryProcessList(getChildrenOrDescendants(getParentPidsFromProcFiles(pidFiles), parentPid, false));
        }
        Set<Integer> descendantPids = new HashSet<>();
        for (File procFile : pidFiles) {
            int pid = ParseUtil.parseIntOrDefault(procFile.getName(), -2);
            if (pid != -2) {
                descendantPids.add(pid);
            }
        }
        return queryProcessList(descendantPids);
    }

    @Override
    public List<OSProcess> queryDescendantProcesses(int parentPid) {
        File[] pidFiles = ProcessStat.getPidFiles();
        return queryProcessList(getChildrenOrDescendants(getParentPidsFromProcFiles(pidFiles), parentPid, true));
    }

    private List<OSProcess> queryProcessList(Set<Integer> descendantPids) {
        List<OSProcess> procs = new ArrayList<>();
        for (int pid : descendantPids) {
            OSProcess proc = createOSProcess(pid);
            if (!proc.getState().equals(State.INVALID)) {
                procs.add(proc);
            }
        }
        return procs;
    }

    /**
     * Creates a new {@link OSProcess} instance for the given PID using the appropriate native implementation.
     *
     * @param pid the process ID
     * @return a new OS process instance
     */
    protected abstract OSProcess createOSProcess(int pid);

    protected static Map<Integer, Integer> getParentPidsFromProcFiles(File[] pidFiles) {
        Map<Integer, Integer> parentPidMap = new HashMap<>();
        for (File procFile : pidFiles) {
            int pid = ParseUtil.parseIntOrDefault(procFile.getName(), 0);
            parentPidMap.put(pid, getParentPidFromProcFile(pid));
        }
        return parentPidMap;
    }

    private static int getParentPidFromProcFile(int pid) {
        String stat = FileUtil.getStringFromFile(String.format(Locale.ROOT, "/proc/%d/stat", pid));
        // A race condition may leave us with an empty string
        if (stat.isEmpty()) {
            return 0;
        }
        // Grab PPID
        long[] statArray = ParseUtil.parseStringToLongArray(stat, PPID_INDEX, ProcessStat.PROC_PID_STAT_LENGTH, ' ');
        return (int) statArray[0];
    }

    @Override
    public int getProcessCount() {
        return ProcessStat.getPidFiles().length;
    }

    @Override
    public OSThread getCurrentThread() {
        return new LinuxOSThread(getProcessId(), getThreadId(), this);
    }

    @Override
    public long getSystemUptime() {
        return (long) UpTime.getSystemUptimeSeconds();
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    @Override
    public List<ApplicationInfo> getInstalledApplications() {
        return installedAppsSupplier.get();
    }

    private static Triplet<String, String, String> queryFamilyVersionCodenameFromReleaseFiles() {
        Triplet<String, String, String> familyVersionCodename;
        // There are two competing options for family/version information.
        // Newer systems are adopting a standard /etc/os-release file:
        // https://www.freedesktop.org/software/systemd/man/os-release.html
        //
        // Some systems are still using the lsb standard which parses a
        // variety of /etc/*-release files and is most easily accessed via
        // the commandline lsb_release -a, see here:
        // https://linux.die.net/man/1/lsb_release
        // In this case, the /etc/lsb-release file (if it exists) has
        // optional overrides to the information in the /etc/distrib-release
        // files, which show: "Distributor release x.x (Codename)"

        // Attempt to read /etc/system-release which has more details than
        // os-release on (CentOS and Fedora)
        if ((familyVersionCodename = readDistribRelease("/etc/system-release")) != null) {
            return familyVersionCodename;
        }

        // Attempt to read /etc/os-release file.
        if ((familyVersionCodename = readOsRelease()) != null) {
            return familyVersionCodename;
        }

        // Attempt to execute the `lsb_release` command
        if ((familyVersionCodename = execLsbRelease()) != null) {
            return familyVersionCodename;
        }

        // Attempt to read /etc/lsb-release file
        if ((familyVersionCodename = readLsbRelease()) != null) {
            return familyVersionCodename;
        }

        // Search for any /etc/*-release (or similar) filename
        String etcDistribRelease = getReleaseFilename();
        if ((familyVersionCodename = readDistribRelease(etcDistribRelease)) != null) {
            return familyVersionCodename;
        }
        String family = filenameToFamily(etcDistribRelease.replace("/etc/", "").replace("release", "")
                .replace("version", "").replace("-", "").replace("_", ""));
        return new Triplet<>(family, Constants.UNKNOWN, Constants.UNKNOWN);
    }

    /**
     * Attempts to read /etc/os-release
     *
     * @return a triplet with the parsed family, versionID and codeName if file successfully read and NAME= found, null
     *         otherwise
     */
    private static Triplet<String, String, String> readOsRelease() {
        return readOsRelease(FileUtil.readFile("/etc/os-release"));
    }

    /**
     * Parse /etc/os-release content.
     *
     * @param osRelease lines from /etc/os-release
     * @return a triplet with the parsed family, versionID and codeName if NAME= found, null otherwise
     */
    static Triplet<String, String, String> readOsRelease(List<String> osRelease) {
        String family = null;
        String versionId = Constants.UNKNOWN;
        String codeName = Constants.UNKNOWN;
        // Search for NAME=
        for (String line : osRelease) {
            if (line.startsWith("VERSION=")) {
                LOG.debug(OS_RELEASE_LOG, line);
                line = line.replace("VERSION=", "").replaceAll(DOUBLE_QUOTES, "").trim();
                String[] split = line.split("[()]");
                if (split.length <= 1) {
                    split = line.split(", ");
                }
                if (split.length > 0) {
                    versionId = split[0].trim();
                }
                if (split.length > 1) {
                    codeName = split[1].trim();
                }
            } else if (line.startsWith("NAME=") && family == null) {
                LOG.debug(OS_RELEASE_LOG, line);
                family = line.replace("NAME=", "").replaceAll(DOUBLE_QUOTES, "").trim();
            } else if (line.startsWith("VERSION_ID=") && versionId.equals(Constants.UNKNOWN)) {
                LOG.debug(OS_RELEASE_LOG, line);
                versionId = line.replace("VERSION_ID=", "").replaceAll(DOUBLE_QUOTES, "").trim();
            }
        }
        return family == null ? null : new Triplet<>(family, versionId, codeName);
    }

    /**
     * Attempts to execute `lsb_release -a`
     *
     * @return a triplet with the parsed family, versionID and codeName if the command successfully executed and
     *         Distributor ID: or Description: found, null otherwise
     */
    private static Triplet<String, String, String> execLsbRelease() {
        return execLsbRelease(ExecutingCommand.runNative("lsb_release -a"));
    }

    /**
     * Parse lsb_release -a output.
     *
     * @param lines output of {@code lsb_release -a}
     * @return a triplet with the parsed family, versionID and codeName if Distributor ID: or Description: found, null
     *         otherwise
     */
    static Triplet<String, String, String> execLsbRelease(List<String> lines) {
        String family = null;
        String versionId = Constants.UNKNOWN;
        String codeName = Constants.UNKNOWN;
        for (String line : lines) {
            if (line.startsWith("Description:")) {
                LOG.debug(LSB_RELEASE_A_LOG, line);
                line = line.replace("Description:", "").trim();
                if (line.contains(RELEASE_DELIM)) {
                    Triplet<String, String, String> triplet = parseRelease(line, RELEASE_DELIM);
                    family = triplet.getA();
                    if (versionId.equals(Constants.UNKNOWN)) {
                        versionId = triplet.getB();
                    }
                    if (codeName.equals(Constants.UNKNOWN)) {
                        codeName = triplet.getC();
                    }
                }
            } else if (line.startsWith("Distributor ID:") && family == null) {
                LOG.debug(LSB_RELEASE_A_LOG, line);
                family = line.replace("Distributor ID:", "").trim();
            } else if (line.startsWith("Release:") && versionId.equals(Constants.UNKNOWN)) {
                LOG.debug(LSB_RELEASE_A_LOG, line);
                versionId = line.replace("Release:", "").trim();
            } else if (line.startsWith("Codename:") && codeName.equals(Constants.UNKNOWN)) {
                LOG.debug(LSB_RELEASE_A_LOG, line);
                codeName = line.replace("Codename:", "").trim();
            }
        }
        return family == null ? null : new Triplet<>(family, versionId, codeName);
    }

    /**
     * Attempts to read /etc/lsb-release
     *
     * @return a triplet with the parsed family, versionID and codeName if file successfully read and and DISTRIB_ID or
     *         DISTRIB_DESCRIPTION, null otherwise
     */
    private static Triplet<String, String, String> readLsbRelease() {
        String family = null;
        String versionId = Constants.UNKNOWN;
        String codeName = Constants.UNKNOWN;
        List<String> osRelease = FileUtil.readFile("/etc/lsb-release");
        for (String line : osRelease) {
            if (line.startsWith("DISTRIB_DESCRIPTION=")) {
                LOG.debug(LSB_RELEASE_LOG, line);
                line = line.replace("DISTRIB_DESCRIPTION=", "").replaceAll(DOUBLE_QUOTES, "").trim();
                if (line.contains(RELEASE_DELIM)) {
                    Triplet<String, String, String> triplet = parseRelease(line, RELEASE_DELIM);
                    family = triplet.getA();
                    if (versionId.equals(Constants.UNKNOWN)) {
                        versionId = triplet.getB();
                    }
                    if (codeName.equals(Constants.UNKNOWN)) {
                        codeName = triplet.getC();
                    }
                }
            } else if (line.startsWith("DISTRIB_ID=") && family == null) {
                LOG.debug(LSB_RELEASE_LOG, line);
                family = line.replace("DISTRIB_ID=", "").replaceAll(DOUBLE_QUOTES, "").trim();
            } else if (line.startsWith("DISTRIB_RELEASE=") && versionId.equals(Constants.UNKNOWN)) {
                LOG.debug(LSB_RELEASE_LOG, line);
                versionId = line.replace("DISTRIB_RELEASE=", "").replaceAll(DOUBLE_QUOTES, "").trim();
            } else if (line.startsWith("DISTRIB_CODENAME=") && codeName.equals(Constants.UNKNOWN)) {
                LOG.debug(LSB_RELEASE_LOG, line);
                codeName = line.replace("DISTRIB_CODENAME=", "").replaceAll(DOUBLE_QUOTES, "").trim();
            }
        }
        return family == null ? null : new Triplet<>(family, versionId, codeName);
    }

    /**
     * Attempts to read /etc/distrib-release (for some value of distrib)
     *
     * @param filename The /etc/distrib-release file
     * @return a triplet with the parsed family, versionID and codeName if file successfully read and " release " or "
     *         VERSION " found, null otherwise
     */
    private static Triplet<String, String, String> readDistribRelease(String filename) {
        if (new File(filename).exists()) {
            List<String> osRelease = FileUtil.readFile(filename);
            for (String line : osRelease) {
                LOG.debug("{}: {}", filename, line);
                if (line.contains(RELEASE_DELIM)) {
                    return parseRelease(line, RELEASE_DELIM);
                } else if (line.contains(" VERSION ")) {
                    return parseRelease(line, " VERSION ");
                }
            }
        }
        return null;
    }

    /**
     * Helper method to parse version description line style
     *
     * @param line      a String of the form "Distributor release x.x (Codename)"
     * @param splitLine A regex to split on, e.g. " release "
     * @return a triplet with the parsed family, versionID and codeName
     */
    private static Triplet<String, String, String> parseRelease(String line, String splitLine) {
        String[] split = line.split(splitLine);
        String family = split[0].trim();
        String versionId = Constants.UNKNOWN;
        String codeName = Constants.UNKNOWN;
        if (split.length > 1) {
            split = split[1].split("[()]");
            if (split.length > 0) {
                versionId = split[0].trim();
            }
            if (split.length > 1) {
                codeName = split[1].trim();
            }
        }
        return new Triplet<>(family, versionId, codeName);
    }

    /**
     * Looks for a collection of possible distrib-release filenames
     *
     * @return The first valid matching filename
     */
    protected static String getReleaseFilename() {
        // Look for any /etc/*-release, *-version, and variants
        File etc = new File("/etc");
        File[] matchingFiles = etc.listFiles(//
                f -> (f.getName().endsWith("-release") || //
                        f.getName().endsWith("-version") || //
                        f.getName().endsWith("_release") || //
                        f.getName().endsWith("_version")) //
                        && !(f.getName().endsWith("os-release") || //
                                f.getName().endsWith("lsb-release") || //
                                f.getName().endsWith("system-release")));
        if (matchingFiles != null && matchingFiles.length > 0) {
            return matchingFiles[0].getPath();
        }
        if (new File("/etc/release").exists()) {
            return "/etc/release";
        }
        return "/etc/issue";
    }

    /**
     * Converts a portion of a filename (e.g. the 'redhat' in /etc/redhat-release) to a mixed case string representing
     * the family (e.g., Red Hat)
     *
     * @param name Stripped version of filename after removing /etc and -release
     * @return Mixed case family
     */
    private static String filenameToFamily(String name) {
        if (name.isEmpty()) {
            return "Solaris";
        } else if ("issue".equalsIgnoreCase(name)) {
            return "Unknown";
        } else {
            Properties filenameProps = FileUtil.readPropertiesFromFilename(FILENAME_PROPERTIES);
            String family = filenameProps.getProperty(name.toLowerCase(Locale.ROOT));
            return family != null ? family : name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
        }
    }

    @Override
    public List<OSService> getServices() {
        List<OSService> services = new ArrayList<>();
        Set<String> running = new HashSet<>();
        for (OSProcess p : getChildProcesses(1, ProcessFiltering.ALL_PROCESSES, ProcessSorting.PID_ASC, 0)) {
            OSService s = new OSService(p.getName(), p.getProcessID(), RUNNING);
            services.add(s);
            running.add(p.getName());
        }
        boolean systemctlFound = false;
        List<String> systemctl = ExecutingCommand.runNative("systemctl list-unit-files");
        for (String str : systemctl) {
            String[] split = ParseUtil.whitespaces.split(str);
            if (split.length >= 2 && split[0].endsWith(".service") && "enabled".equals(split[1])) {
                String name = split[0].substring(0, split[0].length() - 8);
                int index = name.lastIndexOf('.');
                String shortName = (index < 0 || index > name.length() - 2) ? name : name.substring(index + 1);
                if (!running.contains(name) && !running.contains(shortName)) {
                    OSService s = new OSService(name, 0, STOPPED);
                    services.add(s);
                    systemctlFound = true;
                }
            }
        }
        if (!systemctlFound) {
            File dir = new File("/etc/init");
            if (dir.exists() && dir.isDirectory()) {
                for (File f : dir.listFiles((f, name) -> name.toLowerCase(Locale.ROOT).endsWith(".conf"))) {
                    String name = f.getName().substring(0, f.getName().length() - 5);
                    int index = name.lastIndexOf('.');
                    String shortName = (index < 0 || index > name.length() - 2) ? name : name.substring(index + 1);
                    if (!running.contains(name) && !running.contains(shortName)) {
                        OSService s = new OSService(name, 0, STOPPED);
                        services.add(s);
                    }
                }
            } else {
                LOG.error("Directory: /etc/init does not exist");
            }
        }
        return services;
    }

    /**
     * Gets Jiffies per second, useful for converting ticks to milliseconds and vice versa.
     *
     * @return Jiffies per second.
     */
    public abstract long getHz();

    /**
     * Gets Page Size, for converting memory stats from pages to bytes
     *
     * @return Page Size
     */
    public abstract long getPageSize();

    /**
     * Gets the system boot time in seconds since the epoch.
     *
     * @return Boot time in seconds since the epoch.
     */
    public static long getBootTime() {
        return BOOTTIME;
    }
}
