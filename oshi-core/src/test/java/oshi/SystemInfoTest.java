/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.PhysicalProcessor;
import oshi.hardware.CentralProcessor.ProcessorCache;
import oshi.hardware.CentralProcessor.TickType;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
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
import oshi.software.os.OSService;
import oshi.software.os.OSSession;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystem.ProcessFiltering;
import oshi.software.os.OperatingSystem.ProcessSorting;
import oshi.util.FormatUtil;
import oshi.util.Util;

/**
 * A demonstration of access to many of OSHI's capabilities
 */
@Execution(ExecutionMode.SAME_THREAD)
@EnabledForJreRange(max = JRE.JAVA_25)
public class SystemInfoTest { // NOSONAR squid:S5786

    private static final Logger logger = LoggerFactory.getLogger(SystemInfoTest.class);

    /**
     * Test that this platform is implemented..
     */
    @Test
    void testPlatformEnum() {
        assertThat("Unsupported OS", SystemInfo.getCurrentPlatform(), is(not(PlatformEnum.UNKNOWN)));
        // Exercise the main method
        main(null);
    }

    /**
     * The main method, demonstrating use of classes.
     *
     * @param args the arguments (unused)
     */
    public static void main(String[] args) {

        logger.info("------------------------------------------------------------------------");
        logger.info("Using JNA");
        logger.info("------------------------------------------------------------------------");
        logger.info("Initializing System...");
        SystemInfo si = new SystemInfo();

        HardwareAbstractionLayer hal = si.getHardware();
        OperatingSystem os = si.getOperatingSystem();

        List<String> lines = new ArrayList<>();

        printOperatingSystem(lines, os);

        logger.info("Checking Installed Apps...");
        printInstalledApps(lines, os.getInstalledApplications());

        logger.info("Checking computer system...");
        printComputerSystem(lines, hal.getComputerSystem());

        logger.info("Checking Processor...");
        printProcessor(lines, hal.getProcessor());

        logger.info("Checking Memory...");
        printMemory(lines, hal.getMemory());

        logger.info("Checking CPU...");
        printCpu(lines, hal.getProcessor());

        logger.info("Checking Processes...");
        printProcesses(lines, os, hal.getMemory());

        logger.info("Checking Services...");
        printServices(lines, os);

        logger.info("Checking Sensors...");
        printSensors(lines, hal.getSensors());

        logger.info("Checking Power sources...");
        printPowerSources(lines, hal.getPowerSources());

        logger.info("Checking Disks...");
        printDisks(lines, hal.getDiskStores());

        logger.info("Checking Logical Volume Groups ...");
        printLVgroups(lines, hal.getLogicalVolumeGroups());

        logger.info("Checking File System...");
        printFileSystem(lines, os.getFileSystem());

        logger.info("Checking Network interfaces...");
        printNetworkInterfaces(lines, hal.getNetworkIFs());

        logger.info("Checking Network parameters...");
        printNetworkParameters(lines, os.getNetworkParams());

        logger.info("Checking IP statistics...");
        printInternetProtocolStats(lines, os.getInternetProtocolStats());

        logger.info("Checking Displays...");
        printDisplays(lines, hal.getDisplays());

        logger.info("Checking USB Devices...");
        printUsbDevices(lines, hal.getUsbDevices(true));

        logger.info("Checking Sound Cards...");
        printSoundCards(lines, hal.getSoundCards());

        logger.info("Checking Graphics Cards...");
        printGraphicsCards(lines, hal.getGraphicsCards());

        logger.info("Checking Printers...");
        printPrinters(lines, hal.getPrinters());

        StringBuilder output = new StringBuilder();
        for (String line : lines) {
            output.append(line);
            if (line != null && !line.endsWith("\n")) {
                output.append('\n');
            }
        }
        logger.info("Printing Operating System and Hardware Info:{}{}", '\n', output);
    }

