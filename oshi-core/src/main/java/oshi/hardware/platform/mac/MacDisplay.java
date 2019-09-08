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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFDataRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IOObject;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.ptr.PointerByReference;

import oshi.hardware.Display;
import oshi.hardware.common.AbstractDisplay;
import oshi.jna.platform.mac.IOKit;
import oshi.util.platform.mac.IOKitUtil;

/**
 * A Display
 */
public class MacDisplay extends AbstractDisplay {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(MacDisplay.class);

    private static final CFStringRef cfEdid = CFStringRef.createCFString("IODisplayEDID");

    /**
     * <p>
     * Constructor for MacDisplay.
     * </p>
     *
     * @param edid
     *            an array of {@link byte} objects.
     */
    public MacDisplay(byte[] edid) {
        super(edid);
        LOG.debug("Initialized MacDisplay");
    }

    /**
     * Gets Display Information
     *
     * @return An array of Display objects representing monitors, etc.
     */
    public static Display[] getDisplays() {
        List<Display> displays = new ArrayList<>();
        // Iterate IO Registry IODisplayConnect
        PointerByReference serviceIteratorPtr = new PointerByReference();
        IOKitUtil.getMatchingServices("IODisplayConnect", serviceIteratorPtr);
        IOIterator serviceIterator = new IOIterator(serviceIteratorPtr.getValue());
        IOObject sdServiceObj = IOKit.INSTANCE.IOIteratorNext(serviceIterator);
        while (sdServiceObj != null) {
            IORegistryEntry sdService = new IORegistryEntry(sdServiceObj.getPointer());
            // Display properties are in a child entry
            PointerByReference propertiesPtr = new PointerByReference();
            int ret = IOKit.INSTANCE.IORegistryEntryGetChildEntry(sdService, "IOService", propertiesPtr);
            if (ret == 0) {
                IORegistryEntry properties = new IORegistryEntry(propertiesPtr.getValue());
                // look up the edid by key
                CFTypeRef edidRaw = IOKit.INSTANCE.IORegistryEntryCreateCFProperty(properties, cfEdid,
                        CoreFoundation.INSTANCE.CFAllocatorGetDefault(), 0);
                CFDataRef edid = new CFDataRef(edidRaw.getPointer());
                if (edid != null) {
                    // Edid is a byte array of 128 bytes
                    int length = (int) CoreFoundation.INSTANCE.CFDataGetLength(edid);
                    Pointer p = CoreFoundation.INSTANCE.CFDataGetBytePtr(edid);
                    displays.add(new MacDisplay(p.getByteArray(0, length)));
                    edid.release();
                }
            }
            // iterate
            IOKit.INSTANCE.IOObjectRelease(sdService);
            sdServiceObj = IOKit.INSTANCE.IOIteratorNext(serviceIterator);
        }
        IOKit.INSTANCE.IOObjectRelease(serviceIterator);
        return displays.toArray(new Display[0]);
    }
}
