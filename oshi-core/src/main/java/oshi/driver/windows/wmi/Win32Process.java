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
package oshi.driver.windows.wmi;

import java.util.Collection;
import java.util.Set;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery; //NOSONAR squid:S1191
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_Process}
 */
@ThreadSafe
public final class Win32Process {

    private static final String WIN32_PROCESS = "Win32_Process";

    /**
     * Process command lines.
     */
    public enum CommandLineProperty {
        PROCESSID, COMMANDLINE;
    }

    /**
     * Process properties accessible from WTSEnumerateProcesses in Vista+
     */
    public enum ProcessXPProperty {
        PROCESSID, NAME, KERNELMODETIME, USERMODETIME, THREADCOUNT, PAGEFILEUSAGE, HANDLECOUNT, EXECUTABLEPATH;
    }

    private Win32Process() {
    }

    /**
     * Returns process command lines
     *
     * @param pidsToQuery
     *            Process IDs to query for command lines. Pass {@code null} to query
     *            all processes.
     * @return A {@link WmiResult} containing process IDs and command lines used to
     *         start the provided processes.
     */
    public static WmiResult<CommandLineProperty> queryCommandLines(Set<Integer> pidsToQuery) {
        StringBuilder sb = new StringBuilder(WIN32_PROCESS);
        if (pidsToQuery != null) {
            boolean first = true;
            for (Integer pid : pidsToQuery) {
                if (first) {
                    sb.append(" WHERE ProcessID=");
                    first = false;
                } else {
                    sb.append(" OR ProcessID=");
                }
                sb.append(pid);
            }
        }
        WmiQuery<CommandLineProperty> commandLineQuery = new WmiQuery<>(sb.toString(), CommandLineProperty.class);
        return WmiQueryHandler.createInstance().queryWMI(commandLineQuery);
    }

    /**
     * Returns process info
     *
     * @param pids
     *            Process IDs to query.
     * @return Information on the provided processes.
     */
    public static WmiResult<ProcessXPProperty> queryProcesses(Collection<Integer> pids) {
        StringBuilder sb = new StringBuilder(WIN32_PROCESS);
        if (pids != null) {
            boolean first = true;
            for (Integer pid : pids) {
                if (first) {
                    sb.append(" WHERE ProcessID=");
                    first = false;
                } else {
                    sb.append(" OR ProcessID=");
                }
                sb.append(pid);
            }
        }
        WmiQuery<ProcessXPProperty> processQueryXP = new WmiQuery<>(sb.toString(), ProcessXPProperty.class);
        return WmiQueryHandler.createInstance().queryWMI(processQueryXP);
    }
}
