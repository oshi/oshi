/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.comparison;

import static org.assertj.core.api.Assertions.assertThat;
import static oshi.comparison.ComparisonAssertions.assertWithinRatio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.driver.common.windows.wmi.LhmSensor.LhmHardwareProperty;
import oshi.driver.common.windows.wmi.LhmSensor.LhmSensorProperty;
import oshi.driver.common.windows.wmi.MSAcpiThermalZoneTemperature.TemperatureProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.PhysicalDiskProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.StoragePoolProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.StoragePoolToPhysicalDiskProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.VirtualDiskProperty;
import oshi.driver.common.windows.wmi.OhmHardware.IdentifierProperty;
import oshi.driver.common.windows.wmi.OhmSensor.ValueProperty;
import oshi.driver.common.windows.wmi.Win32BaseBoard.BaseBoardProperty;
import oshi.driver.common.windows.wmi.Win32Bios.BiosProperty;
import oshi.driver.common.windows.wmi.Win32Bios.BiosSerialProperty;
import oshi.driver.common.windows.wmi.Win32ComputerSystem.ComputerSystemProperty;
import oshi.driver.common.windows.wmi.Win32ComputerSystemProduct.ComputerSystemProductProperty;
import oshi.driver.common.windows.wmi.Win32DiskDrive.DiskDriveProperty;
import oshi.driver.common.windows.wmi.Win32DiskDriveToDiskPartition.DriveToPartitionProperty;
import oshi.driver.common.windows.wmi.Win32DiskPartition.DiskPartitionProperty;
import oshi.driver.common.windows.wmi.Win32Fan.SpeedProperty;
import oshi.driver.common.windows.wmi.Win32LogicalDisk.LogicalDiskProperty;
import oshi.driver.common.windows.wmi.Win32LogicalDiskToPartition.DiskToPartitionProperty;
import oshi.driver.common.windows.wmi.Win32OperatingSystem.OSVersionProperty;
import oshi.driver.common.windows.wmi.Win32PhysicalMemory.PhysicalMemoryProperty;
import oshi.driver.common.windows.wmi.Win32Printer.PrinterProperty;
import oshi.driver.common.windows.wmi.Win32Process.CommandLineProperty;
import oshi.driver.common.windows.wmi.Win32Process.ProcessXPProperty;
import oshi.driver.common.windows.wmi.Win32Processor.BitnessProperty;
import oshi.driver.common.windows.wmi.Win32Processor.ProcessorIdProperty;
import oshi.driver.common.windows.wmi.Win32Processor.VoltProperty;
import oshi.driver.common.windows.wmi.Win32VideoController.VideoControllerProperty;
import oshi.driver.windows.wmi.LhmSensorFFM;
import oshi.driver.windows.wmi.LhmSensorJNA;
import oshi.driver.windows.wmi.MSAcpiThermalZoneTemperatureFFM;
import oshi.driver.windows.wmi.MSAcpiThermalZoneTemperatureJNA;
import oshi.driver.windows.wmi.MSFTStorageFFM;
import oshi.driver.windows.wmi.MSFTStorageJNA;
import oshi.driver.windows.wmi.OhmHardwareFFM;
import oshi.driver.windows.wmi.OhmHardwareJNA;
import oshi.driver.windows.wmi.OhmSensorFFM;
import oshi.driver.windows.wmi.OhmSensorJNA;
import oshi.driver.windows.wmi.Win32BaseBoardFFM;
import oshi.driver.windows.wmi.Win32BaseBoardJNA;
import oshi.driver.windows.wmi.Win32BiosFFM;
import oshi.driver.windows.wmi.Win32BiosJNA;
import oshi.driver.windows.wmi.Win32ComputerSystemFFM;
import oshi.driver.windows.wmi.Win32ComputerSystemJNA;
import oshi.driver.windows.wmi.Win32ComputerSystemProductFFM;
import oshi.driver.windows.wmi.Win32ComputerSystemProductJNA;
import oshi.driver.windows.wmi.Win32DiskDriveFFM;
import oshi.driver.windows.wmi.Win32DiskDriveJNA;
import oshi.driver.windows.wmi.Win32DiskDriveToDiskPartitionFFM;
import oshi.driver.windows.wmi.Win32DiskDriveToDiskPartitionJNA;
import oshi.driver.windows.wmi.Win32DiskPartitionFFM;
import oshi.driver.windows.wmi.Win32DiskPartitionJNA;
import oshi.driver.windows.wmi.Win32FanFFM;
import oshi.driver.windows.wmi.Win32FanJNA;
import oshi.driver.windows.wmi.Win32LogicalDiskFFM;
import oshi.driver.windows.wmi.Win32LogicalDiskJNA;
import oshi.driver.windows.wmi.Win32LogicalDiskToPartitionFFM;
import oshi.driver.windows.wmi.Win32LogicalDiskToPartitionJNA;
import oshi.driver.windows.wmi.Win32OperatingSystemFFM;
import oshi.driver.windows.wmi.Win32OperatingSystemJNA;
import oshi.driver.windows.wmi.Win32PhysicalMemoryFFM;
import oshi.driver.windows.wmi.Win32PhysicalMemoryJNA;
import oshi.driver.windows.wmi.Win32PrinterFFM;
import oshi.driver.windows.wmi.Win32PrinterJNA;
import oshi.driver.windows.wmi.Win32ProcessFFM;
import oshi.driver.windows.wmi.Win32ProcessJNA;
import oshi.driver.windows.wmi.Win32ProcessorFFM;
import oshi.driver.windows.wmi.Win32ProcessorJNA;
import oshi.driver.windows.wmi.Win32VideoControllerFFM;
import oshi.driver.windows.wmi.Win32VideoControllerJNA;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM;
import oshi.ffm.util.platform.windows.WmiQueryHandlerFFM;
import oshi.ffm.util.platform.windows.WmiUtilFFM;
import oshi.util.PlatformEnum;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

