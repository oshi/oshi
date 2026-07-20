/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.unix.solaris.disk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import oshi.util.tuples.Quintet;

class IostatTest {

    @Test
    void testParsePartitionToMountMapTypicalOutput() {
        // iostat -er output: errors with device name in first column
        List<String> mountNames = Arrays.asList("errors", "device,s/w,h/w,trn,tot", "cmdk0,0,0,0,0", "sd0,0,0,0,0");
        // iostat -ern output: errors with device name in last column
        List<String> mountPoints = Arrays.asList("errors", "s/w,h/w,trn,tot,device", "0,0,0,0,c1d0", "0,0,0,0,c1t1d0");

        Map<String, String> result = Iostat.parsePartitionToMountMap(mountNames, mountPoints);

        assertThat(result.size(), is(2));
        assertThat(result.get("cmdk0"), is("c1d0"));
        assertThat(result.get("sd0"), is("c1t1d0"));
    }

    @Test
    void testParsePartitionToMountMapEmptyInput() {
        Map<String, String> result = Iostat.parsePartitionToMountMap(Collections.emptyList(), Collections.emptyList());
        assertThat(result, is(anEmptyMap()));
    }

    @Test
    void testParsePartitionToMountMapHeaderOnly() {
        List<String> mountNames = Arrays.asList("errors", "device,s/w,h/w,trn,tot");
        List<String> mountPoints = Arrays.asList("errors", "s/w,h/w,trn,tot,device");

        Map<String, String> result = Iostat.parsePartitionToMountMap(mountNames, mountPoints);
        assertThat(result, is(anEmptyMap()));
    }

    @Test
    void testParsePartitionToMountMapMismatchedLengths() {
        // mountNames has more entries than mountPoints - only paired entries are processed
        List<String> mountNames = Arrays.asList("errors", "device,s/w,h/w,trn,tot", "cmdk0,0,0,0,0", "sd0,0,0,0,0");
        List<String> mountPoints = Arrays.asList("errors", "s/w,h/w,trn,tot,device", "0,0,0,0,c1d0");

        Map<String, String> result = Iostat.parsePartitionToMountMap(mountNames, mountPoints);

        assertThat(result.size(), is(1));
        assertThat(result.get("cmdk0"), is("c1d0"));
    }

    @Test
    void testParseDeviceStringsTypicalOutput() {
        List<String> iostat = Arrays.asList("cmdk0,Soft Errors: 0,Hard Errors: 0,Transport Errors: 0",
                "Model: VBOX HARDDISK,Serial No: VB12345678-abcdefgh,Size: 21.47GB <21474836480 bytes>",
                "sd0,Soft Errors: 0,Hard Errors: 0,Transport Errors: 0",
                "Vendor: ATA,Product: Samsung SSD 860,Serial No: S3Z2NB0K999999,Size: 500.11GB <500107862016 bytes>");

        Set<String> diskSet = new HashSet<>(Arrays.asList("cmdk0", "sd0"));

        Map<String, Quintet<String, String, String, String, Long>> result = Iostat.parseDeviceStrings(iostat, diskSet);

        assertThat(result.size(), is(2));

        Quintet<String, String, String, String, Long> cmdk0 = result.get("cmdk0");
        assertThat(cmdk0.getA(), is("VBOX HARDDISK"));
        assertThat(cmdk0.getB(), is(""));
        assertThat(cmdk0.getC(), is(""));
        assertThat(cmdk0.getD(), is("VB12345678-abcdefgh"));
        assertThat(cmdk0.getE(), is(21474836480L));

        Quintet<String, String, String, String, Long> sd0 = result.get("sd0");
        assertThat(sd0.getA(), is(""));
        assertThat(sd0.getB(), is("ATA"));
        assertThat(sd0.getC(), is("Samsung SSD 860"));
        assertThat(sd0.getD(), is("S3Z2NB0K999999"));
        assertThat(sd0.getE(), is(500107862016L));
    }

    @Test
    void testParseDeviceStringsFiltersByDiskSet() {
        List<String> iostat = Arrays.asList("cmdk0,Soft Errors: 0,Hard Errors: 0,Transport Errors: 0",
                "Model: VBOX HARDDISK,Serial No: VB12345,Size: 21.47GB <21474836480 bytes>",
                "sd0,Soft Errors: 0,Hard Errors: 0,Transport Errors: 0",
                "Vendor: ATA,Product: Test,Serial No: SN999,Size: 100GB <100000000000 bytes>");

        // Only include cmdk0 in the disk set
        Set<String> diskSet = new HashSet<>(Arrays.asList("cmdk0"));

        Map<String, Quintet<String, String, String, String, Long>> result = Iostat.parseDeviceStrings(iostat, diskSet);

        assertThat(result.size(), is(1));
        assertThat(result.containsKey("cmdk0"), is(true));
        assertThat(result.containsKey("sd0"), is(false));
    }

    @Test
    void testParseDeviceStringsEmptyInput() {
        Set<String> diskSet = new HashSet<>(Arrays.asList("cmdk0"));
        Map<String, Quintet<String, String, String, String, Long>> result = Iostat
                .parseDeviceStrings(Collections.emptyList(), diskSet);
        assertThat(result, is(anEmptyMap()));
    }
}
