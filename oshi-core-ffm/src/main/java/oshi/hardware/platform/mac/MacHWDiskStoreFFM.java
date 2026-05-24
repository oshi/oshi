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
import oshi.driver.mac.disk.FsstatFFM;
import oshi.ffm.mac.CoreFoundation.CFAllocatorRef;
import oshi.ffm.mac.CoreFoundation.CFDictionaryRef;
import oshi.ffm.mac.CoreFoundation.CFMutableDictionaryRef;
import oshi.ffm.mac.CoreFoundation.CFNumberRef;
import oshi.ffm.mac.CoreFoundation.CFStringRef;
import oshi.ffm.mac.CoreFoundation.CFTypeRef;
import oshi.ffm.mac.CoreFoundationFunctions;
import oshi.ffm.mac.DiskArbitration.DADiskRef;
import oshi.ffm.mac.DiskArbitration.DASessionRef;
import oshi.ffm.mac.IOKit.IOIterator;
import oshi.ffm.mac.IOKit.IORegistryEntry;
import oshi.ffm.util.platform.mac.CFUtilFFM;
import oshi.ffm.util.platform.mac.IOKitUtilFFM;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.platform.mac.MacHWDiskStore;
import oshi.util.Constants;

/**
 * Mac hard disk FFM implementation.
 */
@ThreadSafe
public final class MacHWDiskStoreFFM extends MacHWDiskStore {

    private static final Logger LOG = LoggerFactory.getLogger(MacHWDiskStoreFFM.class);

    private MacHWDiskStoreFFM(String name, String model, String serial, long size, DASessionRef session,
            Map<String, String> mountPointMap, Map<CFKey, CFStringRef> cfKeyMap) {
        super(name, model, serial, size);
        updateDiskStats(session, mountPointMap, cfKeyMap);
    }