    protected static void printOperatingSystem(List<String> lines, final OperatingSystem os) {
        lines.add(String.valueOf(os));
        lines.add("Booted: " + Instant.ofEpochSecond(os.getSystemBootTime()));
        lines.add("Uptime: " + FormatUtil.formatElapsedSecs(os.getSystemUptime()));
        lines.add("Running with" + (os.isElevated() ? "" : "out") + " elevated permissions.");
        lines.add("Sessions:");
        for (OSSession s : os.getSessions()) {
            lines.add(" " + s.toString());
        }
    }

    protected static void printInstalledApps(List<String> lines, List<ApplicationInfo> installedApplications) {
        lines.add("Apps: ");
        for (int i = 0; i < 5 && i < installedApplications.size(); i++) {
            lines.add(" " + installedApplications.get(i).toString());
        }
    }

    protected static void printComputerSystem(List<String> lines, final ComputerSystem computerSystem) {
        lines.add("System: " + computerSystem.toString());
        lines.add(" Firmware: " + computerSystem.getFirmware().toString());
        lines.add(" Baseboard: " + computerSystem.getBaseboard().toString());
    }

    protected static void printProcessor(List<String> lines, CentralProcessor processor) {
        lines.add(processor.toString());

        Map<Integer, Integer> efficiencyCount = new HashMap<>();
        int maxEfficiency = 0;
        for (PhysicalProcessor cpu : processor.getPhysicalProcessors()) {
            int eff = cpu.getEfficiency();
            efficiencyCount.merge(eff, 1, Integer::sum);
            if (eff > maxEfficiency) {
                maxEfficiency = eff;
            }
        }
        lines.add(" Topology:");
        lines.add(String.format(Locale.ROOT, "  %7s %4s %4s %4s %4s %4s", "LogProc", "P/E", "Proc", "Pkg", "NUMA",
                "PGrp"));
        for (PhysicalProcessor cpu : processor.getPhysicalProcessors()) {
            lines.add(String.format(Locale.ROOT, "  %7s %4s %4d %4s %4d %4d",
                    processor.getLogicalProcessors().stream()
                            .filter(p -> p.getPhysicalProcessorNumber() == cpu.getPhysicalProcessorNumber())
                            .filter(p -> p.getPhysicalPackageNumber() == cpu.getPhysicalPackageNumber())
                            .map(p -> Integer.toString(p.getProcessorNumber())).collect(Collectors.joining(",")),
                    cpu.getEfficiency() == maxEfficiency ? "P" : "E", cpu.getPhysicalProcessorNumber(),
                    cpu.getPhysicalPackageNumber(),
                    processor.getLogicalProcessors().stream()
                            .filter(p -> p.getPhysicalProcessorNumber() == cpu.getPhysicalProcessorNumber())
                            .filter(p -> p.getPhysicalPackageNumber() == cpu.getPhysicalPackageNumber())
                            .mapToInt(p -> p.getNumaNode()).findFirst().orElse(0),
                    processor.getLogicalProcessors().stream()
                            .filter(p -> p.getPhysicalProcessorNumber() == cpu.getPhysicalProcessorNumber())
                            .filter(p -> p.getPhysicalPackageNumber() == cpu.getPhysicalPackageNumber())
                            .mapToInt(p -> p.getProcessorGroup()).findFirst().orElse(0)));
        }
        List<ProcessorCache> caches = processor.getProcessorCaches();
        if (!caches.isEmpty()) {
            lines.add(" Caches:");
        }
        for (int i = 0; i < caches.size(); i++) {
            ProcessorCache cache = caches.get(i);
            boolean perCore = cache.getLevel() < 3;
            boolean pCore = perCore && i < caches.size() - 1 && cache.getLevel() == caches.get(i + 1).getLevel()
                    && cache.getType() == caches.get(i + 1).getType();
            boolean eCore = perCore && i > 0 && cache.getLevel() == caches.get(i - 1).getLevel()
                    && cache.getType() == caches.get(i - 1).getType();
            StringBuilder sb = new StringBuilder("  ").append(cache);
            if (perCore) {
                sb.append(" (per ");
                if (pCore) {
                    sb.append("P-");
                } else if (eCore) {
                    sb.append("E-");
                }
                sb.append("core)");
            }
            lines.add(sb.toString());
        }
    }

