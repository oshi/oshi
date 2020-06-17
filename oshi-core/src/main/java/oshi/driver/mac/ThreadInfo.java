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

import java.util.Arrays;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.mac.SystemB;
import oshi.jna.platform.mac.SystemB.ThreadBasicInfo;

/**
 * Utility to query threads for a process
 */
@ThreadSafe
public final class ThreadInfo {

    private static final SystemB SYS = SystemB.INSTANCE;

    private static final int TH_STATE_RUNNING = 1;
    private static final int TH_STATE_STOPPED = 2;
    private static final int TH_STATE_WAITING = 3;
    private static final int TH_STATE_UNINTERRUPTIBLE = 4; // uninterruptible wait
    private static final int TH_STATE_HALTED = 5;

    private static final int TH_FLAGS_SWAPPED = 0x1;
    private static final int TH_FLAGS_IDLE = 0x2;

    private ThreadInfo() {
    }

    // TODO: Return a list of thread stats
    public static void queryTaskThreads(int pid) {
        // TODO: New list here
        IntByReference port = new IntByReference();
        if (0 == SYS.task_for_pid(SYS.mach_task_self(), pid, port)) {
            System.out.println("Thread port:" + port.getValue());
            int task = port.getValue();
            PointerByReference threadList = new PointerByReference();
            IntByReference threadCount = new IntByReference();
            if (0 != SYS.task_threads(task, threadList, threadCount)) {
                int count = threadCount.getValue();
                System.out.println("Thread count:" + count);
                try {
                    int[] threads = threadList.getValue().getIntArray(0, count);
                    System.out.println("Thread ports: " + Arrays.toString(threads));
                    for (int thread : threads) {
                        IntByReference infoCount = new IntByReference();
                        Pointer p = null;
                        if (0 == SYS.thread_info(thread, SystemB.THREAD_BASIC_INFO, p, infoCount)) {
                            ThreadBasicInfo threadInfo = new ThreadBasicInfo(p);
                            // User and system time have second+microsecond resolution
                            System.out.println("  User secs: " + (threadInfo.user_time.tv_sec.intValue()
                                    + threadInfo.user_time.tv_usec / 1_000_000d));
                            System.out.println("System secs: " + (threadInfo.system_time.tv_sec.intValue()
                                    + threadInfo.system_time.tv_usec / 1_000_000d));
                            // CPU usage is an int scaled percent, divide by 100 or 256 (?) for a fraction
                            // Flags are swapped/idle, not needed.
                            // Some code does not include a thread in this list if state is IDLE, perhaps
                            // the stats aren't updated?
                            // sleep_time in seconds, may need to compare CPU usage vs. this for best
                            // estimate
                            // run_state constants TH_STATE_x above
                            // suspend_count might be context switches?
                            System.out.println("Thread " + thread + ": " + threadInfo.toString());
                            // TODO: Add to list here with the stats we want
                        }
                    }
                } finally {
                    SYS.vm_deallocate(task, threadList.getValue(), new NativeLong(count * Integer.SIZE));
                }
            }
        }
        // TODO Return list here
    }
}
