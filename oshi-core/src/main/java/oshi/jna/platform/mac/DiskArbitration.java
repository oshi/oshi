/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.jna.platform.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.PointerType;

import oshi.jna.platform.mac.CoreFoundation.CFAllocatorRef;
import oshi.jna.platform.mac.CoreFoundation.CFDictionaryRef;

/**
 * DiskArbitration framework for disk stats. This class should be considered
 * non-API as it may be removed if/when its code is incorporated into the JNA
 * project.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface DiskArbitration extends Library {
    DiskArbitration INSTANCE = Native.loadLibrary("DiskArbitration", DiskArbitration.class);

    class DASessionRef extends PointerType {
    }

    class DADiskRef extends PointerType {
    }

    /**
     * Creates a new session. The caller of this function receives a reference
     * to the returned object. The caller also implicitly retains the object and
     * is responsible for releasing it with CFRelease().
     *
     * @param allocator
     *            The allocator object to be used to allocate memory.
     * @return A reference to a new DASession.
     */
    DASessionRef DASessionCreate(CFAllocatorRef allocator);

    /**
     * Creates a new disk object. The caller of this function receives a
     * reference to the returned object. The caller also implicitly retains the
     * object and is responsible for releasing it with CFRelease().
     *
     * @param allocator
     *            The allocator object to be used to allocate memory.
     * @param session
     *            The DASession in which to contact Disk Arbitration.
     * @param diskName
     *            he BSD device name.
     * @return A reference to a new DADisk.
     */
    DADiskRef DADiskCreateFromBSDName(CFAllocatorRef allocator, DASessionRef session, String diskName);

    /**
     * Creates a new disk object. The caller of this function receives a
     * reference to the returned object. The caller also implicitly retains the
     * object and is responsible for releasing it with CFRelease().
     *
     * @param allocator
     *            The allocator object to be used to allocate memory.
     * @param session
     *            The DASession in which to contact Disk Arbitration.
     * @param media
     *            The I/O Kit media object.
     * @return A reference to a new DADisk.
     */
    DADiskRef DADiskCreateFromIOMedia(CFAllocatorRef allocator, DASessionRef session, int media);

    /**
     * Obtains the Disk Arbitration description of the specified disk. This
     * function will contact Disk Arbitration to acquire the latest description
     * of the specified disk, unless this function is called on a disk object
     * passed within the context of a registered callback, in which case the
     * description is current as of that callback event.
     *
     * The caller of this function receives a reference to the returned object.
     * The caller also implicitly retains the object and is responsible for
     * releasing it with CFRelease().
     *
     * @param disk
     *            The DADisk for which to obtain the Disk Arbitration
     *            description.
     * @return The disk's Disk Arbitration description.
     */
    CFDictionaryRef DADiskCopyDescription(DADiskRef disk);

    /**
     * Obtains the BSD device name for the specified disk.
     *
     * @param disk
     *            The DADisk for which to obtain the BSD device name.
     *
     * @return The disk's BSD device name.
     */
    String DADiskGetBSDName(DADiskRef disk);
}
