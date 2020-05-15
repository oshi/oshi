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
package oshi.software.os.mac;

import static oshi.software.os.OSService.State.RUNNING;
import static oshi.software.os.OSService.State.STOPPED;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.mac.SystemB; // NOSONAR squid:S1191
import com.sun.jna.platform.mac.SystemB.ProcTaskAllInfo;
import com.sun.jna.platform.mac.SystemB.ProcTaskInfo;
import com.sun.jna.platform.mac.SystemB.Timeval;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.mac.net.Who;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSProcess.State;
import oshi.software.os.OSService;
import oshi.software.os.OSSession;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.mac.SysctlUtil;

/**
 * macOS, previously Mac OS X and later OS X) is a series of proprietary
 * graphical operating systems developed and marketed by Apple Inc. since 2001.
 * It is the primary operating system for Apple's Mac computers.
 */
@ThreadSafe
public class MacOperatingSystem extends AbstractOperatingSystem {

    private static final Logger LOG = LoggerFactory.getLogger(MacOperatingSystem.class);

    private int maxProc = 1024;

    private final String osXVersion;
    private final int major;
    private final int minor;

    private static final long BOOTTIME;
    static {
        Timeval tv = new Timeval();
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

    /**
     * <p>
     * Constructor for MacOperatingSystem.
     * </p>
     */
    public MacOperatingSystem() {
        this.osXVersion = System.getProperty("os.version");
        this.major = ParseUtil.getFirstIntValue(this.osXVersion);
        this.minor = ParseUtil.getNthIntValue(this.osXVersion, 2);
        // Set max processes
        this.maxProc = SysctlUtil.sysctl("kern.maxproc", 0x1000);
    }

    @Override
    public String queryManufacturer() {
        return "Apple";
    }

    @Override
    public FamilyVersionInfo queryFamilyVersionInfo() {
        String family = this.major == 10 && this.minor >= 12 ? "macOS" : System.getProperty("os.name");
        String codeName = parseCodeName();
        String buildNumber = SysctlUtil.sysctl("kern.osversion", "");
        return new FamilyVersionInfo(family, new OSVersionInfo(this.osXVersion, codeName, buildNumber));
    }

    private String parseCodeName() {
        if (this.major == 10) {
            switch (this.minor) {
            // MacOS
            case 15:
                return "Catalina";
            case 14:
                return "Mojave";
            case 13:
                return "High Sierra";
            case 12:
                return "Sierra";
            // OS X
            case 11:
                return "El Capitan";
            case 10:
                return "Yosemite";
            case 9:
                return "Mavericks";
            case 8:
                return "Mountain Lion";
            case 7:
                return "Lion";
            case 6:
                return "Snow Leopard";
            case 5:
                return "Leopard";
            case 4:
                return "Tiger";
            case 3:
                return "Panther";
            case 2:
                return "Jaguar";
            case 1:
                return "Puma";
            case 0:
                return "Cheetah";
            default:
            }
        }
        LOG.warn("Unable to parse version {}.{} to a codename.", this.major, this.minor);
        return "";
    }

    @Override
    protected int queryBitness(int jvmBitness) {
        if (jvmBitness == 64 || (this.major == 10 && this.minor > 6)) {
            return 64;
        }
        return ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("getconf LONG_BIT"), 32);
    }

    @Override
    protected boolean queryElevated() {
        return System.getenv("SUDO_COMMAND") != null;
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
        return Collections.unmodifiableList(Who.queryUtxent());
    }

    @Override
    public List<OSProcess> getProcesses(int limit, ProcessSort sort) {
        List<OSProcess> procs = new ArrayList<>();
        int[] pids = new int[this.maxProc];
        int numberOfProcesses = SystemB.INSTANCE.proc_listpids(SystemB.PROC_ALL_PIDS, 0, pids,
                pids.length * SystemB.INT_SIZE) / SystemB.INT_SIZE;
        for (int i = 0; i < numberOfProcesses; i++) {
            // Handle off-by-one bug in proc_listpids where the size returned
            // is: SystemB.INT_SIZE * (pids + 1)
            if (pids[i] > 0) {
                OSProcess proc = getProcess(pids[i]);
                if (proc != null) {
                    procs.add(proc);
                }
            }
        }
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return Collections.unmodifiableList(sorted);
    }