    protected static void printMemory(List<String> lines, GlobalMemory memory) {
        lines.add("Physical Memory: \n " + memory.toString());
        VirtualMemory vm = memory.getVirtualMemory();
        lines.add("Virtual Memory: \n " + vm.toString());
        List<PhysicalMemory> pmList = memory.getPhysicalMemory();
        if (!pmList.isEmpty()) {
            lines.add("Physical Memory: ");
            for (PhysicalMemory pm : pmList) {
                lines.add(" " + pm.toString());
            }
        }
    }

    protected static void printCpu(List<String> lines, CentralProcessor processor) {
        lines.add("Context Switches/Interrupts: " + processor.getContextSwitches() + " / " + processor.getInterrupts());

        long[] prevTicks = processor.getSystemCpuLoadTicks();
        long[][] prevProcTicks = processor.getProcessorCpuLoadTicks();
        lines.add("CPU, IOWait, and IRQ ticks @ 0 sec:" + Arrays.toString(prevTicks));
        // Wait a second...
        Util.sleep(1000);
        long[] ticks = processor.getSystemCpuLoadTicks();
        lines.add("CPU, IOWait, and IRQ ticks @ 1 sec:" + Arrays.toString(ticks));
        long user = ticks[TickType.USER.getIndex()] - prevTicks[TickType.USER.getIndex()];
        long nice = ticks[TickType.NICE.getIndex()] - prevTicks[TickType.NICE.getIndex()];
        long sys = ticks[TickType.SYSTEM.getIndex()] - prevTicks[TickType.SYSTEM.getIndex()];
        long idle = ticks[TickType.IDLE.getIndex()] - prevTicks[TickType.IDLE.getIndex()];
        long iowait = ticks[TickType.IOWAIT.getIndex()] - prevTicks[TickType.IOWAIT.getIndex()];
        long irq = ticks[TickType.IRQ.getIndex()] - prevTicks[TickType.IRQ.getIndex()];
        long softirq = ticks[TickType.SOFTIRQ.getIndex()] - prevTicks[TickType.SOFTIRQ.getIndex()];
        long steal = ticks[TickType.STEAL.getIndex()] - prevTicks[TickType.STEAL.getIndex()];
        long totalCpu = user + nice + sys + idle + iowait + irq + softirq + steal;

        lines.add(String.format(Locale.ROOT,
                "User: %.1f%% Nice: %.1f%% System: %.1f%% Idle: %.1f%% IOwait: %.1f%% IRQ: %.1f%% SoftIRQ: %.1f%% Steal: %.1f%%",
                100d * user / totalCpu, 100d * nice / totalCpu, 100d * sys / totalCpu, 100d * idle / totalCpu,
                100d * iowait / totalCpu, 100d * irq / totalCpu, 100d * softirq / totalCpu, 100d * steal / totalCpu));
        lines.add(String.format(Locale.ROOT, "CPU load: %.1f%%",
                processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100));
        double[] loadAverage = processor.getSystemLoadAverage(3);
        lines.add("CPU load averages:"
                + (loadAverage[0] < 0 ? " N/A" : String.format(Locale.ROOT, " %.2f", loadAverage[0]))
                + (loadAverage[1] < 0 ? " N/A" : String.format(Locale.ROOT, " %.2f", loadAverage[1]))
                + (loadAverage[2] < 0 ? " N/A" : String.format(Locale.ROOT, " %.2f", loadAverage[2])));
        // per core CPU
        StringBuilder procCpu = new StringBuilder("CPU load per processor:");
        double[] load = processor.getProcessorCpuLoadBetweenTicks(prevProcTicks);
        for (double avg : load) {
            procCpu.append(String.format(Locale.ROOT, " %.1f%%", avg * 100));
        }
        lines.add(procCpu.toString());
        long freq = processor.getProcessorIdentifier().getVendorFreq();
        if (freq > 0) {
            lines.add("Vendor Frequency: " + FormatUtil.formatHertz(freq));
        }
        freq = processor.getMaxFreq();
        if (freq > 0) {
            lines.add("Max Frequency: " + FormatUtil.formatHertz(freq));
        }
        long[] freqs = processor.getCurrentFreq();
        if (freqs[0] > 0) {
            StringBuilder sb = new StringBuilder("Current Frequencies: ");
            for (int i = 0; i < freqs.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(FormatUtil.formatHertz(freqs[i]));
            }
            lines.add(sb.toString());
        }
        if (!processor.getFeatureFlags().isEmpty()) {
            lines.add("CPU Features:");
            for (String features : processor.getFeatureFlags()) {
                lines.add("  " + features);
            }
        }
    }

