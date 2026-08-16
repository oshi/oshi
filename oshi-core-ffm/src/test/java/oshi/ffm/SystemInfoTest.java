/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static oshi.util.SystemInfoHelper.printBluetoothDevices;
import static oshi.util.SystemInfoHelper.printComputerSystem;
import static oshi.util.SystemInfoHelper.printCpu;
import static oshi.util.SystemInfoHelper.printDisks;
import static oshi.util.SystemInfoHelper.printDisplays;
import static oshi.util.SystemInfoHelper.printFileSystem;
import static oshi.util.SystemInfoHelper.printGraphicsCards;
import static oshi.util.SystemInfoHelper.printInstalledApps;
import static oshi.util.SystemInfoHelper.printInternetProtocolStats;
import static oshi.util.SystemInfoHelper.printLVgroups;
import static oshi.util.SystemInfoHelper.printMemory;
import static oshi.util.SystemInfoHelper.printNetworkInterfaces;
import static oshi.util.SystemInfoHelper.printNetworkParameters;
import static oshi.util.SystemInfoHelper.printOperatingSystem;
import static oshi.util.SystemInfoHelper.printPowerSources;
import static oshi.util.SystemInfoHelper.printPrinters;
import static oshi.util.SystemInfoHelper.printProcesses;
import static oshi.util.SystemInfoHelper.printProcessor;
import static oshi.util.SystemInfoHelper.printSensors;
import static oshi.util.SystemInfoHelper.printServices;
import static oshi.util.SystemInfoHelper.printSoundCards;
import static oshi.util.SystemInfoHelper.printUsbDevices;
import static oshi.util.SystemInfoHelper.printVirtualization;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;
import oshi.util.PlatformEnum;

@Execution(ExecutionMode.SAME_THREAD)
@EnabledForJreRange(min = JRE.JAVA_25)
@EnabledOnOs({ OS.LINUX, OS.MAC, OS.WINDOWS, OS.FREEBSD, OS.OPENBSD, OS.SOLARIS, OS.AIX })
public class SystemInfoTest {

    private static final Logger logger = LoggerFactory.getLogger(SystemInfoTest.class);

    SystemInfoTest() {
    }

    @Test
    void testPlatformEnum() {
        assertThat("Unsupported OS", PlatformEnum.getCurrentPlatform(), is(not(PlatformEnum.UNKNOWN)));
        main(null);
    }

    @Test
    void testGetOperatingSystem() {
        assertNotNull(new SystemInfo().getOperatingSystem());
    }

    @Test
    void testGetHardware() {
        assertNotNull(new SystemInfo().getHardware());
    }

    @Test
    void testIsAvailable() {
        // This suite only runs on JDK 25+ where the FFM API is present, so the provider must report available.
        assertThat("FFM provider should be available when java.lang.foreign is present", new SystemInfo().isAvailable(),
                is(true));
    }

    /**
     * Asserts that a GitHub-hosted runner, which is a virtual machine, is detected as one.
     * <p>
     * {@code RUNNER_ENVIRONMENT} is set to {@code github-hosted} by Actions itself, so this covers every hosted job
     * without per-workflow configuration and picks up new ones for free. It cannot run unconditionally: OSHI's tests
     * must also pass on physical hardware, where an empty result is the correct answer. The workflows set
     * {@code OSHI_BARE_METAL_RUNNER} on the one hosted runner that is not a guest.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "RUNNER_ENVIRONMENT", matches = "github-hosted")
    @DisabledIfEnvironmentVariable(named = "OSHI_BARE_METAL_RUNNER", matches = "true")
    void testVirtualizationDetected() {
        HardwareAbstractionLayer hal = new SystemInfo().getHardware();
        ComputerSystem cs = hal.getComputerSystem();
        // Report the signatures on failure, so the fix is to add whichever one is missing from the tables
        assertThat(
                "Runner is virtualized but no signature matched. CPU vendor: '"
                        + hal.getProcessor().getProcessorIdentifier().getVendor() + "', manufacturer: '"
                        + cs.getManufacturer() + "', model: '" + cs.getModel() + "', MACs: "
                        + hal.getNetworkIFs().stream().map(NetworkIF::getMacaddr).collect(Collectors.joining(", ")),
                hal.getVirtualization().isPresent(), is(true));
    }

    public static void main(String @Nullable [] args) {
        logger.info("------------------------------------------------------------------------");
        logger.info("Using FFM");
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

        logger.info("Checking virtualization...");
        printVirtualization(lines, hal);

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

        logger.info("Checking Bluetooth Devices...");
        printBluetoothDevices(lines, hal.getBluetoothDevices());

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
}