/**
 * Compares JNA and FFM WMI driver implementations to verify they return equivalent results.
 */
@DisabledIf("isNotWindows")
class WmiComparisonTest {

    // --- LogicalDisk: exhaustive field-by-field comparison ---
    // This test intentionally validates every WMI value type (string, uint16, uint32, uint64)
    // so the remaining driver tests can focus on result count and a few representative fields.

    @Test
    void testLogicalDiskUnfiltered() {
        WmiResult<LogicalDiskProperty> jna = Win32LogicalDiskJNA.queryLogicalDisk(null, false);
        WbemcliUtilFFM.WmiResult<LogicalDiskProperty> ffm = Win32LogicalDiskFFM.queryLogicalDisk(null, false);

        assertThat(ffm.getResultCount()).as("LogicalDisk result count").isEqualTo(jna.getResultCount());

        var jnaByName = buildJnaStringMap(jna, LogicalDiskProperty.NAME);
        var ffmByName = buildFfmStringMap(ffm, LogicalDiskProperty.NAME);

        assertThat(ffmByName.keySet()).as("LogicalDisk names").containsExactlyInAnyOrderElementsOf(jnaByName.keySet());

        for (String name : jnaByName.keySet()) {
            List<Integer> jnaIndices = jnaByName.get(name);
            List<Integer> ffmIndices = ffmByName.get(name);
            assertThat(ffmIndices).as("LogicalDisk index count [" + name + "]").hasSameSizeAs(jnaIndices);
            for (int idx = 0; idx < jnaIndices.size(); idx++) {
                int ji = jnaIndices.get(idx);
                int fi = ffmIndices.get(idx);
                for (LogicalDiskProperty key : LogicalDiskProperty.values()) {
                    String desc = "LogicalDisk " + key + " [" + name + "]";
                    switch (key) {
                        case NAME, DESCRIPTION, FILESYSTEM, VOLUMENAME, PROVIDERNAME -> assertThat(
                                WmiUtilFFM.getString(ffm, key, fi)).as(desc).isEqualTo(WmiUtil.getString(jna, key, ji));
                        case DRIVETYPE -> assertThat(WmiUtilFFM.getUint32(ffm, key, fi)).as(desc)
                                .isEqualTo(WmiUtil.getUint32(jna, key, ji));
                        case ACCESS -> assertThat(WmiUtilFFM.getUint16(ffm, key, fi)).as(desc)
                                .isEqualTo(WmiUtil.getUint16(jna, key, ji));
                        case SIZE -> assertThat(WmiUtilFFM.getUint64(ffm, key, fi)).as(desc)
                                .isEqualTo(WmiUtil.getUint64(jna, key, ji));
                        default -> assertWithinRatio(WmiUtilFFM.getUint64(ffm, key, fi),
                                WmiUtil.getUint64(jna, key, ji), 0.05, desc);
                    }
                }
            }
        }
    }

