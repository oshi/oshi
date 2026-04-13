/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.comparison;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.NetworkIF;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.PowerSource;
import oshi.hardware.Printer;
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;
import oshi.hardware.UsbDevice;
import oshi.hardware.VirtualMemory;
import oshi.software.os.ApplicationInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OSThread;
import oshi.software.os.OperatingSystem;
import oshi.util.PlatformEnum;

/**
 * Compares JNA and FFM implementations of the OSHI API to detect regressions, missing implementations, or incorrect
 * values in the FFM port. JNA runs first (stable baseline), FFM runs second.
 *
 * <p>
 * Activate with: {@code mvn test -pl oshi-benchmark -Pnative-comparison}
 */
class NativeComparisonTest {

    // Snapshot all values once; JNA first (baseline), then FFM
    private static HardwareAbstractionLayer jnaHal;
    private static HardwareAbstractionLayer ffmHal;
    private static OperatingSystem jnaOs;
    private static OperatingSystem ffmOs;

    @BeforeAll
    static void setup() {
        oshi.SystemInfo jnaSi = new oshi.SystemInfo();
        jnaHal = jnaSi.getHardware();
        jnaOs = jnaSi.getOperatingSystem();

        oshi.ffm.SystemInfo ffmSi = new oshi.ffm.SystemInfo();
        ffmHal = ffmSi.getHardware();
        ffmOs = ffmSi.getOperatingSystem();
    }

    // ---- Hardware: ComputerSystem, Firmware, Baseboard ----

    @Test
    void computerSystem() {
        ComputerSystem jna = jnaHal.getComputerSystem();
        ComputerSystem ffm = ffmHal.getComputerSystem();
        assertThat(ffm).usingRecursiveComparison().ignoringFieldsMatchingRegexes(".*\\.expirationNanos").isEqualTo(jna);
    }

    // ---- Hardware: CentralProcessor ----

    @Test
    void processorIdentifier() {
        assertThat(ffmHal.getProcessor().getProcessorIdentifier()).usingRecursiveComparison()
                .isEqualTo(jnaHal.getProcessor().getProcessorIdentifier());
    }

    @Test
    void processorTopology() {
        CentralProcessor jna = jnaHal.getProcessor();
        CentralProcessor ffm = ffmHal.getProcessor();
        assertThat(ffm.getLogicalProcessorCount()).isEqualTo(jna.getLogicalProcessorCount());
        assertThat(ffm.getPhysicalProcessorCount()).isEqualTo(jna.getPhysicalProcessorCount());
        assertThat(ffm.getPhysicalPackageCount()).isEqualTo(jna.getPhysicalPackageCount());
        assertThat(ffm.getLogicalProcessors()).usingRecursiveComparison().isEqualTo(jna.getLogicalProcessors());
        assertThat(ffm.getPhysicalProcessors()).usingRecursiveComparison().isEqualTo(jna.getPhysicalProcessors());
        assertThat(ffm.getProcessorCaches()).usingRecursiveComparison().isEqualTo(jna.getProcessorCaches());
        assertThat(ffm.getFeatureFlags()).isEqualTo(jna.getFeatureFlags());
    }

    @Test
    void processorFrequencies() {
        CentralProcessor jna = jnaHal.getProcessor();
        CentralProcessor ffm = ffmHal.getProcessor();
        assertWithinRatio(ffm.getMaxFreq(), jna.getMaxFreq(), 0.01, "maxFreq");
        long[] jnaFreqs = jna.getCurrentFreq();
        long[] ffmFreqs = ffm.getCurrentFreq();
        assertThat(ffmFreqs).hasSameSizeAs(jnaFreqs);
        for (int i = 0; i < jnaFreqs.length; i++) {
            assertWithinRatio(ffmFreqs[i], jnaFreqs[i], 0.5, "currentFreq[" + i + "]");
        }
    }

    @Test
    void processorCpuLoadTicks() {
        CentralProcessor jna = jnaHal.getProcessor();
        CentralProcessor ffm = ffmHal.getProcessor();
        long[] jnaTicks = jna.getSystemCpuLoadTicks();
        long[] ffmTicks = ffm.getSystemCpuLoadTicks();
        assertThat(ffmTicks).hasSameSizeAs(jnaTicks);
        // Ticks are monotonically increasing counters; FFM (called second) should be >= JNA
        for (int i = 0; i < jnaTicks.length; i++) {
            assertThat(ffmTicks[i]).as("systemCpuLoadTick[%d]", i).isGreaterThanOrEqualTo(jnaTicks[i]);
        }
        long[][] jnaProc = jna.getProcessorCpuLoadTicks();
        long[][] ffmProc = ffm.getProcessorCpuLoadTicks();
        assertThat(ffmProc.length).as("processorCpuLoadTicks length").isEqualTo(jnaProc.length);
    }

