/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer; // NOSONAR squid:S1191
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.platform.mac.SystemB.Statfs;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.hardware.Disks;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.jna.platform.mac.CoreFoundation;
import oshi.jna.platform.mac.CoreFoundation.CFBooleanRef;
import oshi.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import oshi.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import oshi.jna.platform.mac.CoreFoundation.CFNumberRef;
import oshi.jna.platform.mac.CoreFoundation.CFStringRef;
import oshi.jna.platform.mac.DiskArbitration;
import oshi.jna.platform.mac.DiskArbitration.DADiskRef;
import oshi.jna.platform.mac.DiskArbitration.DASessionRef;
import oshi.jna.platform.mac.IOKit;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.mac.CfUtil;
import oshi.util.platform.mac.IOKitUtil;

/**
 * Mac hard disk implementation.
 */
public class MacDisks implements Disks {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(MacDisks.class);

    /*
     * Strings to convert to CFStringRef for pointer lookups
     */
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

    /**
     * Temporarily cache pointers to keys. The values from this map must be released
     * after use.}
     * 
     * @return A map of keys in the {@link CFKey} enum to corresponding
     *         {@link CFStringRef}.
     */
    private static Map<CFKey, CFStringRef> mapCFKeys() {
        Map<CFKey, CFStringRef> keyMap = new EnumMap<>(CFKey.class);
        for (CFKey cfKey : CFKey.values()) {
            keyMap.put(cfKey, CFStringRef.toCFString(cfKey.getKey()));
        }
        return keyMap;
    }

    private static Map<String, String> queryMountPointMap() {
        final Map<String, String> mountPointMap = new HashMap<>();
        // Use statfs to populate mount point map
        int numfs = SystemB.INSTANCE.getfsstat64(null, 0, 0);
        // Create array to hold results
        Statfs[] fs = new Statfs[numfs];
        // Fill array with results
        SystemB.INSTANCE.getfsstat64(fs, numfs * new Statfs().size(), SystemB.MNT_NOWAIT);
        // Iterate all mounted file systems
        for (Statfs f : fs) {
            String mntFrom = new String(f.f_mntfromname).trim();
            mountPointMap.put(mntFrom.replace("/dev/", ""), new String(f.f_mntonname).trim());
        }
        return mountPointMap;
    }

    private static Map<String, String> queryLogicalVolumeMap() {
        final Map<String, String> logicalVolumeMap = new HashMap<>();
        // Parse `diskutil cs list` to populate logical volume map
        Set<String> physicalVolumes = new HashSet<>();
        boolean logicalVolume = false;
        for (String line : ExecutingCommand.runNative("diskutil cs list")) {
            if (line.contains("Logical Volume Group")) {
                // Logical Volume Group defines beginning of grouping which will
                // list multiple physical volumes followed by the logical volume
                // they are associated with. Each physical volume will be a key
                // with the logical volume as its value, but since the value
                // doesn't appear until the end we collect the keys in a list
                physicalVolumes.clear();
                logicalVolume = false;
            } else if (line.contains("Logical Volume Family")) {
                // Done collecting physical volumes, prepare to store logical
                // volume
                logicalVolume = true;
            } else if (line.contains("Disk:")) {
                String volume = ParseUtil.parseLastString(line);
                if (logicalVolume) {
                    // Store this disk as the logical volume value for all the
                    // physical volume keys
                    for (String pv : physicalVolumes) {
                        logicalVolumeMap.put(pv, volume);
                    }
                    physicalVolumes.clear();
                } else {
                    physicalVolumes.add(ParseUtil.parseLastString(line));
                }
            }
        }
        return logicalVolumeMap;
    }

