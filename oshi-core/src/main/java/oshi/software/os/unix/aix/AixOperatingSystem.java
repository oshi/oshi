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
package oshi.software.os.unix.aix;

import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.proc.ProcessStat;
import oshi.driver.unix.aix.Who;
import oshi.jna.platform.unix.aix.AixLibc;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.Util;

/**
 * AIX (Advanced Interactive eXecutive) is a series of proprietary Unix
 * operating systems developed and sold by IBM for several of its computer
 * platforms.
 */
@ThreadSafe
public class AixOperatingSystem extends AbstractOperatingSystem {

    private static final long BOOTTIME = querySystemBootTime();

    @Override
    public String queryManufacturer() {
        return "IBM";
    }

    @Override
    public FamilyVersionInfo queryFamilyVersionInfo() {
        String[] split = ParseUtil.whitespaces.split(ExecutingCommand.getFirstAnswer("uname -sp"));
        // AIX powerpc
        String systemName = split[0];
        String archName = null;
        if (split.length > 1) {
            archName = split[1];
        }
        String versionNumber = System.getProperty("os.version");
        if (Util.isBlank(versionNumber)) {
            versionNumber = ExecutingCommand.getFirstAnswer("oslevel");
        }
        String releaseNumber = ExecutingCommand.getFirstAnswer("oslevel -s");
        return new FamilyVersionInfo(systemName, new OSVersionInfo(versionNumber, archName, releaseNumber));
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness == 64) {
            return 64;
        }
        return ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("getconf KERNEL_BITMODE"), 32);
    }

    @Override
    protected boolean queryElevated() {
        return System.getenv("SUDO_COMMAND") != null; // Not sure of this in AIX
    }

    @Override
    public FileSystem getFileSystem() {
        return new AixFileSystem();
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return new AixInternetProtocolStats();
    }

    @Override
    public List<OSProcess> getProcesses(int limit, ProcessSort sort) {
        List<OSProcess> procs = getProcessListFromPS(
                "ps -e -o st,pid,ppid,user,uid,group,gid,thcount,pri,vsize,rssize,etime,time,comm,pagein,args", -1);
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return Collections.unmodifiableList(sorted);
    }

    @Override
    public OSProcess getProcess(int pid) {
        List<OSProcess> procs = getProcessListFromPS(
                "ps -o st,pid,ppid,user,uid,group,gid,thcount,pri,vsize,rssize,etime,time,comm,pagein,args -p ", pid);
        if (procs.isEmpty()) {
            return null;
        }
        return procs.get(0);
    }

    @Override
    public List<OSProcess> getChildProcesses(int parentPid, int limit, ProcessSort sort) {
        // Needs updating for flags on AIX or to use /proc fs
        // Get list of children
        List<String> childPids = ExecutingCommand.runNative("pgrep -P " + parentPid);
        if (childPids.isEmpty()) {
            return Collections.emptyList();
        }
        List<OSProcess> procs = getProcessListFromPS(
                "ps -o s,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etime,time,comm,args -p "
                        + String.join(",", childPids),
                -1);
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return Collections.unmodifiableList(sorted);
    }

    private static List<OSProcess> getProcessListFromPS(String psCommand, int pid) {
        List<OSProcess> procs = new ArrayList<>();
        List<String> procList = ExecutingCommand.runNative(psCommand + (pid < 0 ? "" : pid));
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
                procs.add(new AixOSProcess(pid < 0 ? ParseUtil.parseIntOrDefault(split[1], 0) : pid, split));
            }
        }
        return procs;
    }

    @Override
    public int getProcessId() {
        return AixLibc.INSTANCE.getpid();
    }

    @Override
    public int getProcessCount() {
        // This should work as is, but let's create an AIX driver for it
        return ProcessStat.getPidFiles().length;
    }

    @Override
    public int getThreadCount() {
        // Needs updating for flags on AIX or to use /proc fs
        List<String> threadList = ExecutingCommand.runNative("ps -eLo pid");
        if (!threadList.isEmpty()) {
            // Subtract 1 for header
            return threadList.size() - 1;
        }
        return getProcessCount();
    }

    @Override
    public long getSystemUptime() {
        return System.currentTimeMillis() / 1000L - BOOTTIME;
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    private static long querySystemBootTime() {
        return Who.queryBootTime() / 1000L;
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new AixNetworkParams();
    }

    @Override
    public OSService[] getServices() {
        // Need to update for whatever services AIX uses
        List<OSService> services = new ArrayList<>();
        // Get legacy RC service name possibilities
        List<String> legacySvcs = new ArrayList<>();
        File dir = new File("/etc/init.d");
        File[] listFiles;
        if (dir.exists() && dir.isDirectory() && (listFiles = dir.listFiles()) != null) {
            for (File f : listFiles) {
                legacySvcs.add(f.getName());
            }
        }
        // Iterate service list
        List<String> svcs = ExecutingCommand.runNative("svcs -p");
        /*-
         Output:
         STATE          STIME    FRMI
         legacy_run     23:56:49 lrc:/etc/rc2_d/S47pppd
         legacy_run     23:56:49 lrc:/etc/rc2_d/S81dodatadm_udaplt
         legacy_run     23:56:49 lrc:/etc/rc2_d/S89PRESERVE
         online         23:56:25 svc:/system/early-manifest-import:default
         online         23:56:25 svc:/system/svc/restarter:default
                        23:56:24       13 svc.startd
                        ...
         */
        for (String line : svcs) {
            if (line.startsWith("online")) {
                int delim = line.lastIndexOf(":/");
                if (delim > 0) {
                    String name = line.substring(delim + 1);
                    if (name.endsWith(":default")) {
                        name = name.substring(0, name.length() - 8);
                    }
                    services.add(new OSService(name, 0, STOPPED));
                }
            } else if (line.startsWith(" ")) {
                String[] split = ParseUtil.whitespaces.split(line.trim());
                if (split.length == 3) {
                    services.add(new OSService(split[2], ParseUtil.parseIntOrDefault(split[1], 0), RUNNING));
                }
            } else if (line.startsWith("legacy_run")) {
                for (String svc : legacySvcs) {
                    if (line.endsWith(svc)) {
                        services.add(new OSService(svc, 0, STOPPED));
                        break;
                    }
                }
            }
        }
        return services.toArray(new OSService[0]);
    }
}
