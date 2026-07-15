/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static oshi.software.os.linux.LinuxOperatingSystemJNA.HAS_UDEV;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.linux.Udev;
import com.sun.jna.platform.linux.Udev.UdevContext;
import com.sun.jna.platform.linux.Udev.UdevEnumerate;
import com.sun.jna.platform.linux.Udev.UdevListEntry;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.proc.AuxvJNA;
import oshi.hardware.common.platform.linux.LinuxCentralProcessor;
import oshi.jna.platform.linux.LinuxLibc;
import oshi.software.os.linux.LinuxOperatingSystemJNA;
import oshi.util.driver.linux.proc.Auxv;

/**
 * JNA-based Linux central processor implementation. Extends {@link LinuxCentralProcessor}, supplying the udev CPU
 * enumeration and native library calls.
 */
@ThreadSafe
final class LinuxCentralProcessorJNA extends LinuxCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxCentralProcessorJNA.class);

    LinuxCentralProcessorJNA() {
        super(LinuxOperatingSystemJNA.hz());
    }

    @Override
    protected List<String> enumerateCpuSyspathsViaUdev() {
        if (!HAS_UDEV) {
            return null;
        }
        UdevContext udev = Udev.INSTANCE.udev_new();
        if (udev == null) {
            return null;
        }
        try {
            List<String> syspaths = new ArrayList<>();
            UdevEnumerate enumerate = udev.enumerateNew();
            if (enumerate == null) {
                return null;
            }
            try {
                enumerate.addMatchSubsystem("cpu");
                enumerate.scanDevices();
                for (UdevListEntry entry = enumerate.getListEntry(); entry != null; entry = entry.getNext()) {
                    syspaths.add(entry.getName());
                }
            } finally {
                enumerate.unref();
            }
            return syspaths;
        } catch (RuntimeException e) {
            // Any udev failure falls back to the shared sysfs scan
            LOG.warn("Error enumerating CPUs via udev, falling back to sysfs", e);
            return null;
        } finally {
            udev.unref();
        }
    }

    @Override
    protected long queryHwcap() {
        return AuxvJNA.queryAuxv().getOrDefault(Auxv.AT_HWCAP, 0L);
    }

    @Override
    protected int getloadavgNative(double[] loadavg, int nelem) {
        return LinuxLibc.INSTANCE.getloadavg(loadavg, nelem);
    }
}