    private static boolean updateDiskStats(HWDiskStore diskStore, DASessionRef session,
            Map<String, String> mountPointMap, Map<String, String> logicalVolumeMap, Map<CFKey, CFStringRef> cfKeyMap) {
        // Now look up the device using the BSD Name to get its
        // statistics
        String bsdName = diskStore.getName();
        CFMutableDictionaryRef matchingDict = IOKitUtil.getBSDNameMatchingDict(bsdName);
        if (matchingDict != null) {
            // search for all IOservices that match the bsd name
            IntByReference driveList = new IntByReference();
            IOKitUtil.getMatchingServices(matchingDict, driveList);
            // getMatchingServices releases matchingDict
            int drive = IOKit.INSTANCE.IOIteratorNext(driveList.getValue());
            // Should only match one drive
            if (drive != 0) {
                // Should be an IOMedia object with a parent
                // IOBlockStorageDriver object
                // Get the properties from the parent
                IntByReference parent = new IntByReference();
                if (IOKit.INSTANCE.IOObjectConformsTo(drive, "IOMedia")
                        && IOKit.INSTANCE.IORegistryEntryGetParentEntry(drive, "IOService", parent) == 0) {
                    PointerByReference propsPtr = new PointerByReference();
                    if (IOKit.INSTANCE.IOObjectConformsTo(parent.getValue(), "IOBlockStorageDriver") && IOKit.INSTANCE
                            .IORegistryEntryCreateCFProperties(parent.getValue(), propsPtr, CfUtil.ALLOCATOR, 0) == 0) {
                        CFMutableDictionaryRef properties = new CFMutableDictionaryRef();
                        properties.setPointer(propsPtr.getValue());
                        // We now have a properties object with the
                        // statistics we need on it. Fetch them
                        Pointer statsPtr = CoreFoundation.INSTANCE.CFDictionaryGetValue(properties,
                                cfKeyMap.get(CFKey.STATISTICS));
                        diskStore.setTimeStamp(System.currentTimeMillis());
                        CFDictionaryRef statistics = new CFDictionaryRef();
                        statistics.setPointer(statsPtr);

                        // Now get the stats we want
                        Pointer stat = CoreFoundation.INSTANCE.CFDictionaryGetValue(statistics,
                                cfKeyMap.get(CFKey.READ_OPS));
                        diskStore.setReads(CfUtil.cfPointerToLong(stat));
                        stat = CoreFoundation.INSTANCE.CFDictionaryGetValue(statistics, cfKeyMap.get(CFKey.READ_BYTES));
                        diskStore.setReadBytes(CfUtil.cfPointerToLong(stat));

                        stat = CoreFoundation.INSTANCE.CFDictionaryGetValue(statistics, cfKeyMap.get(CFKey.WRITE_OPS));
                        diskStore.setWrites(CfUtil.cfPointerToLong(stat));
                        stat = CoreFoundation.INSTANCE.CFDictionaryGetValue(statistics,
                                cfKeyMap.get(CFKey.WRITE_BYTES));
                        diskStore.setWriteBytes(CfUtil.cfPointerToLong(stat));

                        // Total time is in nanoseconds. Add read+write
                        // and convert total to ms
                        stat = CoreFoundation.INSTANCE.CFDictionaryGetValue(statistics, cfKeyMap.get(CFKey.READ_TIME));
                        long xferTime = CfUtil.cfPointerToLong(stat);
                        stat = CoreFoundation.INSTANCE.CFDictionaryGetValue(statistics, cfKeyMap.get(CFKey.WRITE_TIME));
                        xferTime += CfUtil.cfPointerToLong(stat);
                        diskStore.setTransferTime(xferTime / 1_000_000L);

                        CfUtil.release(properties);
                    } else {
                        // This is normal for FileVault drives, Fusion
                        // drives, and other virtual bsd names
                        LOG.debug("Unable to find block storage driver properties for {}", bsdName);
                    }
                    // Now get partitions for this disk.
                    List<HWPartition> partitions = new ArrayList<>();
                    if (IOKit.INSTANCE.IORegistryEntryCreateCFProperties(drive, propsPtr, CfUtil.ALLOCATOR, 0) == 0) {
                        CFMutableDictionaryRef properties = new CFMutableDictionaryRef();
                        properties.setPointer(propsPtr.getValue());
                        // Partitions will match BSD Unit property
                        Pointer p = CoreFoundation.INSTANCE.CFDictionaryGetValue(properties,
                                cfKeyMap.get(CFKey.BSD_UNIT));
                        CFNumberRef bsdUnit = new CFNumberRef();
                        bsdUnit.setPointer(p);
                        // We need a CFBoolean that's false.
                        // Whole disk has 'true' for Whole and 'false'
                        // for leaf; store the boolean false
                        p = CoreFoundation.INSTANCE.CFDictionaryGetValue(properties, cfKeyMap.get(CFKey.LEAF));
                        CFBooleanRef cfFalse = new CFBooleanRef();
                        cfFalse.setPointer(p);
                        // create a matching dict for BSD Unit
                        CFMutableDictionaryRef propertyDict = CoreFoundation.INSTANCE
                                .CFDictionaryCreateMutable(CfUtil.ALLOCATOR, 0, null, null);
                        CoreFoundation.INSTANCE.CFDictionarySetValue(propertyDict, cfKeyMap.get(CFKey.BSD_UNIT),
                                bsdUnit);
                        CoreFoundation.INSTANCE.CFDictionarySetValue(propertyDict, cfKeyMap.get(CFKey.WHOLE), cfFalse);
                        matchingDict = CoreFoundation.INSTANCE.CFDictionaryCreateMutable(CfUtil.ALLOCATOR, 0, null,
                                null);
                        CoreFoundation.INSTANCE.CFDictionarySetValue(matchingDict,
                                cfKeyMap.get(CFKey.IO_PROPERTY_MATCH), propertyDict);

                        // search for IOservices that match the BSD Unit
                        // with whole=false; these are partitions
                        IntByReference serviceIterator = new IntByReference();
                        IOKitUtil.getMatchingServices(matchingDict, serviceIterator);
                        // getMatchingServices releases matchingDict
                        CfUtil.release(properties);
                        CfUtil.release(propertyDict);

                        // Iterate disks
                        int sdService = IOKit.INSTANCE.IOIteratorNext(serviceIterator.getValue());
                        while (sdService != 0) {
                            // look up the BSD Name
                            String partBsdName = IOKitUtil.getIORegistryStringProperty(sdService, "BSD Name");
                            String name = partBsdName;
                            String type = "";
                            // Get the DiskArbitration dictionary for
                            // this partition
                            DADiskRef disk = DiskArbitration.INSTANCE.DADiskCreateFromBSDName(CfUtil.ALLOCATOR, session,
                                    partBsdName);
                            if (disk != null) {
                                CFDictionaryRef diskInfo = DiskArbitration.INSTANCE.DADiskCopyDescription(disk);
                                if (diskInfo != null) {
                                    // get volume name from its key
                                    Pointer volumePtr = CoreFoundation.INSTANCE.CFDictionaryGetValue(diskInfo,
                                            cfKeyMap.get(CFKey.DA_MEDIA_NAME));
                                    type = CfUtil.cfPointerToString(volumePtr);
                                    volumePtr = CoreFoundation.INSTANCE.CFDictionaryGetValue(diskInfo,
                                            cfKeyMap.get(CFKey.DA_VOLUME_NAME));
                                    if (volumePtr == null) {
                                        name = type;
                                    } else {
                                        name = CfUtil.cfPointerToString(volumePtr);
                                    }
                                    CfUtil.release(diskInfo);
                                }
                                CfUtil.release(disk);
                            }
                            String mountPoint;
                            if (logicalVolumeMap.containsKey(partBsdName)) {
                                mountPoint = "Logical Volume: " + logicalVolumeMap.get(partBsdName);
                            } else {
                                mountPoint = mountPointMap.getOrDefault(partBsdName, "");
                            }
                            partitions.add(new HWPartition(partBsdName, name, type,
                                    IOKitUtil.getIORegistryStringProperty(sdService, "UUID"),
                                    IOKitUtil.getIORegistryLongProperty(sdService, "Size"),
                                    IOKitUtil.getIORegistryIntProperty(sdService, "BSD Major"),
                                    IOKitUtil.getIORegistryIntProperty(sdService, "BSD Minor"), mountPoint));
                            IOKit.INSTANCE.IOObjectRelease(sdService);
                            // iterate
                            sdService = IOKit.INSTANCE.IOIteratorNext(serviceIterator.getValue());
                        }
                        IOKit.INSTANCE.IOObjectRelease(serviceIterator.getValue());

                    } else {
                        LOG.error("Unable to find properties for {}", bsdName);
                    }
                    Collections.sort(partitions);
                    diskStore.setPartitions(partitions.toArray(new HWPartition[0]));
                    IOKit.INSTANCE.IOObjectRelease(parent.getValue());
                } else {
                    LOG.error("Unable to find IOMedia device or parent for {}", bsdName);
                }
                IOKit.INSTANCE.IOObjectRelease(drive);
            }
            IOKit.INSTANCE.IOObjectRelease(driveList.getValue());
            return true;
        } else {
            return false;
        }
    }