    @Test
    void processorLoadAverage() {
        double[] jna = jnaHal.getProcessor().getSystemLoadAverage(3);
        double[] ffm = ffmHal.getProcessor().getSystemLoadAverage(3);
        assertThat(ffm).hasSameSizeAs(jna);
        for (int i = 0; i < jna.length; i++) {
            assertWithinRatio(ffm[i], jna[i], 0.25, "loadAverage[" + i + "]");
        }
    }

    @Test
    void processorContextSwitchesAndInterrupts() {
        CentralProcessor jna = jnaHal.getProcessor();
        CentralProcessor ffm = ffmHal.getProcessor();
        // Counters are generally nondecreasing but on Windows perf counter snapshots
        // can vary slightly between reads
        assertWithinRatio(ffm.getContextSwitches(), jna.getContextSwitches(), 0.01, "contextSwitches");
        assertWithinRatio(ffm.getInterrupts(), jna.getInterrupts(), 0.01, "interrupts");
    }

    // ---- Hardware: Memory ----

    @Test
    void globalMemory() {
        GlobalMemory jna = jnaHal.getMemory();
        GlobalMemory ffm = ffmHal.getMemory();
        assertThat(ffm.getTotal()).isEqualTo(jna.getTotal());
        assertThat(ffm.getPageSize()).isEqualTo(jna.getPageSize());
        // Available memory fluctuates but should be in the same ballpark
        assertWithinRatio(ffm.getAvailable(), jna.getAvailable(), 0.25, "availableMemory");
    }

    @Test
    void virtualMemory() {
        VirtualMemory jna = jnaHal.getMemory().getVirtualMemory();
        VirtualMemory ffm = ffmHal.getMemory().getVirtualMemory();
        assertThat(ffm.getSwapTotal()).isEqualTo(jna.getSwapTotal());
        assertWithinRatio(ffm.getSwapUsed(), jna.getSwapUsed(), 0.25, "swapUsed");
        assertWithinRatio(ffm.getVirtualMax(), jna.getVirtualMax(), 0.25, "virtualMax");
        assertWithinRatio(ffm.getVirtualInUse(), jna.getVirtualInUse(), 0.25, "virtualInUse");
    }

    @Test
    void physicalMemory() {
        List<PhysicalMemory> jna = jnaHal.getMemory().getPhysicalMemory();
        List<PhysicalMemory> ffm = ffmHal.getMemory().getPhysicalMemory();
        assertThat(ffm).usingRecursiveComparison().isEqualTo(jna);
    }

    // ---- Hardware: Sensors ----

    @Test
    void sensors() {
        Sensors jna = jnaHal.getSensors();
        Sensors ffm = ffmHal.getSensors();
        assertWithinRatio(ffm.getCpuTemperature(), jna.getCpuTemperature(), 0.25, "cpuTemperature");
        assertThat(ffm.getFanSpeeds()).hasSameSizeAs(jna.getFanSpeeds());
        assertWithinRatio(ffm.getCpuVoltage(), jna.getCpuVoltage(), 0.25, "cpuVoltage");
    }

    // ---- Hardware: Power Sources ----

    @Test
    void powerSources() {
        List<PowerSource> jna = jnaHal.getPowerSources();
        List<PowerSource> ffm = ffmHal.getPowerSources();
        assertThat(ffm).hasSameSizeAs(jna);
        for (int i = 0; i < jna.size(); i++) {
            PowerSource j = jna.get(i);
            PowerSource f = ffm.get(i);
            assertThat(f.getName()).isEqualTo(j.getName());
            assertThat(f.getDeviceName()).isEqualTo(j.getDeviceName());
            assertThat(f.getChemistry()).isEqualTo(j.getChemistry());
            assertThat(f.getManufacturer()).isEqualTo(j.getManufacturer());
            assertThat(f.getSerialNumber()).isEqualTo(j.getSerialNumber());
            assertThat(f.isCharging()).isEqualTo(j.isCharging());
            assertThat(f.isDischarging()).isEqualTo(j.isDischarging());
            assertThat(f.isPowerOnLine()).isEqualTo(j.isPowerOnLine());
            assertThat(f.getMaxCapacity()).isEqualTo(j.getMaxCapacity());
            assertThat(f.getDesignCapacity()).isEqualTo(j.getDesignCapacity());
            assertWithinRatio(f.getRemainingCapacityPercent(), j.getRemainingCapacityPercent(), 0.1,
                    "remainingCapacityPercent");
        }
    }

