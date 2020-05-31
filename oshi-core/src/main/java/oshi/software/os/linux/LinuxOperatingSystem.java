/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.software.os.linux;

import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native; // NOSONAR squid:S1191
import com.sun.jna.platform.linux.LibC;
import com.sun.jna.platform.linux.LibC.Sysinfo;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.Who;
import oshi.driver.linux.proc.CpuStat;
import oshi.driver.linux.proc.ProcessStat;
import oshi.driver.linux.proc.UpTime;
import oshi.jna.platform.linux.LinuxLibc;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OSSession;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Linux is a family of open source Unix-like operating systems based on the
 * Linux kernel, an operating system kernel first released on September 17,
 * 1991, by Linus Torvalds. Linux is typically packaged in a Linux distribution.
 */
@ThreadSafe
public class LinuxOperatingSystem extends AbstractOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxOperatingSystem.class);

    private static final String OS_RELEASE_LOG = "os-release: {}";
    private static final String LSB_RELEASE_A_LOG = "lsb_release -a: {}";
    private static final String LSB_RELEASE_LOG = "lsb-release: {}";
    private static final String RELEASE_DELIM = " release ";
    private static final String DOUBLE_QUOTES = "^\"|\"$";
    private static final String LS_F_PROC_PID_FD = "ls -f " + ProcPath.PID_FD;

    private static final long BOOTTIME;
    static {
        long tempBT = CpuStat.getBootTime();
        // If above fails, current time minus uptime.
        if (tempBT == 0) {
            tempBT = System.currentTimeMillis() / 1000L - (long) UpTime.getSystemUptimeSeconds();
        }
        BOOTTIME = tempBT;
    }

    // Resident Set Size is given as number of pages the process has in real
    // memory.
    // To get the actual size in bytes we need to multiply that with page size.
    private final int memoryPageSize;

    // Order the field is in /proc/pid/stat
    enum ProcPidStat {
        // The parsing implementation in ParseUtil requires these to be declared
        // in increasing order
        PPID(4), USER_TIME(14), KERNEL_TIME(15), PRIORITY(18), THREAD_COUNT(20), START_TIME(22), VSZ(23), RSS(24);

        private int order;

        public int getOrder() {
            return this.order;
        }

        ProcPidStat(int order) {
            this.order = order;
        }
    }

    // Get a list of orders to pass to ParseUtil
    private static final int[] PROC_PID_STAT_ORDERS = new int[ProcPidStat.values().length];
    static {
        for (ProcPidStat stat : ProcPidStat.values()) {
            // The PROC_PID_STAT enum indices are 1-indexed.
            // Subtract one to get a zero-based index
            PROC_PID_STAT_ORDERS[stat.ordinal()] = stat.getOrder() - 1;
        }
    }

    // 2.6 Kernel has 44 elements, 3.3 has 47, and 3.5 has 52.
    // Check /proc/self/stat to find its length
    private static final int PROC_PID_STAT_LENGTH;
    static {
        String stat = FileUtil.getStringFromFile(ProcPath.SELF_STAT);
        if (!stat.isEmpty() && stat.contains(")")) {
            // add 3 to account for pid, process name in prarenthesis, and state
            PROC_PID_STAT_LENGTH = ParseUtil.countStringToLongArray(stat, ' ') + 3;
        } else {
            // Default assuming recent kernel
            PROC_PID_STAT_LENGTH = 52;
        }
    }

    // Jiffies per second, used for process time counters.
    private static final long USER_HZ = ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("getconf CLK_TCK"),
            100L);
    // Boot time in MS.
    private static final long BOOT_TIME;
    static {
        // Uptime is only in hundredths of seconds but we need thousandths.
        // We can grab uptime twice and take average to reduce error, getting
        // current time in between
        double uptime = UpTime.getSystemUptimeSeconds();
        long now = System.currentTimeMillis();
        uptime += UpTime.getSystemUptimeSeconds();
        // Uptime is now 2x seconds, so divide by 2, but
        // we want milliseconds so multiply by 1000
        // Ultimately multiply by 1000/2 = 500
        BOOT_TIME = now - (long) (500d * uptime + 0.5);
        // Cast/truncation is effectively rounding. Boot time could
        // be +/- 5 ms due to System Uptime rounding to nearest 10ms.
    }

    /**
     * <p>
     * Constructor for LinuxOperatingSystem.
     * </p>
     */
    @SuppressWarnings("deprecation")
    public LinuxOperatingSystem() {
        super.getVersionInfo();
        // The above call may also populate versionId and codeName
        // to pass to version constructor
        this.version = new LinuxOSVersionInfoEx(Constants.UNKNOWN, Constants.UNKNOWN);
        this.memoryPageSize = ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("getconf PAGESIZE"), 4096);
    }

    @Override
    public String queryManufacturer() {
        return "GNU/Linux";
    }

    @Override
    public FamilyVersionInfo queryFamilyVersionInfo() {
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
        return new FamilyVersionInfo(familyVersionCodename.getA(), versionInfo);
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness < 64 && ExecutingCommand.getFirstAnswer("uname -m").indexOf("64") == -1) {
            return jvmBitness;
        }
        return 64;
    }

    @Override
    protected boolean queryElevated() {
        return System.getenv("SUDO_COMMAND") != null;
    }

    @Override
    public FileSystem getFileSystem() {
        return new LinuxFileSystem();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new LinuxInternetProtocolStats();
    }

    @Override
    public List<OSSession> getSessions() {
        return Collections.unmodifiableList(USE_WHO_COMMAND ? super.getSessions() : Who.queryUtxent());
    }

    @Override
    public OSProcess[] getProcesses(int limit, ProcessSort sort, boolean slowFields) {
        List<OSProcess> procs = new ArrayList<>();
        File[] pids = ProcessStat.getPidFiles();
        LinuxUserGroupInfo userGroupInfo = new LinuxUserGroupInfo();

        // now for each file (with digit name) get process info
        for (File pidFile : pids) {
            int pid = ParseUtil.parseIntOrDefault(pidFile.getName(), 0);
            OSProcess proc = getProcess(pid, userGroupInfo, slowFields);
            if (proc != null) {
                procs.add(proc);
            }
        }
        // Sort
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return sorted.toArray(new OSProcess[0]);
    }

    @Override
    public OSProcess getProcess(int pid, boolean slowFields) {
        return getProcess(pid, new LinuxUserGroupInfo(), slowFields);
    }

    private OSProcess getProcess(int pid, LinuxUserGroupInfo userGroupInfo, boolean slowFields) {
        String path = "";
        String procPidExe = String.format(ProcPath.PID_EXE, pid);
        try {
            Path link = Paths.get(procPidExe);
            path = Files.readSymbolicLink(link).toString();
            int index = path.indexOf(" (deleted)");
            if (index != -1) {
                path = path.substring(0, index);
            }
        } catch (InvalidPathException | IOException | UnsupportedOperationException | SecurityException e) {
            LOG.debug("Unable to open symbolic link {}", procPidExe);
        }
        Map<String, String> io = FileUtil.getKeyValueMapFromFile(String.format(ProcPath.PID_IO, pid), ":");
        // See man proc for how to parse /proc/[pid]/stat
        long now = System.currentTimeMillis();
        String stat = FileUtil.getStringFromFile(String.format(ProcPath.PID_STAT, pid));
        // A race condition may leave us with an empty string
        if (stat.isEmpty()) {
            return null;
        }
        // We can get name and status more easily from /proc/pid/status which we
        // call later, so just get the numeric bits here
        long[] statArray = ParseUtil.parseStringToLongArray(stat, PROC_PID_STAT_ORDERS, PROC_PID_STAT_LENGTH, ' ');
        // Fetch cached process if it exists
        OSProcess proc = new OSProcess(this);
        proc.setProcessID(pid);
        // The /proc/pid/cmdline value is null-delimited
        proc.setCommandLine(FileUtil.getStringFromFile(String.format(ProcPath.PID_CMDLINE, pid)));
        long startTime = BOOT_TIME + statArray[ProcPidStat.START_TIME.ordinal()] * 1000L / USER_HZ;
        // BOOT_TIME could be up to 5ms off. In rare cases when a process has
        // started within 5ms of boot it is possible to get negative uptime.
        if (startTime >= now) {
            startTime = now - 1;
        }
        proc.setStartTime(startTime);
        proc.setParentProcessID((int) statArray[ProcPidStat.PPID.ordinal()]);
        proc.setThreadCount((int) statArray[ProcPidStat.THREAD_COUNT.ordinal()]);
        proc.setPriority((int) statArray[ProcPidStat.PRIORITY.ordinal()]);
        proc.setVirtualSize(statArray[ProcPidStat.VSZ.ordinal()]);
        proc.setResidentSetSize(statArray[ProcPidStat.RSS.ordinal()] * this.memoryPageSize);
        proc.setKernelTime(statArray[ProcPidStat.KERNEL_TIME.ordinal()] * 1000L / USER_HZ);
        proc.setUserTime(statArray[ProcPidStat.USER_TIME.ordinal()] * 1000L / USER_HZ);
        proc.setUpTime(now - proc.getStartTime());
        // See man proc for how to parse /proc/[pid]/io
        proc.setBytesRead(ParseUtil.parseLongOrDefault(io.getOrDefault("read_bytes", ""), 0L));
        proc.setBytesWritten(ParseUtil.parseLongOrDefault(io.getOrDefault("write_bytes", ""), 0L));

        // gets the open files count
        if (slowFields) {
            List<String> openFilesList = ExecutingCommand.runNative(String.format(LS_F_PROC_PID_FD, pid));
            proc.setOpenFiles(openFilesList.size() - 1L);

            // get 5th byte of file for 64-bit check
            // https://en.wikipedia.org/wiki/Executable_and_Linkable_Format#File_header
            byte[] buffer = new byte[5];
            if (!path.isEmpty()) {
                try (InputStream is = new FileInputStream(path)) {
                    if (is.read(buffer) == buffer.length) {
                        proc.setBitness(buffer[4] == 1 ? 32 : 64);
                    }
                } catch (IOException e) {
                    LOG.warn("Failed to read process file: {}", path);
                }
            }
        }

        Map<String, String> status = FileUtil.getKeyValueMapFromFile(String.format(ProcPath.PID_STATUS, pid), ":");
        proc.setName(status.getOrDefault("Name", ""));
        proc.setPath(path);
        switch (status.getOrDefault("State", "U").charAt(0)) {
        case 'R':
            proc.setState(OSProcess.State.RUNNING);
            break;
        case 'S':
            proc.setState(OSProcess.State.SLEEPING);
            break;
        case 'D':
            proc.setState(OSProcess.State.WAITING);
            break;
        case 'Z':
            proc.setState(OSProcess.State.ZOMBIE);
            break;
        case 'T':
            proc.setState(OSProcess.State.STOPPED);
            break;
        default:
            proc.setState(OSProcess.State.OTHER);
            break;
        }
        proc.setUserID(ParseUtil.whitespaces.split(status.getOrDefault("Uid", ""))[0]);
        proc.setGroupID(ParseUtil.whitespaces.split(status.getOrDefault("Gid", ""))[0]);
        Pair<String, String> user = userGroupInfo.getUser(proc.getUserID());
        proc.setUser(user.getB());
        proc.setGroup(userGroupInfo.getGroupName(proc.getGroupID()));

        try {
            String cwdLink = String.format(ProcPath.PID_CWD, pid);
            String cwd = new File(cwdLink).getCanonicalPath();
            if (!cwd.equals(cwdLink)) {
                proc.setCurrentWorkingDirectory(cwd);
            }
        } catch (IOException e) {
            LOG.trace("Couldn't find cwd for pid {}: {}", pid, e.getMessage());
        }
        return proc;
    }

    @Override
    public OSProcess[] getChildProcesses(int parentPid, int limit, ProcessSort sort) {
        List<OSProcess> procs = new ArrayList<>();
        File[] procFiles = ProcessStat.getPidFiles();
        LinuxUserGroupInfo userGroupInfo = new LinuxUserGroupInfo();

        // now for each file (with digit name) get process info
        for (File procFile : procFiles) {
            int pid = ParseUtil.parseIntOrDefault(procFile.getName(), 0);
            if (parentPid == getParentPidFromProcFile(pid)) {
                OSProcess proc = getProcess(pid, userGroupInfo, true);
                if (proc != null) {
                    procs.add(proc);
                }
            }
        }
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return sorted.toArray(new OSProcess[0]);
    }

    private static int getParentPidFromProcFile(int pid) {
        String stat = FileUtil.getStringFromFile(String.format("/proc/%d/stat", pid));
        // A race condition may leave us with an empty string
        if (stat.isEmpty()) {
            return 0;
        }
        long[] statArray = ParseUtil.parseStringToLongArray(stat, PROC_PID_STAT_ORDERS, PROC_PID_STAT_LENGTH, ' ');
        return (int) statArray[ProcPidStat.PPID.ordinal()];
    }

    @Override
    public long getProcessAffinityMask(int processId) {
        // Would prefer to use native sched_getaffinity call but variable sizing is
        // kernel-dependent and requires C macros, so we use command line instead.
        String mask = ExecutingCommand.getFirstAnswer("taskset -p " + processId);
        // Output:
        // pid 3283's current affinity mask: 3
        // pid 9726's current affinity mask: f
        String[] split = ParseUtil.whitespaces.split(mask);
        try {
            return new BigInteger(split[split.length - 1], 16).longValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public int getProcessId() {
        return LinuxLibc.INSTANCE.getpid();
    }

    @Override
    public int getProcessCount() {
        return ProcessStat.getPidFiles().length;
    }

    @Override
    public int getThreadCount() {
        try {
            Sysinfo info = new Sysinfo();
            if (0 != LibC.INSTANCE.sysinfo(info)) {
                LOG.error("Failed to get process thread count. Error code: {}", Native.getLastError());
                return 0;
            }
            return info.procs;
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            LOG.error("Failed to get procs from sysinfo. {}", e.getMessage());
        }
        return 0;
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
    public NetworkParams getNetworkParams() {
        return new LinuxNetworkParams();
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
        // http://linux.die.net/man/1/lsb_release
        // In this case, the /etc/lsb-release file (if it exists) has
        // optional overrides to the information in the /etc/distrib-release
        // files, which show: "Distributor release x.x (Codename)"

        // Attempt to read /etc/system-release which has more details than
        // os-release on (CentOS and Fedora)
        if ((familyVersionCodename = readDistribRelease("/etc/system-release")) != null) {
            // If successful, we're done. this.family has been set and
            // possibly the versionID and codeName
            return familyVersionCodename;
        }

        // Attempt to read /etc/os-release file.
        if ((familyVersionCodename = readOsRelease()) != null) {
            // If successful, we're done. this.family has been set and
            // possibly the versionID and codeName
            return familyVersionCodename;
        }

        // Attempt to execute the `lsb_release` command
        if ((familyVersionCodename = execLsbRelease()) != null) {
            // If successful, we're done. this.family has been set and
            // possibly the versionID and codeName
            return familyVersionCodename;
        }

        // The above two options should hopefully work on most
        // distributions. If not, we keep having fun.
        // Attempt to read /etc/lsb-release file
        if ((familyVersionCodename = readLsbRelease()) != null) {
            // If successful, we're done. this.family has been set and
            // possibly the versionID and codeName
            return familyVersionCodename;
        }

        // If we're still looking, we search for any /etc/*-release (or
        // similar) filename, for which the first line should be of the
        // "Distributor release x.x (Codename)" format or possibly a
        // "Distributor VERSION x.x (Codename)" format
        String etcDistribRelease = getReleaseFilename();
        if ((familyVersionCodename = readDistribRelease(etcDistribRelease)) != null) {
            // If successful, we're done. this.family has been set and
            // possibly the versionID and codeName
            return familyVersionCodename;
        }
        // If we've gotten this far with no match, use the distrib-release
        // filename (defaults will eventually give "Unknown")
        String family = filenameToFamily(etcDistribRelease.replace("/etc/", "").replace("release", "")
                .replace("version", "").replace("-", "").replace("_", ""));
        return new Triplet<>(family, Constants.UNKNOWN, Constants.UNKNOWN);
    }

    /**
     * Attempts to read /etc/os-release
     *
     * @return a triplet with the parsed family, versionID and codeName if file
     *         successfully read and NAME= found, null otherwise
     */
    private static Triplet<String, String, String> readOsRelease() {
        String family = null;
        String versionId = Constants.UNKNOWN;
        String codeName = Constants.UNKNOWN;
        List<String> osRelease = FileUtil.readFile("/etc/os-release");
        // Search for NAME=
        for (String line : osRelease) {
            if (line.startsWith("VERSION=")) {
                LOG.debug(OS_RELEASE_LOG, line);
                // remove beginning and ending '"' characters, etc from
                // VERSION="14.04.4 LTS, Trusty Tahr" (Ubuntu style)
                // or VERSION="17 (Beefy Miracle)" (os-release doc style)
                line = line.replace("VERSION=", "").replaceAll(DOUBLE_QUOTES, "").trim();
                String[] split = line.split("[()]");
                if (split.length <= 1) {
                    // If no parentheses, check for Ubuntu's comma format
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
                // remove beginning and ending '"' characters, etc from
                // NAME="Ubuntu"
                family = line.replace("NAME=", "").replaceAll(DOUBLE_QUOTES, "").trim();
            } else if (line.startsWith("VERSION_ID=") && versionId.equals(Constants.UNKNOWN)) {
                LOG.debug(OS_RELEASE_LOG, line);
                // remove beginning and ending '"' characters, etc from
                // VERSION_ID="14.04"
                versionId = line.replace("VERSION_ID=", "").replaceAll(DOUBLE_QUOTES, "").trim();
            }
        }
        return family == null ? null : new Triplet<>(family, versionId, codeName);
    }

    /**
     * Attempts to execute `lsb_release -a`
     *
     * @return a triplet with the parsed family, versionID and codeName if the
     *         command successfully executed and Distributor ID: or Description:
     *         found, null otherwise
     */
    private static Triplet<String, String, String> execLsbRelease() {
        String family = null;
        String versionId = Constants.UNKNOWN;
        String codeName = Constants.UNKNOWN;
        // If description is of the format Distrib release x.x (Codename)
        // that is primary, otherwise use Distributor ID: which returns the
        // distribution concatenated, e.g., RedHat instead of Red Hat
        for (String line : ExecutingCommand.runNative("lsb_release -a")) {
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
     * @return a triplet with the parsed family, versionID and codeName if file
     *         successfully read and and DISTRIB_ID or DISTRIB_DESCRIPTION, null
     *         otherwise
     */
    private static Triplet<String, String, String> readLsbRelease() {
        String family = null;
        String versionId = Constants.UNKNOWN;
        String codeName = Constants.UNKNOWN;
        List<String> osRelease = FileUtil.readFile("/etc/lsb-release");
        // Search for NAME=
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
     * @return a triplet with the parsed family, versionID and codeName if file
     *         successfully read and " release " or " VERSION " found, null
     *         otherwise
     */
    private static Triplet<String, String, String> readDistribRelease(String filename) {
        if (new File(filename).exists()) {
            List<String> osRelease = FileUtil.readFile(filename);
            // Search for Distrib release x.x (Codename)
            for (String line : osRelease) {
                LOG.debug("{}: {}", filename, line);
                if (line.contains(RELEASE_DELIM)) {
                    // If this parses properly we're done
                    return parseRelease(line, RELEASE_DELIM);
                } else if (line.contains(" VERSION ")) {
                    // If this parses properly we're done
                    return parseRelease(line, " VERSION ");
                }
            }
        }
        return null;
    }

    /**
     * Helper method to parse version description line style
     *
     * @param line
     *            a String of the form "Distributor release x.x (Codename)"
     * @param splitLine
     *            A regex to split on, e.g. " release "
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
        // Find any *_input files in that path
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
        // If all else fails, try this
        return "/etc/issue";
    }

    /**
     * Converts a portion of a filename (e.g. the 'redhat' in /etc/redhat-release)
     * to a mixed case string representing the family (e.g., Red Hat)
     *
     * @param name
     *            Stripped version of filename after removing /etc and -release
     * @return Mixed case family
     */
    private static String filenameToFamily(String name) {
        switch (name.toLowerCase()) {
        // Handle known special cases
        case "":
            return "Solaris";
        case "blackcat":
            return "Black Cat";
        case "bluewhite64":
            return "BlueWhite64";
        case "e-smith":
            return "SME Server";
        case "eos":
            return "FreeEOS";
        case "hlfs":
            return "HLFS";
        case "lfs":
            return "Linux-From-Scratch";
        case "linuxppc":
            return "Linux-PPC";
        case "meego":
            return "MeeGo";
        case "mandakelinux":
            return "Mandrake";
        case "mklinux":
            return "MkLinux";
        case "nld":
            return "Novell Linux Desktop";
        case "novell":
        case "SuSE":
            return "SUSE Linux";
        case "pld":
            return "PLD";
        case "redhat":
            return "Red Hat Linux";
        case "sles":
            return "SUSE Linux ES9";
        case "sun":
            return "Sun JDS";
        case "synoinfo":
            return "Synology";
        case "tinysofa":
            return "Tiny Sofa";
        case "turbolinux":
            return "TurboLinux";
        case "ultrapenguin":
            return "UltraPenguin";
        case "va":
            return "VA-Linux";
        case "vmware":
            return "VMWareESX";
        case "yellowdog":
            return "Yellow Dog";

        // /etc/issue will end up here:
        case "issue":
            return "Unknown";
        // If not a special case just capitalize first letter
        default:
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
    }

    /**
     * Gets Jiffies per second, useful for converting ticks to milliseconds and vice
     * versa.
     *
     * @return Jiffies per second.
     */
    public static long getHz() {
        return USER_HZ;
    }

    @Override
    public OSService[] getServices() {
        // Get running services
        List<OSService> services = new ArrayList<>();
        Set<String> running = new HashSet<>();
        for (OSProcess p : getChildProcesses(1, 0, ProcessSort.PID)) {
            OSService s = new OSService(p.getName(), p.getProcessID(), RUNNING);
            services.add(s);
            running.add(p.getName());
        }
        boolean systemctlFound = false;
        List<String> systemctl = ExecutingCommand.runNative("systemctl list-unit-files");
        for (String str : systemctl) {
            String[] split = ParseUtil.whitespaces.split(str);
            if (split.length == 2 && split[0].endsWith(".service") && "enabled".equals(split[1])) {
                // remove .service extension
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
            // Get Directories for stopped services
            File dir = new File("/etc/init");
            if (dir.exists() && dir.isDirectory()) {
                for (File f : dir.listFiles((f, name) -> name.toLowerCase().endsWith(".conf"))) {
                    // remove .conf extension
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
        return services.toArray(new OSService[0]);
    }
}