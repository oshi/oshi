/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.mac.disk.Fsstat;
import oshi.ffm.mac.CoreFoundation.CFAllocatorRef;
import oshi.ffm.mac.CoreFoundation.CFDictionaryRef;
import oshi.ffm.mac.CoreFoundation.CFMutableDictionaryRef;
import oshi.ffm.mac.CoreFoundation.CFNumberRef;
import oshi.ffm.mac.CoreFoundation.CFStringRef;
import oshi.ffm.mac.CoreFoundationFunctions;
import oshi.ffm.mac.DiskArbitration.DADiskRef;
import oshi.ffm.mac.DiskArbitration.DASessionRef;
import oshi.ffm.mac.IOKit.IOIterator;
import oshi.ffm.mac.IOKit.IORegistryEntry;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.Constants;
import oshi.util.platform.mac.CFUtilFFM;
import oshi.util.platform.mac.IOKitUtilFFM;

/**
 * Mac hard disk FFM implementation.
 */
@ThreadSafe
public final class MacHWDiskStoreFFM extends AbstractHWDiskStore {

    private static final Logger LOG = LoggerFactory.getLogger(MacHWDiskStoreFFM.class);

    private long reads = 0L;
    private long readBytes = 0L;
    private long writes = 0L;
    private long writeBytes = 0L;
    private long currentQueueLength = 0L;
    private long transferTime = 0L;
    private long timeStamp = 0L;
    private List<HWPartition> partitionList;

    private MacHWDiskStoreFFM(String name, String model, String serial, long size, DASessionRef session,
            Map<String, String> mountPointMap, Map<CFKey, CFStringRef> cfKeyMap) {
        super(name, model, serial, size);
        updateDiskStats(session, mountPointMap, cfKeyMap);
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
        try {
            CFAllocatorRef alloc = new CFAllocatorRef(CoreFoundationFunctions.CFAllocatorGetDefault());
            DASessionRef session = DASessionRef.create(alloc);
            if (session.isNull()) {
                LOG.error("Unable to open session to DiskArbitration framework.");
                return false;
            }
            Map<CFKey, CFStringRef> cfKeyMap = mapCFKeys();
            try {
                return updateDiskStats(session, Fsstat.queryPartitionToMountMap(), cfKeyMap);
            } finally {
                session.release();
                for (CFStringRef value : cfKeyMap.values()) {
                    value.release();
                }
            }
        } catch (Throwable e) {
            return false;
        }
    }

