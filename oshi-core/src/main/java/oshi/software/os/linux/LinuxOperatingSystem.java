/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.software.os.linux;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import oshi.jna.platform.linux.Libc;
import oshi.jna.platform.linux.Libc.Sysinfo;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSUser;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.MapUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcUtil;

/**
 * Linux is a family of free operating systems most commonly used on personal
 * computers.
 *
 * @author widdis[at]gmail[dot]com
 */
public class LinuxOperatingSystem extends AbstractOperatingSystem {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(LinuxOperatingSystem.class);

    // Populated with results of reading /etc/os-release or other files
    protected String versionId;

    protected String codeName;

    // Resident Set Size is given as number of pages the process has in real
    // memory.
    // To get the actual size in bytes we need to multiply that with page size.
    private final int memoryPageSize;

    // Jiffies per second, used for process time counters.
    private static long hz = 1000L;
    // Boot time in MS
    private static long bootTime = 0L;

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
    // Since we parse a proc/pid value as part of jiffy calculation, we can
    // count the length there to set this value
    private static int procPidStatLength = 52;

    private transient LinuxUserGroupInfo userGroupInfo = new LinuxUserGroupInfo();

    static {
        init();
    }

    public LinuxOperatingSystem() {
        this.manufacturer = "GNU/Linux";
        setFamilyFromReleaseFiles();
        // The above call may also populate versionId and codeName
        // to pass to version constructor
        this.version = new LinuxOSVersionInfoEx(this.versionId, this.codeName);
        this.memoryPageSize = getMemoryPageSize();
        init();
        initBitness();
    }

