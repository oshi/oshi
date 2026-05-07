/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.NOT_TOTAL_INSTANCES;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Thread performance counter enums
 */
@ThreadSafe
public final class ThreadInformation {

    /**
     * Thread performance counters
     */
    public enum ThreadPerformanceProperty implements PdhCounterWildcardProperty {
        /** Instance filter (not _Total). */
        NAME(NOT_TOTAL_INSTANCES),
        /** Percent user time. */
        PERCENTUSERTIME("% User Time"),
        /** Percent privileged time. */
        PERCENTPRIVILEGEDTIME("% Privileged Time"),
        /** Elapsed time. */
        ELAPSEDTIME("Elapsed Time"),
        /** Current priority. */
        PRIORITYCURRENT("Priority Current"),
        /** Start address. */
        STARTADDRESS("Start Address"),
        /** Thread state. */
        THREADSTATE("Thread State"),
        /** Thread wait reason. */
        THREADWAITREASON("Thread Wait Reason"),
        /** Process ID. */
        IDPROCESS("ID Process"),
        /** Thread ID. */
        IDTHREAD("ID Thread"),
        /** Context switches per second. */
        CONTEXTSWITCHESPERSEC("Context Switches/sec");

        private final String counter;

        ThreadPerformanceProperty(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    private ThreadInformation() {
    }
}