    @Test
    void testLogicalDiskLocalOnly() {
        WmiResult<LogicalDiskProperty> jna = Win32LogicalDiskJNA.queryLogicalDisk(null, true);
        WbemcliUtilFFM.WmiResult<LogicalDiskProperty> ffm = Win32LogicalDiskFFM.queryLogicalDisk(null, true);

        assertThat(ffm.getResultCount()).as("LogicalDisk local-only result count").isEqualTo(jna.getResultCount());

        var jnaByName = buildJnaStringMap(jna, LogicalDiskProperty.NAME);
        var ffmByName = buildFfmStringMap(ffm, LogicalDiskProperty.NAME);

        assertThat(ffmByName.keySet()).as("LogicalDisk local names")
                .containsExactlyInAnyOrderElementsOf(jnaByName.keySet());

        for (String name : jnaByName.keySet()) {
            int fi = ffmByName.get(name).get(0);
            int driveType = WmiUtilFFM.getUint32(ffm, LogicalDiskProperty.DRIVETYPE, fi);
            assertThat(driveType).as("LogicalDisk local DRIVETYPE [%s]", name).isIn(2, 3, 6);
        }
    }

    @Test
    void testLogicalDiskNameFilter() {
        WmiResult<LogicalDiskProperty> all = Win32LogicalDiskJNA.queryLogicalDisk(null, false);
        Assumptions.assumeTrue(all.getResultCount() > 0, "No logical disks found");
        String firstName = WmiUtil.getString(all, LogicalDiskProperty.NAME, 0);

        WmiResult<LogicalDiskProperty> jna = Win32LogicalDiskJNA.queryLogicalDisk(firstName, false);
        WbemcliUtilFFM.WmiResult<LogicalDiskProperty> ffm = Win32LogicalDiskFFM.queryLogicalDisk(firstName, false);

        assertThat(ffm.getResultCount()).as("LogicalDisk filtered result count").isEqualTo(jna.getResultCount());
        assertThat(jna.getResultCount()).as("LogicalDisk filtered should find match").isGreaterThan(0);

        assertThat(WmiUtilFFM.getString(ffm, LogicalDiskProperty.NAME, 0)).as("LogicalDisk filtered NAME")
                .isEqualTo(firstName);
    }

    // --- Single-row drivers: compare all string properties ---

    @Test
    void testBaseBoard() {
        WmiResult<BaseBoardProperty> jna = Win32BaseBoardJNA.queryBaseboardInfo();
        WbemcliUtilFFM.WmiResult<BaseBoardProperty> ffm = Win32BaseBoardFFM.queryBaseboardInfo();
        assertThat(ffm.getResultCount()).as("BaseBoard count").isEqualTo(jna.getResultCount());
        Assumptions.assumeTrue(jna.getResultCount() > 0, "No baseboard found");
        for (BaseBoardProperty p : BaseBoardProperty.values()) {
            assertThat(WmiUtilFFM.getString(ffm, p, 0)).as("BaseBoard " + p).isEqualTo(WmiUtil.getString(jna, p, 0));
        }
    }

    @Test
    void testBiosInfo() {
        WmiResult<BiosProperty> jna = Win32BiosJNA.queryBiosInfo();
        WbemcliUtilFFM.WmiResult<BiosProperty> ffm = Win32BiosFFM.queryBiosInfo();
        assertThat(ffm.getResultCount()).as("Bios count").isEqualTo(jna.getResultCount());
        Assumptions.assumeTrue(jna.getResultCount() > 0, "No BIOS found");
        for (BiosProperty p : BiosProperty.values()) {
            if (p == BiosProperty.RELEASEDATE) {
                assertThat(WmiUtilFFM.getDateString(ffm, p, 0)).as("Bios " + p)
                        .isEqualTo(WmiUtil.getDateString(jna, p, 0));
            } else {
                assertThat(WmiUtilFFM.getString(ffm, p, 0)).as("Bios " + p).isEqualTo(WmiUtil.getString(jna, p, 0));
            }
        }
    }

    @Test
    void testBiosSerialNumber() {
        WmiResult<BiosSerialProperty> jna = Win32BiosJNA.querySerialNumber();
        WbemcliUtilFFM.WmiResult<BiosSerialProperty> ffm = Win32BiosFFM.querySerialNumber();
        assertThat(ffm.getResultCount()).as("BiosSerial count").isEqualTo(jna.getResultCount());
        Assumptions.assumeTrue(jna.getResultCount() > 0, "No BIOS serial found");
        assertThat(WmiUtilFFM.getString(ffm, BiosSerialProperty.SERIALNUMBER, 0)).as("BiosSerial SERIALNUMBER")
                .isEqualTo(WmiUtil.getString(jna, BiosSerialProperty.SERIALNUMBER, 0));
    }

