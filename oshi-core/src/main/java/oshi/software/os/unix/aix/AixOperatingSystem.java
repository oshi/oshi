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
import static oshi.util.Memoizer.memoize;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;

import com.sun.jna.Native;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.proc.ProcessStat;
import oshi.driver.unix.aix.Who;
import oshi.driver.unix.aix.perfstat.PerfstatConfig;
import oshi.jna.platform.unix.aix.AixLibc;
import oshi.jna.platform.unix.aix.Perfstat.perfstat_partition_config_t;
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

    private final Supplier<perfstat_partition_config_t> config = memoize(PerfstatConfig::queryConfig);
    private static final long BOOTTIME = querySystemBootTime();

    @Override
    public String queryManufacturer() {
        return "IBM";
    }

    @Override
    public FamilyVersionInfo queryFamilyVersionInfo() {
        perfstat_partition_config_t cfg = config.get();

        String systemName = Native.toString(cfg.OSName);
        String archName = Native.toString(cfg.processorFamily);
        String versionNumber = Native.toString(cfg.OSVersion);
        if (Util.isBlank(versionNumber)) {
            versionNumber = ExecutingCommand.getFirstAnswer("oslevel");
        }
        String releaseNumber = Native.toString(cfg.OSBuild);
        if (Util.isBlank(releaseNumber)) {
            releaseNumber = ExecutingCommand.getFirstAnswer("oslevel -s");
        } else {
            // strip leading date
            int idx = releaseNumber.lastIndexOf(' ');
            if (idx > 0 && idx < releaseNumber.length()) {
                releaseNumber = releaseNumber.substring(idx + 1);
            }
        }
        return new FamilyVersionInfo(systemName, new OSVersionInfo(versionNumber, archName, releaseNumber));
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness == 64) {
            return 64;
        }
        // 9th bit of conf is 64-bit kernel
        return (config.get().conf & 0x0080_0000) > 0 ? 64 : 32;
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
        // Get system services from lssrc command
        /*-
         Output:
         Subsystem         Group            PID          Status
            platform_agent                    2949214      active
            cimsys                            2490590      active
            snmpd            tcpip            2883698      active
            syslogd          ras              2359466      active
            sendmail         mail             3145828      active
            portmap          portmap          2818188      active
            inetd            tcpip            2752656      active
            lpd              spooler                       inoperative
                        ...
         */
        List<String> systemServicesInfoList = ExecutingCommand.runNative("lssrc -a");
        if (systemServicesInfoList.size() > 1) {
            systemServicesInfoList.remove(0); // remove header
            for (String systemService : systemServicesInfoList) {
                String[] serviceSplit = ParseUtil.whitespaces.split(systemService.trim());
                if (systemService.contains("active")) {
                    if (serviceSplit.length == 4) {
                        services.add(new OSService(serviceSplit[0], ParseUtil.parseIntOrDefault(serviceSplit[2], 0),
                                RUNNING));
                    } else if (serviceSplit.length == 3) {
                        services.add(new OSService(serviceSplit[0], ParseUtil.parseIntOrDefault(serviceSplit[1], 0),
                                RUNNING));
                    }
                } else if (systemService.contains("inoperative")) {
                    services.add(new OSService(serviceSplit[0], 0, STOPPED));
                }
            }
        }
        // Get installed services from /etc/rc.d/init.d
        File dir = new File("/etc/rc.d/init.d");
        File[] listFiles;
        if (dir.exists() && dir.isDirectory() && (listFiles = dir.listFiles()) != null) {
            for (File file : listFiles) {
                String installedService = ExecutingCommand.getFirstAnswer(file.getAbsolutePath() + " status");
                // Apache httpd daemon is running with PID 3997858.
                if (installedService.contains("running")) {
                    Matcher m = ParseUtil.AIX_RUNNING_SERVICE_INFO.matcher(installedService);
                    if (m.find()) {
                        services.add(
                                new OSService(file.getName(), ParseUtil.parseIntOrDefault(m.group(3), 0), RUNNING));
                    }
                } else {
                    services.add(new OSService(file.getName(), 0, STOPPED));
                }
            }
        }
        return services.toArray(new OSService[0]);
    }
}
