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
package oshi.software.os.unix.openbsd;

import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.CTL_KERN;
import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.KERN_OSRELEASE;
import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.KERN_OSTYPE;
import static oshi.jna.platform.unix.openbsd.OpenBsdLibc.KERN_VERSION;
import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.openbsd.OpenBsdLibc;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;

/**
 *
 * OpenBsd is a free and open-source Unix-like operating system descended from
 * the Berkeley Software Distribution (BSD), which was based on Research Unix.
 * The first version of OpenBsd was released in 1993. In 2005, OpenBsd was the
 * most popular open-source BSD operating system, accounting for more than
 * three-quarters of all installed simply, permissively licensed BSD systems.
 */
@ThreadSafe
public class OpenBsdOperatingSystem extends AbstractOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(OpenBsdOperatingSystem.class);

    private static final long BOOTTIME = querySystemBootTime();

    @Override
    public String queryManufacturer() {
        return "Unix/BSD";
    }

    @Override
    public FamilyVersionInfo queryFamilyVersionInfo() {
        int[] mib = new int[2];
        mib[0] = CTL_KERN;
        mib[1] = KERN_OSTYPE;
        String family = OpenBsdSysctlUtil.sysctl(mib, "OpenBSD");
        mib[1] = KERN_OSRELEASE;
        String version = OpenBsdSysctlUtil.sysctl(mib, "");
        mib[1] = KERN_VERSION;
        String versionInfo = OpenBsdSysctlUtil.sysctl(mib, "");
        String buildNumber = versionInfo.split(":")[0].replace(family, "").replace(version, "").trim();

        return new FamilyVersionInfo(family, new OSVersionInfo(version, null, buildNumber));
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness < 64 && ExecutingCommand.getFirstAnswer("uname -m").indexOf("64") == -1) {
            return jvmBitness;
        }
        return 64;
    }

    @Override
    public FileSystem getFileSystem() {
        return new OpenBsdFileSystem();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new OpenBsdInternetProtocolStats();
    }

    @Override
    public List<OSProcess> queryAllProcesses() {
        return getProcessListFromPS(-1);
    }

    @Override
    public List<OSProcess> queryChildProcesses(int parentPid) {
        return queryAllProcesses().stream().filter(p -> p.getParentProcessID() == parentPid)
                .collect(Collectors.toList());
    }

    @Override
    public OSProcess getProcess(int pid) {
        List<OSProcess> procs = getProcessListFromPS(pid);
        if (procs.isEmpty()) {
            return null;
        }
        return procs.get(0);
    }

    private static List<OSProcess> getProcessListFromPS(int pid) {
        List<OSProcess> procs = new ArrayList<>();
        // https://man.openbsd.org/ps#KEYWORDS
        // missing are threadCount and kernelTime which is included in cputime
        String psCommand = "ps -awwxo state,pid,ppid,user,uid,group,gid,pri,vsz,rss,etime,cputime,comm,majflt,minflt,args";
        if (pid >= 0) {
            psCommand += " -p " + pid;
        }
        List<String> procList = ExecutingCommand.runNative(psCommand);
        if (procList.isEmpty() || procList.size() < 2) {
            return procs;
        }
        // remove header row
        procList.remove(0);
        // Fill list
        for (String proc : procList) {
            String[] split = ParseUtil.whitespaces.split(proc.trim(), 16);
            // Elements should match ps command order
            if (split.length == 16) {
                procs.add(new OpenBsdOSProcess(pid < 0 ? ParseUtil.parseIntOrDefault(split[1], 0) : pid, split));
            }
        }
        return procs;
    }

    @Override
    public int getProcessId() {
        return OpenBsdLibc.INSTANCE.getpid();
    }

    @Override
    public int getProcessCount() {
        List<String> procList = ExecutingCommand.runNative("ps -axo pid");
        if (!procList.isEmpty()) {
            // Subtract 1 for header
            return procList.size() - 1;
        }
        return 0;
    }

    @Override
    public int getThreadCount() {
        // -H "Also display information about kernel visible threads"
        // -k "Also display information about kernel threads"
        // column TID holds thread ID
        List<String> threadList = ExecutingCommand.runNative("ps -axHo tid");
        if (!threadList.isEmpty()) {
            // Subtract 1 for header
            return threadList.size() - 1;
        }
        return 0;
    }

    @Override
    public long getSystemUptime() {
        return System.currentTimeMillis() / 1000 - BOOTTIME;
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    private static long querySystemBootTime() {
        // Boot time will be the first consecutive string of digits.
        return ParseUtil.parseLongOrDefault(
                ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""),
                System.currentTimeMillis() / 1000);
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new OpenBsdNetworkParams();
    }

    @Override
    public OSService[] getServices() {
        // Get running services
        List<OSService> services = new ArrayList<>();
        Set<String> running = new HashSet<>();
        for (OSProcess p : getChildProcesses(1, ProcessFiltering.ALL_PROCESSES, ProcessSorting.PID_ASC, 0)) {
            OSService s = new OSService(p.getName(), p.getProcessID(), RUNNING);
            services.add(s);
            running.add(p.getName());
        }
        // Get Directories for stopped services
        File dir = new File("/etc/rc.d");
        File[] listFiles;
        if (dir.exists() && dir.isDirectory() && (listFiles = dir.listFiles()) != null) {
            for (File f : listFiles) {
                String name = f.getName();
                if (!running.contains(name)) {
                    OSService s = new OSService(name, 0, STOPPED);
                    services.add(s);
                }
            }
        } else {
            LOG.error("Directory: /etc/rc.d does not exist");
        }
        return services.toArray(new OSService[0]);
    }
}