    /**
     * <p>
     * updateDiskStats.
     * </p>
     *
     * @param diskStore
     *            a {@link oshi.hardware.HWDiskStore} object.
     * @return a boolean.
     */
    public static boolean updateDiskStats(HWDiskStore diskStore) {
        DASessionRef session = DiskArbitration.INSTANCE.DASessionCreate(CfUtil.ALLOCATOR);
        if (session == null) {
            LOG.error("Unable to open session to DiskArbitration framework.");
            return false;
        }
        Map<CFKey, CFStringRef> cfKeyMap = mapCFKeys();

        boolean diskFound = updateDiskStats(diskStore, session, queryMountPointMap(), queryLogicalVolumeMap(),
                cfKeyMap);

        CfUtil.release(session);
        CfUtil.releaseAll(cfKeyMap.values());

        return diskFound;
    }

    /** {@inheritDoc} */
    @Override
    public HWDiskStore[] getDisks() {
        Map<String, String> mountPointMap = queryMountPointMap();
        Map<String, String> logicalVolumeMap = queryLogicalVolumeMap();
        Map<CFKey, CFStringRef> cfKeyMap = mapCFKeys();

        List<HWDiskStore> result = new ArrayList<>();

        // Open a DiskArbitration session
        DASessionRef session = DiskArbitration.INSTANCE.DASessionCreate(CfUtil.ALLOCATOR);
        if (session == null) {
            LOG.error("Unable to open session to DiskArbitration framework.");
            return new HWDiskStore[0];
        }

        // Get IOMedia objects representing whole drives
        List<String> bsdNames = new ArrayList<>();
        IntByReference iter = new IntByReference();
        IOKitUtil.getMatchingServices("IOMedia", iter);
        int media = IOKit.INSTANCE.IOIteratorNext(iter.getValue());
        while (media != 0) {
            if (IOKitUtil.getIORegistryBooleanProperty(media, "Whole")) {
                DADiskRef disk = DiskArbitration.INSTANCE.DADiskCreateFromIOMedia(CfUtil.ALLOCATOR, session, media);
                bsdNames.add(DiskArbitration.INSTANCE.DADiskGetBSDName(disk));
            }
            IOKit.INSTANCE.IOObjectRelease(media);
            media = IOKit.INSTANCE.IOIteratorNext(iter.getValue());
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
            DADiskRef disk = DiskArbitration.INSTANCE.DADiskCreateFromBSDName(CfUtil.ALLOCATOR, session, path);
            if (disk != null) {
                CFDictionaryRef diskInfo = DiskArbitration.INSTANCE.DADiskCopyDescription(disk);
                if (diskInfo != null) {
                    // Parse out model and size from their respective keys
                    Pointer modelPtr = CoreFoundation.INSTANCE.CFDictionaryGetValue(diskInfo,
                            cfKeyMap.get(CFKey.DA_DEVICE_MODEL));
                    model = CfUtil.cfPointerToString(modelPtr);
                    Pointer sizePtr = CoreFoundation.INSTANCE.CFDictionaryGetValue(diskInfo,
                            cfKeyMap.get(CFKey.DA_MEDIA_SIZE));
                    size = CfUtil.cfPointerToLong(sizePtr);
                    CfUtil.release(diskInfo);

                    // Use the model as a key to get serial from IOKit
                    if (!"Disk Image".equals(model)) {
                        CFStringRef modelNameRef = CFStringRef.toCFString(model);
                        CFMutableDictionaryRef propertyDict = CoreFoundation.INSTANCE
                                .CFDictionaryCreateMutable(CfUtil.ALLOCATOR, 0, null, null);
                        CoreFoundation.INSTANCE.CFDictionarySetValue(propertyDict, cfKeyMap.get(CFKey.MODEL),
                                modelNameRef);
                        CFMutableDictionaryRef matchingDict = CoreFoundation.INSTANCE
                                .CFDictionaryCreateMutable(CfUtil.ALLOCATOR, 0, null, null);
                        CoreFoundation.INSTANCE.CFDictionarySetValue(matchingDict,
                                cfKeyMap.get(CFKey.IO_PROPERTY_MATCH), propertyDict);

                        // search for all IOservices that match the model
                        IntByReference serviceIterator = new IntByReference();
                        IOKitUtil.getMatchingServices(matchingDict, serviceIterator);
                        // getMatchingServices releases matchingDict
                        CfUtil.release(modelNameRef);
                        CfUtil.release(propertyDict);
                        int sdService = IOKit.INSTANCE.IOIteratorNext(serviceIterator.getValue());
                        while (sdService != 0) {
                            // look up the serial number
                            serial = IOKitUtil.getIORegistryStringProperty(sdService, "Serial Number");
                            IOKit.INSTANCE.IOObjectRelease(sdService);
                            if (serial != null) {
                                break;
                            }
                            // iterate
                            sdService = IOKit.INSTANCE.IOIteratorNext(serviceIterator.getValue());
                        }
                        if (serial == null) {
                            serial = "";
                        }
                        IOKit.INSTANCE.IOObjectRelease(serviceIterator.getValue());
                    }
                }
                CfUtil.release(disk);

                // If empty, ignore
                if (size <= 0) {
                    continue;
                }
                HWDiskStore diskStore = new HWDiskStore();
                diskStore.setName(bsdName);
                diskStore.setModel(model.trim());
                diskStore.setSerial(serial.trim());
                diskStore.setSize(size);

                updateDiskStats(diskStore, session, mountPointMap, logicalVolumeMap, cfKeyMap);
                result.add(diskStore);
            }
        }
        // Close DA session
        CfUtil.release(session);
        CfUtil.releaseAll(cfKeyMap.values());
        Collections.sort(result);
        return result.toArray(new HWDiskStore[0]);
    }
}