    private MacHWDiskStoreFFM(String name, String model, String serial, long size, String diskType,
            DASessionRef session, Map<String, String> mountPointMap, Map<CFKey, CFStringRef> cfKeyMap) {
        super(name, model, serial, size, diskType);
        updateDiskStats(session, mountPointMap, cfKeyMap);
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
            Map<CFKey, CFStringRef> cfKeyMap = null;
            try (session) {
                cfKeyMap = mapCFKeys();
                return updateDiskStats(session, FsstatFFM.queryPartitionToMountMap(), cfKeyMap);
            } finally {
                if (cfKeyMap != null) {
                    for (CFStringRef value : cfKeyMap.values()) {
                        value.release();
                    }
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
        try (driveListIter) {
            IORegistryEntry drive = driveListIter.next();
            if (drive == null) {
                return false;
            }
            try (drive) {
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
                            try (CFDictionaryRef properties = new CFDictionaryRef(propertiesSeg)) {
                                MemorySegment result = properties.getValue(cfKeyMap.get(CFKey.STATISTICS));
                                if (!result.equals(MemorySegment.NULL)) {
                                    CFDictionaryRef statistics = new CFDictionaryRef(result);
                                    setTimeStamp(System.currentTimeMillis());

                                    result = statistics.getValue(cfKeyMap.get(CFKey.READ_OPS));
                                    if (!result.equals(MemorySegment.NULL)) {
                                        setReads(new CFNumberRef(result).longValue());
                                    }
                                    result = statistics.getValue(cfKeyMap.get(CFKey.READ_BYTES));
                                    if (!result.equals(MemorySegment.NULL)) {
                                        setReadBytes(new CFNumberRef(result).longValue());
                                    }
                                    result = statistics.getValue(cfKeyMap.get(CFKey.WRITE_OPS));
                                    if (!result.equals(MemorySegment.NULL)) {
                                        setWrites(new CFNumberRef(result).longValue());
                                    }
                                    result = statistics.getValue(cfKeyMap.get(CFKey.WRITE_BYTES));
                                    if (!result.equals(MemorySegment.NULL)) {
                                        setWriteBytes(new CFNumberRef(result).longValue());
                                    }

                                    MemorySegment readTimeResult = statistics.getValue(cfKeyMap.get(CFKey.READ_TIME));
                                    MemorySegment writeTimeResult = statistics.getValue(cfKeyMap.get(CFKey.WRITE_TIME));
                                    if (!readTimeResult.equals(MemorySegment.NULL)
                                            && !writeTimeResult.equals(MemorySegment.NULL)) {
                                        long xferTime = new CFNumberRef(readTimeResult).longValue();
                                        xferTime += new CFNumberRef(writeTimeResult).longValue();
                                        setTransferTime(xferTime / 1_000_000L);
                                    }
                                }
                            }
                        }
                    } else {
                        LOG.debug("Unable to find block storage driver properties for {}", bsdName);
                    }

                    // Partitions
                    List<HWPartition> partitions = new ArrayList<>();
                    MemorySegment drivePropsSeg = drive.createCFProperties();
                    if (!drivePropsSeg.equals(MemorySegment.NULL)) {
                        try (CFDictionaryRef driveProps = new CFDictionaryRef(drivePropsSeg)) {
                            MemorySegment bsdUnitSeg = driveProps.getValue(cfKeyMap.get(CFKey.BSD_UNIT));
                            MemorySegment cfFalseSeg = driveProps.getValue(cfKeyMap.get(CFKey.LEAF));

                            try {
                                CFAllocatorRef alloc = new CFAllocatorRef(
                                        CoreFoundationFunctions.CFAllocatorGetDefault());
                                CFMutableDictionaryRef propertyDict = new CFMutableDictionaryRef(
                                        CoreFoundationFunctions.CFDictionaryCreateMutable(alloc.segment(), 0,
                                                MemorySegment.NULL, MemorySegment.NULL));
                                if (!bsdUnitSeg.equals(MemorySegment.NULL)) {
                                    propertyDict.setValue(cfKeyMap.get(CFKey.BSD_UNIT), new CFNumberRef(bsdUnitSeg));
                                }
                                if (!cfFalseSeg.equals(MemorySegment.NULL)) {
                                    propertyDict.setValue(cfKeyMap.get(CFKey.WHOLE), new CFTypeRef(cfFalseSeg));
                                }

                                CFMutableDictionaryRef partMatchingDict = new CFMutableDictionaryRef(
                                        CoreFoundationFunctions.CFDictionaryCreateMutable(alloc.segment(), 0,
                                                MemorySegment.NULL, MemorySegment.NULL));
                                partMatchingDict.setValue(cfKeyMap.get(CFKey.IO_PROPERTY_MATCH), propertyDict);

                                IOIterator serviceIterator = IOKitUtilFFM
                                        .getMatchingServices(partMatchingDict.segment());
                                propertyDict.release();

                                if (serviceIterator != null) {
                                    try (serviceIterator) {
                                        IORegistryEntry sdService = serviceIterator.next();
                                        while (sdService != null) {
                                            try (IORegistryEntry current = sdService) {
                                                String partBsdName = current.getStringProperty("BSD Name");
                                                String name = partBsdName;
                                                String type = "";
                                                String label = "";

                                                try (DADiskRef disk = DADiskRef.createFromBSDName(alloc, session,
                                                        partBsdName)) {
                                                    if (!disk.isNull()) {
                                                        try (CFDictionaryRef diskInfo = disk.copyDescription()) {
                                                            if (!diskInfo.isNull()) {
                                                                MemorySegment r = diskInfo
                                                                        .getValue(cfKeyMap.get(CFKey.DA_MEDIA_NAME));
                                                                type = CFUtilFFM.cfPointerToString(r);
                                                                r = diskInfo
                                                                        .getValue(cfKeyMap.get(CFKey.DA_VOLUME_NAME));
                                                                if (r.equals(MemorySegment.NULL)) {
                                                                    name = type;
                                                                } else {
                                                                    String volumeName = CFUtilFFM.cfPointerToString(r);
                                                                    name = volumeName;
                                                                    label = volumeName;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                String mountPoint = mountPointMap.getOrDefault(partBsdName, "");
                                                Long size = current.getLongProperty("Size");
                                                Integer bsdMajor = current.getIntegerProperty("BSD Major");
                                                Integer bsdMinor = current.getIntegerProperty("BSD Minor");
                                                String uuid = current.getStringProperty("UUID");
                                                partitions.add(new HWPartition(partBsdName, name, type,
                                                        uuid == null ? Constants.UNKNOWN : uuid, label,
                                                        size == null ? 0L : size, bsdMajor == null ? 0 : bsdMajor,
                                                        bsdMinor == null ? 0 : bsdMinor, mountPoint));
                                            }
                                            sdService = serviceIterator.next();
                                        }
                                    }
                                }
                            } catch (Throwable e) {
                                LOG.debug("Error building partition list for {}", bsdName, e);
                            }
                        }
                    }
                    setPartitionList(Collections.unmodifiableList(partitions.stream()
                            .sorted(Comparator.comparing(HWPartition::getName)).collect(Collectors.toList())));
                } finally {
                    if (parent != null) {
                        parent.release();
                    }
                }
            }
        }
        return true;
    }

    public static List<HWDiskStore> getDisks() {
        Map<String, String> mountPointMap = FsstatFFM.queryPartitionToMountMap();
        Map<CFKey, CFStringRef> cfKeyMap = mapCFKeys();
        List<HWDiskStore> diskList = new ArrayList<>();

        try {
            CFAllocatorRef alloc = new CFAllocatorRef(CoreFoundationFunctions.CFAllocatorGetDefault());
            DASessionRef session = DASessionRef.create(alloc);
            if (session.isNull()) {
                LOG.error("Unable to open session to DiskArbitration framework.");
                return Collections.emptyList();
            }
            try (session) {
                List<String> bsdNames = new ArrayList<>();
                IOIterator iter = IOKitUtilFFM.getMatchingServices("IOMedia");
                if (iter != null) {
                    try (iter) {
                        IORegistryEntry media = iter.next();
                        while (media != null) {
                            try (IORegistryEntry current = media) {
                                Boolean whole = current.getBooleanProperty("Whole");
                                if (Boolean.TRUE.equals(whole)) {
                                    try (DADiskRef disk = DADiskRef.createFromIOMedia(alloc, session, current)) {
                                        if (!disk.isNull()) {
                                            String bsdName = disk.getBSDName();
                                            if (bsdName != null) {
                                                bsdNames.add(bsdName);
                                            }
                                        }
                                    }
                                }
                            }
                            media = iter.next();
                        }
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
                    try (disk) {
                        try (CFDictionaryRef diskInfo = disk.copyDescription()) {
                            if (!diskInfo.isNull()) {
                                MemorySegment result = diskInfo.getValue(cfKeyMap.get(CFKey.DA_DEVICE_MODEL));
                                model = CFUtilFFM.cfPointerToString(result);
                                result = diskInfo.getValue(cfKeyMap.get(CFKey.DA_MEDIA_SIZE));
                                if (!result.equals(MemorySegment.NULL)) {
                                    size = new CFNumberRef(result).longValue();
                                }

                                if (!"Disk Image".equals(model)) {
                                    try (CFStringRef modelNameRef = CFStringRef.createCFString(model)) {
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
                                            try (serviceIterator) {
                                                IORegistryEntry sdService = serviceIterator.next();
                                                while (sdService != null) {
                                                    try (IORegistryEntry current = sdService) {
                                                        serial = current.getStringProperty("Serial Number");
                                                    }
                                                    if (serial != null) {
                                                        break;
                                                    }
                                                    sdService = serviceIterator.next();
                                                }
                                            }
                                        }
                                    }
                                    if (serial == null) {
                                        serial = "";
                                    }
                                }
                            }
                        }
                    }

                    if (size <= 0) {
                        continue;
                    }
                    diskList.add(new MacHWDiskStoreFFM(bsdName, model.trim(), serial.trim(), size,
                            detectDiskType(bsdName), session, mountPointMap, cfKeyMap));
                }
            }
        } catch (Throwable e) {
            LOG.error("Error enumerating disks", e);
        } finally {
            for (CFStringRef value : cfKeyMap.values()) {
                value.release();
            }
        }
        return diskList;
    }

    /**
     * Creates a map of {@link CFKey} to {@link CFStringRef} for use in IOKit/CoreFoundation lookups. Callers
     * ({@link #getDisks()} and {@link #updateAttributes()}) are responsible for calling {@link CFStringRef#release()}
     * on all values to avoid native memory leaks.
     *
     * @return a new map of all {@link CFKey} values to their corresponding {@link CFStringRef}
     */
    private static Map<CFKey, CFStringRef> mapCFKeys() {
        Map<CFKey, CFStringRef> keyMap = new EnumMap<>(CFKey.class);
        for (CFKey cfKey : CFKey.values()) {
            keyMap.put(cfKey, CFStringRef.createCFString(cfKey.getKey()));
        }
        return keyMap;
    }

    private static String detectDiskType(String bsdName) {
        MemorySegment matchingDict = IOKitUtilFFM.getBSDNameMatchingDict(bsdName);
        if (matchingDict == null || matchingDict.equals(MemorySegment.NULL)) {
            return "Unknown";
        }
        IOIterator iter = IOKitUtilFFM.getMatchingServices(matchingDict);
        if (iter == null) {
            return "Unknown";
        }
        try (iter) {
            IORegistryEntry media = iter.next();
            if (media == null) {
                return "Unknown";
            }
            try (media) {
                // Check removable at the IOMedia level
                Boolean removable = media.getBooleanProperty("Removable");
                if (removable != null && removable) {
                    return "Removable";
                }
                // Traverse up: IOMedia -> IOBlockStorageDriver -> IOBlockStorageDevice
                IORegistryEntry driver = media.getParentEntry("IOService");
                if (driver != null) {
                    try (driver) {
                        IORegistryEntry device = driver.getParentEntry("IOService");
                        if (device != null) {
                            try (device) {
                                MemorySegment propsSeg = device.createCFProperties();
                                if (!propsSeg.equals(MemorySegment.NULL)) {
                                    try (CFDictionaryRef props = new CFDictionaryRef(propsSeg)) {
                                        try (CFStringRef devCharKey = CFStringRef
                                                .createCFString("Device Characteristics")) {
                                            MemorySegment charSeg = props.getValue(devCharKey);
                                            if (!charSeg.equals(MemorySegment.NULL)) {
                                                CFDictionaryRef chars = new CFDictionaryRef(charSeg);
                                                try (CFStringRef medTypeKey = CFStringRef
                                                        .createCFString("Medium Type")) {
                                                    MemorySegment typeSeg = chars.getValue(medTypeKey);
                                                    if (!typeSeg.equals(MemorySegment.NULL)) {
                                                        String type = new CFStringRef(typeSeg).stringValue();
                                                        if (type != null) {
                                                            if (type.contains("Solid State") || type.contains("SSD")) {
                                                                return "SSD";
                                                            } else if (type.contains("Rotational")) {
                                                                return "HDD";
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return "Unknown";
    }

}
