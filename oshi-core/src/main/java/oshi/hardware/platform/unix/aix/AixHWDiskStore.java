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
package oshi.hardware.platform.unix.aix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.sun.jna.Native;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.jna.platform.unix.aix.Perfstat.perfstat_disk_t;
import oshi.util.Constants;
import oshi.util.ParseUtil;

/**
 * Solaris hard disk implementation.
 */
@ThreadSafe
public final class AixHWDiskStore extends AbstractHWDiskStore {

    private final Supplier<perfstat_disk_t[]> diskStats;

    private long reads = 0L;
    private long readBytes = 0L;
    private long writes = 0L;
    private long writeBytes = 0L;
    private long currentQueueLength = 0L;
    private long transferTime = 0L;
    private long timeStamp = 0L;
    private List<HWPartition> partitionList;

    private AixHWDiskStore(String name, String model, String serial, long size, Supplier<perfstat_disk_t[]> diskStats) {
        super(name, model, serial, size);
        this.diskStats = diskStats;
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
        for (perfstat_disk_t stat : diskStats.get()) {
            String name = Native.toString(stat.name);
            if (name.equals(this.getName())) {
                // we only have total transfers so estimate read/write ratio from blocks
                long blks = stat.rblks + stat.wblks;
                if (blks > 0L) {
                    this.writes = stat.xfers * stat.wblks / blks;
                    this.reads = stat.xfers - this.writes;
                } else {
                    this.reads = stat.xfers;
                }
                this.readBytes = stat.rblks * stat.bsize;
                this.writeBytes = stat.wblks * stat.bsize;
                this.currentQueueLength = stat.qdepth;
                this.transferTime = stat.time;
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the disks on this machine
     * 
     * @param lscfg
     * 
     * @param diskStats
     *
     * @return an {@code UnmodifiableList} of {@link HWDiskStore} objects
     *         representing the disks
     */
    public static List<HWDiskStore> getDisks(Supplier<List<String>> lscfg, Supplier<perfstat_disk_t[]> diskStats) {
        List<AixHWDiskStore> storeList = new ArrayList<>();
        List<String> cfg = lscfg.get();
        String serialMarker = "Serial Number";
        String modelMarker = "Machine Type and Model";
        String deviceSpecificMarker = "Device Specific";
        for (perfstat_disk_t disk : diskStats.get()) {
            String storeName = Native.toString(disk.name);
            String model = Native.toString(disk.description);
            String serial = Constants.UNKNOWN;
            boolean thisDisk = false;
            for (String s : cfg) {
                if (!thisDisk && s.trim().startsWith(storeName)) {
                    thisDisk = true;
                }
                if (thisDisk) {
                    if (s.contains(modelMarker)) {
                        model = ParseUtil.removeLeadingDots(s.split(modelMarker)[1].trim());
                    } else if (s.contains(serialMarker)) {
                        serial = ParseUtil.removeLeadingDots(s.split(serialMarker)[1].trim());
                    } else if (s.contains(deviceSpecificMarker)) {
                        break;
                    }
                }
            }
            // volume group, probably not right but sub later
            String mount = Native.toString(disk.vgname);
            storeList.add(createStore(storeName, model, serial, disk.size << 20, mount, diskStats));
        }

        return Collections.unmodifiableList(storeList);
    }

    private static AixHWDiskStore createStore(String diskName, String model, String serial, long size, String mount,
            Supplier<perfstat_disk_t[]> diskStats) {
        AixHWDiskStore store = new AixHWDiskStore(diskName, model.isEmpty() ? Constants.UNKNOWN : model, serial, size,
                diskStats);
        store.partitionList = Collections.emptyList();
        store.updateAttributes();
        return store;
    }
}
