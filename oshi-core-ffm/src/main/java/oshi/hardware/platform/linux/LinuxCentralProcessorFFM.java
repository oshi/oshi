/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static oshi.ffm.ForeignFunctions.callInArenaIntOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.software.os.linux.LinuxOperatingSystemFFM.HAS_UDEV;
import static oshi.util.LogLevel.WARN;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.proc.AuxvFFM;
import oshi.ffm.NativeHandle;
import oshi.ffm.platform.linux.LinuxLibcFunctions;
import oshi.ffm.platform.linux.UdevFunctions;
import oshi.hardware.common.platform.linux.LinuxCentralProcessor;
import oshi.software.os.linux.LinuxOperatingSystemFFM;
import oshi.util.driver.linux.proc.Auxv;

/**
 * FFM-based Linux central processor implementation. Extends {@link LinuxCentralProcessor}, supplying the udev CPU
 * enumeration via {@link UdevFunctions} and native library calls.
 */
@ThreadSafe
public final class LinuxCentralProcessorFFM extends LinuxCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxCentralProcessorFFM.class);

    public LinuxCentralProcessorFFM() {
        super(LinuxOperatingSystemFFM.hz());
    }

    @Override
    protected List<String> enumerateCpuSyspathsViaUdev() {
        if (!HAS_UDEV) {
            return null;
        }
        return callInArenaOrDefault(arena -> {
            MemorySegment udev = UdevFunctions.udev_new();
            if (MemorySegment.NULL.equals(udev)) {
                return null;
            }
            List<String> syspaths = new ArrayList<>();
            // wrapped only to release the native handle on close
            try (var _ = NativeHandle.of(udev, UdevFunctions::udev_unref)) {
                MemorySegment enumerate = UdevFunctions.udev_enumerate_new(udev);
                // wrapped only to release the native handle on close
                try (var _ = NativeHandle.of(enumerate, UdevFunctions::udev_enumerate_unref)) {
                    UdevFunctions.addMatchSubsystem(enumerate, "cpu", arena);
                    UdevFunctions.udev_enumerate_scan_devices(enumerate);
                    for (MemorySegment entry = UdevFunctions
                            .udev_enumerate_get_list_entry(enumerate); !MemorySegment.NULL
                                    .equals(entry); entry = UdevFunctions.udev_list_entry_get_next(entry)) {
                        String syspath = UdevFunctions.getString(UdevFunctions.udev_list_entry_get_name(entry), arena);
                        if (syspath != null) {
                            syspaths.add(syspath);
                        }
                    }
                }
            }
            return syspaths;
        }, LOG, WARN, "Error enumerating CPUs via udev, falling back to sysfs", null);
    }

    @Override
    protected long queryHwcap() {
        return AuxvFFM.queryAuxv().getOrDefault(Auxv.AT_HWCAP, 0L);
    }

    @Override
    protected int getloadavgNative(double[] loadavg, int nelem) {
        return callInArenaIntOrDefault(arena -> {
            MemorySegment seg = arena.allocate(ValueLayout.JAVA_DOUBLE, nelem);
            int retval = LinuxLibcFunctions.getloadavg(seg, nelem);
            for (int i = 0; i < nelem && i < retval; i++) {
                loadavg[i] = seg.getAtIndex(ValueLayout.JAVA_DOUBLE, i);
            }
            return retval;
        }, LOG, WARN, "FFM getloadavg failed", -1);
    }
}