    @Test
    void testComputerSystem() {
        WmiResult<ComputerSystemProperty> jna = Win32ComputerSystemJNA.queryComputerSystem();
        WbemcliUtilFFM.WmiResult<ComputerSystemProperty> ffm = Win32ComputerSystemFFM.queryComputerSystem();
        assertThat(ffm.getResultCount()).as("ComputerSystem count").isEqualTo(jna.getResultCount());
        Assumptions.assumeTrue(jna.getResultCount() > 0, "No computer system found");
        for (ComputerSystemProperty p : ComputerSystemProperty.values()) {
            assertThat(WmiUtilFFM.getString(ffm, p, 0)).as("ComputerSystem " + p)
                    .isEqualTo(WmiUtil.getString(jna, p, 0));
        }
    }

    @Test
    void testComputerSystemProduct() {
        WmiResult<ComputerSystemProductProperty> jna = Win32ComputerSystemProductJNA.queryIdentifyingNumberUUID();
        WbemcliUtilFFM.WmiResult<ComputerSystemProductProperty> ffm = Win32ComputerSystemProductFFM
                .queryIdentifyingNumberUUID();
        assertThat(ffm.getResultCount()).as("ComputerSystemProduct count").isEqualTo(jna.getResultCount());
        Assumptions.assumeTrue(jna.getResultCount() > 0, "No computer system product found");
        for (ComputerSystemProductProperty p : ComputerSystemProductProperty.values()) {
            assertThat(WmiUtilFFM.getString(ffm, p, 0)).as("ComputerSystemProduct " + p)
                    .isEqualTo(WmiUtil.getString(jna, p, 0));
        }
    }

    @Test
    void testOperatingSystem() {
        WmiResult<OSVersionProperty> jna = Win32OperatingSystemJNA.queryOsVersion();
        WbemcliUtilFFM.WmiResult<OSVersionProperty> ffm = Win32OperatingSystemFFM.queryOsVersion();
        assertThat(ffm.getResultCount()).as("OS count").isEqualTo(jna.getResultCount());
        Assumptions.assumeTrue(jna.getResultCount() > 0, "No OS found");
        for (OSVersionProperty p : OSVersionProperty.values()) {
            switch (p) {
                case PRODUCTTYPE, SUITEMASK -> assertThat(WmiUtilFFM.getUint32(ffm, p, 0)).as("OS " + p)
                        .isEqualTo(WmiUtil.getUint32(jna, p, 0));
                default -> assertThat(WmiUtilFFM.getString(ffm, p, 0)).as("OS " + p)
                        .isEqualTo(WmiUtil.getString(jna, p, 0));
            }
        }
    }

    @Test
    void testProcessor() {
        WmiResult<ProcessorIdProperty> jna = Win32ProcessorJNA.queryProcessorId();
        WbemcliUtilFFM.WmiResult<ProcessorIdProperty> ffm = Win32ProcessorFFM.queryProcessorId();
        assertThat(ffm.getResultCount()).as("ProcessorId count").isEqualTo(jna.getResultCount());
        Assumptions.assumeTrue(jna.getResultCount() > 0, "No processor found");
        for (int i = 0; i < jna.getResultCount(); i++) {
            assertThat(WmiUtilFFM.getString(ffm, ProcessorIdProperty.PROCESSORID, i)).as("PROCESSORID [" + i + "]")
                    .isEqualTo(WmiUtil.getString(jna, ProcessorIdProperty.PROCESSORID, i));
        }

        WmiResult<BitnessProperty> jnaBit = Win32ProcessorJNA.queryBitness();
        WbemcliUtilFFM.WmiResult<BitnessProperty> ffmBit = Win32ProcessorFFM.queryBitness();
        assertThat(ffmBit.getResultCount()).as("Bitness count").isEqualTo(jnaBit.getResultCount());
        Assumptions.assumeTrue(jnaBit.getResultCount() > 0, "No bitness found");
        for (int i = 0; i < jnaBit.getResultCount(); i++) {
            assertThat(WmiUtilFFM.getUint16(ffmBit, BitnessProperty.ADDRESSWIDTH, i)).as("ADDRESSWIDTH [" + i + "]")
                    .isEqualTo(WmiUtil.getUint16(jnaBit, BitnessProperty.ADDRESSWIDTH, i));
        }

        WmiResult<VoltProperty> jnaVolt = Win32ProcessorJNA.queryVoltage();
        WbemcliUtilFFM.WmiResult<VoltProperty> ffmVolt = Win32ProcessorFFM.queryVoltage();
        assertThat(ffmVolt.getResultCount()).as("Voltage count").isEqualTo(jnaVolt.getResultCount());
        Assumptions.assumeTrue(jnaVolt.getResultCount() > 0, "No voltage found");
        for (int i = 0; i < jnaVolt.getResultCount(); i++) {
            assertThat(WmiUtilFFM.getUint16(ffmVolt, VoltProperty.CURRENTVOLTAGE, i)).as("CURRENTVOLTAGE [" + i + "]")
                    .isEqualTo(WmiUtil.getUint16(jnaVolt, VoltProperty.CURRENTVOLTAGE, i));
        }
    }

