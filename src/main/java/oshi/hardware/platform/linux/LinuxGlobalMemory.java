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
 * enrico[dot]bianchi[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.linux;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import java.util.HashMap;

import oshi.hardware.GlobalMemory;
import oshi.jna.platform.linux.Libc;
import oshi.jna.platform.linux.Libc.Sysinfo;
import oshi.json.NullAwareJsonObjectBuilder;

/**
 * Memory obtained using JNA sysinfo's call
 *
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */
public class LinuxGlobalMemory implements GlobalMemory {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxGlobalMemory.class);
    private final HashMap<String, Long> memInfo;
    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    public LinuxGlobalMemory() {
        this.memInfo = new HashMap<>();
        this.computeMeminfo();
    }

    private void computeMeminfo() {
        Sysinfo si;

        si = new Sysinfo();

        if (0 != Libc.INSTANCE.sysinfo(si)) {
            LOG.error("Failed to get system memory information. Error code: " + Native.getLastError());
            return;
        }

        this.memInfo.put("totalram", si.totalram.longValue());
        this.memInfo.put("freeram", si.freeram.longValue());
    }

    @Override
    public long getAvailable() {
        return this.memInfo.get("freeram");
    }

    @Override
    public long getTotal() {
        return this.memInfo.get("totalram");
    }

    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("available", getAvailable())
                .add("total", getTotal()).build();
    }
}