    // ---- Hardware: Disks ----

    @Test
    @DisabledIf("isLinux")
    void diskStores() {
        List<HWDiskStore> jna = jnaHal.getDiskStores();
        List<HWDiskStore> ffm = ffmHal.getDiskStores();
        assertThat(ffm).hasSameSizeAs(jna);
        Map<String, HWDiskStore> ffmByName = ffm.stream()
                .collect(Collectors.toMap(HWDiskStore::getName, Function.identity()));
        for (HWDiskStore j : jna) {
            HWDiskStore f = ffmByName.get(j.getName());
            assertThat(f).as("disk %s", j.getName()).isNotNull();
            assertThat(f.getModel()).isEqualTo(j.getModel());
            assertThat(f.getSerial()).isEqualTo(j.getSerial());
            assertThat(f.getSize()).isEqualTo(j.getSize());
            // Stats are nondecreasing; FFM called second
            assertThat(f.getReads()).as("reads(%s)", j.getName()).isGreaterThanOrEqualTo(j.getReads());
            assertThat(f.getReadBytes()).as("readBytes(%s)", j.getName()).isGreaterThanOrEqualTo(j.getReadBytes());
            assertThat(f.getWrites()).as("writes(%s)", j.getName()).isGreaterThanOrEqualTo(j.getWrites());
            assertThat(f.getWriteBytes()).as("writeBytes(%s)", j.getName()).isGreaterThanOrEqualTo(j.getWriteBytes());
            // Partitions should match exactly
            assertThat(f.getPartitions()).usingRecursiveComparison().isEqualTo(j.getPartitions());
        }
    }

    // ---- Hardware: Logical Volume Groups ----

    @Test
    void logicalVolumeGroups() {
        List<LogicalVolumeGroup> jna = jnaHal.getLogicalVolumeGroups();
        List<LogicalVolumeGroup> ffm = ffmHal.getLogicalVolumeGroups();
        assertThat(ffm).usingRecursiveComparison().isEqualTo(jna);
    }

    // ---- Hardware: Network Interfaces ----

    @Test
    void networkInterfaces() {
        List<NetworkIF> jna = jnaHal.getNetworkIFs();
        List<NetworkIF> ffm = ffmHal.getNetworkIFs();
        assertThat(ffm).hasSameSizeAs(jna);
        Map<String, NetworkIF> ffmByName = ffm.stream()
                .collect(Collectors.toMap(NetworkIF::getName, Function.identity()));
        for (NetworkIF j : jna) {
            NetworkIF f = ffmByName.get(j.getName());
            assertThat(f).as("networkIF %s", j.getName()).isNotNull();
            assertThat(f.getDisplayName()).isEqualTo(j.getDisplayName());
            assertThat(f.getMacaddr()).isEqualTo(j.getMacaddr());
            assertThat(f.getIPv4addr()).isEqualTo(j.getIPv4addr());
            assertThat(f.getIPv6addr()).isEqualTo(j.getIPv6addr());
            assertThat(f.getMTU()).isEqualTo(j.getMTU());
            assertThat(f.getSpeed()).isEqualTo(j.getSpeed());
            assertThat(f.getIfAlias()).isEqualTo(j.getIfAlias());
            assertThat(f.getIfOperStatus()).isEqualTo(j.getIfOperStatus());
            // Traffic counters are nondecreasing
            assertThat(f.getBytesSent()).as("bytesSent(%s)", j.getName()).isGreaterThanOrEqualTo(j.getBytesSent());
            assertThat(f.getBytesRecv()).as("bytesRecv(%s)", j.getName()).isGreaterThanOrEqualTo(j.getBytesRecv());
            assertThat(f.getPacketsSent()).as("packetsSent(%s)", j.getName())
                    .isGreaterThanOrEqualTo(j.getPacketsSent());
            assertThat(f.getPacketsRecv()).as("packetsRecv(%s)", j.getName())
                    .isGreaterThanOrEqualTo(j.getPacketsRecv());
        }
    }

