/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

@Execution(ExecutionMode.SAME_THREAD)
@EnabledForJreRange(min = JRE.JAVA_25)
public class SystemInfoFFMTest extends SystemInfoTest {

    private static final Logger logger = LoggerFactory.getLogger(SystemInfoFFMTest.class);

    /**
     * Test that this platform is implemented..
     */
    @Override
    @Test
    void testPlatformEnum() {
        assertThat("Unsupported OS", SystemInfoFFM.getCurrentPlatform(), is(not(PlatformEnumFFM.UNSUPPORTED)));
        // Exercise the main method
        main(null);
    }

    @Test
    public void testGetCurrentPlatform() {
        PlatformEnumFFM platform = SystemInfoFFM.getCurrentPlatform();
        assertNotNull(platform, "Platform should not be null");
        assertNotEquals(PlatformEnumFFM.UNSUPPORTED, platform, "Platform should be recognized");
    }

    @Test
    void testGetOperatingSystem() {
        SystemInfoFFM si = new SystemInfoFFM();
        OperatingSystem os = si.getOperatingSystem();
        assertNotNull(os);
    }

    @Test
    void testGetHardware() {
        SystemInfoFFM si = new SystemInfoFFM();
        HardwareAbstractionLayer hw = si.getHardware();
        assertNotNull(hw);
    }

    /**
     * The main method, demonstrating use of classes.
     *
     * @param args the arguments (unused)
     */
    public static void main(String[] args) {

        logger.info("------------------------------------------------------------------------");
        logger.info("Using FFM");
        logger.info("------------------------------------------------------------------------");
        logger.info("Initializing System...");
        SystemInfoFFM si = new SystemInfoFFM();

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
