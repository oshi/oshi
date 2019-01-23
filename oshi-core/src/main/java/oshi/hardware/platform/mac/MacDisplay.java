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

import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.hardware.Display;
import oshi.hardware.common.AbstractDisplay;
import oshi.jna.platform.mac.CoreFoundation;
import oshi.jna.platform.mac.CoreFoundation.CFStringRef;
import oshi.jna.platform.mac.CoreFoundation.CFTypeRef;
import oshi.jna.platform.mac.IOKit;
import oshi.util.platform.mac.CfUtil;
import oshi.util.platform.mac.IOKitUtil;

/**
 * A Display
 *
 * @author widdis[at]gmail[dot]com
 */
public class MacDisplay extends AbstractDisplay {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(MacDisplay.class);

    private static final CFStringRef cfEdid = CFStringRef.toCFString("IODisplayEDID");

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
        IntByReference serviceIterator = new IntByReference();
        IOKitUtil.getMatchingServices("IODisplayConnect", serviceIterator);
        int sdService = IOKit.INSTANCE.IOIteratorNext(serviceIterator.getValue());
        while (sdService != 0) {
            // Display properties are in a child entry
            IntByReference properties = new IntByReference();
            int ret = IOKit.INSTANCE.IORegistryEntryGetChildEntry(sdService, "IOService", properties);
            if (ret == 0) {
                // look up the edid by key
                CFTypeRef edid = IOKit.INSTANCE.IORegistryEntryCreateCFProperty(properties.getValue(), cfEdid,
                        CfUtil.ALLOCATOR, 0);
                if (edid != null) {
                    // Edid is a byte array of 128 bytes
                    int length = CoreFoundation.INSTANCE.CFDataGetLength(edid);
                    PointerByReference p = CoreFoundation.INSTANCE.CFDataGetBytePtr(edid);
                    displays.add(new MacDisplay(p.getPointer().getByteArray(0, length)));
                    CfUtil.release(edid);
                }
            }
            // iterate
            IOKit.INSTANCE.IOObjectRelease(sdService);
            sdService = IOKit.INSTANCE.IOIteratorNext(serviceIterator.getValue());
        }
        IOKit.INSTANCE.IOObjectRelease(serviceIterator.getValue());
        return displays.toArray(new Display[0]);
    }
}
