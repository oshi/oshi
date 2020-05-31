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
package oshi.hardware.platform.linux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.jna.platform.linux.Udev;
import oshi.jna.platform.linux.Udev.UdevContext;
import oshi.jna.platform.linux.Udev.UdevDevice;
import oshi.jna.platform.linux.Udev.UdevEnumerate;
import oshi.jna.platform.linux.Udev.UdevListEntry;
import oshi.util.Constants;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;

/**
 * Linux hard disk implementation.
 */
@ThreadSafe
public final class LinuxHWDiskStore extends AbstractHWDiskStore {

    private static final String BLOCK = "block";
    private static final String DISK = "disk";
    private static final String PARTITION = "partition";

    private static final String STAT = "stat";
    private static final String SIZE = "size";
    private static final String MINOR = "MINOR";
    private static final String MAJOR = "MAJOR";

    private static final String ID_FS_TYPE = "ID_FS_TYPE";
    private static final String ID_FS_UUID = "ID_FS_UUID";
    private static final String ID_MODEL = "ID_MODEL";
    private static final String ID_SERIAL_SHORT = "ID_SERIAL_SHORT";

    private static final int SECTORSIZE = 512;

    // Get a list of orders to pass to ParseUtil
    private static final int[] UDEV_STAT_ORDERS = new int[UdevStat.values().length];
    static {
        for (UdevStat stat : UdevStat.values()) {
            UDEV_STAT_ORDERS[stat.ordinal()] = stat.getOrder();
        }
    }

    // There are at least 11 elements in udev stat output or sometimes 15. We want
    // the rightmost 11 or 15 if there is leading text.
    private static final int UDEV_STAT_LENGTH;
    static {
        String stat = FileUtil.getStringFromFile(ProcPath.DISKSTATS);
        int statLength = 11;
        if (!stat.isEmpty()) {
            statLength = ParseUtil.countStringToLongArray(stat, ' ');
        }
        UDEV_STAT_LENGTH = statLength;
    }

    private long reads = 0L;
    private long readBytes = 0L;
    private long writes = 0L;
    private long writeBytes = 0L;
    private long currentQueueLength = 0L;
    private long transferTime = 0L;
    private long timeStamp = 0L;
    private List<HWPartition> partitionList = new ArrayList<>();