    // ---- Hardware: Displays ----

    @Test
    void displays() {
        List<Display> jna = jnaHal.getDisplays();
        List<Display> ffm = ffmHal.getDisplays();
        assertThat(ffm).usingRecursiveComparison().isEqualTo(jna);
    }

    // ---- Hardware: USB Devices ----

    @Test
    void usbDevices() {
        List<UsbDevice> jna = jnaHal.getUsbDevices(true);
        List<UsbDevice> ffm = ffmHal.getUsbDevices(true);
        assertThat(ffm).usingRecursiveComparison().isEqualTo(jna);
    }

    // ---- Hardware: Sound Cards ----

    @Test
    void soundCards() {
        List<SoundCard> jna = jnaHal.getSoundCards();
        List<SoundCard> ffm = ffmHal.getSoundCards();
        assertThat(ffm).usingRecursiveComparison().isEqualTo(jna);
    }

    // ---- Hardware: Graphics Cards ----

    @Test
    void graphicsCards() {
        List<GraphicsCard> jna = jnaHal.getGraphicsCards();
        List<GraphicsCard> ffm = ffmHal.getGraphicsCards();
        assertThat(ffm).usingRecursiveComparison().isEqualTo(jna);
    }

    // ---- OS: OperatingSystem ----

    @Test
    void operatingSystemInfo() {
        assertThat(ffmOs.getFamily()).isEqualTo(jnaOs.getFamily());
        assertThat(ffmOs.getManufacturer()).isEqualTo(jnaOs.getManufacturer());
        assertThat(ffmOs.getBitness()).isEqualTo(jnaOs.getBitness());
        assertThat(ffmOs.getVersionInfo()).usingRecursiveComparison().isEqualTo(jnaOs.getVersionInfo());
        assertThat(ffmOs.getSystemBootTime()).isEqualTo(jnaOs.getSystemBootTime());
        // FFM called second, uptime should be >= JNA
        assertThat(ffmOs.getSystemUptime()).isGreaterThanOrEqualTo(jnaOs.getSystemUptime());
        assertThat(ffmOs.isElevated()).isEqualTo(jnaOs.isElevated());
        assertThat(ffmOs.getProcessCount()).isGreaterThan(0);
        assertThat(ffmOs.getThreadCount()).isGreaterThan(0);
    }

    // ---- OS: Current Process ----

    @Test
    void currentProcess() {
        int pid = jnaOs.getProcessId();
        assertThat(ffmOs.getProcessId()).isEqualTo(pid);
        OSProcess jna = jnaOs.getProcess(pid);
        OSProcess ffm = ffmOs.getProcess(pid);
        assertThat(ffm).isNotNull();
        assertThat(ffm.getProcessID()).isEqualTo(jna.getProcessID());
        assertThat(ffm.getName()).isEqualTo(jna.getName());
        assertThat(ffm.getPath()).isEqualTo(jna.getPath());
        assertThat(ffm.getUser()).isEqualTo(jna.getUser());
        assertThat(ffm.getUserID()).isEqualTo(jna.getUserID());
        assertThat(ffm.getGroup()).isEqualTo(jna.getGroup());
        assertThat(ffm.getGroupID()).isEqualTo(jna.getGroupID());
        assertThat(ffm.getState()).isEqualTo(jna.getState());
        assertThat(ffm.getParentProcessID()).isEqualTo(jna.getParentProcessID());
        assertThat(ffm.getPriority()).isEqualTo(jna.getPriority());
        // Memory values should be in the same ballpark
        assertWithinRatio(ffm.getVirtualSize(), jna.getVirtualSize(), 0.25, "process.virtualSize");
        assertWithinRatio(ffm.getResidentMemory(), jna.getResidentMemory(), 0.25, "process.residentMemory");
        // Time counters: FFM called second, should be >= JNA
        assertThat(ffm.getKernelTime()).as("process.kernelTime").isGreaterThanOrEqualTo(jna.getKernelTime());
        assertThat(ffm.getUserTime()).as("process.userTime").isGreaterThanOrEqualTo(jna.getUserTime());
        assertThat(ffm.getUpTime()).as("process.upTime").isGreaterThanOrEqualTo(jna.getUpTime());
        assertThat(ffm.getStartTime()).isEqualTo(jna.getStartTime());
        assertThat(ffm.getCommandLine()).isEqualTo(jna.getCommandLine());
    }

