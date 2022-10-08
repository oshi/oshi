/*
 * Copyright 2019-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.util.FileUtil;

/**
 * Uses OSHI to attempt to identify whether the user is on a Virtual Machine
 */
public class DetectVM {

    private static final String OSHI_VM_MAC_ADDR_PROPERTIES = "oshi.vmmacaddr.properties";
    private static final Properties vmMacAddressProps = FileUtil
            .readPropertiesFromFilename(OSHI_VM_MAC_ADDR_PROPERTIES);

    // Constant for CPU vendor string
    private static final Map<String, String> vmVendor = new HashMap<>();
    static {
        vmVendor.put("bhyve bhyve", "bhyve");
        vmVendor.put("KVMKVMKVM", "KVM");
        vmVendor.put("TCGTCGTCGTCG", "QEMU");
        vmVendor.put("Microsoft Hv", "Microsoft Hyper-V or Windows Virtual PC");
        vmVendor.put("lrpepyh vr", "Parallels");// (endianness mismatch of "prl hyperv ")
        vmVendor.put("VMwareVMware", "VMware");
        vmVendor.put("XenVMMXenVMM", "Xen HVM");
        vmVendor.put("ACRNACRNACRN", "Project ACRN");
        vmVendor.put("QNXQVMBSQG", "QNX Hypervisor");
    }

    private static final String[] vmModelArray = new String[] { "Linux KVM", "Linux lguest", "OpenVZ", "Qemu",
            "Microsoft Virtual PC", "VMWare", "linux-vserver", "Xen", "FreeBSD Jail", "VirtualBox", "Parallels",
            "Linux Containers", "LXC" };

    /**
     * The main method, executing the {@link #identifyVM} method.
     *
     * @param args Arguments, ignored.
     */
    public static void main(String[] args) {
        String vmString = identifyVM();

        if (vmString.isEmpty()) {
            System.out.println("You do not appear to be on a Virtual Machine.");
        } else {
            System.out.println("You appear to be on a VM: " + vmString);
        }
    }

    /**
     * The function attempts to identify which Virtual Machine (VM) based on common VM signatures in MAC address and
     * computer model.
     *
     * @return A string indicating the machine's virtualization info if it can be determined, or an emptry string
     *         otherwise.
     */
    public static String identifyVM() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hw = si.getHardware();
        // Check CPU Vendor
        String vendor = hw.getProcessor().getProcessorIdentifier().getVendor().trim();
        if (vmVendor.containsKey(vendor)) {
            return vmVendor.get(vendor);
        }

        // Try well known MAC addresses
        List<NetworkIF> nifs = hw.getNetworkIFs();
        for (NetworkIF nif : nifs) {
            String mac = nif.getMacaddr().toUpperCase();
            String oui = mac.length() > 7 ? mac.substring(0, 8) : mac;
            if (vmMacAddressProps.containsKey(oui)) {
                return vmMacAddressProps.getProperty(oui);
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
