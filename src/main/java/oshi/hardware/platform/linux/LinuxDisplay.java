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
package oshi.hardware.platform.linux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.Display;
import oshi.json.NullAwareJsonObjectBuilder;
import oshi.util.EdidUtil;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * A Display
 * 
 * @author widdis[at]gmail[dot]com
 */
public class LinuxDisplay implements Display {
    private static final Logger LOG = LoggerFactory.getLogger(LinuxDisplay.class);

    private byte[] edid;

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    public LinuxDisplay(byte[] edid) {
        this.edid = edid;
        LOG.debug("Initialized LinuxDisplay");
    }

    @Override
    public byte[] getEdid() {
        return Arrays.copyOf(edid, edid.length);
    }

    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder())
                .add("edid", EdidUtil.toString(getEdid())).build();
    }

    /**
     * Gets Display Information
     * 
     * @return An array of Display objects representing monitors, etc.
     */
    public static Display[] getDisplays() {
        List<Display> displays = new ArrayList<Display>();
        ArrayList<String> xrandr = ExecutingCommand.runNative("xrandr --verbose");
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
                    Display display = new LinuxDisplay(ParseUtil.hexStringToByteArray(edidStr));
                    displays.add(display);
                    foundEdid = false;
                }
            }
        }

        return displays.toArray(new Display[displays.size()]);
    }
}