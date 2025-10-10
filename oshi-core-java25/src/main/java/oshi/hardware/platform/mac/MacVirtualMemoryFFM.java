package oshi.hardware.platform.mac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.mac.MacSystem;
import oshi.util.ParseUtil;
import oshi.util.platform.mac.SysctlUtilFFM;
import oshi.util.tuples.Pair;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.ForeignFunctions.CAPTURED_STATE_LAYOUT;
import static oshi.ffm.ForeignFunctions.getErrno;
import static oshi.ffm.mac.MacSystem.VM_STATISTICS;
import static oshi.ffm.mac.MacSystem.XSW_USAGE_TOTAL;
import static oshi.ffm.mac.MacSystem.XSW_USAGE_USED;
import static oshi.ffm.mac.MacSystemFunctions.host_statistics;
import static oshi.ffm.mac.MacSystemFunctions.mach_host_self;

@ThreadSafe
final class MacVirtualMemoryFFM extends MacVirtualMemory {
    private static final Logger LOG = LoggerFactory.getLogger(MacVirtualMemoryFFM.class);

    MacVirtualMemoryFFM(MacGlobalMemoryFFM macGlobalMemory) {
        super(macGlobalMemory);
    }

    @Override
    protected Pair<Long, Long> querySwapUsage() {
        long swapUsed = 0L;
        long swapTotal = 0L;
        try (Arena arena = Arena.ofConfined()) {
            var xswUsage = arena.allocate(MacSystem.XSW_USAGE);
            if (SysctlUtilFFM.sysctl("vm.swapusage", xswUsage)) {
                swapUsed = xswUsage.get(JAVA_LONG, MacSystem.XSW_USAGE.byteOffset(XSW_USAGE_USED));
                swapTotal = xswUsage.get(JAVA_LONG, MacSystem.XSW_USAGE.byteOffset(XSW_USAGE_TOTAL));
            }
        }
        return new Pair<>(swapUsed, swapTotal);
    }

    @Override
    protected Pair<Long, Long> queryVmStat() {
        long swapPagesIn = 0L;
        long swapPagesOut = 0L;
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
            } else {
                swapPagesIn = ParseUtil.unsignedIntToLong(vmStats.get(JAVA_INT, MacSystem.VM_STATISTICS.byteOffset(MacSystem.VM_PAGEINS)));
                swapPagesOut = ParseUtil.unsignedIntToLong(vmStats.get(JAVA_INT, MacSystem.VM_STATISTICS.byteOffset(MacSystem.VM_PAGEOUTS)));
            }
        }
        return new Pair<>(swapPagesIn, swapPagesOut);
    }
}
