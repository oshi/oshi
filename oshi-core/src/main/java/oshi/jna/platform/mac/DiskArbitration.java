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
    DiskArbitration INSTANCE = Native.load("DiskArbitration", DiskArbitration.class);

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