    // --- Multi-row drivers with key fields ---

    @Test
    void testPhysicalMemory() {
        WmiResult<PhysicalMemoryProperty> jna = Win32PhysicalMemoryJNA.queryPhysicalMemory();
        WbemcliUtilFFM.WmiResult<PhysicalMemoryProperty> ffm = Win32PhysicalMemoryFFM.queryPhysicalMemory();
        assertThat(ffm.getResultCount()).as("PhysicalMemory count").isEqualTo(jna.getResultCount());
        Assumptions.assumeTrue(jna.getResultCount() > 0, "No physical memory found");

        var jnaByKey = buildJnaStringMap(jna, PhysicalMemoryProperty.SERIALNUMBER);
        var ffmByKey = buildFfmStringMap(ffm, PhysicalMemoryProperty.SERIALNUMBER);
        assertThat(ffmByKey.keySet()).as("PhysicalMemory keys").containsExactlyInAnyOrderElementsOf(jnaByKey.keySet());

        for (String key : jnaByKey.keySet()) {
            List<Integer> jnaIndices = jnaByKey.get(key);
            List<Integer> ffmIndices = ffmByKey.get(key);
            assertThat(ffmIndices).as("PhysicalMemory index count [" + key + "]").hasSameSizeAs(jnaIndices);
            for (int idx = 0; idx < jnaIndices.size(); idx++) {
                int ji = jnaIndices.get(idx);
                int fi = ffmIndices.get(idx);
                assertThat(WmiUtilFFM.getString(ffm, PhysicalMemoryProperty.BANKLABEL, fi))
                        .as("BANKLABEL [" + key + "#" + idx + "]")
                        .isEqualTo(WmiUtil.getString(jna, PhysicalMemoryProperty.BANKLABEL, ji));
                assertThat(WmiUtilFFM.getUint64(ffm, PhysicalMemoryProperty.CAPACITY, fi))
                        .as("CAPACITY [" + key + "#" + idx + "]")
                        .isEqualTo(WmiUtil.getUint64(jna, PhysicalMemoryProperty.CAPACITY, ji));
                assertThat(WmiUtilFFM.getString(ffm, PhysicalMemoryProperty.MANUFACTURER, fi))
                        .as("MANUFACTURER [" + key + "#" + idx + "]")
                        .isEqualTo(WmiUtil.getString(jna, PhysicalMemoryProperty.MANUFACTURER, ji));
            }
        }
    }

    @Test
    void testVideoController() {
        WmiResult<VideoControllerProperty> jna = Win32VideoControllerJNA.queryVideoController();
        WbemcliUtilFFM.WmiResult<VideoControllerProperty> ffm = Win32VideoControllerFFM.queryVideoController();
        assertThat(ffm.getResultCount()).as("VideoController count").isEqualTo(jna.getResultCount());
        if (jna.getResultCount() == 0) {
            return;
        }

        var jnaByKey = buildJnaStringMap(jna, VideoControllerProperty.PNPDEVICEID);
        var ffmByKey = buildFfmStringMap(ffm, VideoControllerProperty.PNPDEVICEID);
        assertThat(ffmByKey.keySet()).as("VideoController keys").containsExactlyInAnyOrderElementsOf(jnaByKey.keySet());

        for (String key : jnaByKey.keySet()) {
            List<Integer> jnaIndices = jnaByKey.get(key);
            List<Integer> ffmIndices = ffmByKey.get(key);
            assertThat(ffmIndices).as("VideoController index count [" + key + "]").hasSameSizeAs(jnaIndices);
            for (int idx = 0; idx < jnaIndices.size(); idx++) {
                int ji = jnaIndices.get(idx);
                int fi = ffmIndices.get(idx);
                assertThat(WmiUtilFFM.getString(ffm, VideoControllerProperty.NAME, fi)).as("VC NAME [" + key + "]")
                        .isEqualTo(WmiUtil.getString(jna, VideoControllerProperty.NAME, ji));
                assertThat(WmiUtilFFM.getString(ffm, VideoControllerProperty.DRIVERVERSION, fi))
                        .as("VC DRIVERVERSION [" + key + "]")
                        .isEqualTo(WmiUtil.getString(jna, VideoControllerProperty.DRIVERVERSION, ji));
            }
        }
    }

