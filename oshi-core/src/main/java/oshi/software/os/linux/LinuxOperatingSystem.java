/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.linux;

import java.io.File;
import java.util.ArrayList;
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
import oshi.software.os.OSProcess;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
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

    // Resident Set Size is given as number of pages the process has in real memory.
    // To get the actual size in bytes we need to multiply that with page size.
    private final int memoryPageSize;

    public LinuxOperatingSystem() {
        this.manufacturer = "GNU/Linux";
        setFamilyFromReleaseFiles();
        // The above call may also populate versionId and codeName
        // to pass to version constructor
        this.version = new LinuxOSVersionInfoEx(this.versionId, this.codeName);
        this.memoryPageSize = getMemoryPageSize();
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
        String[] split = FileUtil.getSplitFromFile(String.format("/proc/%d/stat", pid));
        if (split.length < 24) {
            return null;
        }
        String path = "";
        Pointer buf = new Memory(1024);
        int size = Libc.INSTANCE.readlink(String.format("/proc/%d/exe", pid), buf, 1023);
        if (size > 0) {
            path = buf.getString(0).substring(0, size);
        }
        Map<String, String> io = FileUtil.getKeyValueMapFromFile(String.format("/proc/%d/io", pid), ":");
        return new LinuxProcess(
                split[1].replaceFirst("\\(", "").replace(")", ""), // name
                // See man proc for how to parse /proc/[pid]/stat
                path, // path
                split[2].charAt(0), // state, one of RSDZTW
                pid, // also split[0] but we already have
                ParseUtil.parseIntOrDefault(split[3], 0), // ppid
                ParseUtil.parseIntOrDefault(split[19], 0), // thread count
                ParseUtil.parseIntOrDefault(split[17], 0), // priority
                ParseUtil.parseLongOrDefault(split[22], 0L), // VSZ
                ParseUtil.parseLongOrDefault(split[23], 0L) * memoryPageSize, // RSS pages * page_size
                // The below values are in jiffies
                ParseUtil.parseLongOrDefault(split[14], 0L), // kernelTime
                ParseUtil.parseLongOrDefault(split[13], 0L), // userTime
                ParseUtil.parseLongOrDefault(split[21], 0L), // startTime (after
                                                             // uptime)
                // See man proc for how to parse /proc/[pid]/io
                ParseUtil.parseLongOrDefault(io.getOrDefault("read_bytes", ""), 0L),
                ParseUtil.parseLongOrDefault(io.getOrDefault("write_bytes", ""), 0L),
                System.currentTimeMillis() //
        );
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
                LOG.error("Failed to get process thread count. Error code: " + Native.getLastError());
                return 0;
            }
            return info.procs;
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            LOG.error("Failed to get procs from sysinfo. {}", e);
        }
        return 0;
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
        File[] matchingFiles = etc.listFiles(f -> (f.getName().endsWith("-release") || f.getName().endsWith("-version")
                || f.getName().endsWith("_release") || f.getName().endsWith("_version"))
                && !(f.getName().endsWith("os-release") || f.getName().endsWith("lsb-release")));
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
}
