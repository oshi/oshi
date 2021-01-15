/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.hardware.platform.unix.openbsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.openbsd.disk.Disklabel;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;
import oshi.util.tuples.Quartet;

/**
 * OpenBSD hard disk implementation.
 */
@ThreadSafe
public final class OpenBsdHWDiskStore extends AbstractHWDiskStore {

    private final Supplier<List<String>> iostat = memoize(this::querySystatIostat, defaultExpiration());

    private long reads = 0L;
    private long readBytes = 0L;
    private long writes = 0L;
    private long writeBytes = 0L;
    private long currentQueueLength = 0L;
    private long transferTime = 0L;
    private long timeStamp = 0L;
    private List<HWPartition> partitionList;

    private OpenBsdHWDiskStore(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    /**
     * Gets the disks on this machine.
     *
     * @return an {@code UnmodifiableList} of {@link HWDiskStore} objects
     *         representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        List<OpenBsdHWDiskStore> diskList = new ArrayList<>();
        List<String> dmesg = null; // Lazily fetch in loop if needed

        // Get list of disks from sysctl
        // hw.disknames=sd0:2cf69345d371cd82,cd0:,sd1:
        String[] devices = OpenBsdSysctlUtil.sysctl("hw.disknames", "").split(",");
        OpenBsdHWDiskStore store;
        String diskName;
        for (String device : devices) {
            diskName = device.split(":")[0];
            // get partitions using disklabel command (requires root)
            Quartet<String, String, Long, List<HWPartition>> diskdata = Disklabel.getDiskParams(diskName);
            String model = diskdata.getA();
            long size = diskdata.getC();
            if (size <= 1) {
                if (dmesg == null) {
                    dmesg = ExecutingCommand.runNative("dmesg");
                }
                Pattern diskAt = Pattern.compile(diskName + " at .*<(.+)>");
                Pattern diskMB = Pattern.compile(diskName + ": .* (\\d+)MB, .*");
                for (String line : dmesg) {
                    Matcher m = diskAt.matcher(line);
                    if (m.matches()) {
                        model = m.group(1);
                    }
                    m = diskMB.matcher(line);
                    if (m.matches()) {
                        size = ParseUtil.parseLongOrDefault(m.group(1), 0L) << 20;
                        break;
                    }
                }
            }
            store = new OpenBsdHWDiskStore(diskName, model, diskdata.getB(), size);
            store.partitionList = diskdata.getD();
            store.updateAttributes();

            diskList.add(store);
        }
        return Collections.unmodifiableList(diskList);
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
        /*-
         └─ $ ▶ iostat -I sd0
            tty                 sd0                   cpu
         tin     tout    KB/t   xfr     MB       us ni sy sp in id
         7894    75610  28.09 1668381 45761.14    4  0  1  0  0 94

                https:man.openbsd.org/systat.1#iostat

        └─ $ ▶ systat -b iostat


                0 users Load 2.04 4.02 3.96                          thinkpad.local 00:14:35
                DEVICE          READ    WRITE     RTPS    WTPS     SEC            STATS
                sd0           49937M   25774M  1326555 1695370   945.9
                cd0                0        0        0       0     0.0
                sd1          1573888      204       29       0     0.1
                Totals        49939M   25774M  1326585 1695371   946.0
                                                                               126568 total pages
                                                                               126568 dma pages
                                                                                  100 dirty pages
                                                                                   14 delwri bufs
                                                                                    0 busymap bufs
                                                                                 6553 avail kvaslots
                                                                                 6553 kvaslots
                                                                                    0 pending writes
                                                                                   12 pending reads
                                                                                    0 cache hits
                                                                                    0 high flips
                                                                                    0 high flops
                                                                                    0 dma flips
        */
        long now = System.currentTimeMillis();
        boolean diskFound = false;
        for (String line : iostat.get()) {
            String[] split = ParseUtil.whitespaces.split(line);
            if (split.length < 7 && split[0].equals(getName())) {
                diskFound = true;
                this.readBytes = ParseUtil.parseMultipliedToLongs(split[1]);
                this.writeBytes = ParseUtil.parseMultipliedToLongs(split[2]);
                this.reads = (long) ParseUtil.parseDoubleOrDefault(split[3], 0d);
                this.writes = (long) ParseUtil.parseDoubleOrDefault(split[4], 0d);
                // In seconds, multiply for ms
                this.transferTime = (long) (ParseUtil.parseDoubleOrDefault(split[5], 0d) * 1000);
                // this.currentQueueLength = ParseUtil.parseLongOrDefault(split[5], 0L);
                this.timeStamp = now;
            }
        }
        return diskFound;
    }

    private List<String> querySystatIostat() {
        return ExecutingCommand.runNative("systat -ab iostat");
    }
}
