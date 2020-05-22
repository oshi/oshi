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
package oshi.software.os.windows;

import static oshi.util.Memoizer.memoize;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util.Account;
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTRByReference;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;
import com.sun.jna.ptr.IntByReference;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.registry.ProcessPerformanceData.PerfCounterBlock;
import oshi.driver.windows.registry.ProcessWtsData.WtsInfo;
import oshi.driver.windows.wmi.Win32Process;
import oshi.driver.windows.wmi.Win32Process.CommandLineProperty;
import oshi.driver.windows.wmi.Win32ProcessCached;
import oshi.jna.platform.windows.Advapi32Util;
import oshi.jna.platform.windows.Kernel32;
import oshi.software.common.AbstractOSProcess;
import oshi.util.Constants;
import oshi.util.GlobalConfig;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Pair;

@ThreadSafe
public class WindowsOSProcess extends AbstractOSProcess {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOSProcess.class);

    // Config param to enable cache
    public static final String OSHI_OS_WINDOWS_COMMANDLINE_BATCH = "oshi.os.windows.commandline.batch";
    private static final boolean USE_BATCH_COMMANDLINE = GlobalConfig.get(OSHI_OS_WINDOWS_COMMANDLINE_BATCH, false);

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();
    private static final boolean IS_WINDOWS7_OR_GREATER = VersionHelpers.IsWindows7OrGreater();

    private Supplier<Pair<String, String>> userInfo = memoize(this::queryUserInfo);
    private Supplier<Pair<String, String>> groupInfo = memoize(this::queryGroupInfo);
    private Supplier<String> commandLine = memoize(this::queryCommandLine);

    private String name;
    private String path;
    private String currentWorkingDirectory;
    private State state = State.INVALID;
    private int parentProcessID;
    private int threadCount;
    private int priority;
    private long virtualSize;
    private long residentSetSize;
    private long kernelTime;
    private long userTime;
    private long startTime;
    private long upTime;
    private long bytesRead;
    private long bytesWritten;
    private long openFiles;
    private int bitness;

    public WindowsOSProcess(int pid, int myPid, int osBitness, Map<Integer, PerfCounterBlock> processMap,
            Map<Integer, WtsInfo> processWtsMap) {
        super(pid);
        // For executing process, set CWD
        if (pid == myPid) {
            String cwd = new File(".").getAbsolutePath();
            // trim off trailing "."
            this.currentWorkingDirectory = cwd.isEmpty() ? "" : cwd.substring(0, cwd.length() - 1);
        }
        // There is no easy way to get ExecutuionState for a process.
        // The WMI value is null. It's possible to get thread Execution
        // State and possibly roll up.
        this.state = State.RUNNING;
        // Initially set to match OS bitness. If 64 will check later for 32-bit process
        this.bitness = osBitness;
        updateAttributes(processMap.get(pid), processWtsMap.get(pid));
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public String getCommandLine() {
        return this.commandLine.get();
    }

    @Override
    public String getCurrentWorkingDirectory() {
        return this.currentWorkingDirectory;
    }

    @Override
    public String getUser() {
        return userInfo.get().getA();
    }

    @Override
    public String getUserID() {
        return userInfo.get().getB();
    }

    @Override
    public String getGroup() {
        return groupInfo.get().getA();
    }

    @Override
    public String getGroupID() {
        return groupInfo.get().getB();
    }

    @Override
    public State getState() {
        return this.state;
    }

    @Override
    public int getParentProcessID() {
        return this.parentProcessID;
    }

    @Override
    public int getThreadCount() {
        return this.threadCount;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public long getVirtualSize() {
        return this.virtualSize;
    }

    @Override
    public long getResidentSetSize() {
        return this.residentSetSize;
    }

    @Override
    public long getKernelTime() {
        return this.kernelTime;
    }

    @Override
    public long getUserTime() {
        return this.userTime;
    }

    @Override
    public long getUpTime() {
        return this.upTime;
    }

    @Override
    public long getStartTime() {
        return this.startTime;
    }

    @Override
    public long getBytesRead() {
        return this.bytesRead;
    }

    @Override
    public long getBytesWritten() {
        return this.bytesWritten;
    }

    @Override
    public long getOpenFiles() {
        return this.openFiles;
    }

    @Override
    public int getBitness() {
        return this.bitness;
    }

    @Override
    public long getAffinityMask() {
        final HANDLE pHandle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION, false, getProcessID());
        if (pHandle != null) {
            ULONG_PTRByReference processAffinity = new ULONG_PTRByReference();
            ULONG_PTRByReference systemAffinity = new ULONG_PTRByReference();
            if (Kernel32.INSTANCE.GetProcessAffinityMask(pHandle, processAffinity, systemAffinity)) {
                return Pointer.nativeValue(processAffinity.getValue().toPointer());
            }
        }
        return 0L;
    }

    @Override
    public boolean updateAttributes() {
        return true;
    }

    private boolean updateAttributes(PerfCounterBlock pcb, WtsInfo wts) {
        this.name = pcb.getName();
        this.path = wts.getPath(); // Empty string for Win7+
        this.parentProcessID = pcb.getParentProcessID();
        this.threadCount = wts.getThreadCount();
        this.priority = pcb.getPriority();
        this.virtualSize = wts.getVirtualSize();
        this.residentSetSize = pcb.getResidentSetSize();
        this.kernelTime = wts.getKernelTime();
        this.userTime = wts.getUserTime();
        this.startTime = pcb.getStartTime();
        this.upTime = pcb.getUpTime();
        this.bytesRead = pcb.getBytesRead();
        this.bytesWritten = pcb.getBytesWritten();
        this.openFiles = wts.getOpenFiles();

        // Get a handle to the process for various extended info. Only gets
        // current user unless running as administrator
        final HANDLE pHandle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION, false, getProcessID());
        if (pHandle != null) {
            // Test for 32-bit process on 64-bit windows
            if (IS_VISTA_OR_GREATER && this.bitness == 64) {
                IntByReference wow64 = new IntByReference(0);
                if (Kernel32.INSTANCE.IsWow64Process(pHandle, wow64) && wow64.getValue() > 0) {
                    this.bitness = 32;
                }
            }
            // Full path
            final HANDLEByReference phToken = new HANDLEByReference();
            try { // EXECUTABLEPATH
                if (IS_WINDOWS7_OR_GREATER) {
                    this.path = Kernel32Util.QueryFullProcessImageName(pHandle, 0);
                }
            } catch (Win32Exception e) {
                this.state = State.INVALID;
            } finally {
                final HANDLE token = phToken.getValue();
                if (token != null) {
                    Kernel32.INSTANCE.CloseHandle(token);
                }
            }
            Kernel32.INSTANCE.CloseHandle(pHandle);
        }

        return !this.state.equals(State.INVALID);
    }

    private String queryCommandLine() {
        // If using batch mode fetch from WMI Cache
        if (USE_BATCH_COMMANDLINE) {
            return Win32ProcessCached.getInstance().getCommandLine(getProcessID(), getStartTime());
        }
        // If no cache enabled, query line by line
        WmiResult<CommandLineProperty> commandLineProcs = Win32Process
                .queryCommandLines(Collections.singleton(getProcessID()));
        if (commandLineProcs.getResultCount() > 0) {
            return WmiUtil.getString(commandLineProcs, CommandLineProperty.COMMANDLINE, 0);
        }
        return Constants.UNKNOWN;
    }

    private Pair<String, String> queryUserInfo() {
        Pair<String, String> pair = null;
        final HANDLE pHandle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION, false, getProcessID());
        if (pHandle != null) {
            final HANDLEByReference phToken = new HANDLEByReference();
            if (Advapi32.INSTANCE.OpenProcessToken(pHandle, WinNT.TOKEN_DUPLICATE | WinNT.TOKEN_QUERY, phToken)) {
                Account account = Advapi32Util.getTokenAccount(phToken.getValue());
                pair = new Pair<>(account.name, account.sidString);
            } else {
                int error = Kernel32.INSTANCE.GetLastError();
                // Access denied errors are common. Fail silently.
                if (error != WinError.ERROR_ACCESS_DENIED) {
                    LOG.error("Failed to get process token for process {}: {}", getProcessID(),
                            Kernel32.INSTANCE.GetLastError());
                }
            }
            final HANDLE token = phToken.getValue();
            if (token != null) {
                Kernel32.INSTANCE.CloseHandle(token);
            }
            Kernel32.INSTANCE.CloseHandle(pHandle);
        }
        if (pair == null) {
            return new Pair<>(Constants.UNKNOWN, Constants.UNKNOWN);
        }
        return pair;
    }

    private Pair<String, String> queryGroupInfo() {
        Pair<String, String> pair = null;
        final HANDLE pHandle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_INFORMATION, false, getProcessID());
        if (pHandle != null) {
            final HANDLEByReference phToken = new HANDLEByReference();
            if (Advapi32.INSTANCE.OpenProcessToken(pHandle, WinNT.TOKEN_DUPLICATE | WinNT.TOKEN_QUERY, phToken)) {
                Account account = Advapi32Util.getTokenPrimaryGroup(phToken.getValue());
                pair = new Pair<>(account.name, account.sidString);
            } else {
                int error = Kernel32.INSTANCE.GetLastError();
                // Access denied errors are common. Fail silently.
                if (error != WinError.ERROR_ACCESS_DENIED) {
                    LOG.error("Failed to get process token for process {}: {}", getProcessID(),
                            Kernel32.INSTANCE.GetLastError());
                }
            }
            final HANDLE token = phToken.getValue();
            if (token != null) {
                Kernel32.INSTANCE.CloseHandle(token);
            }
            Kernel32.INSTANCE.CloseHandle(pHandle);
        }
        if (pair == null) {
            return new Pair<>(Constants.UNKNOWN, Constants.UNKNOWN);
        }
        return pair;
    }
}