    /**
     * Correlate the youngest process start time in seconds with start time in
     * jiffies
     */
    private static void init() {
        // To correlate a process start time in seconds with the same process
        // start time in jiffies. We prefer the youngest (or close to it) which
        // minimizes its up time (etime)
        // Timeline:
        // BOOT|<----jiffies---->|<----etime---->|NOW
        // BOOT|<------------uptime------------->|NOW

        // To avoid having to check all processes we can just pick the highest
        // PID. This will either be the youngest one or at least big enough.

        // Get all the pid files (guaranteed to be digit-only filenames)
        File[] pids = ProcUtil.getPidFiles();
        // Sort descending "numerically"
        Arrays.sort(pids, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return Integer.valueOf(f2.getName()).compareTo(Integer.valueOf(f1.getName()));
            }
        });

        // Iterate /proc/[pid]/stat checking the creation time (field 22,
        // jiffies since boot). Since we're working on descending PIDs, we
        // expect the first (higher PIDs) to be younger, but may have processes
        // that ended since we collected the files. The first time we get a
        // value we'll save it as the youngest and quit.
        long youngestJiffies = 0L;
        String youngestPid = "";
        for (File pid : pids) {
            List<String> stat = FileUtil.readFile(String.format("/proc/%s/stat", pid.getName()), false);
            if (!stat.isEmpty() && stat.get(0).contains(")")) {
                String procPidStat = stat.get(0);
                // 2nd elment is process name and may contain spaces, but is
                // within parenthesis. Split from the ')' index.
                int parenIndex = procPidStat.lastIndexOf(')');
                String[] split = ParseUtil.whitespaces.split(procPidStat.substring(parenIndex));
                if (split.length >= 21) {
                    // ')' is split index 0 but stat element 2.
                    // We want element 22 so it will be at index 20 of the split
                    youngestJiffies = ParseUtil.parseLongOrDefault(split[20], 0L);
                    youngestPid = pid.getName();
                    // Add 1 to account for pid that didn't make the split
                    procPidStatLength = split.length + 1;
                    break;
                }
            }
        }
        LOG.debug("Youngest PID is {} with {} jiffies", youngestPid, youngestJiffies);
        // Shouldn't happen but avoiding Division by zero
        if (youngestJiffies == 0) {
            LOG.error("Couldn't find any running processes, which is odd since we are in a running process. "
                    + "Process time values are in jiffies, not milliseconds.");
            return;
        }

        float startTimeSecsSinceBoot = ProcUtil.getSystemUptimeFromProc();
        bootTime = System.currentTimeMillis() - (long) (1000 * startTimeSecsSinceBoot);

        // This takes advantage of the fact that ps does all the heavy lifting
        // of sorting out HZ internally.
        String etime = ExecutingCommand.getFirstAnswer(String.format("ps -p %s -o etimes=", youngestPid));
        // Since we picked the youngest process, it's safe to assume an
        // etime close to 0 in case this command fails; the longer the system
        // has been up, the less impact this assumption will have
        if (!etime.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Etime is {} seconds", etime.trim());
            }
            startTimeSecsSinceBoot -= Float.parseFloat(etime.trim());
        }
        // By subtracting etime (secs) from uptime (secs) we get uptime (in
        // secs) when the process was started. This correlates with startTime in
        // jiffies for this process
        LOG.debug("Start time in secs: {}", startTimeSecsSinceBoot);
        if (startTimeSecsSinceBoot <= 0) {
            LOG.warn("Couldn't calculate jiffies per second. "
                    + "Process time values are in jiffies, not milliseconds.");
            return;
        }

        // divide jiffies (since boot) by seconds (since boot)
        hz = (long) (youngestJiffies / startTimeSecsSinceBoot + 0.5f);
        // reset to default if value is invalid
        if (hz == 0)
            hz = 1000L;
    }

    private void initBitness() {
        if (this.bitness < 64 && ExecutingCommand.getFirstAnswer("uname -m").indexOf("64") != -1) {
            this.bitness = 64;
        }
    }

    private static int getMemoryPageSize() {
        try {
            return Libc.INSTANCE.getpagesize();
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            LOG.error("Failed to get the memory page size.", e);
        }
        // default to 4K if the above call fails
        return 4096;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystem getFileSystem() {
        return new LinuxFileSystem();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess[] getProcesses(int limit, ProcessSort sort) {
        List<OSProcess> procs = new ArrayList<>();
        File[] pids = ProcUtil.getPidFiles();

        // now for each file (with digit name) get process info
        for (File pid : pids) {
            OSProcess proc = getProcess(ParseUtil.parseIntOrDefault(pid.getName(), 0));
            if (proc != null) {
                procs.add(proc);
            }
        }
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return sorted.toArray(new OSProcess[sorted.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess getProcess(int pid) {
        String path = "";
        Pointer buf = new Memory(1024);
        int size = Libc.INSTANCE.readlink(String.format("/proc/%d/exe", pid), buf, 1023);
        if (size > 0) {
            path = buf.getString(0).substring(0, size);
        }
        Map<String, String> io = FileUtil.getKeyValueMapFromFile(String.format("/proc/%d/io", pid), ":");
        long now = System.currentTimeMillis();
        // See man proc for how to parse /proc/[pid]/stat
        String stat = FileUtil.getStringFromFile(String.format("/proc/%d/stat", pid));
        // A race condition may leave us with an empty string
        if (stat.isEmpty()) {
            return null;
        }
        OSProcess proc = new OSProcess();
        // We can get name and status more easily from /proc/pid/status which we
        // call later, so just get the numeric bits here
        proc.setProcessID(pid);
        long[] statArray = ParseUtil.parseStringToLongArray(stat, PROC_PID_STAT_ORDERS, procPidStatLength, ' ');
        proc.setParentProcessID((int) statArray[ProcPidStat.PPID.ordinal()]);
        proc.setThreadCount((int) statArray[ProcPidStat.THREAD_COUNT.ordinal()]);
        proc.setPriority((int) statArray[ProcPidStat.PRIORITY.ordinal()]);
        proc.setVirtualSize(statArray[ProcPidStat.VSZ.ordinal()]);
        proc.setResidentSetSize(statArray[ProcPidStat.RSS.ordinal()] * this.memoryPageSize);
        proc.setKernelTime(statArray[ProcPidStat.KERNEL_TIME.ordinal()] * 1000L / hz);
        proc.setUserTime(statArray[ProcPidStat.USER_TIME.ordinal()] * 1000L / hz);
        proc.setStartTime(bootTime + statArray[ProcPidStat.START_TIME.ordinal()] * 1000L / hz);
        proc.setUpTime(now - proc.getStartTime());
        // See man proc for how to parse /proc/[pid]/io
        proc.setBytesRead(ParseUtil.parseLongOrDefault(MapUtil.getOrDefault(io, "read_bytes", ""), 0L));
        proc.setBytesWritten(ParseUtil.parseLongOrDefault(MapUtil.getOrDefault(io, "write_bytes", ""), 0L));

        // gets the open files count
        List<String> openFilesList = ExecutingCommand.runNative(String.format("ls -f /proc/%d/fd", pid));
        proc.setOpenFiles(openFilesList.size() - 1L);

        Map<String, String> status = FileUtil.getKeyValueMapFromFile(String.format("/proc/%d/status", pid), ":");
        proc.setName(MapUtil.getOrDefault(status, "Name", ""));
        proc.setPath(path);
        switch (MapUtil.getOrDefault(status, "State", "U").charAt(0)) {
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
        proc.setUserID(ParseUtil.whitespaces.split(MapUtil.getOrDefault(status, "Uid", ""))[0]);
        proc.setGroupID(ParseUtil.whitespaces.split(MapUtil.getOrDefault(status, "Gid", ""))[0]);
        OSUser user = userGroupInfo.getUser(proc.getUserID());
        if (user != null) {
            proc.setUser(user.getUserName());
        }
        proc.setGroup(userGroupInfo.getGroupName(proc.getGroupID()));

        // THe /proc/pid/cmdline value is null-delimited
        proc.setCommandLine(FileUtil.getStringFromFile(String.format("/proc/%d/cmdline", pid)));
        try {
            String cwdLink = String.format("/proc/%d/cwd", pid);
            String cwd = new File(cwdLink).getCanonicalPath();
            if (!cwd.equals(cwdLink)) {
                proc.setCurrentWorkingDirectory(cwd);
            }
        } catch (IOException e) {
            LOG.trace("Couldn't find cwd for pid {}: {}", pid, e);
        }
        return proc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OSProcess[] getChildProcesses(int parentPid, int limit, ProcessSort sort) {
        List<OSProcess> procs = new ArrayList<>();
        File[] procFiles = ProcUtil.getPidFiles();

        // now for each file (with digit name) get process info
        for (File procFile : procFiles) {
            int pid = ParseUtil.parseIntOrDefault(procFile.getName(), 0);
            if (parentPid == getParentPidFromProcFile(pid)) {
                OSProcess proc = getProcess(pid);
                if (proc != null) {
                    procs.add(proc);
                }
            }
        }
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return sorted.toArray(new OSProcess[sorted.size()]);
    }

    private static int getParentPidFromProcFile(int pid) {
        String stat = FileUtil.getStringFromFile(String.format("/proc/%d/stat", pid));
        long[] statArray = ParseUtil.parseStringToLongArray(stat, PROC_PID_STAT_ORDERS, procPidStatLength, ' ');
        return (int) statArray[ProcPidStat.PPID.ordinal()];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getProcessId() {
        return Libc.INSTANCE.getpid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getProcessCount() {
        return ProcUtil.getPidFiles().length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getThreadCount() {
        try {
            Sysinfo info = new Sysinfo();
            if (0 != Libc.INSTANCE.sysinfo(info)) {
                LOG.error("Failed to get process thread count. Error code: {}", Native.getLastError());
                return 0;
            }
            return info.procs;
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            LOG.error("Failed to get procs from sysinfo. {}", e);
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkParams getNetworkParams() {
        return new LinuxNetworkParams();
    }

    private void setFamilyFromReleaseFiles() {
        if (this.family == null) {
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
            //

            // Attempt to read /etc/os-release file.
            if (readOsRelease()) {
                // If successful, we're done. this.family has been set and
                // possibly the versionID and codeName
                return;
            }

            // Attempt to execute the `lsb_release` command
            if (execLsbRelease()) {
                // If successful, we're done. this.family has been set and
                // possibly the versionID and codeName
                return;
            }

            // The above two options should hopefully work on most
            // distributions. If not, we keep having fun.
            // Attempt to read /etc/lsb-release file
            if (readLsbRelease()) {
                // If successful, we're done. this.family has been set and
                // possibly the versionID and codeName
                return;
            }

            // If we're still looking, we search for any /etc/*-release (or
            // similar) filename, for which the first line should be of the
            // "Distributor release x.x (Codename)" format or possibly a
            // "Distributor VERSION x.x (Codename)" format
            String etcDistribRelease = getReleaseFilename();
            if (readDistribRelease(etcDistribRelease)) {
                // If successful, we're done. this.family has been set and
                // possibly the versionID and codeName
                return;
            }
            // If we've gotten this far with no match, use the distrib-release
            // filename (defaults will eventually give "Unknown")
            this.family = filenameToFamily(etcDistribRelease.replace("/etc/", "").replace("release", "")
                    .replace("version", "").replace("-", "").replace("_", ""));
        }
    }

    /**
     * Attempts to read /etc/os-release
     *
     * @return true if file successfully read and NAME= found
     */
    private boolean readOsRelease() {
        if (new File("/etc/os-release").exists()) {
            List<String> osRelease = FileUtil.readFile("/etc/os-release");
            // Search for NAME=
            for (String line : osRelease) {
                if (line.startsWith("VERSION=")) {
                    LOG.debug("os-release: {}", line);
                    // remove beginning and ending '"' characters, etc from
                    // VERSION="14.04.4 LTS, Trusty Tahr" (Ubuntu style)
                    // or VERSION="17 (Beefy Miracle)" (os-release doc style)
                    line = line.replace("VERSION=", "").replaceAll("^\"|\"$", "").trim();
                    String[] split = line.split("[()]");
                    if (split.length <= 1) {
                        // If no parentheses, check for Ubuntu's comma format
                        split = line.split(", ");
                    }
                    if (split.length > 0) {
                        this.versionId = split[0].trim();
                    }
                    if (split.length > 1) {
                        this.codeName = split[1].trim();
                    }
                } else if (line.startsWith("NAME=") && this.family == null) {
                    LOG.debug("os-release: {}", line);
                    // remove beginning and ending '"' characters, etc from
                    // NAME="Ubuntu"
                    this.family = line.replace("NAME=", "").replaceAll("^\"|\"$", "").trim();
                } else if (line.startsWith("VERSION_ID=") && this.versionId == null) {
                    LOG.debug("os-release: {}", line);
                    // remove beginning and ending '"' characters, etc from
                    // VERSION_ID="14.04"
                    this.versionId = line.replace("VERSION_ID=", "").replaceAll("^\"|\"$", "").trim();
                }
            }
        }
        return this.family != null;
    }

    /**
     * Attempts to execute `lsb_release -a`
     *
     * @return true if the command successfully executed and Distributor ID: or
     *         Description: found
     */
    private boolean execLsbRelease() {
        // If description is of the format Distrib release x.x (Codename)
        // that is primary, otherwise use Distributor ID: which returns the
        // distribution concatenated, e.g., RedHat instead of Red Hat
        for (String line : ExecutingCommand.runNative("lsb_release -a")) {
            if (line.startsWith("Description:")) {
                LOG.debug("lsb_release -a: {}", line);
                line = line.replace("Description:", "").trim();
                if (line.contains(" release ")) {
                    this.family = parseRelease(line, " release ");
                }
            } else if (line.startsWith("Distributor ID:") && this.family == null) {
                LOG.debug("lsb_release -a: {}", line);
                this.family = line.replace("Distributor ID:", "").trim();
            } else if (line.startsWith("Release:") && this.versionId == null) {
                LOG.debug("lsb_release -a: {}", line);
                this.versionId = line.replace("Release:", "").trim();
            } else if (line.startsWith("Codename:") && this.codeName == null) {
                LOG.debug("lsb_release -a: {}", line);
                this.codeName = line.replace("Codename:", "").trim();
            }
        }
        return this.family != null;
    }

    /**
     * Attempts to read /etc/lsb-release
     *
     * @return true if file successfully read and DISTRIB_ID or
     *         DISTRIB_DESCRIPTION found
     */
    private boolean readLsbRelease() {
        if (new File("/etc/lsb-release").exists()) {
            List<String> osRelease = FileUtil.readFile("/etc/lsb-release");
            // Search for NAME=
            for (String line : osRelease) {
                if (line.startsWith("DISTRIB_DESCRIPTION=")) {
                    LOG.debug("lsb-release: {}", line);
                    line = line.replace("DISTRIB_DESCRIPTION=", "").replaceAll("^\"|\"$", "").trim();
                    if (line.contains(" release ")) {
                        this.family = parseRelease(line, " release ");
                    }
                } else if (line.startsWith("DISTRIB_ID=") && this.family == null) {
                    LOG.debug("lsb-release: {}", line);
                    this.family = line.replace("DISTRIB_ID=", "").replaceAll("^\"|\"$", "").trim();
                } else if (line.startsWith("DISTRIB_RELEASE=") && this.versionId == null) {
                    LOG.debug("lsb-release: {}", line);
                    this.versionId = line.replace("DISTRIB_RELEASE=", "").replaceAll("^\"|\"$", "").trim();
                } else if (line.startsWith("DISTRIB_CODENAME=") && this.codeName == null) {
                    LOG.debug("lsb-release: {}", line);
                    this.codeName = line.replace("DISTRIB_CODENAME=", "").replaceAll("^\"|\"$", "").trim();
                }
            }
        }
        return this.family != null;
    }

    /**
     * Attempts to read /etc/distrib-release (for some value of distrib)
     *
     * @return true if file successfully read and " release " or " VERSION "
     *         found
     */
    private boolean readDistribRelease(String filename) {
        if (new File(filename).exists()) {
            List<String> osRelease = FileUtil.readFile(filename);
            // Search for Distrib release x.x (Codename)
            for (String line : osRelease) {
                LOG.debug("{}: {}", filename, line);
                if (line.contains(" release ")) {
                    this.family = parseRelease(line, " release ");
                    // If this parses properly we're done
                    break;
                } else if (line.contains(" VERSION ")) {
                    this.family = parseRelease(line, " VERSION ");
                    // If this parses properly we're done
                    break;
                }
            }
        }
        return this.family != null;
    }

    /**
     * Helper method to parse version description line style
     *
     * @param line
     *            a String of the form "Distributor release x.x (Codename)"
     * @param splitLine
     *            A regex to split on, e.g. " release "
     * @return the parsed family (versionID and codeName may have also been set)
     */
    private String parseRelease(String line, String splitLine) {
        String[] split = line.split(splitLine);
        String family = split[0].trim();
        if (split.length > 1) {
            split = split[1].split("[()]");
            if (split.length > 0) {
                this.versionId = split[0].trim();
            }
            if (split.length > 1) {
                this.codeName = split[1].trim();
            }
        }
        return family;
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
        File[] matchingFiles = etc.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return (f.getName().endsWith("-release") || f.getName().endsWith("-version")
                        || f.getName().endsWith("_release") || f.getName().endsWith("_version"))
                        && !(f.getName().endsWith("os-release") || f.getName().endsWith("lsb-release"));
            }
        });
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
     * Converts a portion of a filename (e.g. the 'redhat' in
     * /etc/redhat-release) to a mixed case string representing the family
     * (e.g., Red Hat)
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
     * gets the calculated Jiffies per second, useful for converting ticks to
     * milliseconds and vice versa
     * 
     * @return Jiffies per second
     */
    public static long getHz() {
        return hz;
    }

}
