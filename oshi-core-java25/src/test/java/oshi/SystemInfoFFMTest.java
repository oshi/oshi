/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnabledForJreRange(min = JRE.JAVA_24)
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

        logger.info("Initializing System...");
        SystemInfoFFM si = new SystemInfoFFM();

        HardwareAbstractionLayer hal = si.getHardware();
        OperatingSystem os = si.getOperatingSystem();

        printOperatingSystem(os);

        logger.info("Checking computer system...");
        printComputerSystem(hal.getComputerSystem());

        logger.info("Checking Processor...");
        printProcessor(hal.getProcessor());

        logger.info("Checking Memory...");
        printMemory(hal.getMemory());

        logger.info("Checking CPU...");
        printCpu(hal.getProcessor());

        logger.info("Checking Processes...");
        printProcesses(os, hal.getMemory());

        logger.info("Checking Services...");
        printServices(os);

        logger.info("Checking Sensors...");
        printSensors(hal.getSensors());

        logger.info("Checking Power sources...");
        printPowerSources(hal.getPowerSources());

        logger.info("Checking Disks...");
        printDisks(hal.getDiskStores());

        logger.info("Checking Logical Volume Groups ...");
        printLVgroups(hal.getLogicalVolumeGroups());

        logger.info("Checking File System...");
        printFileSystem(os.getFileSystem());

        logger.info("Checking Network interfaces...");
        printNetworkInterfaces(hal.getNetworkIFs());

        logger.info("Checking Network parameters...");
        printNetworkParameters(os.getNetworkParams());

        logger.info("Checking IP statistics...");
        printInternetProtocolStats(os.getInternetProtocolStats());

        logger.info("Checking Displays...");
        printDisplays(hal.getDisplays());

        logger.info("Checking USB Devices...");
        printUsbDevices(hal.getUsbDevices(true));

        logger.info("Checking Sound Cards...");
        printSoundCards(hal.getSoundCards());

        logger.info("Checking Graphics Cards...");
        printGraphicsCards(hal.getGraphicsCards());

        StringBuilder output = new StringBuilder();
        for (String line : oshi) {
            output.append(line);
            if (line != null && !line.endsWith("\n")) {
                output.append('\n');
            }
        }
        logger.info("Printing Operating System and Hardware Info:{}{}", '\n', output);
    }

}