    protected static void printProcesses(List<String> lines, OperatingSystem os, GlobalMemory memory) {
        OSProcess myProc = os.getProcess(os.getProcessId());
        // current process will never be null. Other code should check for null here
        lines.add(
                "My PID: " + myProc.getProcessID() + " with affinity " + Long.toBinaryString(myProc.getAffinityMask()));
        lines.add("My TID: " + os.getThreadId() + " with details " + os.getCurrentThread());

        lines.add("Processes: " + os.getProcessCount() + ", Threads: " + os.getThreadCount());
        // Sort by highest CPU
        List<OSProcess> procs = os.getProcesses(ProcessFiltering.ALL_PROCESSES, ProcessSorting.CPU_DESC, 5);
        lines.add("   PID  %CPU %MEM       VSZ       RSS   Private Name");
        for (int i = 0; i < procs.size(); i++) {
            OSProcess p = procs.get(i);
            lines.add(String.format(Locale.ROOT, " %5d %5.1f %4.1f %9s %9s %9s %s", p.getProcessID(),
                    100d * (p.getKernelTime() + p.getUserTime()) / p.getUpTime(),
                    100d * p.getResidentMemory() / memory.getTotal(), FormatUtil.formatBytes(p.getVirtualSize()),
                    FormatUtil.formatBytes(p.getResidentMemory()), FormatUtil.formatBytes(p.getPrivateResidentMemory()),
                    p.getName()));
        }
        OSProcess p = os.getProcess(os.getProcessId());
        lines.add("Current process arguments: ");
        for (String s : p.getArguments()) {
            lines.add("  " + s);
        }
        lines.add("Current process environment: ");
        for (Entry<String, String> e : p.getEnvironmentVariables().entrySet()) {
            lines.add("  " + e.getKey() + "=" + e.getValue());
        }
    }

    protected static void printServices(List<String> lines, OperatingSystem os) {
        lines.add("Services: ");
        lines.add("   PID   State   Name");
        // DO 5 each of running and stopped
        int i = 0;
        for (OSService s : os.getServices()) {
            if (s.getState().equals(OSService.State.RUNNING) && i++ < 5) {
                lines.add(String.format(Locale.ROOT, " %5d  %7s  %s", s.getProcessID(), s.getState(), s.getName()));
            }
        }
        i = 0;
        for (OSService s : os.getServices()) {
            if (s.getState().equals(OSService.State.STOPPED) && i++ < 5) {
                lines.add(String.format(Locale.ROOT, " %5d  %7s  %s", s.getProcessID(), s.getState(), s.getName()));
            }
        }
    }

    protected static void printSensors(List<String> lines, Sensors sensors) {
        lines.add("Sensors: " + sensors.toString());
    }

    protected static void printPowerSources(List<String> lines, List<PowerSource> list) {
        StringBuilder sb = new StringBuilder("Power Sources: ");
        if (list.isEmpty()) {
            sb.append("Unknown");
        }
        for (PowerSource powerSource : list) {
            sb.append("\n ").append(powerSource.toString());
        }
        lines.add(sb.toString());
    }

    protected static void printDisks(List<String> lines, List<HWDiskStore> list) {
        lines.add("Disks:");
        for (HWDiskStore disk : list) {
            lines.add(" " + disk.toString());

            List<HWPartition> partitions = disk.getPartitions();
            for (HWPartition part : partitions) {
                lines.add(" |-- " + part.toString());
            }
        }

    }

