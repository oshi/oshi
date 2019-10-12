/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.os.unix.solaris;

import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat; // NOSONAR squid:S1191

import oshi.jna.platform.unix.solaris.SolarisLibc;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.util.ExecutingCommand;
import oshi.util.LsofUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcUtil;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.platform.unix.solaris.KstatUtil.KstatChain;

/**
 * Linux is a family of free operating systems most commonly used on personal
 * computers.
 */
public class SolarisOperatingSystem extends AbstractOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(SolarisOperatingSystem.class);

    private static final long BOOTTIME;
    static {
        KstatChain kc = KstatUtil.getChain();
        Kstat ksp = kc.lookup("unix", 0, "system_misc");
        if (ksp != null && kc.read(ksp)) {
            BOOTTIME = KstatUtil.dataLookupLong(ksp, "boot_time");
        } else {
            BOOTTIME = System.currentTimeMillis() / 1000L - querySystemUptime();
        }
        kc.close();
    }

    /**
     * <p>
     * Constructor for SolarisOperatingSystem.
     * </p>
     */
    @SuppressWarnings("deprecation")
    public SolarisOperatingSystem() {
        this.version = new SolarisOSVersionInfoEx();
    }

    @Override
    public String queryManufacturer() {
        return "Oracle";
    }

    @Override
    public FamilyVersionInfo queryFamilyVersionInfo() {
        String[] split = ParseUtil.whitespaces.split(ExecutingCommand.getFirstAnswer("uname -rv"));
        String version = split[0];
        String buildNumber = null;
        if (split.length > 1) {
            buildNumber = split[1];
        }
        return new FamilyVersionInfo("SunOS", new OSVersionInfo(version,"Solaris",buildNumber));
    }

    @Override
    protected int queryBitness() {
        if (this.jvmBitness < 64) {
            return ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("isainfo -b"), 32);
        }
        return 64;
    }

    @Override
    protected boolean queryElevated() {
        return System.getenv("SUDO_COMMAND") != null;
    }

    @Override
    public FileSystem getFileSystem() {
        return new SolarisFileSystem();
    }

    @Override
    public OSProcess[] getProcesses(int limit, ProcessSort sort, boolean slowFields) {
        List<OSProcess> procs = getProcessListFromPS(
                "ps -eo s,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etime,time,comm,args", -1, slowFields);
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return sorted.toArray(new OSProcess[0]);
    }

    @Override
    public OSProcess getProcess(int pid) {
        return getProcess(pid, true);
    }

    private OSProcess getProcess(int pid, boolean slowFields) {
        List<OSProcess> procs = getProcessListFromPS(
                "ps -o s,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etime,time,comm,args -p ", pid, slowFields);
        if (procs.isEmpty()) {
            return null;
        }
        return procs.get(0);
    }

    @Override
    public OSProcess[] getChildProcesses(int parentPid, int limit, ProcessSort sort) {
        List<OSProcess> procs = getProcessListFromPS(
                "ps -eo s,pid,ppid,user,uid,group,gid,nlwp,pri,vsz,rss,etime,time,comm,args --ppid", parentPid, true);
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return sorted.toArray(new OSProcess[0]);
    }

    private List<OSProcess> getProcessListFromPS(String psCommand, int pid, boolean slowFields) {
        Map<Integer, String> cwdMap = LsofUtil.getCwdMap(pid);
        List<OSProcess> procs = new ArrayList<>();
        List<String> procList = ExecutingCommand.runNative(psCommand + (pid < 0 ? "" : pid));
        if (procList.isEmpty() || procList.size() < 2) {
            return procs;
        }
        // remove header row
        procList.remove(0);
        // Fill list
        for (String proc : procList) {
            String[] split = ParseUtil.whitespaces.split(proc.trim(), 15);
            // Elements should match ps command order
            if (split.length < 15) {
                continue;
            }
            long now = System.currentTimeMillis();
            OSProcess sproc = new OSProcess(this);
            switch (split[0].charAt(0)) {
            case 'O':
                sproc.setState(OSProcess.State.RUNNING);
                break;
            case 'S':
                sproc.setState(OSProcess.State.SLEEPING);
                break;
            case 'R':
            case 'W':
                sproc.setState(OSProcess.State.WAITING);
                break;
            case 'Z':
                sproc.setState(OSProcess.State.ZOMBIE);
                break;
            case 'T':
                sproc.setState(OSProcess.State.STOPPED);
                break;
            default:
                sproc.setState(OSProcess.State.OTHER);
                break;
            }
            sproc.setProcessID(ParseUtil.parseIntOrDefault(split[1], 0));
            sproc.setParentProcessID(ParseUtil.parseIntOrDefault(split[2], 0));
            sproc.setUser(split[3]);
            sproc.setUserID(split[4]);
            sproc.setGroup(split[5]);
            sproc.setGroupID(split[6]);
            sproc.setThreadCount(ParseUtil.parseIntOrDefault(split[7], 0));
            sproc.setPriority(ParseUtil.parseIntOrDefault(split[8], 0));
            // These are in KB, multiply
            sproc.setVirtualSize(ParseUtil.parseLongOrDefault(split[9], 0) * 1024);
            sproc.setResidentSetSize(ParseUtil.parseLongOrDefault(split[10], 0) * 1024);
            // Avoid divide by zero for processes up less than a second
            long elapsedTime = ParseUtil.parseDHMSOrDefault(split[11], 0L);
            sproc.setUpTime(elapsedTime < 1L ? 1L : elapsedTime);
            sproc.setStartTime(now - sproc.getUpTime());
            sproc.setUserTime(ParseUtil.parseDHMSOrDefault(split[12], 0L));
            sproc.setPath(split[13]);
            sproc.setName(sproc.getPath().substring(sproc.getPath().lastIndexOf('/') + 1));
            sproc.setCommandLine(split[14]);
            sproc.setCurrentWorkingDirectory(cwdMap.getOrDefault(sproc.getProcessID(), ""));
            // bytes read/written not easily available

            if (slowFields) {
                List<String> openFilesList = ExecutingCommand.runNative(String.format("lsof -p %d", pid));
                sproc.setOpenFiles(openFilesList.size() - 1L);

                List<String> pflags = ExecutingCommand.runNative("pflags " + pid);
                for (String line : pflags) {
                    if (line.contains("data model")) {
                        if (line.contains("LP32")) {
                            sproc.setBitness(32);
                        } else if (line.contains("LP64")) {
                            sproc.setBitness(64);
                        }
                        break;
                    }
                }
            }
            procs.add(sproc);
        }
        return procs;
    }

    @Override
    public int getProcessId() {
        return SolarisLibc.INSTANCE.getpid();
    }

    @Override
    public int getProcessCount() {
        return ProcUtil.getPidFiles().length;
    }

    @Override
    public int getThreadCount() {
        List<String> threadList = ExecutingCommand.runNative("ps -eLo pid");
        if (!threadList.isEmpty()) {
            // Subtract 1 for header
            return threadList.size() - 1;
        }
        return getProcessCount();
    }

    @Override
    public long getSystemUptime() {
        return querySystemUptime();
    }

    private static long querySystemUptime() {
        KstatChain kc = KstatUtil.getChain();
        Kstat ksp = kc.lookup("unix", 0, "system_misc");
        long snaptime = 0L;
        if (ksp != null) {
            // Snap Time is in nanoseconds; divide for seconds
            snaptime = ksp.ks_snaptime / 1_000_000_000L;
        }
        kc.close();
        return snaptime;
    }

    @Override
    public long getSystemBootTime() {
        return BOOTTIME;
    }

    @Override
    public NetworkParams getNetworkParams() {
        return new SolarisNetworkParams();
    }

    @Override
    public OSService[] getServices() {
        // Get running services
        List<OSService> services = new ArrayList<>();
        Set<String> running = new HashSet<>();
        for (OSProcess p : getChildProcesses(1, 0, ProcessSort.PID)) {
            OSService s = new OSService(p.getName(), p.getProcessID(), RUNNING);
            ;
            services.add(s);
            running.add(p.getName());
        }
        // Get Directories for stopped services
        File dir = new File("/etc/inittab");
        if (dir.exists() && dir.isDirectory()) {
            for (File f : dir.listFiles((f, name) -> name.toLowerCase().endsWith(".conf"))) {
                // remove .conf extension
                String name = f.getName().substring(0, f.getName().length() - 5);
                int index = name.lastIndexOf('.');
                String shortName = (index < 0 && index < name.length()) ? name : name.substring(index + 1);
                if (!running.contains(name) && !running.contains(shortName)) {
                    OSService s = new OSService(name, 0, STOPPED);
                    services.add(s);
                }
            }
        } else {
            LOG.error("Directory: /etc/inittab does not exist");
        }
        return services.toArray(new OSService[0]);
    }
}
