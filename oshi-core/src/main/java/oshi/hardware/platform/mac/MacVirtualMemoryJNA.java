package oshi.hardware.platform.mac;

import com.sun.jna.Native;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef.CloseableIntByReference;
import oshi.jna.Struct.CloseableVMStatistics;
import oshi.jna.platform.mac.SystemB;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

@ThreadSafe
final class MacVirtualMemoryJNA extends MacVirtualMemory {
    private static final Logger LOG = LoggerFactory.getLogger(MacVirtualMemoryJNA.class);

    MacVirtualMemoryJNA(MacGlobalMemoryJNA macGlobalMemory) {
        super(macGlobalMemory);
    }

    @Override
    protected Pair<Long, Long> queryVmStat() {
        long swapPagesIn = 0L;
        long swapPagesOut = 0L;
        try (CloseableVMStatistics vmStats = new CloseableVMStatistics();
             CloseableIntByReference size = new CloseableIntByReference(vmStats.size() / SystemB.INT_SIZE)) {
            if (0 == SystemB.INSTANCE.host_statistics(SystemB.INSTANCE.mach_host_self(), SystemB.HOST_VM_INFO, vmStats,
                size)) {
                swapPagesIn = ParseUtil.unsignedIntToLong(vmStats.pageins);
                swapPagesOut = ParseUtil.unsignedIntToLong(vmStats.pageouts);
            } else {
                LOG.error("Failed to get host VM info. Error code: {}", Native.getLastError());
            }
        }
        return new Pair<>(swapPagesIn, swapPagesOut);
    }

}
