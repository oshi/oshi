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
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.Display;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * A Display
 * 
 * @author widdis[at]gmail[dot]com
 */
public class MacDisplay implements Display {
    private static final Logger LOG = LoggerFactory.getLogger(MacDisplay.class);

    private byte[] edid;

    public MacDisplay(byte[] edid) {
        this.edid = edid;
        LOG.debug("Initialized MacDisplay");
    }

    @Override
    public byte[] getEdid() {
        return Arrays.copyOf(edid, edid.length);
    }

    /**
     * Gets Display Information
     * 
     * @return An array of Display objects representing monitors, etc.
     */
    public static Display[] getDisplays() {
        List<Display> displays = new ArrayList<Display>();
        ArrayList<String> ioReg = ExecutingCommand.runNative("ioreg -lw0 -r -c IODisplayConnect");
        for (String s : ioReg) {
            if (s.contains("IODisplayEDID")) {
                String edidStr = s.substring(s.indexOf("<") + 1, s.indexOf(">"));
                LOG.debug("Parsed EDID: {}", edidStr);
                Display display = new MacDisplay(ParseUtil.hexStringToByteArray(edidStr));
                displays.add(display);
            }
        }

        return displays.toArray(new Display[displays.size()]);
    }
}