    private boolean updateDiskStats(DASessionRef session, Map<String, String> mountPointMap,
            Map<CFKey, CFStringRef> cfKeyMap) {
        String bsdName = getName();
        MemorySegment matchingDict = IOKitUtilFFM.getBSDNameMatchingDict(bsdName);
        if (matchingDict == null || matchingDict.equals(MemorySegment.NULL)) {
            return false;
        }
        IOIterator driveListIter = IOKitUtilFFM.getMatchingServices(matchingDict);
        if (driveListIter == null) {
            return false;
        }
        try {
            IORegistryEntry drive = driveListIter.next();
            if (drive == null) {
                return false;
            }
            try {
                if (!drive.conformsTo("IOMedia")) {
                    LOG.error("Unable to find IOMedia device or parent for {}", bsdName);
                    return false;
                }
                IORegistryEntry parent = drive.getParentEntry("IOService");
                try {
                    if (parent != null && (parent.conformsTo("IOBlockStorageDriver")
                            || parent.conformsTo("AppleAPFSContainerScheme"))) {
                        MemorySegment propertiesSeg = parent.createCFProperties();
                        if (!propertiesSeg.equals(MemorySegment.NULL)) {
                            CFDictionaryRef properties = new CFDictionaryRef(propertiesSeg);
                            try {
                                MemorySegment result = properties.getValue(cfKeyMap.get(CFKey.STATISTICS));
                                CFDictionaryRef statistics = new CFDictionaryRef(result);
                                this.timeStamp = System.currentTimeMillis();

                                result = statistics.getValue(cfKeyMap.get(CFKey.READ_OPS));
                                this.reads = new CFNumberRef(result).longValue();
                                result = statistics.getValue(cfKeyMap.get(CFKey.READ_BYTES));
                                this.readBytes = new CFNumberRef(result).longValue();
                                result = statistics.getValue(cfKeyMap.get(CFKey.WRITE_OPS));
                                this.writes = new CFNumberRef(result).longValue();
                                result = statistics.getValue(cfKeyMap.get(CFKey.WRITE_BYTES));
                                this.writeBytes = new CFNumberRef(result).longValue();

                                MemorySegment readTimeResult = statistics.getValue(cfKeyMap.get(CFKey.READ_TIME));
                                MemorySegment writeTimeResult = statistics.getValue(cfKeyMap.get(CFKey.WRITE_TIME));
                                if (!readTimeResult.equals(MemorySegment.NULL)
                                        && !writeTimeResult.equals(MemorySegment.NULL)) {
                                    long xferTime = new CFNumberRef(readTimeResult).longValue();
                                    xferTime += new CFNumberRef(writeTimeResult).longValue();
                                    this.transferTime = xferTime / 1_000_000L;
                                }
                            } finally {
                                properties.release();
                            }
                        }
                    } else {
                        LOG.debug("Unable to find block storage driver properties for {}", bsdName);
                    }

                    // Partitions
                    List<HWPartition> partitions = new ArrayList<>();
                    MemorySegment drivePropsSeg = drive.createCFProperties();
                    if (!drivePropsSeg.equals(MemorySegment.NULL)) {
                        CFDictionaryRef driveProps = new CFDictionaryRef(drivePropsSeg);
                        try {
                            MemorySegment result = driveProps.getValue(cfKeyMap.get(CFKey.BSD_UNIT));
                            CFNumberRef bsdUnit = new CFNumberRef(result);
                            result = driveProps.getValue(cfKeyMap.get(CFKey.LEAF));
                            // cfFalse is the value stored under LEAF key for whole disks
                            MemorySegment cfFalseSeg = result;

                            try {
                                CFAllocatorRef alloc = new CFAllocatorRef(
                                        CoreFoundationFunctions.CFAllocatorGetDefault());
                                CFMutableDictionaryRef propertyDict = new CFMutableDictionaryRef(
                                        CoreFoundationFunctions.CFDictionaryCreateMutable(alloc.segment(), 0,
                                                MemorySegment.NULL, MemorySegment.NULL));
                                // setValue uses CFTypeRef; wrap raw segments
                                propertyDict.setValue(cfKeyMap.get(CFKey.BSD_UNIT), bsdUnit);
                                // For WHOLE=false: use the cfFalse value from the drive properties
                                oshi.ffm.mac.CoreFoundation.CFTypeRef cfFalse = new oshi.ffm.mac.CoreFoundation.CFTypeRef(
                                        cfFalseSeg);
                                propertyDict.setValue(cfKeyMap.get(CFKey.WHOLE), cfFalse);

                                CFMutableDictionaryRef partMatchingDict = new CFMutableDictionaryRef(
                                        CoreFoundationFunctions.CFDictionaryCreateMutable(alloc.segment(), 0,
                                                MemorySegment.NULL, MemorySegment.NULL));
                                partMatchingDict.setValue(cfKeyMap.get(CFKey.IO_PROPERTY_MATCH), propertyDict);

                                IOIterator serviceIterator = IOKitUtilFFM
                                        .getMatchingServices(partMatchingDict.segment());
                                propertyDict.release();

                                if (serviceIterator != null) {
                                    try {
                                        IORegistryEntry sdService = serviceIterator.next();
                                        while (sdService != null) {
                                            String partBsdName = sdService.getStringProperty("BSD Name");
                                            String name = partBsdName;
                                            String type = "";
                                            String label = "";

                                            DADiskRef disk = DADiskRef.createFromBSDName(alloc, session, partBsdName);
                                            if (!disk.isNull()) {
                                                CFDictionaryRef diskInfo = disk.copyDescription();
                                                if (!diskInfo.isNull()) {
                                                    MemorySegment r = diskInfo
                                                            .getValue(cfKeyMap.get(CFKey.DA_MEDIA_NAME));
                                                    type = CFUtilFFM.cfPointerToString(r);
                                                    r = diskInfo.getValue(cfKeyMap.get(CFKey.DA_VOLUME_NAME));
                                                    if (r.equals(MemorySegment.NULL)) {
                                                        name = type;
                                                    } else {
                                                        String volumeName = CFUtilFFM.cfPointerToString(r);
                                                        name = volumeName;
                                                        label = volumeName;
                                                    }
                                                    diskInfo.release();
                                                }
                                                disk.release();
                                            }
                                            String mountPoint = mountPointMap.getOrDefault(partBsdName, "");
                                            Long size = sdService.getLongProperty("Size");
                                            Integer bsdMajor = sdService.getIntegerProperty("BSD Major");
                                            Integer bsdMinor = sdService.getIntegerProperty("BSD Minor");
                                            String uuid = sdService.getStringProperty("UUID");
                                            partitions.add(new HWPartition(partBsdName, name, type,
                                                    uuid == null ? Constants.UNKNOWN : uuid, label,
                                                    size == null ? 0L : size, bsdMajor == null ? 0 : bsdMajor,
                                                    bsdMinor == null ? 0 : bsdMinor, mountPoint));
                                            sdService.release();
                                            sdService = serviceIterator.next();
                                        }
                                    } finally {
                                        serviceIterator.release();
                                    }
                                }
                            } catch (Throwable e) {
                                LOG.debug("Error building partition list for {}", bsdName);
                            }
                        } finally {
                            driveProps.release();
                        }
                    }
                    this.partitionList = Collections.unmodifiableList(partitions.stream()
                            .sorted(Comparator.comparing(HWPartition::getName)).collect(Collectors.toList()));
                } finally {
                    if (parent != null) {
                        parent.release();
                    }
                }
            } finally {
                drive.release();
            }
        } finally {
            driveListIter.release();
        }
        return true;
    }

