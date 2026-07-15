/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.mac.SystemB;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.platform.mac.IOKitProvider;
import oshi.hardware.common.platform.mac.MacCentralProcessor;
import oshi.hardware.common.platform.mac.SysctlProvider;
import oshi.jna.ByRef.CloseableIntByReference;
import oshi.jna.ByRef.CloseablePointerByReference;
import oshi.jna.Struct.CloseableHostCpuLoadInfo;

/**
 * A CPU using JNA.
 */
@ThreadSafe
final class MacCentralProcessorJNA extends MacCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MacCentralProcessorJNA.class);

    @Override
    protected SysctlProvider sysctlProvider() {
        return SysctlProviderJNA.INSTANCE;
    }

    @Override
    protected int[] queryHostCpuLoadTicks() {
        int machPort = SystemB.INSTANCE.mach_host_self();
        try (CloseableHostCpuLoadInfo cpuLoadInfo = new CloseableHostCpuLoadInfo();
                CloseableIntByReference size = new CloseableIntByReference(cpuLoadInfo.size())) {
            int ret = SystemB.INSTANCE.host_statistics(machPort, SystemB.HOST_CPU_LOAD_INFO, cpuLoadInfo, size);
            if (0 != ret) {
                LOG.error("Failed to get System CPU ticks. Error code: {} ", ret);
                return new int[0];
            }
            return cpuLoadInfo.cpu_ticks;
        }
    }

    @Override
    protected int getloadavgNative(double[] loadavg, int nelem) {
        return SystemB.INSTANCE.getloadavg(loadavg, nelem);
    }

    @Override
    protected IOKitProvider ioKitProvider() {
        return IOKitProviderJNA.INSTANCE;
    }

    @Override
    protected int[] queryProcessorCpuTicks() {
        int machPort = SystemB.INSTANCE.mach_host_self();
        try (CloseableIntByReference procCount = new CloseableIntByReference();
                CloseablePointerByReference procCpuLoadInfo = new CloseablePointerByReference();
                CloseableIntByReference procInfoCount = new CloseableIntByReference()) {
            int ret = SystemB.INSTANCE.host_processor_info(machPort, SystemB.PROCESSOR_CPU_LOAD_INFO, procCount,
                    procCpuLoadInfo, procInfoCount);
            if (0 != ret) {
                LOG.error("Failed to update CPU Load. Error code: {}", ret);
                return new int[0];
            }
            try {
                return procCpuLoadInfo.getValue().getIntArray(0, procInfoCount.getValue());
            } finally {
                try {
                    SystemB.INSTANCE.vm_deallocate(SystemB.INSTANCE.mach_task_self(),
                            com.sun.jna.Pointer.nativeValue(procCpuLoadInfo.getValue()),
                            (long) procInfoCount.getValue() * SystemB.INT_SIZE);
                } catch (Exception e) {
                    LOG.warn("Failed to vm_deallocate processor info buffer", e);
                }
            }
        }
    }

}
