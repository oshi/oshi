/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.unix.freebsd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.freebsd.disk.GeomDiskList;
import oshi.driver.unix.freebsd.disk.GeomPartList;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;
import oshi.util.tuples.Triplet;

/**
 * FreeBSD hard disk implementation.
 */
@ThreadSafe
public final class FreeBsdHWDiskStore extends AbstractHWDiskStore {

    private long reads = 0L;
    private long readBytes = 0L;
    private long writes = 0L;
    private long writeBytes = 0L;
    private long currentQueueLength = 0L;
    private long transferTime = 0L;
    private long timeStamp = 0L;
    private List<HWPartition> partitionList;

    private FreeBsdHWDiskStore(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    @Override
    public long getReads() {
        return reads;
    }

    @Override
    public long getReadBytes() {
        return readBytes;
    }

    @Override
    public long getWrites() {
        return writes;
    }

    @Override
    public long getWriteBytes() {
        return writeBytes;
    }

    @Override
    public long getCurrentQueueLength() {
        return currentQueueLength;
    }

    @Override
    public long getTransferTime() {
        return transferTime;
    }

    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

    @Override
    public List<HWPartition> getPartitions() {
        return this.partitionList;
    }

    @Override
    public boolean updateAttributes() {
        List<String> output = ExecutingCommand.runNative("iostat -Ix " + getName());
        long now = System.currentTimeMillis();
        boolean diskFound = false;
        for (String line : output) {
            String[] split = ParseUtil.whitespaces.split(line);
            if (split.length < 7 || !split[0].equals(getName())) {
                continue;
            }
            diskFound = true;
            this.reads = (long) ParseUtil.parseDoubleOrDefault(split[1], 0d);
            this.writes = (long) ParseUtil.parseDoubleOrDefault(split[2], 0d);
            // In KB
            this.readBytes = (long) (ParseUtil.parseDoubleOrDefault(split[3], 0d) * 1024);
            this.writeBytes = (long) (ParseUtil.parseDoubleOrDefault(split[4], 0d) * 1024);
            // # transactions
            this.currentQueueLength = ParseUtil.parseLongOrDefault(split[5], 0L);
            // In seconds, multiply for ms
            this.transferTime = (long) (ParseUtil.parseDoubleOrDefault(split[6], 0d) * 1000);
            this.timeStamp = now;
        }
        return diskFound;
    }

    /**
     * Gets the disks on this machine
     *
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        // Result
        List<HWDiskStore> diskList = new ArrayList<>();

        // Get map of disk names to partitions
        Map<String, List<HWPartition>> partitionMap = GeomPartList.queryPartitions();

        // Get map of disk names to disk info
        Map<String, Triplet<String, String, Long>> diskInfoMap = GeomDiskList.queryDisks();

        // Get list of disks from sysctl
        List<String> devices = Arrays.asList(ParseUtil.whitespaces.split(BsdSysctlUtil.sysctl("kern.disks", "")));

        // Run iostat -Ix to enumerate disks by name and get kb r/w
        List<String> iostat = ExecutingCommand.runNative("iostat -Ix");
        long now = System.currentTimeMillis();
        for (String line : iostat) {
            String[] split = ParseUtil.whitespaces.split(line);
            if (split.length > 6 && devices.contains(split[0])) {
                Triplet<String, String, Long> storeInfo = diskInfoMap.get(split[0]);
                FreeBsdHWDiskStore store = (storeInfo == null)
                        ? new FreeBsdHWDiskStore(split[0], Constants.UNKNOWN, Constants.UNKNOWN, 0L)
                        : new FreeBsdHWDiskStore(split[0], storeInfo.getA(), storeInfo.getB(), storeInfo.getC());
                store.reads = (long) ParseUtil.parseDoubleOrDefault(split[1], 0d);
                store.writes = (long) ParseUtil.parseDoubleOrDefault(split[2], 0d);
                // In KB
                store.readBytes = (long) (ParseUtil.parseDoubleOrDefault(split[3], 0d) * 1024);
                store.writeBytes = (long) (ParseUtil.parseDoubleOrDefault(split[4], 0d) * 1024);
                // # transactions
                store.currentQueueLength = ParseUtil.parseLongOrDefault(split[5], 0L);
                // In seconds, multiply for ms
                store.transferTime = (long) (ParseUtil.parseDoubleOrDefault(split[6], 0d) * 1000);
                store.partitionList = Collections
                        .unmodifiableList(partitionMap.getOrDefault(split[0], Collections.emptyList()).stream()
                                .sorted(Comparator.comparing(HWPartition::getName)).collect(Collectors.toList()));
                store.timeStamp = now;
                diskList.add(store);
            }
        }
        return diskList;
    }
}
