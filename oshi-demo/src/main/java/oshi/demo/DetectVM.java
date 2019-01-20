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

import java.util.HashMap;
import java.util.Map;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

/**
 * Uses OSHI to attempt to identify whether the user is on a Virtual Machine
 */
public class DetectVM {

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

    public static void main(String[] args) {
        String vmString = identifyVM();

        if (vmString.isEmpty()) {
            System.out.println("You do not appear to be on a Virtual Machine.");
        } else {
            System.out.println("You appear to be on a VM: " + vmString);
        }
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
}
