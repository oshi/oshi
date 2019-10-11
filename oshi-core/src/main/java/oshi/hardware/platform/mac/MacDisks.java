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

import oshi.hardware.Disks;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.jna.platform.mac.CoreFoundation;
import oshi.jna.platform.mac.CoreFoundation.CFBooleanRef;
import oshi.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import oshi.jna.platform.mac.CoreFoundation.CFIndex;
import oshi.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import oshi.jna.platform.mac.CoreFoundation.CFNumberRef;
import oshi.jna.platform.mac.CoreFoundation.CFStringRef;
import oshi.jna.platform.mac.CoreFoundation.CFTypeRef;
import oshi.jna.platform.mac.DiskArbitration;
import oshi.jna.platform.mac.DiskArbitration.DADiskRef;
import oshi.jna.platform.mac.DiskArbitration.DASessionRef;
import oshi.jna.platform.mac.IOKit;
import oshi.jna.platform.mac.IOKit.IOIterator;
import oshi.jna.platform.mac.IOKit.IORegistryEntry;
import oshi.jna.platform.mac.IOKitUtil;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Mac hard disk implementation.
 */
public class MacDisks implements Disks {

    private static final long serialVersionUID = 1L;

    private static final CoreFoundation CF = CoreFoundation.INSTANCE;
    private static final DiskArbitration DA = DiskArbitration.INSTANCE;

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
            keyMap.put(cfKey, CFStringRef.createCFString(cfKey.getKey()));
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
            IOIterator driveListIter = IOKitUtil.getMatchingServices(matchingDict);
            if (driveListIter != null) {
                // getMatchingServices releases matchingDict
                IORegistryEntry drive = driveListIter.next();
                // Should only match one drive
                if (drive != null) {
                    // Should be an IOMedia object with a parent
                    // IOBlockStorageDriver object
                    // Get the properties from the parent
                    if (drive.conformsTo("IOMedia")) {
                        IORegistryEntry parent = drive.getParentEntry("IOService");
                        if (parent != null && parent.conformsTo("IOBlockStorageDriver")) {
                            CFMutableDictionaryRef properties = parent.createCFProperties();
                            // We now have a properties object with the
                            // statistics we need on it. Fetch them
                            Pointer result = properties.getValue(cfKeyMap.get(CFKey.STATISTICS));
                            CFDictionaryRef statistics = new CFDictionaryRef(result);
                            diskStore.setTimeStamp(System.currentTimeMillis());

                            // Now get the stats we want
                            result = statistics.getValue(cfKeyMap.get(CFKey.READ_OPS));
                            CFNumberRef stat = new CFNumberRef(result);
                            diskStore.setReads(stat.longValue());
                            result = statistics.getValue(cfKeyMap.get(CFKey.READ_BYTES));
                            stat.setPointer(result);
                            diskStore.setReadBytes(stat.longValue());

                            result = statistics.getValue(cfKeyMap.get(CFKey.WRITE_OPS));
                            stat.setPointer(result);
                            diskStore.setWrites(stat.longValue());
                            result = statistics.getValue(cfKeyMap.get(CFKey.WRITE_BYTES));
                            stat.setPointer(result);
                            diskStore.setWriteBytes(stat.longValue());

                            // Total time is in nanoseconds. Add read+write
                            // and convert total to ms
                            result = statistics.getValue(cfKeyMap.get(CFKey.READ_TIME));
                            stat.setPointer(result);
                            long xferTime = stat.longValue();
                            result = statistics.getValue(cfKeyMap.get(CFKey.WRITE_TIME));
                            stat.setPointer(result);
                            xferTime += stat.longValue();
                            diskStore.setTransferTime(xferTime / 1_000_000L);

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
                                // Get the DiskArbitration dictionary for
                                // this partition
                                DADiskRef disk = DA.DADiskCreateFromBSDName(CF.CFAllocatorGetDefault(), session,
                                        partBsdName);
                                if (disk != null) {
                                    CFDictionaryRef diskInfo = DA.DADiskCopyDescription(disk);
                                    if (diskInfo != null) {
                                        // get volume name from its key
                                        result = diskInfo.getValue(cfKeyMap.get(CFKey.DA_MEDIA_NAME));
                                        CFStringRef volumePtr = new CFStringRef(result);
                                        type = volumePtr.stringValue();
                                        if (type == null) {
                                            type = Constants.UNKNOWN;
                                        }
                                        result = diskInfo.getValue(cfKeyMap.get(CFKey.DA_VOLUME_NAME));
                                        if (result == null) {
                                            name = type;
                                        } else {
                                            volumePtr.setPointer(result);
                                            name = volumePtr.stringValue();
                                        }
                                        diskInfo.release();
                                    }
                                    disk.release();
                                }
                                String mountPoint;
                                if (logicalVolumeMap.containsKey(partBsdName)) {
                                    mountPoint = "Logical Volume: " + logicalVolumeMap.get(partBsdName);
                                } else {
                                    mountPoint = mountPointMap.getOrDefault(partBsdName, "");
                                }
                                Long size = sdService.getLongProperty("Size");
                                Integer bsdMajor = sdService.getIntegerProperty("BSD Major");
                                Integer bsdMinor = sdService.getIntegerProperty("BSD Minor");
                                partitions.add(new HWPartition(partBsdName, name, type,
                                        sdService.getStringProperty("UUID"), size == null ? 0L : size,
                                        bsdMajor == null ? 0 : bsdMajor, bsdMinor == null ? 0 : bsdMinor, mountPoint));
                                // iterate
                                sdService.release();
                                sdService = IOKit.INSTANCE.IOIteratorNext(serviceIterator);
                            }
                            serviceIterator.release();
                        }
                        Collections.sort(partitions);
                        diskStore.setPartitions(partitions.toArray(new HWPartition[0]));
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
     * <p>
     * updateDiskStats.
     * </p>
     *
     * @param diskStore
     *            a {@link oshi.hardware.HWDiskStore} object.
     * @return a boolean.
     */
    public static boolean updateDiskStats(HWDiskStore diskStore) {
        DASessionRef session = DA.DASessionCreate(CF.CFAllocatorGetDefault());
        if (session == null) {
            LOG.error("Unable to open session to DiskArbitration framework.");
            return false;
        }
        Map<CFKey, CFStringRef> cfKeyMap = mapCFKeys();

        boolean diskFound = updateDiskStats(diskStore, session, queryMountPointMap(), queryLogicalVolumeMap(),
                cfKeyMap);

        session.release();
        for (CFTypeRef value : cfKeyMap.values()) {
            value.release();
        }

        return diskFound;
    }

    /** {@inheritDoc} */
    @Override
    public HWDiskStore[] getDisks() {
        Map<String, String> mountPointMap = queryMountPointMap();
        Map<String, String> logicalVolumeMap = queryLogicalVolumeMap();
        Map<CFKey, CFStringRef> cfKeyMap = mapCFKeys();

        List<HWDiskStore> diskList = new ArrayList<>();

        // Open a DiskArbitration session
        DASessionRef session = DA.DASessionCreate(CF.CFAllocatorGetDefault());
        if (session == null) {
            LOG.error("Unable to open session to DiskArbitration framework.");
            return new HWDiskStore[0];
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
                    CFStringRef modelPtr = new CFStringRef(result);
                    model = modelPtr.stringValue();
                    if (model == null) {
                        model = Constants.UNKNOWN;
                    }
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
                HWDiskStore diskStore = new HWDiskStore();
                diskStore.setName(bsdName);
                diskStore.setModel(model.trim());
                diskStore.setSerial(serial.trim());
                diskStore.setSize(size);

                updateDiskStats(diskStore, session, mountPointMap, logicalVolumeMap, cfKeyMap);
                diskList.add(diskStore);
            }
        }
        // Close DA session
        session.release();
        for (CFTypeRef value : cfKeyMap.values()) {
            value.release();
        }
        Collections.sort(diskList);
        return diskList.toArray(new HWDiskStore[0]);
    }
}