    @Test
    void testPrinter() {
        WmiResult<PrinterProperty> jna = Win32PrinterJNA.queryPrinters();
        WbemcliUtilFFM.WmiResult<PrinterProperty> ffm = Win32PrinterFFM.queryPrinters();
        assertThat(ffm.getResultCount()).as("Printer count").isEqualTo(jna.getResultCount());
        if (jna.getResultCount() == 0) {
            return;
        }

        var jnaByKey = buildJnaStringMap(jna, PrinterProperty.NAME);
        var ffmByKey = buildFfmStringMap(ffm, PrinterProperty.NAME);
        assertThat(ffmByKey.keySet()).as("Printer names").containsExactlyInAnyOrderElementsOf(jnaByKey.keySet());

        for (String name : jnaByKey.keySet()) {
            List<Integer> jnaIndices = jnaByKey.get(name);
            List<Integer> ffmIndices = ffmByKey.get(name);
            assertThat(ffmIndices).as("Printer index count [" + name + "]").hasSameSizeAs(jnaIndices);
            for (int idx = 0; idx < jnaIndices.size(); idx++) {
                int ji = jnaIndices.get(idx);
                int fi = ffmIndices.get(idx);
                assertThat(WmiUtilFFM.getString(ffm, PrinterProperty.DRIVERNAME, fi))
                        .as("Printer DRIVERNAME [" + name + "]")
                        .isEqualTo(WmiUtil.getString(jna, PrinterProperty.DRIVERNAME, ji));
                // Boolean type validation
                assertThat(ffm.getValue(PrinterProperty.DEFAULT, fi)).as("Printer DEFAULT [" + name + "]")
                        .isEqualTo(jna.getValue(PrinterProperty.DEFAULT, ji));
                assertThat(ffm.getValue(PrinterProperty.LOCAL, fi)).as("Printer LOCAL [" + name + "]")
                        .isEqualTo(jna.getValue(PrinterProperty.LOCAL, ji));
            }
        }
    }

    // --- Count-only comparisons for volatile/optional data ---

    @Test
    void testWin32ProcessCommandLines() {
        Set<Integer> currentPid = currentPidFilter();
        WmiResult<CommandLineProperty> jna = Win32ProcessJNA.queryCommandLines(currentPid);
        WbemcliUtilFFM.WmiResult<CommandLineProperty> ffm = Win32ProcessFFM.queryCommandLines(currentPid);
        assertThat(ffm.getResultCount()).as("Win32Process commandLine count").isEqualTo(jna.getResultCount());
        if (jna.getResultCount() > 0) {
            assertThat(WmiUtilFFM.getUint32(ffm, CommandLineProperty.PROCESSID, 0)).as("Win32Process PID")
                    .isEqualTo(WmiUtil.getUint32(jna, CommandLineProperty.PROCESSID, 0));
            assertThat(WmiUtilFFM.getString(ffm, CommandLineProperty.COMMANDLINE, 0)).as("Win32Process commandLine")
                    .isEqualTo(WmiUtil.getString(jna, CommandLineProperty.COMMANDLINE, 0));
        }
    }

    @Test
    void testWin32ProcessXP() {
        Set<Integer> currentPid = currentPidFilter();
        WmiResult<ProcessXPProperty> jna = Win32ProcessJNA.queryProcesses(currentPid);
        WbemcliUtilFFM.WmiResult<ProcessXPProperty> ffm = Win32ProcessFFM.queryProcesses(currentPid);
        assertThat(ffm.getResultCount()).as("Win32Process XP count").isEqualTo(jna.getResultCount());
    }

    @Test
    void testLhmSensors() {
        // LHM may not be installed; just verify both return the same count
        WmiResult<LhmSensorProperty> jna = LhmSensorJNA.querySensors(null, null);
        WbemcliUtilFFM.WmiResult<LhmSensorProperty> ffm = LhmSensorFFM.querySensors(null, null);
        assertThat(ffm.getResultCount()).as("LhmSensor count").isEqualTo(jna.getResultCount());
    }

