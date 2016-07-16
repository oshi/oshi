/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.hardware.HWDiskStore;
import oshi.hardware.common.AbstractDisks;
import oshi.jna.platform.mac.CoreFoundation;
import oshi.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import oshi.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import oshi.jna.platform.mac.CoreFoundation.CFStringRef;
import oshi.jna.platform.mac.DiskArbitration;
import oshi.jna.platform.mac.DiskArbitration.DADiskRef;
import oshi.jna.platform.mac.DiskArbitration.DASessionRef;
import oshi.jna.platform.mac.IOKit;
import oshi.jna.platform.mac.SystemB;
import oshi.jna.platform.mac.SystemB.Statfs;
import oshi.util.platform.mac.CfUtil;
import oshi.util.platform.mac.IOKitUtil;

/**
 * Mac hard disk implementation.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class MacDisks extends AbstractDisks {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(MacDisks.class);

    @Override
    public HWDiskStore[] getDisks() {
        List<HWDiskStore> result = new ArrayList<>();

        // Use statfs to find all drives
        int numfs = SystemB.INSTANCE.getfsstat64(null, 0, 0);
        // Create array to hold results
        Statfs[] fs = new Statfs[numfs];
        // Fill array with results
        SystemB.INSTANCE.getfsstat64(fs, numfs * new Statfs().size(), SystemB.MNT_NOWAIT);

        // Open a DiskArbitration session to get model and size of disks
        DASessionRef session = DiskArbitration.INSTANCE.DASessionCreate(CfUtil.ALLOCATOR);
        if (session == null) {
            LOG.error("Unable to open session to DiskArbitration framework.");
            return new HWDiskStore[0];
        }

        // Iterate all mounted file systems
        for (Statfs f : fs) {
            String model = "";
            String serial = "";
            long size = 0L;
            long xferTime = 0L;
            // Get a reference to the disk - only matching /dev/disk*s2
            String[] split = new String(f.f_mntfromname).trim().split("/dev/|s2");
            if (split.length < 2) {
                continue;
            }
            // OS X registry uses the BSD Name, e.g. disk0, disk1, etc.
            String bsdName = split[1];
            String path = "/dev/" + bsdName;

            // Get the DiskArbitration dictionary for this disk, which has model
            // and size (capacity)
            DADiskRef disk = DiskArbitration.INSTANCE.DADiskCreateFromBSDName(CfUtil.ALLOCATOR, session, path);
            if (disk != null) {
                CFDictionaryRef diskInfo = DiskArbitration.INSTANCE.DADiskCopyDescription(disk);
                if (diskInfo != null) {
                    // Parse out model and size from their respective keys
                    Pointer modelPtr = CoreFoundation.INSTANCE.CFDictionaryGetValue(diskInfo,
                            CfUtil.getCFString("DADeviceModel"));
                    model = CfUtil.cfPointerToString(modelPtr);
                    Pointer sizePtr = CoreFoundation.INSTANCE.CFDictionaryGetValue(diskInfo,
                            CfUtil.getCFString("DAMediaSize"));
                    size = CfUtil.cfPointerToLong(sizePtr);
                    CfUtil.release(diskInfo);

                    // Use the model as a key to get serial from IOKit
                    if (!model.equals("Disk Image")) {
                        CFStringRef modelNameRef = CFStringRef.toCFString(model);
                        CFMutableDictionaryRef propertyDict = CoreFoundation.INSTANCE
                                .CFDictionaryCreateMutable(CfUtil.ALLOCATOR, 0, null, null);
                        CoreFoundation.INSTANCE.CFDictionarySetValue(propertyDict, CfUtil.getCFString("Model"),
                                modelNameRef);
                        CFMutableDictionaryRef matchingDict = CoreFoundation.INSTANCE
                                .CFDictionaryCreateMutable(CfUtil.ALLOCATOR, 0, null, null);
                        CoreFoundation.INSTANCE.CFDictionarySetValue(matchingDict,
                                CfUtil.getCFString("IOPropertyMatch"), propertyDict);

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

                //
                if (size <= 0) {
                    continue;
                }
                HWDiskStore diskStore = new HWDiskStore(bsdName, model.trim(), serial.trim(), size, 0L, 0L, 0L, 0L, 0L);

                // Now look up the device using the BSD Name to get its
                // statistics
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
                            if (IOKit.INSTANCE.IOObjectConformsTo(parent.getValue(), "IOBlockStorageDriver")
                                    && IOKit.INSTANCE.IORegistryEntryCreateCFProperties(parent.getValue(), propsPtr,
                                            CfUtil.ALLOCATOR, 0) == 0) {
                                CFMutableDictionaryRef properties = new CFMutableDictionaryRef();
                                properties.setPointer(propsPtr.getValue());
                                // We now have a properties object with the
                                // statistics we need on it. Fetch them
                                Pointer statsPtr = CoreFoundation.INSTANCE.CFDictionaryGetValue(properties,
                                        CfUtil.getCFString("Statistics"));
                                CFDictionaryRef statistics = new CFDictionaryRef();
                                statistics.setPointer(statsPtr);

                                // Now get the stats we want
                                Pointer stat = CoreFoundation.INSTANCE.CFDictionaryGetValue(statistics,
                                        CfUtil.getCFString("Operations (Read)"));
                                diskStore.setReads(CfUtil.cfPointerToLong(stat));
                                stat = CoreFoundation.INSTANCE.CFDictionaryGetValue(statistics,
                                        CfUtil.getCFString("Bytes (Read)"));
                                diskStore.setReadBytes(CfUtil.cfPointerToLong(stat));

                                stat = CoreFoundation.INSTANCE.CFDictionaryGetValue(statistics,
                                        CfUtil.getCFString("Operations (Write)"));
                                diskStore.setWrites(CfUtil.cfPointerToLong(stat));
                                stat = CoreFoundation.INSTANCE.CFDictionaryGetValue(statistics,
                                        CfUtil.getCFString("Bytes (Write)"));
                                diskStore.setWriteBytes(CfUtil.cfPointerToLong(stat));

                                // Total time is in nanoseconds. Add read+write
                                // and convert total to ms
                                stat = CoreFoundation.INSTANCE.CFDictionaryGetValue(statistics,
                                        CfUtil.getCFString("Total Time (Read)"));
                                xferTime = CfUtil.cfPointerToLong(stat);
                                stat = CoreFoundation.INSTANCE.CFDictionaryGetValue(statistics,
                                        CfUtil.getCFString("Total Time (Write)"));
                                xferTime += CfUtil.cfPointerToLong(stat);
                                diskStore.setTransferTime(xferTime / 10000L);

                                CfUtil.release(properties);
                            } else {
                                LOG.error("Unable to find block storage driver properties for {}", bsdName);
                            }
                            IOKit.INSTANCE.IOObjectRelease(parent.getValue());
                        } else {
                            LOG.error("Unable to find IOMedia device or parent for ", bsdName);
                        }
                        IOKit.INSTANCE.IOObjectRelease(drive);
                    }
                    IOKit.INSTANCE.IOObjectRelease(driveList.getValue());
                }
                result.add(diskStore);
            }
        }
        // Close DA session
        CfUtil.release(session);
        return result.toArray(new HWDiskStore[result.size()]);
    }

}