    protected static void printLVgroups(List<String> lines, List<LogicalVolumeGroup> list) {
        if (!list.isEmpty()) {
            lines.add("Logical Volume Groups:");
            for (LogicalVolumeGroup lvg : list) {
                lines.add(" " + lvg.toString());
            }
        }
    }

    protected static void printFileSystem(List<String> lines, FileSystem fileSystem) {
        lines.add("File System:");

        lines.add(String.format(Locale.ROOT, " File Descriptors: %d/%d", fileSystem.getOpenFileDescriptors(),
                fileSystem.getMaxFileDescriptors()));

        for (OSFileStore fs : fileSystem.getFileStores()) {
            long usable = fs.getUsableSpace();
            long total = fs.getTotalSpace();
            lines.add(String.format(Locale.ROOT,
                    " %s (%s) [%s] %s of %s free (%.1f%%), %s of %s files free (%.1f%%) is %s "
                            + (fs.getLogicalVolume() != null && fs.getLogicalVolume().length() > 0 ? "[%s]" : "%s")
                            + " and is mounted at %s",
                    fs.getName(), fs.getDescription().isEmpty() ? "file system" : fs.getDescription(), fs.getType(),
                    FormatUtil.formatBytes(usable), FormatUtil.formatBytes(fs.getTotalSpace()), 100d * usable / total,
                    FormatUtil.formatValue(fs.getFreeInodes(), ""), FormatUtil.formatValue(fs.getTotalInodes(), ""),
                    100d * fs.getFreeInodes() / fs.getTotalInodes(), fs.getVolume(), fs.getLogicalVolume(),
                    fs.getMount()));
        }
    }

    protected static void printNetworkInterfaces(List<String> lines, List<NetworkIF> list) {
        StringBuilder sb = new StringBuilder("Network Interfaces:");
        if (list.isEmpty()) {
            sb.append(" Unknown");
        } else {
            for (NetworkIF net : list) {
                sb.append("\n ").append(net.toString());
            }
        }
        lines.add(sb.toString());
    }

    protected static void printNetworkParameters(List<String> lines, NetworkParams networkParams) {
        lines.add("Network parameters:\n " + networkParams.toString());
    }

    protected static void printInternetProtocolStats(List<String> lines, InternetProtocolStats ip) {
        lines.add("Internet Protocol statistics:");
        lines.add(" TCPv4: " + ip.getTCPv4Stats());
        lines.add(" TCPv6: " + ip.getTCPv6Stats());
        lines.add(" UDPv4: " + ip.getUDPv4Stats());
        lines.add(" UDPv6: " + ip.getUDPv6Stats());
    }

    protected static void printDisplays(List<String> lines, List<Display> list) {
        lines.add("Displays:");
        int i = 0;
        for (Display display : list) {
            lines.add(" Display " + i + ":");
            lines.add(String.valueOf(display));
            i++;
        }
    }

    protected static void printUsbDevices(List<String> lines, List<UsbDevice> list) {
        lines.add("USB Devices:");
        for (UsbDevice usbDevice : list) {
            lines.add(String.valueOf(usbDevice));
        }
    }

    protected static void printSoundCards(List<String> lines, List<SoundCard> list) {
        lines.add("Sound Cards:");
        for (SoundCard card : list) {
            lines.add(" " + String.valueOf(card));
        }
    }

    protected static void printGraphicsCards(List<String> lines, List<GraphicsCard> list) {
        lines.add("Graphics Cards:");
        if (list.isEmpty()) {
            lines.add(" None detected.");
        } else {
            for (GraphicsCard card : list) {
                lines.add(" " + String.valueOf(card));
            }
        }
    }

    protected static void printPrinters(List<String> lines, List<Printer> list) {
        lines.add("Printers:");
        if (list.isEmpty()) {
            lines.add(" None detected.");
        } else {
            for (Printer printer : list) {
                lines.add(" " + String.valueOf(printer));
            }
        }
    }
}
