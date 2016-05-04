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

import oshi.hardware.HWDiskStore;
import oshi.hardware.common.AbstractDisks;
import oshi.jna.platform.mac.CoreFoundation;
import oshi.jna.platform.mac.CoreFoundation.CFAllocatorRef;
import oshi.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import oshi.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import oshi.jna.platform.mac.CoreFoundation.CFStringRef;
import oshi.jna.platform.mac.CoreFoundation.CFTypeRef;
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
    private static final Logger LOG = LoggerFactory.getLogger(MacDisks.class);

    private static final CFAllocatorRef allocatorDefault = CoreFoundation.INSTANCE.CFAllocatorGetDefault();

    private static final CFStringRef cfModel = CFStringRef.toCFString("Model");
    private static final CFStringRef cfIOPropertyMatch = CFStringRef.toCFString("IOPropertyMatch");
    private static final CFStringRef cfDADeviceModel = CFStringRef.toCFString("DADeviceModel");
    private static final CFStringRef cfDAMediaSize = CFStringRef.toCFString("DAMediaSize");
    private static final CFStringRef cfSerialNumber = CFStringRef.toCFString("Serial Number");

    @Override
    public HWDiskStore[] getDisks() {
        List<HWDiskStore> result = new ArrayList<>();

        // Use statfs to find all drives
        int numfs = SystemB.INSTANCE.getfsstat64(null, 0, 0);
        // Create array to hold results
        Statfs[] fs = new Statfs[numfs];
        // Fill array with results
        numfs = SystemB.INSTANCE.getfsstat64(fs, numfs * new Statfs().size(), SystemB.MNT_NOWAIT);

        // Open a DiskArbitration session to get model and size of disks
        DASessionRef session = DiskArbitration.INSTANCE.DASessionCreate(allocatorDefault);
        if (session == null) {
            LOG.error("Unable to open session to DiskArbitration framework.");
            return new HWDiskStore[0];
        }

        // Iterate all mounted file systems
        for (Statfs f : fs) {
            String model = "";
            String serial = "";
            long size = 0L;
            // Get a reference to the disk - only matching /dev/disk*s2
            String[] split = new String(f.f_mntfromname).trim().split("/dev/|s2");
            if (split.length < 2) {
                continue;
            }
            String name = split[1];
            String path = "/dev/" + name;
            DADiskRef disk = DiskArbitration.INSTANCE.DADiskCreateFromBSDName(allocatorDefault, session, path);

            if (disk != null) {
                // Get the DiskArbitration dictionary for this disk, which
                // has model and size (capacity)
                CFDictionaryRef diskInfo = DiskArbitration.INSTANCE.DADiskCopyDescription(disk);
                if (diskInfo != null) {
                    // Parse out model and size from their respective keys
                    Pointer modelPtr = CoreFoundation.INSTANCE.CFDictionaryGetValue(diskInfo, cfDADeviceModel);
                    model = CfUtil.cfPointerToString(modelPtr);
                    Pointer sizePtr = CoreFoundation.INSTANCE.CFDictionaryGetValue(diskInfo, cfDAMediaSize);
                    size = CfUtil.cfPointerToLong(sizePtr);
                    CfUtil.release(diskInfo);

                    // Use the model as a key to get serial from IOKit
                    if (!model.equals("Disk Image")) {
                        CFStringRef modelNameRef = CFStringRef.toCFString(model);
                        CFMutableDictionaryRef propertyDict = CoreFoundation.INSTANCE
                                .CFDictionaryCreateMutable(allocatorDefault, 0, null, null);
                        CoreFoundation.INSTANCE.CFDictionarySetValue(propertyDict, cfModel, modelNameRef);
                        CFMutableDictionaryRef matchingDict = CoreFoundation.INSTANCE
                                .CFDictionaryCreateMutable(allocatorDefault, 0, null, null);
                        CoreFoundation.INSTANCE.CFDictionarySetValue(matchingDict, cfIOPropertyMatch, propertyDict);

                        // search for all IOservices that match the model
                        IntByReference serviceIterator = new IntByReference();
                        IOKitUtil.getMatchingServices(matchingDict, serviceIterator);
                        // getMatchingServices releases matchingDict
                        CfUtil.release(modelNameRef);
                        CfUtil.release(propertyDict);
                        int sdService = IOKit.INSTANCE.IOIteratorNext(serviceIterator.getValue());
                        while (sdService != 0) {
                            // look up the serial number
                            CFTypeRef serNo = IOKit.INSTANCE.IORegistryEntryCreateCFProperty(sdService, cfSerialNumber,
                                    allocatorDefault, 0);
                            if (serNo != null) {
                                serial = CfUtil.cfPointerToString(serNo.getPointer());
                                CfUtil.release(serNo);
                                break;
                            }
                            // iterate
                            sdService = IOKit.INSTANCE.IOIteratorNext(sdService);
                        }
                    }
                }
                CfUtil.release(disk);
                if (size > 0L) {
                    result.add(new HWDiskStore(name, model.trim(), serial.trim(), size));
                }
            }
        }
        CfUtil.release(session);
        return result.toArray(new HWDiskStore[result.size()]);
    }

}
