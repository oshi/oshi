/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.comparison;

import static org.assertj.core.api.Assertions.assertThat;
import static oshi.comparison.ComparisonAssertions.assertWithinRatio;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.VirtualMemory;
import oshi.software.common.os.linux.LinuxOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

/**
 * Compares the native-free (NF) provider against the JNA provider on Linux to verify that procfs/sysfs/command-line
 * implementations produce the same results as native calls.
 * <p>
 * JNA is the baseline (stable, well-tested). NF values should match for deterministic fields and be within tolerance
 * for dynamic values.
 */
@EnabledOnOs(OS.LINUX)
class NativeFreeComparisonTest {

    private static HardwareAbstractionLayer jnaHal;
    private static HardwareAbstractionLayer nfHal;
    private static OperatingSystem jnaOs;
    private static OperatingSystem nfOs;

    @BeforeAll
    static void setup() {
        oshi.SystemInfo jnaSi = new oshi.SystemInfo();
        jnaHal = jnaSi.getHardware();
        jnaOs = jnaSi.getOperatingSystem();

        oshi.nativefree.SystemInfo nfSi = new oshi.nativefree.SystemInfo();
        nfHal = nfSi.getHardware();
        nfOs = nfSi.getOperatingSystem();
    }

    // ---- OS: System constants (getconf vs native) ----

    @Test
    void hzMatchesNative() {
        assertThat(((LinuxOperatingSystem) nfOs).getHz()).as("CLK_TCK: getconf vs native")
                .isEqualTo(((LinuxOperatingSystem) jnaOs).getHz());
    }

    @Test
    void pageSizeMatchesNative() {
        assertThat(((LinuxOperatingSystem) nfOs).getPageSize()).as("PAGE_SIZE: getconf vs native")
                .isEqualTo(((LinuxOperatingSystem) jnaOs).getPageSize());
    }

    // ---- OS: Basic info ----

    @Test
    void operatingSystemInfo() {
        assertThat(nfOs.getFamily()).isEqualTo(jnaOs.getFamily());
        assertThat(nfOs.getManufacturer()).isEqualTo(jnaOs.getManufacturer());
        assertThat(nfOs.getBitness()).isEqualTo(jnaOs.getBitness());
        assertThat(nfOs.getVersionInfo()).usingRecursiveComparison().isEqualTo(jnaOs.getVersionInfo());
        assertThat(nfOs.getSystemBootTime()).isEqualTo(jnaOs.getSystemBootTime());
        assertThat(nfOs.getSystemUptime()).isGreaterThanOrEqualTo(jnaOs.getSystemUptime());
        assertThat(nfOs.getProcessId()).isEqualTo(jnaOs.getProcessId());
        assertThat(nfOs.getThreadId()).isEqualTo(jnaOs.getThreadId());
        assertWithinRatio(nfOs.getThreadCount(), jnaOs.getThreadCount(), 0.1, "threadCount");
    }

    // ---- Hardware: Processor ----

    @Test
    void processorIdentifier() {
        CentralProcessor.ProcessorIdentifier nfId = nfHal.getProcessor().getProcessorIdentifier();
        CentralProcessor.ProcessorIdentifier jnaId = jnaHal.getProcessor().getProcessorIdentifier();
        assertThat(nfId.getVendor()).isEqualTo(jnaId.getVendor());
        assertThat(nfId.getName()).isEqualTo(jnaId.getName());
        assertThat(nfId.getFamily()).isEqualTo(jnaId.getFamily());
        assertThat(nfId.getModel()).isEqualTo(jnaId.getModel());
        assertThat(nfId.getStepping()).isEqualTo(jnaId.getStepping());
        assertThat(nfId.isCpu64bit()).isEqualTo(jnaId.isCpu64bit());
        assertThat(nfId.getProcessorID()).as("processorID").isEqualTo(jnaId.getProcessorID());
    }

    @Test
    void processorTopology() {
        CentralProcessor jna = jnaHal.getProcessor();
        CentralProcessor nf = nfHal.getProcessor();
        assertThat(nf.getLogicalProcessorCount()).isEqualTo(jna.getLogicalProcessorCount());
        assertThat(nf.getPhysicalProcessorCount()).isEqualTo(jna.getPhysicalProcessorCount());
        assertThat(nf.getPhysicalPackageCount()).isEqualTo(jna.getPhysicalPackageCount());
        assertThat(nf.getLogicalProcessors()).usingRecursiveComparison().isEqualTo(jna.getLogicalProcessors());
        assertThat(nf.getProcessorCaches()).usingRecursiveComparison().isEqualTo(jna.getProcessorCaches());
        assertThat(nf.getFeatureFlags()).isEqualTo(jna.getFeatureFlags());
    }

