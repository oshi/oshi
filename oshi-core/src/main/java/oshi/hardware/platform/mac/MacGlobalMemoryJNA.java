package oshi.hardware.platform.mac;

import com.sun.jna.Native;
import com.sun.jna.platform.mac.SystemB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.Struct.CloseableVMStatistics;
import oshi.jna.ByRef.CloseableIntByReference;


@ThreadSafe
final class MacGlobalMemoryJNA extends MacGlobalMemory {

    private static final Logger LOG = LoggerFactory.getLogger(MacGlobalMemoryJNA.class);

    @Override
    protected long queryVmStats() {
        try (CloseableVMStatistics vmStats = new CloseableVMStatistics();
             CloseableIntByReference size = new CloseableIntByReference(vmStats.size() / SystemB.INT_SIZE)) {
            if (0 != SystemB.INSTANCE.host_statistics(SystemB.INSTANCE.mach_host_self(), SystemB.HOST_VM_INFO, vmStats,
                size)) {
                LOG.error("Failed to get host VM info. Error code: {}", Native.getLastError());
                return 0L;
            }
            return (vmStats.free_count + vmStats.inactive_count) * getPageSize();
        }
    }

}