    @Override
    public OSProcess getProcess(int pid) {
        OSProcess proc = new MacOSProcess(pid, this.minor);
        return proc.getState().equals(State.INVALID) ? null : proc;
    }

    @Override
    public List<OSProcess> getChildProcesses(int parentPid, int limit, ProcessSort sort) {
        List<OSProcess> procs = new ArrayList<>();
        int[] pids = new int[this.maxProc];
        int numberOfProcesses = SystemB.INSTANCE.proc_listpids(SystemB.PROC_ALL_PIDS, 0, pids,
                pids.length * SystemB.INT_SIZE) / SystemB.INT_SIZE;
        for (int i = 0; i < numberOfProcesses; i++) {
            // Handle off-by-one bug in proc_listpids where the size returned
            // is: SystemB.INT_SIZE * (pids + 1)
            if (pids[i] == 0) {
                continue;
            }
            if (parentPid == getParentProcessPid(pids[i])) {
                OSProcess proc = getProcess(pids[i]);
                if (proc != null) {
                    procs.add(proc);
                }
            }
        }
        List<OSProcess> sorted = processSort(procs, limit, sort);
        return Collections.unmodifiableList(sorted);
    }

    private static int getParentProcessPid(int pid) {
        ProcTaskAllInfo taskAllInfo = new ProcTaskAllInfo();
        if (0 > SystemB.INSTANCE.proc_pidinfo(pid, SystemB.PROC_PIDTASKALLINFO, 0, taskAllInfo, taskAllInfo.size())) {
            return 0;
        }
        return taskAllInfo.pbsd.pbi_ppid;
    }

    @Override
    public int getProcessId() {
        return SystemB.INSTANCE.getpid();
    }

    @Override
    public int getProcessCount() {
        return SystemB.INSTANCE.proc_listpids(SystemB.PROC_ALL_PIDS, 0, null, 0) / SystemB.INT_SIZE;
    }

    @Override
    public int getThreadCount() {
        // Get current pids, then slightly pad in case new process starts while
        // allocating array space
        int[] pids = new int[getProcessCount() + 10];
        int numberOfProcesses = SystemB.INSTANCE.proc_listpids(SystemB.PROC_ALL_PIDS, 0, pids, pids.length)
                / SystemB.INT_SIZE;
        int numberOfThreads = 0;
        ProcTaskInfo taskInfo = new ProcTaskInfo();
        for (int i = 0; i < numberOfProcesses; i++) {
            SystemB.INSTANCE.proc_pidinfo(pids[i], SystemB.PROC_PIDTASKINFO, 0, taskInfo, taskInfo.size());
            numberOfThreads += taskInfo.pti_threadnum;
        }
        return numberOfThreads;
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
    public OSService[] getServices() {
        // Get running services
        List<OSService> services = new ArrayList<>();
        Set<String> running = new HashSet<>();
        for (OSProcess p : getChildProcesses(1, 0, ProcessSort.PID)) {
            OSService s = new OSService(p.getName(), p.getProcessID(), RUNNING);
            services.add(s);
            running.add(p.getName());
        }
        // Get Directories for stopped services
        ArrayList<File> files = new ArrayList<>();
        File dir = new File("/System/Library/LaunchAgents");
        if (dir.exists() && dir.isDirectory()) {
            files.addAll(Arrays.asList(dir.listFiles((f, name) -> name.toLowerCase().endsWith(".plist"))));
        } else {
            LOG.error("Directory: /System/Library/LaunchAgents does not exist");
        }
        dir = new File("/System/Library/LaunchDaemons");
        if (dir.exists() && dir.isDirectory()) {
            files.addAll(Arrays.asList(dir.listFiles((f, name) -> name.toLowerCase().endsWith(".plist"))));
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
        return services.toArray(new OSService[0]);
    }
}