    @Test
    void processorFrequencies() {
        CentralProcessor jna = jnaHal.getProcessor();
        CentralProcessor nf = nfHal.getProcessor();
        assertWithinRatio(nf.getMaxFreq(), jna.getMaxFreq(), 0.05, "maxFreq");
        long[] jnaFreqs = jna.getCurrentFreq();
        long[] nfFreqs = nf.getCurrentFreq();
        assertThat(nfFreqs).hasSameSizeAs(jnaFreqs);
    }

    @Test
    void processorCpuLoadTicks() {
        CentralProcessor jna = jnaHal.getProcessor();
        CentralProcessor nf = nfHal.getProcessor();
        long[] jnaTicks = jna.getSystemCpuLoadTicks();
        long[] nfTicks = nf.getSystemCpuLoadTicks();
        assertThat(nfTicks).hasSameSizeAs(jnaTicks);
        long[][] jnaProc = jna.getProcessorCpuLoadTicks();
        long[][] nfProc = nf.getProcessorCpuLoadTicks();
        assertThat(nfProc.length).isEqualTo(jnaProc.length);
    }

    // ---- Hardware: Memory ----

    @Test
    void globalMemory() {
        GlobalMemory jna = jnaHal.getMemory();
        GlobalMemory nf = nfHal.getMemory();
        assertThat(nf.getTotal()).isEqualTo(jna.getTotal());
        assertThat(nf.getPageSize()).isEqualTo(jna.getPageSize());
        assertWithinRatio(nf.getAvailable(), jna.getAvailable(), 0.25, "availableMemory");
    }

    @Test
    void virtualMemory() {
        VirtualMemory jna = jnaHal.getMemory().getVirtualMemory();
        VirtualMemory nf = nfHal.getMemory().getVirtualMemory();
        assertThat(nf.getSwapTotal()).isEqualTo(jna.getSwapTotal());
        assertWithinRatio(nf.getSwapUsed(), jna.getSwapUsed(), 0.25, "swapUsed");
    }

    // ---- OS: Current Process ----

    @Test
    void currentProcess() {
        int pid = jnaOs.getProcessId();
        OSProcess jna = jnaOs.getProcess(pid);
        OSProcess nf = nfOs.getProcess(pid);
        assertThat(nf).isNotNull();
        assertThat(nf.getProcessID()).isEqualTo(jna.getProcessID());
        assertThat(nf.getName()).isEqualTo(jna.getName());
        assertThat(nf.getPath()).isEqualTo(jna.getPath());
        assertThat(nf.getUser()).isEqualTo(jna.getUser());
        assertThat(nf.getUserID()).isEqualTo(jna.getUserID());
        assertThat(nf.getGroup()).isEqualTo(jna.getGroup());
        assertThat(nf.getGroupID()).isEqualTo(jna.getGroupID());
        assertThat(nf.getParentProcessID()).isEqualTo(jna.getParentProcessID());
        assertThat(nf.getCommandLine()).isEqualTo(jna.getCommandLine());
        assertThat(nf.getStartTime()).isEqualTo(jna.getStartTime());
    }

    // ---- OS: FileSystem ----

    @Test
    void fileSystem() {
        FileSystem jnaFs = jnaOs.getFileSystem();
        FileSystem nfFs = nfOs.getFileSystem();

        List<OSFileStore> jnaStores = jnaFs.getFileStores();
        List<OSFileStore> nfStores = nfFs.getFileStores();
        assertThat(nfStores).hasSameSizeAs(jnaStores);
        Map<String, OSFileStore> nfByMount = nfStores.stream()
                .collect(Collectors.toMap(OSFileStore::getMount, Function.identity(), (a, b) -> a));
        for (OSFileStore j : jnaStores) {
            OSFileStore nf = nfByMount.get(j.getMount());
            assertThat(nf).as("fileStore at %s", j.getMount()).isNotNull();
            assertThat(nf.getName()).isEqualTo(j.getName());
            assertThat(nf.getType()).isEqualTo(j.getType());
            assertThat(nf.getVolume()).isEqualTo(j.getVolume());
            assertThat(nf.getTotalSpace()).isEqualTo(j.getTotalSpace());
            assertWithinRatio(nf.getUsableSpace(), j.getUsableSpace(), 0.25, "usableSpace(" + j.getMount() + ")");
        }
    }

    // ---- OS: Network Params ----

    @Test
    void networkParams() {
        NetworkParams jna = jnaOs.getNetworkParams();
        NetworkParams nf = nfOs.getNetworkParams();
        // NF uses Java InetAddress (short hostname); JNA uses native gethostname (may return FQDN)
        assertThat(jna.getHostName()).startsWith(nf.getHostName());
        assertThat(nf.getDnsServers()).isEqualTo(jna.getDnsServers());
        assertThat(nf.getIpv4DefaultGateway()).isEqualTo(jna.getIpv4DefaultGateway());
        assertThat(nf.getIpv6DefaultGateway()).isEqualTo(jna.getIpv6DefaultGateway());
    }
}