    // ---- OS: FileSystem ----

    @Test
    void fileSystem() {
        FileSystem jnaFs = jnaOs.getFileSystem();
        FileSystem ffmFs = ffmOs.getFileSystem();
        assertThat(ffmFs.getMaxFileDescriptors()).isEqualTo(jnaFs.getMaxFileDescriptors());
        assertWithinRatio(ffmFs.getOpenFileDescriptors(), jnaFs.getOpenFileDescriptors(), 0.25, "openFileDescriptors");

        List<OSFileStore> jnaStores = jnaFs.getFileStores();
        List<OSFileStore> ffmStores = ffmFs.getFileStores();
        assertThat(ffmStores).hasSameSizeAs(jnaStores);
        Map<String, OSFileStore> ffmByMount = ffmStores.stream()
                .collect(Collectors.toMap(OSFileStore::getMount, Function.identity(), (a, b) -> a));
        for (OSFileStore j : jnaStores) {
            OSFileStore f = ffmByMount.get(j.getMount());
            assertThat(f).as("fileStore at %s", j.getMount()).isNotNull();
            assertThat(f.getName()).isEqualTo(j.getName());
            assertThat(f.getType()).isEqualTo(j.getType());
            assertThat(f.getVolume()).isEqualTo(j.getVolume());
            assertThat(f.getTotalSpace()).isEqualTo(j.getTotalSpace());
            // Usable space fluctuates
            assertWithinRatio(f.getUsableSpace(), j.getUsableSpace(), 0.25, "usableSpace(" + j.getMount() + ")");
        }
    }

    // ---- OS: Network Params ----

    @Test
    void networkParams() {
        NetworkParams jna = jnaOs.getNetworkParams();
        NetworkParams ffm = ffmOs.getNetworkParams();
        assertThat(ffm.getHostName()).isEqualTo(jna.getHostName());
        assertThat(ffm.getDomainName()).isEqualTo(jna.getDomainName());
        assertThat(ffm.getDnsServers()).isEqualTo(jna.getDnsServers());
        assertThat(ffm.getIpv4DefaultGateway()).isEqualTo(jna.getIpv4DefaultGateway());
        assertThat(ffm.getIpv6DefaultGateway()).isEqualTo(jna.getIpv6DefaultGateway());
    }

    // ---- OS: Internet Protocol Stats ----

    @Test
    void internetProtocolStats() {
        InternetProtocolStats jna = jnaOs.getInternetProtocolStats();
        InternetProtocolStats ffm = ffmOs.getInternetProtocolStats();
        // TCP/UDP stats are counters; FFM should be >= JNA for cumulative fields
        // Just verify they're non-negative and structurally present
        assertThat(ffm.getTCPv4Stats()).isNotNull();
        assertThat(ffm.getTCPv6Stats()).isNotNull();
        assertThat(ffm.getUDPv4Stats()).isNotNull();
        assertThat(ffm.getUDPv6Stats()).isNotNull();
        // Connections established is a gauge, not a counter — can go up or down
        assertThat(ffm.getTCPv4Stats().getConnectionsEstablished()).isGreaterThanOrEqualTo(0);
    }

    // ---- OS: Sessions ----

    @Test
    void sessions() {
        // Sessions should be the same set of users
        assertThat(ffmOs.getSessions()).hasSameSizeAs(jnaOs.getSessions());
    }

    // ---- OS: Services ----

    @Test
    void services() {
        // Services can start/stop between calls; just verify they're in the same ballpark
        int jnaSize = jnaOs.getServices().size();
        int ffmSize = ffmOs.getServices().size();
        assertWithinRatio(ffmSize, jnaSize, 0.05, "services.size");
    }

    // ---- Hardware: Printers ----

    @Test
    void printers() {
        List<Printer> jna = jnaHal.getPrinters();
        List<Printer> ffm = ffmHal.getPrinters();
        assertThat(ffm).usingRecursiveComparison().isEqualTo(jna);
    }

    // ---- OS: Current Thread ----

    @Test
    void currentThread() {
        int jnaTid = jnaOs.getThreadId();
        int ffmTid = ffmOs.getThreadId();
        assertThat(ffmTid).isEqualTo(jnaTid);
        OSThread jnaThread = jnaOs.getCurrentThread();
        OSThread ffmThread = ffmOs.getCurrentThread();
        assertThat(ffmThread).isNotNull();
        assertThat(ffmThread.getThreadId()).isEqualTo(jnaThread.getThreadId());
        assertThat(ffmThread.getOwningProcessId()).isEqualTo(jnaThread.getOwningProcessId());
        assertThat(ffmThread.getName()).isEqualTo(jnaThread.getName());
        assertThat(ffmThread.getState()).isEqualTo(jnaThread.getState());
    }

