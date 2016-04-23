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
package oshi.hardware.platform.linux;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import oshi.jna.platform.linux.Udev;
import oshi.json.NullAwareJsonObjectBuilder;
import oshi.json.OshiJsonObject;

/**
 * POJO for mapping Linux block device stats info
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class LinuxBlockDevStats implements OshiJsonObject {

    private JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    public LinuxBlockDevStats(String device, Udev.UdevDevice disk) {
        String devstat;
        String[] splitstats;

        devstat = Udev.INSTANCE.udev_device_get_sysattr_value(disk, "stat");
        splitstats = devstat.split("\\s+");

        this.device = device;
        this.read_ops = Long.parseLong(splitstats[1]);
        this.read_merged = Long.parseLong(splitstats[2]);
        this.read_512bytes = Long.parseLong(splitstats[3]);
        this.read_waits_ms = Long.parseLong(splitstats[4]);
        this.write_ops = Long.parseLong(splitstats[5]);
        this.write_merged = Long.parseLong(splitstats[6]);
        this.write_512bytes = Long.parseLong(splitstats[7]);
        this.write_waits_ms = Long.parseLong(splitstats[8]);
        this.in_flight = Long.parseLong(splitstats[9]);
        this.active_ms = Long.parseLong(splitstats[10]);
        this.waits_ms = Long.parseLong(splitstats[11]);
    }

    // Device name
    public final String device;
    // Number of read I/Os processed
    public final long read_ops;
    // Number of read I/Os merged with in-queue I/O
    public final long read_merged;
    // Number of sectors read
    public final long read_512bytes;
    // Total wait time for read requests, milliseconds
    public final long read_waits_ms;
    // Number of write I/Os processed
    public final long write_ops;
    // Number of write I/Os merged with in-queue I/O
    public final long write_merged;
    // Number of sectors written
    public final long write_512bytes;
    // Total wait time for write requests, milliseconds */
    public final long write_waits_ms;
    // Number of I/Os currently in flight
    public final long in_flight;
    // Total active time, milliseconds
    public final long active_ms;
    // Total wait time, milliseconds
    public final long waits_ms;

    @Override
    public JsonObject toJSON() {
        return NullAwareJsonObjectBuilder.wrap(jsonFactory.createObjectBuilder()).add("device", this.device)
                .add("read_ops", this.read_ops).add("read_merged", this.read_merged)
                .add("read_512bytes", this.read_512bytes).add("read_waits_ms", this.read_waits_ms)
                .add("write_ops", this.write_ops).add("write_merged", this.write_merged)
                .add("write_512bytes", this.write_512bytes).add("write_waits_ms", this.write_waits_ms)
                .add("in_flight", this.in_flight).add("active_ms", this.active_ms).add("waits_ms", this.waits_ms)
                .build();
    }
}