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
package oshi.demo;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.util.Constants;

/**
 * Attempts to create a unique computer identifier. Note that serial numbers
 * won't work on Linux without user cooperation.
 */
public class ComputerID {
    public static void main(String[] args) {
        String unknownHash = String.format("%08x", Constants.UNKNOWN.hashCode());

        System.out.println("Here's a unique (?) id for your computer.");
        System.out.println(getComputerIdentifier());
        System.out.println("If the second field is " + unknownHash
                + " then I couldn't find a serial number, and running as sudo might change this.");

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
}
