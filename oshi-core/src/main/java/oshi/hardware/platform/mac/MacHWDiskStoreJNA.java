/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFBooleanRef;
import com.sun.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFIndex;
import com.sun.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFNumberRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.platform.mac.DiskArbitration;
import com.sun.jna.platform.mac.DiskArbitration.DADiskRef;
import com.sun.jna.platform.mac.DiskArbitration.DASessionRef;
import com.sun.jna.platform.mac.IOKit;
import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.mac.disk.Fsstat;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.platform.mac.MacHWDiskStore;
import oshi.util.Constants;
import oshi.util.platform.mac.CFUtil;

/**
 * Mac hard disk implementation.
 */
@ThreadSafe
public final class MacHWDiskStoreJNA extends MacHWDiskStore {

    private static final CoreFoundation CF = CoreFoundation.INSTANCE;
    private static final DiskArbitration DA = DiskArbitration.INSTANCE;

    private static final Logger LOG = LoggerFactory.getLogger(MacHWDiskStoreJNA.class);

    private MacHWDiskStoreJNA(String name, String model, String serial, long size, String diskType,
            DASessionRef session, Map<String, String> mountPointMap, Map<CFKey, CFStringRef> cfKeyMap) {
        super(name, model, serial, size, diskType);
        updateDiskStats(session, mountPointMap, cfKeyMap);
    }

    @Override
    public boolean updateAttributes() {
        // Open a session and create CFStrings
        DASessionRef session = DA.DASessionCreate(CF.CFAllocatorGetDefault());
        if (session == null) {
            LOG.error("Unable to open session to DiskArbitration framework.");
            return false;
        }
        Map<CFKey, CFStringRef> cfKeyMap = mapCFKeys();
        // Execute the update
        boolean diskFound = updateDiskStats(session, Fsstat.queryPartitionToMountMap(), cfKeyMap);
        // Release the session and CFStrings
        session.release();
        for (CFTypeRef value : cfKeyMap.values()) {
            value.release();
        }

        return diskFound;
    }