    @Test
    void testLhmGpuHardware() {
        WmiResult<LhmHardwareProperty> jna = LhmSensorJNA.queryGpuHardware();
        WbemcliUtilFFM.WmiResult<LhmHardwareProperty> ffm = LhmSensorFFM.queryGpuHardware();
        assertThat(ffm.getResultCount()).as("LhmGpuHardware count").isEqualTo(jna.getResultCount());
    }

    @Test
    void testOhmSensors() {
        // OHM may not be installed; just verify both return the same count
        WmiQueryHandler jnaHandler = WmiQueryHandler.createInstance();
        Assumptions.assumeTrue(jnaHandler != null, "JNA WmiQueryHandler unavailable");
        WmiQueryHandlerFFM ffmHandler = WmiQueryHandlerFFM.createInstance();
        Assumptions.assumeTrue(ffmHandler != null, "FFM WmiQueryHandlerFFM unavailable");

        boolean jnaComInit = jnaHandler.initCOM();
        boolean ffmComInit = ffmHandler.initCOM();
        try {
            WmiResult<ValueProperty> jna = OhmSensorJNA.querySensorValue(jnaHandler, null, null);
            WbemcliUtilFFM.WmiResult<ValueProperty> ffm = OhmSensorFFM.querySensorValue(ffmHandler, null, null);
            assertThat(ffm.getResultCount()).as("OhmSensor count").isEqualTo(jna.getResultCount());

            WmiResult<IdentifierProperty> jnaHw = OhmHardwareJNA.queryHwIdentifier(jnaHandler, null, null);
            WbemcliUtilFFM.WmiResult<IdentifierProperty> ffmHw = OhmHardwareFFM.queryHwIdentifier(ffmHandler, null,
                    null);
            assertThat(ffmHw.getResultCount()).as("OhmHardware count").isEqualTo(jnaHw.getResultCount());
        } finally {
            if (ffmComInit) {
                ffmHandler.unInitCOM();
            }
            if (jnaComInit) {
                jnaHandler.unInitCOM();
            }
        }
    }

    @Test
    void testFanSpeed() {
        WmiResult<SpeedProperty> jna = Win32FanJNA.querySpeed();
        WbemcliUtilFFM.WmiResult<SpeedProperty> ffm = Win32FanFFM.querySpeed();
        assertThat(ffm.getResultCount()).as("Fan count").isEqualTo(jna.getResultCount());
    }

    @Test
    void testMSAcpiTemperature() {
        WmiResult<TemperatureProperty> jna = MSAcpiThermalZoneTemperatureJNA.queryCurrentTemperature();
        WbemcliUtilFFM.WmiResult<TemperatureProperty> ffm = MSAcpiThermalZoneTemperatureFFM.queryCurrentTemperature();
        // Both should succeed or both fail (may require admin privileges)
        assertThat(ffm.getResultCount() > 0).as("MSAcpi temperature parity").isEqualTo(jna.getResultCount() > 0);
    }

    // --- initCom=false drivers: manage COM ourselves ---

