/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;
import com.sun.jna.platform.win32.VersionHelpers;

import oshi.driver.common.windows.wmi.Win32LogicalDisk.LogicalDiskProperty;
import oshi.driver.common.windows.wmi.Win32Process.CommandLineProperty;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

@EnabledOnOs(OS.WINDOWS)
class WMIDriversTest {

    @Test
    void testQueryWMIMSAcpi() {
        assertThat("Failed MSAcpiThermalZoneTemperature.queryCurrentTemperature",
                MSAcpiThermalZoneTemperatureJNA.queryCurrentTemperature().getResultCount(),
                is(greaterThanOrEqualTo(0)));
    }

    @Test
    void testQueryWMIMSFT() {
        WmiQueryHandler handler = WmiQueryHandler.createInstance();
        assertThat(handler, is(notNullValue()));
        boolean comInit = handler.initCOM();

        assertThat("Failed MSFTStorage.queryPhysicalDisks", MSFTStorageJNA.queryPhysicalDisks(handler).getResultCount(),
                is(greaterThanOrEqualTo(0)));
        assertThat("Failed MSFTStorage.queryStoragePoolPhysicalDisks",
                MSFTStorageJNA.queryStoragePoolPhysicalDisks(handler).getResultCount(), is(greaterThanOrEqualTo(0)));
        assertThat("Failed MSFTStorage.queryStoragePools", MSFTStorageJNA.queryStoragePools(handler).getResultCount(),
                is(greaterThanOrEqualTo(0)));
        assertThat("Failed MSFTStorage.queryVirtualDisks", MSFTStorageJNA.queryVirtualDisks(handler).getResultCount(),
                is(greaterThanOrEqualTo(0)));

        if (comInit) {
            handler.unInitCOM();
        }
    }

    // OhmHardware and OhmSensor excluded
    // We'll never have OHM running during testing

    @Test
    void testQueryWMIWin32() {
        WmiQueryHandler handler = WmiQueryHandler.createInstance();
        assertThat(handler, is(notNullValue()));
        boolean comInit = handler.initCOM();

        assertThat("Failed Win32DiskDrive.queryDiskDrive", Win32DiskDriveJNA.queryDiskDrive(handler).getResultCount(),
                is(greaterThanOrEqualTo(0)));

        assertThat("Failed Win32DiskDriveToDiskPartition.queryDriveToPartition",
                Win32DiskDriveToDiskPartitionJNA.queryDriveToPartition(handler).getResultCount(),
                is(greaterThanOrEqualTo(0)));

        assertThat("Failed Win32DiskPartition.queryPartition",
                Win32DiskPartitionJNA.queryPartition(handler).getResultCount(), is(greaterThanOrEqualTo(0)));

        assertThat("Failed Win32LogicalDiskToPartition.queryDiskToPartition",
                Win32LogicalDiskToPartitionJNA.queryDiskToPartition(handler).getResultCount(),
                is(greaterThanOrEqualTo(0)));

        if (comInit) {
            handler.unInitCOM();
        }

        assertThat("Failed Win32BaseBoard.queryBaseboardInfo", Win32BaseBoardJNA.queryBaseboardInfo().getResultCount(),
                is(greaterThan(0)));

        assertThat("Failed Win32Bios.queryBiosInfo", Win32BiosJNA.queryBiosInfo().getResultCount(), is(greaterThan(0)));
        assertThat("Failed Win32Bios.querySerialNumber", Win32BiosJNA.querySerialNumber().getResultCount(),
                is(greaterThan(0)));

        assertThat("Failed Win32ComputerSystemProduct.queryIdentifyingNumberUUID",
                Win32ComputerSystemProductJNA.queryIdentifyingNumberUUID().getResultCount(), is(greaterThan(0)));

        assertThat("Failed Win32Fan.querySpeed", Win32FanJNA.querySpeed().getResultCount(),
                is(greaterThanOrEqualTo(0)));

        WmiResult<LogicalDiskProperty> ld = Win32LogicalDiskJNA.queryLogicalDisk(null, false);
        assertThat("Failed Win32LogicalDisk.queryLogicalDisk", ld.getResultCount(), is(greaterThan(0)));
        assertThat(
                "Failed Win32LogicalDisk.queryLogicalDisk", Win32LogicalDiskJNA
                        .queryLogicalDisk(WmiUtil.getString(ld, LogicalDiskProperty.NAME, 0), false).getResultCount(),
                is(greaterThan(0)));
        assertThat("Failed Win32LogicalDisk.queryLogicalDisk",
                Win32LogicalDiskJNA.queryLogicalDisk(null, true).getResultCount(), is(greaterThan(0)));

        assertThat("Failed Win32OperatingSystem.queryOsVersion",
                Win32OperatingSystemJNA.queryOsVersion().getResultCount(), is(greaterThan(0)));

        if (VersionHelpers.IsWindows10OrGreater()) {
            assertThat("Failed Win32PhysicalMemory.queryPhysicalMemory",
                    Win32PhysicalMemoryJNA.queryPhysicalMemory().getResultCount(), is(greaterThan(0)));
        } else {
            assertThat("Failed Win32PhysicalMemory.queryPhysicalMemoryWin8",
                    Win32PhysicalMemoryJNA.queryPhysicalMemoryWin8().getResultCount(), is(greaterThan(0)));
        }

        WmiResult<CommandLineProperty> cl = Win32ProcessJNA.queryCommandLines(null);
        assertThat("Failed Win32Process.queryCommandLines", cl.getResultCount(), is(greaterThan(0)));
        Set<Integer> clset = IntStream.range(0, cl.getResultCount())
                .map(i -> WmiUtil.getUint32(cl, CommandLineProperty.PROCESSID, i)).boxed().collect(Collectors.toSet());
        assertThat("Failed Win32Process.queryProcesses", Win32ProcessJNA.queryProcesses(clset).getResultCount(),
                is(both(greaterThan(0)).and(lessThanOrEqualTo(clset.size()))));

        Win32ProcessCachedJNA cache = Win32ProcessCachedJNA.getInstance();
        assertThat("Failed Win32ProcessCached.getCommandLine",
                cache.getCommandLine(WmiUtil.getUint32(cl, CommandLineProperty.PROCESSID, 0), 0L), is(notNullValue()));

        assertThat("Failed Win32Processor.queryBitness", Win32ProcessorJNA.queryBitness().getResultCount(),
                is(greaterThan(0)));
        assertThat("Failed Win32Processor.queryProcessorId", Win32ProcessorJNA.queryProcessorId().getResultCount(),
                is(greaterThan(0)));
        assertThat("Failed Win32Processor.queryVoltage", Win32ProcessorJNA.queryVoltage().getResultCount(),
                is(greaterThan(0)));

        assertThat("Failed Win32VideoController.queryVideoController",
                Win32VideoControllerJNA.queryVideoController().getResultCount(), is(greaterThanOrEqualTo(0)));
    }
}