    private LinuxHWDiskStore(String name, String model, String serial, long size) {
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

    /**
     * Gets the disks on this machine
     *
     * @return an {@code UnmodifiableList} of {@link HWDiskStore} objects
     *         representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        return Collections.unmodifiableList(getDisks(null));
    }

    private static List<LinuxHWDiskStore> getDisks(LinuxHWDiskStore storeToUpdate) {
        LinuxHWDiskStore store = null;
        List<LinuxHWDiskStore> result = new ArrayList<>();

        Map<String, String> mountsMap = readMountsMap();

        UdevContext udev = Udev.INSTANCE.udev_new();
        try {
            UdevEnumerate enumerate = udev.enumerateNew();
            try {
                enumerate.addMatchSubsystem(BLOCK);
                enumerate.scanDevices();
                for (UdevListEntry entry = enumerate.getListEntry(); entry != null; entry = entry.getNext()) {
                    String syspath = entry.getName();
                    UdevDevice device = udev.deviceNewFromSyspath(syspath);
                    if (device != null) {
                        try {
                            // devnode is what we use as name, like /dev/sda
                            String devnode = device.getDevnode();
                            // Ignore loopback and ram disks; do nothing
                            if (devnode != null && !devnode.startsWith("/dev/loop")
                                    && !devnode.startsWith("/dev/ram")) {
                                if (DISK.equals(device.getDevtype())) {
                                    // Null model and serial in virtual environments
                                    String devModel = device.getPropertyValue(ID_MODEL);
                                    String devSerial = device.getPropertyValue(ID_SERIAL_SHORT);
                                    long devSize = ParseUtil.parseLongOrDefault(device.getSysattrValue(SIZE), 0L)
                                            * SECTORSIZE;
                                    store = new LinuxHWDiskStore(devnode,
                                            devModel == null ? Constants.UNKNOWN : devModel,
                                            devSerial == null ? Constants.UNKNOWN : devSerial, devSize);
                                    if (storeToUpdate == null) {
                                        // If getting all stores, add to the list with stats
                                        computeDiskStats(store, device.getSysattrValue(STAT));
                                        result.add(store);
                                    } else if (store.getName().equals(storeToUpdate.getName())
                                            && store.getModel().equals(storeToUpdate.getModel())
                                            && store.getSerial().equals(storeToUpdate.getSerial())
                                            && store.getSize() == storeToUpdate.getSize()) {
                                        // If we are only updating a single disk, the name, model, serial, and size are
                                        // sufficient to test if this is a match. Add the (old) object, release handle
                                        // and return.
                                        computeDiskStats(storeToUpdate, device.getSysattrValue(STAT));
                                        result.add(storeToUpdate);
                                        break;
                                    }
                                } else if (storeToUpdate == null && store != null // only add if getting new list
                                        && PARTITION.equals(device.getDevtype())) {
                                    // udev_device_get_parent_*() does not take a reference on the returned device,
                                    // it is automatically unref'd with the parent
                                    UdevDevice parent = device.getParentWithSubsystemDevtype(BLOCK, DISK);
                                    if (parent != null && store.getName().equals(parent.getDevnode())) {
                                        // `store` should still point to the parent HWDiskStore this partition is
                                        // attached to. If not, it's an error, so skip.
                                        String name = device.getDevnode();
                                        store.partitionList.add(new HWPartition(name, device.getSysname(),
                                                device.getPropertyValue(ID_FS_TYPE) == null ? PARTITION
                                                        : device.getPropertyValue(ID_FS_TYPE),
                                                device.getPropertyValue(ID_FS_UUID) == null ? ""
                                                        : device.getPropertyValue(ID_FS_UUID),
                                                ParseUtil.parseLongOrDefault(device.getSysattrValue(SIZE), 0L)
                                                        * SECTORSIZE,
                                                ParseUtil.parseIntOrDefault(device.getPropertyValue(MAJOR), 0),
                                                ParseUtil.parseIntOrDefault(device.getPropertyValue(MINOR), 0),
                                                mountsMap.getOrDefault(name, "")));
                                    }
                                }
                            }
                        } finally {
                            device.unref();
                        }
                    }
                }
            } finally {
                enumerate.unref();
            }
        } finally {
            udev.unref();
        }
        // Iterate the list and make the partitions unmodifiable
        for (LinuxHWDiskStore hwds : result) {
            hwds.partitionList = Collections.unmodifiableList(hwds.partitionList.stream()
                    .sorted(Comparator.comparing(HWPartition::getName)).collect(Collectors.toList()));
        }
        return result;
    }

    @Override
    public boolean updateAttributes() {
        // If this returns non-empty (the same store, but updated) then we were
        // successful in the update
        return !getDisks(this).isEmpty();
    }

    private static Map<String, String> readMountsMap() {
        Map<String, String> mountsMap = new HashMap<>();
        List<String> mounts = FileUtil.readFile(ProcPath.MOUNTS);
        for (String mount : mounts) {
            String[] split = ParseUtil.whitespaces.split(mount);
            if (split.length < 2 || !split[0].startsWith("/dev/")) {
                continue;
            }
            mountsMap.put(split[0], split[1]);
        }
        return mountsMap;
    }

    private static void computeDiskStats(LinuxHWDiskStore store, String devstat) {
        long[] devstatArray = ParseUtil.parseStringToLongArray(devstat, UDEV_STAT_ORDERS, UDEV_STAT_LENGTH, ' ');
        store.timeStamp = System.currentTimeMillis();

        // Reads and writes are converted in bytes
        store.reads = devstatArray[UdevStat.READS.ordinal()];
        store.readBytes = devstatArray[UdevStat.READ_BYTES.ordinal()] * SECTORSIZE;
        store.writes = devstatArray[UdevStat.WRITES.ordinal()];
        store.writeBytes = devstatArray[UdevStat.WRITE_BYTES.ordinal()] * SECTORSIZE;
        store.currentQueueLength = devstatArray[UdevStat.QUEUE_LENGTH.ordinal()];
        store.transferTime = devstatArray[UdevStat.ACTIVE_MS.ordinal()];
    }

    // Order the field is in udev stats
    enum UdevStat {
        // The parsing implementation in ParseUtil requires these to be declared
        // in increasing order. Use 0-ordered index here
        READS(0), READ_BYTES(2), WRITES(4), WRITE_BYTES(6), QUEUE_LENGTH(8), ACTIVE_MS(9);

        private int order;

        public int getOrder() {
            return this.order;
        }

        UdevStat(int order) {
            this.order = order;
        }
    }
}
