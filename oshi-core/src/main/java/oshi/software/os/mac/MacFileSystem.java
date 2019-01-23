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
package oshi.software.os.mac;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.platform.mac.SystemB.Statfs;
import com.sun.jna.ptr.IntByReference;

import oshi.jna.platform.mac.CoreFoundation;
import oshi.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import oshi.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import oshi.jna.platform.mac.DiskArbitration;
import oshi.jna.platform.mac.DiskArbitration.DADiskRef;
import oshi.jna.platform.mac.DiskArbitration.DASessionRef;
import oshi.jna.platform.mac.IOKit;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.platform.mac.CfUtil;
import oshi.util.platform.mac.IOKitUtil;
import oshi.util.platform.mac.SysctlUtil;

/**
 * The Mac File System contains {@link OSFileStore}s which are a storage pool,
 * device, partition, volume, concrete file system or other implementation
 * specific means of file storage. In Mac OS X, these are found in the /Volumes
 * directory.
 *
 * @author widdis[at]gmail[dot]com
 */
public class MacFileSystem implements FileSystem {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(MacFileSystem.class);

    // Regexp matcher for /dev/disk1 etc.
    private static final Pattern LOCAL_DISK = Pattern.compile("/dev/disk\\d");

    /**
     * Gets File System Information.
     *
     * @return An array of {@link OSFileStore} objects representing mounted
     *         volumes. May return disconnected volumes with
     *         {@link OSFileStore#getTotalSpace()} = 0.
     */
    @Override
    public OSFileStore[] getFileStores() {
        // Open a DiskArbitration session to get VolumeName of file systems with
        // bsd names
        DASessionRef session = DiskArbitration.INSTANCE.DASessionCreate(CfUtil.ALLOCATOR);
        if (session == null) {
            LOG.error("Unable to open session to DiskArbitration framework.");
        }

        // List of file systems
        List<OSFileStore> fsList = new ArrayList<>();

        // Use getfsstat to find fileSystems
        // Query with null to get total # required
        int numfs = SystemB.INSTANCE.getfsstat64(null, 0, 0);
        if (numfs > 0) {

            // Create array to hold results
            Statfs[] fs = new Statfs[numfs];
            // Fill array with results
            numfs = SystemB.INSTANCE.getfsstat64(fs, numfs * new Statfs().size(), SystemB.MNT_NOWAIT);
            for (int f = 0; f < numfs; f++) {
                // Mount on name will match mounted path, e.g. /Volumes/foo
                // Mount to name will match canonical path., e.g., /dev/disk0s2
                // Byte arrays are null-terminated strings

                // Get volume name
                String volume = new String(fs[f].f_mntfromname).trim();
                // Skip system types
                if (volume.equals("devfs") || volume.startsWith("map ")) {
                    continue;
                }
                // Set description
                String description = "Volume";
                if (LOCAL_DISK.matcher(volume).matches()) {
                    description = "Local Disk";
                }
                if (volume.startsWith("localhost:") || volume.startsWith("//")) {
                    description = "Network Drive";
                }
                // Set type and path
                String type = new String(fs[f].f_fstypename).trim();
                String path = new String(fs[f].f_mntonname).trim();

                // Set name and uuid
                String name = "";
                String uuid = "";
                // Use volume to find DiskArbitration volume name and search for
                // the registry entry for UUID
                String bsdName = volume.replace("/dev/disk", "disk");
                if (bsdName.startsWith("disk")) {
                    // Get the DiskArbitration dictionary for this disk,
                    // which has volumename
                    DADiskRef disk = DiskArbitration.INSTANCE.DADiskCreateFromBSDName(CfUtil.ALLOCATOR, session,
                            volume);
                    if (disk != null) {
                        CFDictionaryRef diskInfo = DiskArbitration.INSTANCE.DADiskCopyDescription(disk);
                        if (diskInfo != null) {
                            // get volume name from its key
                            Pointer volumePtr = CoreFoundation.INSTANCE.CFDictionaryGetValue(diskInfo,
                                    CfUtil.getCFString("DAVolumeName"));
                            name = CfUtil.cfPointerToString(volumePtr);
                            CfUtil.release(diskInfo);
                        }
                        CfUtil.release(disk);
                    }
                    // Search for bsd name in IOKit registry for UUID
                    CFMutableDictionaryRef matchingDict = IOKitUtil.getBSDNameMatchingDict(bsdName);
                    if (matchingDict != null) {
                        // search for all IOservices that match the bsd name
                        IntByReference fsIter = new IntByReference();
                        IOKitUtil.getMatchingServices(matchingDict, fsIter);
                        // getMatchingServices releases matchingDict
                        // Should only match one logical drive
                        int fsEntry = IOKit.INSTANCE.IOIteratorNext(fsIter.getValue());
                        if (fsEntry != 0 && IOKit.INSTANCE.IOObjectConformsTo(fsEntry, "IOMedia")) {
                            // Now get the UUID
                            uuid = IOKitUtil.getIORegistryStringProperty(fsEntry, "UUID");
                            if (uuid == null) {
                                uuid = "";
                            } else {
                                uuid = uuid.toLowerCase();
                            }
                            IOKit.INSTANCE.IOObjectRelease(fsEntry);
                        }
                        IOKit.INSTANCE.IOObjectRelease(fsIter.getValue());
                    }
                }
                File file = new File(path);
                if (name.isEmpty()) {
                    name = file.getName();
                    // getName() for / is still blank, so:
                    if (name.isEmpty()) {
                        name = file.getPath();
                    }
                }

                // Add to the list
                OSFileStore osStore = new OSFileStore();
                osStore.setName(name);
                osStore.setVolume(volume);
                osStore.setMount(path);
                osStore.setDescription(description);
                osStore.setType(type);
                osStore.setUUID(uuid);
                osStore.setUsableSpace(file.getUsableSpace());
                osStore.setTotalSpace(file.getTotalSpace());
                osStore.setFreeInodes(fs[f].f_ffree);
                osStore.setTotalInodes(fs[f].f_files);
                fsList.add(osStore);
            }
        }
        // Close DA session
        CfUtil.release(session);
        return fsList.toArray(new OSFileStore[0]);
    }

    @Override
    public long getOpenFileDescriptors() {
        return SysctlUtil.sysctl("kern.num_files", 0);
    }

    @Override
    public long getMaxFileDescriptors() {
        return SysctlUtil.sysctl("kern.maxfiles", 0);
    }
}
