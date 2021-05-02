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
package oshi.demo;

import com.sun.jna.platform.win32.COM.COMException; // NOSONAR squid:S1191

import oshi.driver.windows.wmi.Win32OperatingSystem;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Class demonstrating WMI stat performance improvements if the user does COM
 * initialization so OSHI doesn't have to
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
        WmiQueryHandler handlerForSingleCOMinit = WmiQueryHandler.createInstance();

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
