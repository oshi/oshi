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
package oshi.hardware.platform.linux;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.Display;
import oshi.hardware.common.AbstractDisplay;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * A Display
 * 
 * @author widdis[at]gmail[dot]com
 */
public class LinuxDisplay extends AbstractDisplay {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(LinuxDisplay.class);

    public LinuxDisplay(byte[] edid) {
        super(edid);
        LOG.debug("Initialized LinuxDisplay");
    }

    /**
     * Gets Display Information
     * 
     * @return An array of Display objects representing monitors, etc.
     */
    public static Display[] getDisplays() {
        List<Display> displays = new ArrayList<Display>();
        ArrayList<String> xrandr = ExecutingCommand.runNative("xrandr --verbose");
        // xrandr reports edid in multiple lines. After seeing a line containing
        // EDID, read subsequent lines of hex until 256 characters are reached
        if (xrandr != null) {
            boolean foundEdid = false;
            StringBuilder sb = new StringBuilder();
            for (String s : xrandr) {
                if (s.contains("EDID")) {
                    foundEdid = true;
                    sb = new StringBuilder();
                    continue;
                }
                if (foundEdid) {
                    sb.append(s.trim());
                    if (sb.length() >= 256) {
                        String edidStr = sb.toString();
                        LOG.debug("Parsed EDID: {}", edidStr);
                        byte[] edid = ParseUtil.hexStringToByteArray(edidStr);
                        if (edid != null) {
                            displays.add(new LinuxDisplay(edid));
                        }
                        foundEdid = false;
                    }
                }
            }
        }

        return displays.toArray(new Display[displays.size()]);
    }
}