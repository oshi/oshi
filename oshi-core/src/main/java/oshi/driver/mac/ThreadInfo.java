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
package oshi.driver.mac;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.NativeLong; // NOSONAR squid:S1191
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.mac.SystemB;
import oshi.jna.platform.mac.SystemB.ThreadBasicInfo;
import oshi.software.os.OSProcess.State;

/**
 * Utility to query threads for a process
 */
@ThreadSafe
public final class ThreadInfo {

    private static final SystemB SYS = SystemB.INSTANCE;

    private static final int TH_STATE_RUNNING = 1;
    private static final int TH_STATE_STOPPED = 2;
    private static final int TH_STATE_WAITING = 3;
    private static final int TH_STATE_UNINTERRUPTIBLE = 4;
    private static final int TH_STATE_HALTED = 5;

    private ThreadInfo() {
    }

    public static List<ThreadStats> queryTaskThreads(int pid) {
        List<ThreadStats> taskThreads = new ArrayList<>();
        IntByReference port = new IntByReference();
        if (0 == SYS.task_for_pid(SYS.mach_task_self(), pid, port)) {
            int task = port.getValue();
            PointerByReference threadList = new PointerByReference();
            IntByReference threadCount = new IntByReference();
            if (0 == SYS.task_threads(task, threadList, threadCount)) {
                int count = threadCount.getValue();
                try {
                    int[] threads = threadList.getValue().getIntArray(0, count);
                    ThreadBasicInfo threadInfo = new ThreadBasicInfo();
                    IntByReference infoCount = new IntByReference(threadInfo.size() / 4);
                    for (int thread : threads) {
                        if (0 == SYS.thread_info(thread, SystemB.THREAD_BASIC_INFO, threadInfo.getPointer(),
                                infoCount)) {
                            threadInfo.read();
                            taskThreads.add(new ThreadStats(thread, threadInfo.user_time, threadInfo.system_time,
                                    threadInfo.cpu_usage, threadInfo.run_state));
                        }
                    }
                } finally {
                    SYS.vm_deallocate(task, threadList.getValue(), new NativeLong((long) count * Integer.SIZE));
                }
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

        public ThreadStats(int tid, SystemB.TimeValue userTime, SystemB.TimeValue systemTime, int cpuUsage,
                int runState) {
            this.threadId = tid;
            // Start out with microsecond precision
            long uTime = userTime.seconds * 1_000_000L + userTime.microseconds;
            long sTime = systemTime.seconds * 1_000_000L + systemTime.microseconds;
            long microsecs = uTime + sTime;
            this.userTime = uTime / 1000L;
            this.systemTime = sTime / 1000L;
            // cpuUsage is cpu load times 1000
            // user + system / uptime * 1000 = cpuUsage
            // ... uptime (usecs) = user+system (usecs) * 1000 / cpuUsage
            // ... uptime (ms) = user+system (usecs) / cpuUsage
            this.upTime = (long) (microsecs / (cpuUsage + 0.5));
            if (runState == TH_STATE_RUNNING) {
                this.state = State.RUNNING;
            } else if (runState == TH_STATE_WAITING || runState == TH_STATE_UNINTERRUPTIBLE) {
                this.state = State.WAITING;
            } else if (runState == TH_STATE_STOPPED || runState == TH_STATE_HALTED) {
                this.state = State.STOPPED;
            } else {
                this.state = State.OTHER;
            }
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
    }
}
