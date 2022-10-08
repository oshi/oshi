/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo;

import com.sun.jna.platform.win32.COM.COMException;

import oshi.driver.windows.wmi.Win32OperatingSystem;
import oshi.util.platform.windows.WmiQueryHandler;

import java.util.Objects;

/**
 * Class demonstrating WMI stat performance improvements if the user does COM initialization so OSHI doesn't have to
 */
public class UserComInit {

    private static final int REPETITIONS = 300;

    public static void main(String[] args) {
        wakeUpCom();
        System.out.println("Collecting statistics with default COM setup");
        loopWmiQueries();
        System.out.println("Collecting statistics with user-init COM setup");
        loopWmiQueriesWithUserCom();
    }

    private static void wakeUpCom() {
        // The first COM query can take extra long so just do one separately
        Win32OperatingSystem.queryOsVersion();
    }

    private static void loopWmiQueries() {
        long t = System.nanoTime();
        for (int i = 0; i < REPETITIONS; i++) {
            Win32OperatingSystem.queryOsVersion();
        }
        t = System.nanoTime() - t;
        System.out.println("Average ms per rep: " + t / (1_000_000d * REPETITIONS));
    }

    private static void loopWmiQueriesWithUserCom() {
        // Create instance using existing WmiQueryHandler class for convenience, only to
        // be used for COM init/uninit. Not needed if user initializes COM.
        WmiQueryHandler handlerForSingleCOMinit = Objects.requireNonNull(WmiQueryHandler.createInstance());

        boolean singleComInit = false;
        try {
            // Initialize using the default query handler. This is unnecessary in a user
            // application if the application controls COM initialization elsewhere.
            singleComInit = handlerForSingleCOMinit.initCOM();

            // Change query handler class to not initialize COM
            WmiQueryHandler.setInstanceClass(WmiNoComInitQueryHandler.class);

            loopWmiQueries();
        } catch (COMException e) {
            // Ignore for demo. Production code should handle this!
        } finally {
            // User should ununitialize COM in their own application
            if (singleComInit) {
                handlerForSingleCOMinit.unInitCOM();
            }
        }
    }
}
