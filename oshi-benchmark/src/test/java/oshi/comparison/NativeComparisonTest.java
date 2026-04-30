/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.comparison;

import static org.assertj.core.api.Assertions.assertThat;
import static oshi.comparison.ComparisonAssertions.assertWithinRatio;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import oshi.software.os.OSSession;
import oshi.software.os.OSThread;
import oshi.software.os.OperatingSystem;
import oshi.util.GlobalConfig;
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

    /**
     * Initializes JNA (baseline) and FFM hardware and OS instances with memoization disabled and Windows-specific
     * configuration options enabled for maximum coverage.
     */
    @BeforeAll
    static void setup() {
        // Disable memoization so each call gets fresh data for accurate comparison
        GlobalConfig.set(GlobalConfig.OSHI_UTIL_MEMOIZER_EXPIRATION, 0);
        // Enable CPU utility counters (Windows 8+) to exercise that branch;
        // normal CI tests use the default (false) so both paths get coverage
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_CPU_UTILITY, true);
        // Enable suspended process state detection to exercise thread-based state logic
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_PROCSTATE_SUSPENDED, true);
        // Enable batch command line queries to exercise Win32ProcessCached
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_COMMANDLINE_BATCH, true);

        oshi.SystemInfo jnaSi = new oshi.SystemInfo();
        jnaHal = jnaSi.getHardware();
        jnaOs = jnaSi.getOperatingSystem();

        oshi.ffm.SystemInfo ffmSi = new oshi.ffm.SystemInfo();
        ffmHal = ffmSi.getHardware();
        ffmOs = ffmSi.getOperatingSystem();
    }

    // ---- Hardware: ComputerSystem, Firmware, Baseboard ----

    /** Compares {@link ComputerSystem}, firmware, and baseboard fields. */
    @Test
    void computerSystem() {
        ComputerSystem jna = jnaHal.getComputerSystem();
        ComputerSystem ffm = ffmHal.getComputerSystem();
        assertThat(ffm).usingRecursiveComparison().ignoringFieldsMatchingRegexes(".*\\.expirationNanos").isEqualTo(jna);
    }

    // ---- Hardware: CentralProcessor ----

    /** Compares processor identifier fields (vendor, name, stepping, etc.). */
    @Test
    void processorIdentifier() {
        assertThat(ffmHal.getProcessor().getProcessorIdentifier()).usingRecursiveComparison()
                .isEqualTo(jnaHal.getProcessor().getProcessorIdentifier());
    }

    /** Compares processor topology: logical/physical counts, caches, and feature flags. */
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

    /** Compares max and per-core current CPU frequencies. */
    @Test
    void processorFrequencies() {
        CentralProcessor jna = jnaHal.getProcessor();
        CentralProcessor ffm = ffmHal.getProcessor();
        assertWithinRatio(ffm.getMaxFreq(), jna.getMaxFreq(), 0.05, "maxFreq");
        long[] jnaFreqs = jna.getCurrentFreq();
        long[] ffmFreqs = ffm.getCurrentFreq();
        assertThat(ffmFreqs).hasSameSizeAs(jnaFreqs);
        for (int i = 0; i < jnaFreqs.length; i++) {
            assertWithinRatio(ffmFreqs[i], jnaFreqs[i], 0.5, "currentFreq[" + i + "]");
        }
    }

    /** Compares system and per-processor CPU load tick arrays. */
    @Test
    void processorCpuLoadTicks() {
        CentralProcessor jna = jnaHal.getProcessor();
        CentralProcessor ffm = ffmHal.getProcessor();
        long[] jnaTicks = jna.getSystemCpuLoadTicks();
        long[] ffmTicks = ffm.getSystemCpuLoadTicks();
        assertThat(ffmTicks).hasSameSizeAs(jnaTicks);
        // With USE_CPU_UTILITY enabled, ticks are derived from utility counters
        // and may not be strictly monotonic between JNA and FFM calls
        for (int i = 0; i < jnaTicks.length; i++) {
            assertWithinRatio(ffmTicks[i], jnaTicks[i], 0.05, "systemCpuLoadTick[" + i + "]");
        }
        long[][] jnaProc = jna.getProcessorCpuLoadTicks();
        long[][] ffmProc = ffm.getProcessorCpuLoadTicks();
        assertThat(ffmProc.length).as("processorCpuLoadTicks length").isEqualTo(jnaProc.length);
    }

    /** Compares 1-, 5-, and 15-minute system load averages. */
    @Test
    void processorLoadAverage() {
        double[] jna = jnaHal.getProcessor().getSystemLoadAverage(3);
        double[] ffm = ffmHal.getProcessor().getSystemLoadAverage(3);
        assertThat(ffm).hasSameSizeAs(jna);
        for (int i = 0; i < jna.length; i++) {
            assertWithinRatio(ffm[i], jna[i], 0.25, "loadAverage[" + i + "]");
        }
    }

    /** Compares context switch and interrupt counters. */
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

    /** Compares total, page size, and available memory. */
    @Test
    void globalMemory() {
        GlobalMemory jna = jnaHal.getMemory();
        GlobalMemory ffm = ffmHal.getMemory();
        assertThat(ffm.getTotal()).isEqualTo(jna.getTotal());
        assertThat(ffm.getPageSize()).isEqualTo(jna.getPageSize());
        // Available memory fluctuates but should be in the same ballpark
        assertWithinRatio(ffm.getAvailable(), jna.getAvailable(), 0.25, "availableMemory");
    }

    /** Compares swap and virtual memory statistics. */
    @Test
    void virtualMemory() {
        VirtualMemory jna = jnaHal.getMemory().getVirtualMemory();
        VirtualMemory ffm = ffmHal.getMemory().getVirtualMemory();
        assertThat(ffm.getSwapTotal()).isEqualTo(jna.getSwapTotal());
        assertWithinRatio(ffm.getSwapUsed(), jna.getSwapUsed(), 0.25, "swapUsed");
        assertWithinRatio(ffm.getVirtualMax(), jna.getVirtualMax(), 0.25, "virtualMax");
        assertWithinRatio(ffm.getVirtualInUse(), jna.getVirtualInUse(), 0.25, "virtualInUse");
    }

    /** Compares physical memory DIMM details. */
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
        // Virtual size can differ significantly between JNA and FFM due to timing of memory-mapped regions
        // and DLL loading between the two snapshots; 0.95 tolerance accommodates this variance
        assertWithinRatio(ffm.getVirtualSize(), jna.getVirtualSize(), 0.95, "process.virtualSize");
        assertWithinRatio(ffm.getResidentMemory(), jna.getResidentMemory(), 0.75, "process.residentMemory");
        // Time counters: snapshots taken close together, allow small difference
        assertThat(ffm.getKernelTime()).as("process.kernelTime").isGreaterThanOrEqualTo(jna.getKernelTime());
        assertThat(ffm.getUserTime()).as("process.userTime").isGreaterThanOrEqualTo(jna.getUserTime());
        // Uptime may differ due to memoization (300ms TTL), use max of 10% or 300ms
        assertThat(Math.abs(ffm.getUpTime() - jna.getUpTime())).as("process.upTime")
                .isLessThanOrEqualTo(Math.max(jna.getUpTime() / 10, 300L));
        assertThat(ffm.getStartTime()).isEqualTo(jna.getStartTime());
        assertThat(ffm.getCommandLine()).isEqualTo(jna.getCommandLine());
    }

    @Test
    void currentProcessUpdateAttributes() {
        int pid = jnaOs.getProcessId();
        OSProcess jna = jnaOs.getProcess(pid);
        OSProcess ffm = ffmOs.getProcess(pid);
        assertThat(jna).isNotNull();
        assertThat(ffm).isNotNull();
        // Call updateAttributes to refresh and verify it succeeds
        assertThat(jna.updateAttributes()).as("JNA updateAttributes").isTrue();
        assertThat(ffm.updateAttributes()).as("FFM updateAttributes").isTrue();
        // After refresh, basic fields should still match
        assertThat(ffm.getName()).isEqualTo(jna.getName());
        assertThat(ffm.getState()).isEqualTo(jna.getState());
        assertThat(ffm.getProcessID()).isEqualTo(jna.getProcessID());
        // Command line exercises Win32ProcessCached when batch mode is enabled
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

        // TCP stats — counters may not be strictly monotonic between calls on all platforms
        InternetProtocolStats.TcpStats jnaTcp4 = jna.getTCPv4Stats();
        InternetProtocolStats.TcpStats ffmTcp4 = ffm.getTCPv4Stats();
        assertThat(ffmTcp4).as("FFM TCPv4 stats").isNotNull();
        assertThat(ffmTcp4.getConnectionsEstablished()).as("TCPv4 established").isGreaterThanOrEqualTo(0);
        assertWithinRatio(ffmTcp4.getSegmentsSent(), jnaTcp4.getSegmentsSent(), 0.05, "TCPv4 segmentsSent");
        assertWithinRatio(ffmTcp4.getSegmentsReceived(), jnaTcp4.getSegmentsReceived(), 0.05, "TCPv4 segmentsReceived");

        InternetProtocolStats.TcpStats jnaTcp6 = jna.getTCPv6Stats();
        InternetProtocolStats.TcpStats ffmTcp6 = ffm.getTCPv6Stats();
        assertThat(ffmTcp6).as("FFM TCPv6 stats").isNotNull();
        assertWithinRatio(ffmTcp6.getSegmentsSent(), jnaTcp6.getSegmentsSent(), 0.05, "TCPv6 segmentsSent");
        assertWithinRatio(ffmTcp6.getSegmentsReceived(), jnaTcp6.getSegmentsReceived(), 0.05, "TCPv6 segmentsReceived");

        // UDP stats — counters may not be strictly monotonic between calls on all platforms
        InternetProtocolStats.UdpStats jnaUdp4 = jna.getUDPv4Stats();
        InternetProtocolStats.UdpStats ffmUdp4 = ffm.getUDPv4Stats();
        assertThat(ffmUdp4).as("FFM UDPv4 stats").isNotNull();
        assertWithinRatio(ffmUdp4.getDatagramsSent(), jnaUdp4.getDatagramsSent(), 0.05, "UDPv4 datagramsSent");
        assertWithinRatio(ffmUdp4.getDatagramsReceived(), jnaUdp4.getDatagramsReceived(), 0.05,
                "UDPv4 datagramsReceived");

        InternetProtocolStats.UdpStats jnaUdp6 = jna.getUDPv6Stats();
        InternetProtocolStats.UdpStats ffmUdp6 = ffm.getUDPv6Stats();
        assertThat(ffmUdp6).as("FFM UDPv6 stats").isNotNull();
        assertWithinRatio(ffmUdp6.getDatagramsSent(), jnaUdp6.getDatagramsSent(), 0.05, "UDPv6 datagramsSent");
        assertWithinRatio(ffmUdp6.getDatagramsReceived(), jnaUdp6.getDatagramsReceived(), 0.05,
                "UDPv6 datagramsReceived");
    }

    @Test
    void internetProtocolConnections() {
        InternetProtocolStats jna = jnaOs.getInternetProtocolStats();
        InternetProtocolStats ffm = ffmOs.getInternetProtocolStats();

        List<InternetProtocolStats.IPConnection> jnaConns = jna.getConnections();
        List<InternetProtocolStats.IPConnection> ffmConns = ffm.getConnections();
        assertThat(ffmConns).as("FFM connections").isNotNull();
        assertThat(jnaConns).as("JNA connections").isNotNull();

        // Both sides should return connections or both should be empty
        assertThat(ffmConns.isEmpty()).as("FFM and JNA should agree on emptiness").isEqualTo(jnaConns.isEmpty());
        if (jnaConns.isEmpty()) {
            return;
        }

        // Connection counts can fluctuate significantly between the two reads
        assertWithinRatio(ffmConns.size(), jnaConns.size(), 0.50, "connections.size");

        // Build unique tuple sets for overlap check
        Set<String> jnaKeys = new HashSet<>();
        for (InternetProtocolStats.IPConnection c : jnaConns) {
            jnaKeys.add(c.getType() + ":" + c.getLocalPort() + ":" + c.getForeignPort());
        }
        Set<String> ffmKeys = new HashSet<>();
        for (InternetProtocolStats.IPConnection c : ffmConns) {
            ffmKeys.add(c.getType() + ":" + c.getLocalPort() + ":" + c.getForeignPort());
        }
        // Count unique tuples present in both
        Set<String> intersection = new HashSet<>(ffmKeys);
        intersection.retainAll(jnaKeys);
        assertThat(intersection.size()).as("unique connection tuple overlap")
                .isGreaterThanOrEqualTo(ffmKeys.size() / 4);

        // Verify structural correctness of FFM connections
        for (InternetProtocolStats.IPConnection c : ffmConns) {
            assertThat(c.getType()).as("connection type").isNotEmpty();
            assertThat(c.getLocalPort()).as("localPort").isBetween(0, 0xffff);
            assertThat(c.getForeignPort()).as("foreignPort").isBetween(0, 0xffff);
            assertThat(c.getState()).as("state").isNotNull();
        }
    }

    // ---- OS: Sessions ----

    @Test
    void sessions() {
        List<OSSession> jnaSessions = jnaOs.getSessions();
        List<OSSession> ffmSessions = ffmOs.getSessions();
        // Sessions should be the same set of users
        assertThat(ffmSessions).hasSameSizeAs(jnaSessions);
        // On Linux, compare session details (user, device, host)
        if (isLinux() && !jnaSessions.isEmpty()) {
            Map<String, OSSession> ffmByKey = ffmSessions.stream()
                    .collect(Collectors.toMap(s -> s.getUserName() + "|" + s.getTerminalDevice(), s -> s, (a, b) -> a));
            for (OSSession j : jnaSessions) {
                String key = j.getUserName() + "|" + j.getTerminalDevice();
                OSSession f = ffmByKey.get(key);
                assertThat(f).as("session %s", key).isNotNull();
                assertThat(f.getHost()).as("session[%s].host", key).isEqualTo(j.getHost());
                // Login times should be within 1 second of each other
                assertThat(Math.abs(f.getLoginTime() - j.getLoginTime())).as("session[%s].loginTime", key)
                        .isLessThanOrEqualTo(1000L);
            }
        }
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
        Set<String> jna = jnaOs.getInstalledApplications().stream().map(ApplicationInfo::getName)
                .collect(Collectors.toSet());
        Set<String> ffm = ffmOs.getInstalledApplications().stream().map(ApplicationInfo::getName)
                .collect(Collectors.toSet());
        // Apps can be installed/uninstalled between calls; compare name sets with tolerance
        assertWithinRatio(ffm.size(), jna.size(), 0.05, "installedApplications.size");
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
                // Kernel worker threads (kworker) are renamed dynamically based on current work; skip name check
                if (!j.getName().startsWith("kworker/")) {
                    assertThat(f.getName()).as("process[%d].name", j.getProcessID()).isEqualTo(j.getName());
                }
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

}
