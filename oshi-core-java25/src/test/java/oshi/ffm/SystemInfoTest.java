/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.PlatformEnumFFM;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

@Execution(ExecutionMode.SAME_THREAD)
@EnabledForJreRange(min = JRE.JAVA_25)
public class SystemInfoTest {

    private static final Logger logger = LoggerFactory.getLogger(SystemInfoTest.class);

    @Test
    @SuppressWarnings("deprecation")
    public void testPlatformEnum() {
        assertThat("Unsupported OS", PlatformEnumFFM.getCurrentPlatform(), is(not(PlatformEnumFFM.UNSUPPORTED)));
        main(null);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGetCurrentPlatform() {
        assertNotNull(PlatformEnumFFM.getCurrentPlatform(), "Platform should not be null");
        assertNotEquals(PlatformEnumFFM.UNSUPPORTED, PlatformEnumFFM.getCurrentPlatform(),
                "Platform should be recognized");
    }

    @Test
    void testGetOperatingSystem() {
        assertNotNull(new SystemInfo().getOperatingSystem());
    }

    @Test
    void testGetHardware() {
        assertNotNull(new SystemInfo().getHardware());
    }

    public static void main(String[] args) {
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
}
