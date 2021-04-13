/*
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
package oshi.driver.mac;

import static oshi.software.os.OSProcess.State.OTHER;
import static oshi.software.os.OSProcess.State.RUNNING;
import static oshi.software.os.OSProcess.State.SLEEPING;
import static oshi.software.os.OSProcess.State.STOPPED;
import static oshi.software.os.OSProcess.State.WAITING;
import static oshi.software.os.OSProcess.State.ZOMBIE;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSProcess.State;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Utility to query threads for a process
 */
@ThreadSafe
public final class ThreadInfo {

    private static final Pattern PS_M = Pattern.compile(
            "\\D+(\\d+).+(\\d+\\.\\d)\\s+(\\w)\\s+(\\d+)\\D+(\\d+:\\d{2}\\.\\d{2})\\s+(\\d+:\\d{2}\\.\\d{2}).+");

    private ThreadInfo() {
    }

    public static List<ThreadStats> queryTaskThreads(int pid) {
        String pidStr = " " + pid + " ";
        List<ThreadStats> taskThreads = new ArrayList<>();
        // Only way to get thread info without root permissions
        // Using the M switch gives all threads with no possibility to filter
        List<String> psThread = ExecutingCommand.runNative("ps -awwxM").stream().filter(s -> s.contains(pidStr))
                .collect(Collectors.toList());
        int tid = 0;
        for (String thread : psThread) {
            Matcher m = PS_M.matcher(thread);
            if (m.matches() && pid == ParseUtil.parseIntOrDefault(m.group(1), -1)) {
                double cpu = ParseUtil.parseDoubleOrDefault(m.group(2), 0d);
                char state = m.group(3).charAt(0);
                int pri = ParseUtil.parseIntOrDefault(m.group(4), 0);
                long sTime = ParseUtil.parseDHMSOrDefault(m.group(5), 0L);
                long uTime = ParseUtil.parseDHMSOrDefault(m.group(6), 0L);
                taskThreads.add(new ThreadStats(tid++, cpu, state, sTime, uTime, pri));
            }
        }
        return taskThreads;
    }

    /**
     * Class to encapsulate mach thread info
     */
    @Immutable
    public static class ThreadStats {
        private final int threadId;
        private final long userTime;
        private final long systemTime;
        private final long upTime;
        private final State state;
        private final int priority;

        public ThreadStats(int tid, double cpu, char state, long sTime, long uTime, int pri) {
            this.threadId = tid;
            this.userTime = uTime;
            this.systemTime = sTime;
            // user + system / uptime = cpu/100
            // so: uptime = user+system / cpu/100
            this.upTime = (long) ((uTime + sTime) / (cpu / 100d + 0.0005));
            switch (state) {
            case 'I':
            case 'S':
                this.state = SLEEPING;
                break;
            case 'U':
                this.state = WAITING;
                break;
            case 'R':
                this.state = RUNNING;
                break;
            case 'Z':
                this.state = ZOMBIE;
                break;
            case 'T':
                this.state = STOPPED;
                break;
            default:
                this.state = OTHER;
                break;
            }
            this.priority = pri;
        }

        /**
         * @return the threadId
         */
        public int getThreadId() {
            return threadId;
        }

        /**
         * @return the userTime
         */
        public long getUserTime() {
            return userTime;
        }

        /**
         * @return the systemTime
         */
        public long getSystemTime() {
            return systemTime;
        }

        /**
         * @return the upTime
         */
        public long getUpTime() {
            return upTime;
        }

        /**
         * @return the state
         */
        public State getState() {
            return state;
        }

        /**
         * @return the priority
         */
        public int getPriority() {
            return priority;
        }
    }
}
