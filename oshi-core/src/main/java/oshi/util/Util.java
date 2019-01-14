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
package oshi.util;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

/**
 * General utility methods
 *
 * @author widdis[at]gmail[dot]com
 */
public class Util {
    private static final Logger LOG = LoggerFactory.getLogger(Util.class);

    // Constant for Mac address OUI portion, the first 24 bits of MAC address
    // https://www.webopedia.com/TERM/O/OUI.html
    private static final Map<String, String> vmMacAddressOUI = new HashMap<>();
    static {
        vmMacAddressOUI.put("00:50:56", "VMware ESX 3");
        vmMacAddressOUI.put("00:0C:29", "VMware ESX 3");
        vmMacAddressOUI.put("00:05:69", "VMware ESX 3");
        vmMacAddressOUI.put("00:03:FF", "Microsoft Hyper-V");
        vmMacAddressOUI.put("00:1C:42", "Parallels Desktop");
        vmMacAddressOUI.put("00:0F:4B", "Virtual Iron 4");
        vmMacAddressOUI.put("00:16:3E", "Xen or Oracle VM");
        vmMacAddressOUI.put("08:00:27", "VirtualBox");
    }

    private static final String[] vmModelArray = new String[] { "Linux KVM", "Linux lguest", "OpenVZ", "Qemu",
            "Microsoft Virtual PC", "VMWare", "linux-vserver", "Xen", "FreeBSD Jail", "VirtualBox", "Parallels",
            "Linux Containers", "LXC" };

    private Util() {
    }

    /**
     * Sleeps for the specified number of milliseconds.
     *
     * @param ms
     *            How long to sleep
     */
    public static void sleep(long ms) {
        try {
            LOG.trace("Sleeping for {} ms", ms);
            Thread.sleep(ms);
        } catch (InterruptedException e) { // NOSONAR squid:S2142
            LOG.warn("Interrupted while sleeping for {} ms: {}", ms, e);
        }
    }

    /**
     * Sleeps for the specified number of milliseconds after the given system
     * time in milliseconds. If that number of milliseconds has already elapsed,
     * does nothing.
     *
     * @param startTime
     *            System time in milliseconds to sleep after
     * @param ms
     *            How long after startTime to sleep
     */
    public static void sleepAfter(long startTime, long ms) {
        long now = System.currentTimeMillis();
        long until = startTime + ms;
        LOG.trace("Sleeping until {}", until);
        if (now < until) {
            sleep(until - now);
        }
    }

    /**
     * Generates a Computer Identifier, which may be part of a strategy to
     * construct a licence key. (The identifier may not be unique as in one case
     * hashcode could be same for multiple values, and the result may differ
     * based on whether the program is running with sudo/root permission.) The
     * identifier string is based upon the processor serial number, vendor,
     * processor identifier, and total processor count.
     * 
     * @return A string containing four hyphen-delimited fields representing the
     *         processor; the first 3 are 32-bit hexadecimal values and the last
     *         one is an integer value.
     */
    public static String getComputerIdentifier() {
        SystemInfo systemInfo = new SystemInfo();
        OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
        HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();
        CentralProcessor centralProcessor = hardwareAbstractionLayer.getProcessor();
        ComputerSystem computerSystem = hardwareAbstractionLayer.getComputerSystem();

        String vendor = operatingSystem.getManufacturer();
        String processorSerialNumber = computerSystem.getSerialNumber();
        String processorIdentifier = centralProcessor.getIdentifier();
        int processors = centralProcessor.getLogicalProcessorCount();

        String delimiter = "-";

        return String.format("%08x", vendor.hashCode()) + delimiter
                + String.format("%08x", processorSerialNumber.hashCode()) + delimiter
                + String.format("%08x", processorIdentifier.hashCode()) + delimiter + processors;
    }

    /**
     * The function attempts to identify which Virtual Machine (VM) based on
     * common VM signatures in MAC address and computer model.
     * 
     * @return A string indicating the machine's virtualization info if it can
     *         be determined, or an emptry string otherwise.
     */
    public static String identifyVM() {

        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hw = si.getHardware();

        // Try well known MAC addresses
        NetworkIF[] nifs = hw.getNetworkIFs();

        for (NetworkIF nif : nifs) {
            String mac = nif.getMacaddr().substring(0, 8).toUpperCase();
            if (vmMacAddressOUI.containsKey(mac)) {
                return vmMacAddressOUI.get(mac);
            }
        }

        // Try well known models
        String model = hw.getComputerSystem().getModel();
        for (String vm : vmModelArray) {
            if (model.contains(vm)) {
                return vm;
            }
        }
        String manufacturer = hw.getComputerSystem().getManufacturer();
        if ("Microsoft Corporation".equals(manufacturer) && "Virtual Machine".equals(model)) {
            return "Microsoft Hyper-V";
        }

        // Couldn't find VM, return empty string
        return "";
    }

    /**
     * Tests if a String matches another String with a wildcard pattern.
     * 
     * @param text
     *            The String to test
     * @param pattern
     *            The String containing a wildcard pattern where ? represents a
     *            single character and * represents any number of characters. If
     *            the first character of the pattern is a carat (^) the test is
     *            performed against the remaining characters and the result of
     *            the test is the opposite.
     * @return True if the String matches or if the first character is ^ and the
     *         remainder of the String does not match.
     */
    public static boolean wildcardMatch(String text, String pattern) {
        if (pattern.length() > 0 && pattern.charAt(0) == '^') {
            return !wildcardMatch(text, pattern.substring(1));
        }
        return text.matches(pattern.replace("?", ".?").replace("*", ".*?"));
    }
}