    public static List<HWDiskStore> getDisks() {
        Map<String, String> mountPointMap = Fsstat.queryPartitionToMountMap();
        Map<CFKey, CFStringRef> cfKeyMap = mapCFKeys();
        List<HWDiskStore> diskList = new ArrayList<>();

        try {
            CFAllocatorRef alloc = new CFAllocatorRef(CoreFoundationFunctions.CFAllocatorGetDefault());
            DASessionRef session = DASessionRef.create(alloc);
            if (session.isNull()) {
                LOG.error("Unable to open session to DiskArbitration framework.");
                return Collections.emptyList();
            }
            try {
                List<String> bsdNames = new ArrayList<>();
                IOIterator iter = IOKitUtilFFM.getMatchingServices("IOMedia");
                if (iter != null) {
                    try {
                        IORegistryEntry media = iter.next();
                        while (media != null) {
                            Boolean whole = media.getBooleanProperty("Whole");
                            if (Boolean.TRUE.equals(whole)) {
                                DADiskRef disk = DADiskRef.createFromIOMedia(alloc, session, media);
                                String bsdName = disk.getBSDName();
                                if (bsdName != null) {
                                    bsdNames.add(bsdName);
                                }
                                disk.release();
                            }
                            media.release();
                            media = iter.next();
                        }
                    } finally {
                        iter.release();
                    }
                }

                for (String bsdName : bsdNames) {
                    String model = "";
                    String serial = "";
                    long size = 0L;

                    String path = "/dev/" + bsdName;
                    DADiskRef disk = DADiskRef.createFromBSDName(alloc, session, path);
                    if (disk.isNull()) {
                        continue;
                    }
                    try {
                        CFDictionaryRef diskInfo = disk.copyDescription();
                        if (!diskInfo.isNull()) {
                            try {
                                MemorySegment result = diskInfo.getValue(cfKeyMap.get(CFKey.DA_DEVICE_MODEL));
                                model = CFUtilFFM.cfPointerToString(result);
                                result = diskInfo.getValue(cfKeyMap.get(CFKey.DA_MEDIA_SIZE));
                                size = new CFNumberRef(result).longValue();

                                if (!"Disk Image".equals(model)) {
                                    CFStringRef modelNameRef = CFStringRef.createCFString(model);
                                    try {
                                        CFMutableDictionaryRef propertyDict = new CFMutableDictionaryRef(
                                                CoreFoundationFunctions.CFDictionaryCreateMutable(alloc.segment(), 0,
                                                        MemorySegment.NULL, MemorySegment.NULL));
                                        propertyDict.setValue(cfKeyMap.get(CFKey.MODEL), modelNameRef);
                                        CFMutableDictionaryRef matchingDict = new CFMutableDictionaryRef(
                                                CoreFoundationFunctions.CFDictionaryCreateMutable(alloc.segment(), 0,
                                                        MemorySegment.NULL, MemorySegment.NULL));
                                        matchingDict.setValue(cfKeyMap.get(CFKey.IO_PROPERTY_MATCH), propertyDict);

                                        IOIterator serviceIterator = IOKitUtilFFM
                                                .getMatchingServices(matchingDict.segment());
                                        propertyDict.release();

                                        if (serviceIterator != null) {
                                            try {
                                                IORegistryEntry sdService = serviceIterator.next();
                                                while (sdService != null) {
                                                    serial = sdService.getStringProperty("Serial Number");
                                                    sdService.release();
                                                    if (serial != null) {
                                                        break;
                                                    }
                                                    sdService = serviceIterator.next();
                                                }
                                            } finally {
                                                serviceIterator.release();
                                            }
                                        }
                                    } finally {
                                        modelNameRef.release();
                                    }
                                    if (serial == null) {
                                        serial = "";
                                    }
                                }
                            } finally {
                                diskInfo.release();
                            }
                        }
                    } finally {
                        disk.release();
                    }

                    if (size <= 0) {
                        continue;
                    }
                    diskList.add(new MacHWDiskStoreFFM(bsdName, model.trim(), serial.trim(), size, session,
                            mountPointMap, cfKeyMap));
                }
            } finally {
                session.release();
                for (CFStringRef value : cfKeyMap.values()) {
                    value.release();
                }
            }
        } catch (Throwable e) {
            LOG.error("Error enumerating disks", e);
        }
        return diskList;
    }

    private static Map<CFKey, CFStringRef> mapCFKeys() {
        Map<CFKey, CFStringRef> keyMap = new EnumMap<>(CFKey.class);
        for (CFKey cfKey : CFKey.values()) {
            keyMap.put(cfKey, CFStringRef.createCFString(cfKey.getKey()));
        }
        return keyMap;
    }

    private enum CFKey {
        IO_PROPERTY_MATCH("IOPropertyMatch"), //

        STATISTICS("Statistics"), //
        READ_OPS("Operations (Read)"), READ_BYTES("Bytes (Read)"), READ_TIME("Total Time (Read)"), //
        WRITE_OPS("Operations (Write)"), WRITE_BYTES("Bytes (Write)"), WRITE_TIME("Total Time (Write)"), //

        BSD_UNIT("BSD Unit"), LEAF("Leaf"), WHOLE("Whole"), //

        DA_MEDIA_NAME("DAMediaName"), DA_VOLUME_NAME("DAVolumeName"), DA_MEDIA_SIZE("DAMediaSize"), //
        DA_DEVICE_MODEL("DADeviceModel"), MODEL("Model");

        private final String key;

        CFKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }
    }
}
