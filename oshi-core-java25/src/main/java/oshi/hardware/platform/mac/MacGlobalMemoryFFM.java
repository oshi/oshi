package oshi.hardware.platform.mac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.mac.MacSystem;
import oshi.ffm.mac.MacSystemFunctions;
import oshi.hardware.VirtualMemory;
import oshi.util.platform.mac.SysctlUtilFFM;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.getErrno;
import static oshi.ffm.mac.MacSystem.VM_FREE_COUNT;
import static oshi.ffm.mac.MacSystem.VM_INACTIVE_COUNT;
import static oshi.ffm.mac.MacSystem.VM_STATISTICS;
import static oshi.ffm.mac.MacSystemFunctions.host_statistics;
import static oshi.ffm.mac.MacSystemFunctions.mach_host_self;

@ThreadSafe
final class MacGlobalMemoryFFM extends MacGlobalMemory {

    private static final Logger LOG = LoggerFactory.getLogger(MacGlobalMemoryFFM.class);

    @Override
    protected long queryVmStats() {
        try (Arena arena = Arena.ofConfined()) {
            // Allocate memory for VM statistics structure and count
            MemorySegment vmStats = arena.allocate(VM_STATISTICS);
            MemorySegment count = arena.allocate(JAVA_INT);
            MemorySegment callState = arena.allocate(CAPTURED_STATE_LAYOUT);

            // Set the count to the size of the VM statistics structure in integers
            count.set(JAVA_INT, 0, (int) (VM_STATISTICS.byteSize() / JAVA_INT.byteSize()));

            int result = host_statistics(callState, mach_host_self(), MacSystem.HOST_VM_INFO, vmStats, count);
            if (result != 0) {
                LOG.error("Failed to get host VM info. Error code: {}", getErrno(callState));
                return 0L;
            }
            // Read free_count and inactive_count from the structure
            int freeCount = vmStats.get(JAVA_INT, VM_STATISTICS.byteOffset(VM_FREE_COUNT));
            int inactiveCount = vmStats.get(JAVA_INT, VM_STATISTICS.byteOffset(VM_INACTIVE_COUNT));

            return (freeCount + inactiveCount) * getPageSize();
        }
    }

    @Override
    protected long sysctl(String name, long defaultValue) {
        return SysctlUtilFFM.sysctl(name, defaultValue);
    }

    @Override
    protected long host_page_size() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pageSize = arena.allocate(JAVA_LONG);
            int result = MacSystemFunctions.host_page_size(mach_host_self(), pageSize);
            if (result == 0) {
                return pageSize.get(JAVA_LONG, 0);
            }
        }
        return -1;
    }

    @Override
    protected VirtualMemory createVirtualMemory() {
        return new MacVirtualMemoryFFM(this);
    }
}