    @Test
    void testDiskDrivers() {
        WmiQueryHandler jnaHandler = WmiQueryHandler.createInstance();
        Assumptions.assumeTrue(jnaHandler != null, "JNA WmiQueryHandler unavailable");
        WmiQueryHandlerFFM ffmHandler = WmiQueryHandlerFFM.createInstance();
        Assumptions.assumeTrue(ffmHandler != null, "FFM WmiQueryHandlerFFM unavailable");

        boolean jnaComInit = jnaHandler.initCOM();
        boolean ffmComInit = ffmHandler.initCOM();
        try {
            // DiskDrive
            WmiResult<DiskDriveProperty> jnaDd = Win32DiskDriveJNA.queryDiskDrive(jnaHandler);
            WbemcliUtilFFM.WmiResult<DiskDriveProperty> ffmDd = Win32DiskDriveFFM.queryDiskDrive(ffmHandler);
            assertThat(ffmDd.getResultCount()).as("DiskDrive count").isEqualTo(jnaDd.getResultCount());

            // DiskDriveToDiskPartition
            WmiResult<DriveToPartitionProperty> jnaDtp = Win32DiskDriveToDiskPartitionJNA
                    .queryDriveToPartition(jnaHandler);
            WbemcliUtilFFM.WmiResult<DriveToPartitionProperty> ffmDtp = Win32DiskDriveToDiskPartitionFFM
                    .queryDriveToPartition(ffmHandler);
            assertThat(ffmDtp.getResultCount()).as("DriveToPartition count").isEqualTo(jnaDtp.getResultCount());

            // DiskPartition
            WmiResult<DiskPartitionProperty> jnaDp = Win32DiskPartitionJNA.queryPartition(jnaHandler);
            WbemcliUtilFFM.WmiResult<DiskPartitionProperty> ffmDp = Win32DiskPartitionFFM.queryPartition(ffmHandler);
            assertThat(ffmDp.getResultCount()).as("DiskPartition count").isEqualTo(jnaDp.getResultCount());

            // LogicalDiskToPartition
            WmiResult<DiskToPartitionProperty> jnaLtp = Win32LogicalDiskToPartitionJNA.queryDiskToPartition(jnaHandler);
            WbemcliUtilFFM.WmiResult<DiskToPartitionProperty> ffmLtp = Win32LogicalDiskToPartitionFFM
                    .queryDiskToPartition(ffmHandler);
            assertThat(ffmLtp.getResultCount()).as("LogicalDiskToPartition count").isEqualTo(jnaLtp.getResultCount());

            // MSFTStorage: StoragePools
            WmiResult<StoragePoolProperty> jnaSp = MSFTStorageJNA.queryStoragePools(jnaHandler);
            WbemcliUtilFFM.WmiResult<StoragePoolProperty> ffmSp = MSFTStorageFFM.queryStoragePools(ffmHandler);
            assertThat(ffmSp.getResultCount()).as("StoragePool count").isEqualTo(jnaSp.getResultCount());

            // MSFTStorage: PhysicalDisks
            WmiResult<PhysicalDiskProperty> jnaPd = MSFTStorageJNA.queryPhysicalDisks(jnaHandler);
            WbemcliUtilFFM.WmiResult<PhysicalDiskProperty> ffmPd = MSFTStorageFFM.queryPhysicalDisks(ffmHandler);
            assertThat(ffmPd.getResultCount()).as("MSFT PhysicalDisk count").isEqualTo(jnaPd.getResultCount());

            // MSFTStorage: VirtualDisks
            WmiResult<VirtualDiskProperty> jnaVd = MSFTStorageJNA.queryVirtualDisks(jnaHandler);
            WbemcliUtilFFM.WmiResult<VirtualDiskProperty> ffmVd = MSFTStorageFFM.queryVirtualDisks(ffmHandler);
            assertThat(ffmVd.getResultCount()).as("VirtualDisk count").isEqualTo(jnaVd.getResultCount());

            // MSFTStorage: StoragePoolToPhysicalDisk
            WmiResult<StoragePoolToPhysicalDiskProperty> jnaSpPd = MSFTStorageJNA
                    .queryStoragePoolPhysicalDisks(jnaHandler);
            WbemcliUtilFFM.WmiResult<StoragePoolToPhysicalDiskProperty> ffmSpPd = MSFTStorageFFM
                    .queryStoragePoolPhysicalDisks(ffmHandler);
            assertThat(ffmSpPd.getResultCount()).as("StoragePoolToPhysicalDisk count")
                    .isEqualTo(jnaSpPd.getResultCount());
        } finally {
            if (ffmComInit) {
                ffmHandler.unInitCOM();
            }
            if (jnaComInit) {
                jnaHandler.unInitCOM();
            }
        }
    }

    // --- Generic map builders ---

    private static <T extends Enum<T>> Map<String, List<Integer>> buildJnaStringMap(WmiResult<T> result, T keyProp) {
        Map<String, List<Integer>> map = new HashMap<>();
        for (int i = 0; i < result.getResultCount(); i++) {
            map.computeIfAbsent(WmiUtil.getString(result, keyProp, i), k -> new ArrayList<>()).add(i);
        }
        return map;
    }

    private static <T extends Enum<T>> Map<String, List<Integer>> buildFfmStringMap(WbemcliUtilFFM.WmiResult<T> result,
            T keyProp) {
        Map<String, List<Integer>> map = new HashMap<>();
        for (int i = 0; i < result.getResultCount(); i++) {
            map.computeIfAbsent(WmiUtilFFM.getString(result, keyProp, i), k -> new ArrayList<>()).add(i);
        }
        return map;
    }

    private static Set<Integer> currentPidFilter() {
        long pid = ProcessHandle.current().pid();
        Assumptions.assumeTrue(pid <= Integer.MAX_VALUE, "PID too large to fit in int");
        return Collections.singleton((int) pid);
    }

    static boolean isNotWindows() {
        return PlatformEnum.getCurrentPlatform() != PlatformEnum.WINDOWS;
    }
}