    private boolean updateDiskStats(DASessionRef session, Map<String, String> mountPointMap,
            Map<CFKey, CFStringRef> cfKeyMap) {
        // Now look up the device using the BSD Name to get its
        // statistics
        String bsdName = getName();
        CFMutableDictionaryRef matchingDict = IOKitUtil.getBSDNameMatchingDict(bsdName);
        if (matchingDict != null) {
            // search for all IOservices that match the bsd name
            IOIterator driveListIter = IOKitUtil.getMatchingServices(matchingDict);
            if (driveListIter != null) {
                // getMatchingServices releases matchingDict
                IORegistryEntry drive = driveListIter.next();
                // Should only match one drive
                if (drive != null) {
                    // Should be an IOMedia object with a parent
                    // IOBlockStorageDriver or AppleAPFSContainerScheme object
                    // Get the properties from the parent
                    if (drive.conformsTo("IOMedia")) {
                        IORegistryEntry parent = drive.getParentEntry("IOService");
                        if (parent != null && (parent.conformsTo("IOBlockStorageDriver")
                                || parent.conformsTo("AppleAPFSContainerScheme"))) {
                            CFMutableDictionaryRef properties = parent.createCFProperties();
                            // We now have a properties object with the
                            // statistics we need on it. Fetch them
                            Pointer result = properties.getValue(cfKeyMap.get(CFKey.STATISTICS));
                            CFDictionaryRef statistics = new CFDictionaryRef(result);
                            setTimeStamp(System.currentTimeMillis());

                            // Now get the stats we want
                            result = statistics.getValue(cfKeyMap.get(CFKey.READ_OPS));
                            CFNumberRef stat = new CFNumberRef(result);
                            setReads(stat.longValue());
                            result = statistics.getValue(cfKeyMap.get(CFKey.READ_BYTES));
                            stat.setPointer(result);
                            setReadBytes(stat.longValue());

                            result = statistics.getValue(cfKeyMap.get(CFKey.WRITE_OPS));
                            stat.setPointer(result);
                            setWrites(stat.longValue());
                            result = statistics.getValue(cfKeyMap.get(CFKey.WRITE_BYTES));
                            stat.setPointer(result);
                            setWriteBytes(stat.longValue());

                            // Total time is in nanoseconds. Add read+write
                            // and convert total to ms
                            final Pointer readTimeResult = statistics.getValue(cfKeyMap.get(CFKey.READ_TIME));
                            final Pointer writeTimeResult = statistics.getValue(cfKeyMap.get(CFKey.WRITE_TIME));
                            // AppleAPFSContainerScheme does not have timer statistics
                            if (readTimeResult != null && writeTimeResult != null) {
                                stat.setPointer(readTimeResult);
                                long xferTime = stat.longValue();
                                stat.setPointer(writeTimeResult);
                                xferTime += stat.longValue();
                                setTransferTime(xferTime / 1_000_000L);
                            }

                            properties.release();
                        } else {
                            // This is normal for FileVault drives, Fusion
                            // drives, and other virtual bsd names
                            LOG.debug("Unable to find block storage driver properties for {}", bsdName);
                        }
                        // Now get partitions for this disk.
                        List<HWPartition> partitions = new ArrayList<>();

                        CFMutableDictionaryRef properties = drive.createCFProperties();
                        // Partitions will match BSD Unit property
                        Pointer result = properties.getValue(cfKeyMap.get(CFKey.BSD_UNIT));
                        CFNumberRef bsdUnit = new CFNumberRef(result);
                        // We need a CFBoolean that's false.
                        // Whole disk has 'true' for Whole and 'false'
                        // for leaf; store the boolean false
                        result = properties.getValue(cfKeyMap.get(CFKey.LEAF));
                        CFBooleanRef cfFalse = new CFBooleanRef(result);
                        // create a matching dict for BSD Unit
                        CFMutableDictionaryRef propertyDict = CF.CFDictionaryCreateMutable(CF.CFAllocatorGetDefault(),
                                new CFIndex(0), null, null);
                        propertyDict.setValue(cfKeyMap.get(CFKey.BSD_UNIT), bsdUnit);
                        propertyDict.setValue(cfKeyMap.get(CFKey.WHOLE), cfFalse);
                        matchingDict = CF.CFDictionaryCreateMutable(CF.CFAllocatorGetDefault(), new CFIndex(0), null,
                                null);
                        matchingDict.setValue(cfKeyMap.get(CFKey.IO_PROPERTY_MATCH), propertyDict);

                        // search for IOservices that match the BSD Unit
                        // with whole=false; these are partitions
                        IOIterator serviceIterator = IOKitUtil.getMatchingServices(matchingDict);
                        // getMatchingServices releases matchingDict
                        properties.release();
                        propertyDict.release();

                        if (serviceIterator != null) {
                            // Iterate disks
                            IORegistryEntry sdService = IOKit.INSTANCE.IOIteratorNext(serviceIterator);
                            while (sdService != null) {
                                // look up the BSD Name
                                String partBsdName = sdService.getStringProperty("BSD Name");
                                String name = partBsdName;
                                String type = "";
                                String label = "";
                                // Get the DiskArbitration dictionary for
                                // this partition
                                DADiskRef disk = DA.DADiskCreateFromBSDName(CF.CFAllocatorGetDefault(), session,
                                        partBsdName);
                                if (disk != null) {
                                    CFDictionaryRef diskInfo = DA.DADiskCopyDescription(disk);
                                    if (diskInfo != null) {
                                        // get volume name from its key
                                        result = diskInfo.getValue(cfKeyMap.get(CFKey.DA_MEDIA_NAME));
                                        type = CFUtil.cfPointerToString(result);
                                        result = diskInfo.getValue(cfKeyMap.get(CFKey.DA_VOLUME_NAME));
                                        if (result == null) {
                                            name = type;
                                        } else {
                                            String volumeName = CFUtil.cfPointerToString(result);
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
                                        uuid == null ? Constants.UNKNOWN : uuid, label, size == null ? 0L : size,
                                        bsdMajor == null ? 0 : bsdMajor, bsdMinor == null ? 0 : bsdMinor, mountPoint));
                                // iterate
                                sdService.release();
                                sdService = IOKit.INSTANCE.IOIteratorNext(serviceIterator);
                            }
                            serviceIterator.release();
                        }
                        setPartitionList(Collections.unmodifiableList(partitions.stream()
                                .sorted(Comparator.comparing(HWPartition::getName)).collect(Collectors.toList())));
                        if (parent != null) {
                            parent.release();
                        }
                    } else {
                        LOG.error("Unable to find IOMedia device or parent for {}", bsdName);
                    }
                    drive.release();
                }
                driveListIter.release();
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the disks on this machine
     *
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        Map<String, String> mountPointMap = Fsstat.queryPartitionToMountMap();
        Map<CFKey, CFStringRef> cfKeyMap = mapCFKeys();

        List<HWDiskStore> diskList = new ArrayList<>();

        // Open a DiskArbitration session
        DASessionRef session = DA.DASessionCreate(CF.CFAllocatorGetDefault());
        if (session == null) {
            LOG.error("Unable to open session to DiskArbitration framework.");
            return Collections.emptyList();
        }

        // Get IOMedia objects representing whole drives
        List<String> bsdNames = new ArrayList<>();
        IOIterator iter = IOKitUtil.getMatchingServices("IOMedia");
        if (iter != null) {
            IORegistryEntry media = iter.next();
            while (media != null) {
                Boolean whole = media.getBooleanProperty("Whole");
                if (whole != null && whole) {
                    DADiskRef disk = DA.DADiskCreateFromIOMedia(CF.CFAllocatorGetDefault(), session, media);
                    bsdNames.add(DA.DADiskGetBSDName(disk));
                    disk.release();
                }
                media.release();
                media = iter.next();
            }
            iter.release();
        }

        // Now iterate the bsdNames
        for (String bsdName : bsdNames) {
            String model = "";
            String serial = "";
            long size = 0L;

            // Get a reference to the disk - only matching /dev/disk*
            String path = "/dev/" + bsdName;

            // Get the DiskArbitration dictionary for this disk, which has model
            // and size (capacity)
            DADiskRef disk = DA.DADiskCreateFromBSDName(CF.CFAllocatorGetDefault(), session, path);
            if (disk != null) {
                CFDictionaryRef diskInfo = DA.DADiskCopyDescription(disk);
                if (diskInfo != null) {
                    // Parse out model and size from their respective keys
                    Pointer result = diskInfo.getValue(cfKeyMap.get(CFKey.DA_DEVICE_MODEL));
                    model = CFUtil.cfPointerToString(result);
                    result = diskInfo.getValue(cfKeyMap.get(CFKey.DA_MEDIA_SIZE));
                    CFNumberRef sizePtr = new CFNumberRef(result);
                    size = sizePtr.longValue();
                    diskInfo.release();

                    // Use the model as a key to get serial from IOKit
                    if (!"Disk Image".equals(model)) {
                        CFStringRef modelNameRef = CFStringRef.createCFString(model);
                        CFMutableDictionaryRef propertyDict = CF.CFDictionaryCreateMutable(CF.CFAllocatorGetDefault(),
                                new CFIndex(0), null, null);
                        propertyDict.setValue(cfKeyMap.get(CFKey.MODEL), modelNameRef);
                        CFMutableDictionaryRef matchingDict = CF.CFDictionaryCreateMutable(CF.CFAllocatorGetDefault(),
                                new CFIndex(0), null, null);
                        matchingDict.setValue(cfKeyMap.get(CFKey.IO_PROPERTY_MATCH), propertyDict);

                        // search for all IOservices that match the model
                        IOIterator serviceIterator = IOKitUtil.getMatchingServices(matchingDict);
                        // getMatchingServices releases matchingDict
                        modelNameRef.release();
                        propertyDict.release();

                        if (serviceIterator != null) {
                            IORegistryEntry sdService = serviceIterator.next();
                            while (sdService != null) {
                                // look up the serial number
                                serial = sdService.getStringProperty("Serial Number");
                                sdService.release();
                                if (serial != null) {
                                    break;
                                }
                                // iterate
                                sdService.release();
                                sdService = serviceIterator.next();
                            }
                            serviceIterator.release();
                        }
                        if (serial == null) {
                            serial = "";
                        }
                    }
                }
                disk.release();

                // If empty, ignore
                if (size <= 0) {
                    continue;
                }
                HWDiskStore diskStore = new MacHWDiskStoreJNA(bsdName, model.trim(), serial.trim(), size,
                        detectDiskType(bsdName), session, mountPointMap, cfKeyMap);
                diskList.add(diskStore);
            }
        }
        // Close DA session
        session.release();
        for (CFTypeRef value : cfKeyMap.values()) {
            value.release();
        }
        return diskList;
    }

    /**
     * Temporarily cache pointers to keys. The values from this map must be released after use.}
     *
     * @return A map of keys in the {@link CFKey} enum to corresponding {@link CFStringRef}.
     */
    private static Map<CFKey, CFStringRef> mapCFKeys() {
        Map<CFKey, CFStringRef> keyMap = new EnumMap<>(CFKey.class);
        for (CFKey cfKey : CFKey.values()) {
            keyMap.put(cfKey, CFStringRef.createCFString(cfKey.getKey()));
        }
        return keyMap;
    }

    private static String detectDiskType(String bsdName) {
        CFMutableDictionaryRef matchingDict = IOKitUtil.getBSDNameMatchingDict(bsdName);
        if (matchingDict == null) {
            return "Unknown";
        }
        IOIterator iter = IOKitUtil.getMatchingServices(matchingDict);
        if (iter == null) {
            return "Unknown";
        }
        try {
            IORegistryEntry media = iter.next();
            if (media != null) {
                try {
                    // Check removable at the IOMedia level
                    Boolean removable = media.getBooleanProperty("Removable");
                    if (removable != null && removable) {
                        return "Removable";
                    }
                    // Traverse up: IOMedia -> IOBlockStorageDriver -> IOBlockStorageDevice
                    IORegistryEntry driver = media.getParentEntry("IOService");
                    if (driver != null) {
                        try {
                            IORegistryEntry device = driver.getParentEntry("IOService");
                            if (device != null) {
                                try {
                                    CFMutableDictionaryRef props = device.createCFProperties();
                                    if (props != null) {
                                        Pointer charDict = props
                                                .getValue(CFStringRef.createCFString("Device Characteristics"));
                                        if (charDict != null) {
                                            CFDictionaryRef characteristics = new CFDictionaryRef(charDict);
                                            Pointer mediumType = characteristics
                                                    .getValue(CFStringRef.createCFString("Medium Type"));
                                            if (mediumType != null) {
                                                String type = CFUtil.cfPointerToString(mediumType);
                                                if (type != null) {
                                                    if (type.contains("Solid State") || type.contains("SSD")) {
                                                        return "SSD";
                                                    } else if (type.contains("Rotational")) {
                                                        return "HDD";
                                                    }
                                                }
                                            }
                                        }
                                        props.release();
                                    }
                                } finally {
                                    device.release();
                                }
                            }
                        } finally {
                            driver.release();
                        }
                    }
                } finally {
                    media.release();
                }
            }
        } finally {
            iter.release();
        }
        return "Unknown";
    }

}