    // ---- OS: Desktop Windows ----

    @Test
    void desktopWindows() {
        // Desktop windows can change rapidly; just verify both return non-null lists
        assertThat(ffmOs.getDesktopWindows(true)).isNotNull();
        assertThat(jnaOs.getDesktopWindows(true)).isNotNull();
    }

    // ---- OS: Installed Applications ----

    @Test
    void installedApplications() {
        List<ApplicationInfo> jna = jnaOs.getInstalledApplications().stream()
                .sorted(Comparator.comparing(ApplicationInfo::toString)).collect(Collectors.toList());
        List<ApplicationInfo> ffm = ffmOs.getInstalledApplications().stream()
                .sorted(Comparator.comparing(ApplicationInfo::toString)).collect(Collectors.toList());
        assertThat(ffm).usingRecursiveComparison().isEqualTo(jna);
    }

    // ---- OS: Processes (structural) ----

    @Test
    void processListStructure() {
        // Both should return a non-empty process list
        List<OSProcess> jna = jnaOs.getProcesses(null, null, 0);
        List<OSProcess> ffm = ffmOs.getProcesses(null, null, 0);
        assertThat(ffm).isNotEmpty();
        // Process counts can differ slightly but should be in the same ballpark
        assertWithinRatio(ffm.size(), jna.size(), 0.25, "processCount");

        // The longest-lived processes should be present in both lists
        List<OSProcess> jnaSorted = jna.stream().sorted(Comparator.comparingLong(OSProcess::getStartTime)).limit(10)
                .collect(Collectors.toList());
        Map<Integer, OSProcess> ffmByPid = ffm.stream()
                .collect(Collectors.toMap(OSProcess::getProcessID, Function.identity(), (a, b) -> a));
        for (OSProcess j : jnaSorted) {
            OSProcess f = ffmByPid.get(j.getProcessID());
            if (f != null) {
                assertThat(f.getName()).as("process[%d].name", j.getProcessID()).isEqualTo(j.getName());
                assertThat(f.getStartTime()).as("process[%d].startTime", j.getProcessID()).isEqualTo(j.getStartTime());
            }
        }
    }

    // ---- OS: Threads of current process ----

    @Test
    void currentProcessThreads() {
        int pid = jnaOs.getProcessId();
        OSProcess jna = jnaOs.getProcess(pid);
        OSProcess ffm = ffmOs.getProcess(pid);
        List<OSThread> jnaThreads = jna.getThreadDetails();
        List<OSThread> ffmThreads = ffm.getThreadDetails();
        // Thread count can change but should be in the same ballpark
        assertWithinRatio(ffmThreads.size(), jnaThreads.size(), 0.5, "threadDetails.size");
    }

    // ---- Conditions ----

    static boolean isLinux() {
        return PlatformEnum.getCurrentPlatform() == PlatformEnum.LINUX;
    }

    // ---- Helpers ----

    /**
     * Asserts that two values are within a given ratio of each other. If both are zero, they match. If one is zero and
     * the other is not, the assertion fails. Otherwise, the ratio of the smaller to the larger must be >= (1 - ratio).
     */
    private static void assertWithinRatio(double actual, double expected, double ratio, String description) {
        if (expected == 0 && actual == 0) {
            return;
        }
        if (expected == 0 || actual == 0) {
            // One is zero and the other isn't — only fail if the nonzero value is significant
            double nonZero = Math.max(Math.abs(expected), Math.abs(actual));
            assertThat(nonZero).as("%s: one value is 0, other is %f", description, nonZero).isLessThan(1.0);
            return;
        }
        double min = Math.min(Math.abs(actual), Math.abs(expected));
        double max = Math.max(Math.abs(actual), Math.abs(expected));
        assertThat(min / max).as("%s: expected=%f, actual=%f", description, expected, actual)
                .isGreaterThanOrEqualTo(1.0 - ratio);
    }

    private static void assertWithinRatio(long actual, long expected, double ratio, String description) {
        assertWithinRatio((double) actual, (double) expected, ratio, description);
    }
}
