/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
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
package oshi.hardware.platform.linux;

import java.io.Serializable;

import oshi.jna.platform.linux.Udev;
import oshi.util.ParseUtil;

/**
 * POJO for mapping Linux block device stats info
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public class LinuxBlockDevStats implements Serializable {

    private static final long serialVersionUID = 1L;

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

    public LinuxBlockDevStats(String device, Udev.UdevDevice disk) {
        String devstat;
        String[] splitstats;

        devstat = Udev.INSTANCE.udev_device_get_sysattr_value(disk, "stat");
        splitstats = ParseUtil.whitespaces.split(devstat);

        if (splitstats.length < 11) {
            throw new IllegalStateException("Unexpected length of array: " + splitstats.length);
        }

        // read last 11 elements
        int startIndex = splitstats.length - 11;

        this.device = device;
        this.read_ops = ParseUtil.parseLongOrDefault(splitstats[startIndex + 0], 0L);
        this.read_merged = ParseUtil.parseLongOrDefault(splitstats[startIndex + 1], 0L);
        this.read_512bytes = ParseUtil.parseLongOrDefault(splitstats[startIndex + 2], 0L);
        this.read_waits_ms = ParseUtil.parseLongOrDefault(splitstats[startIndex + 3], 0L);
        this.write_ops = ParseUtil.parseLongOrDefault(splitstats[startIndex + 4], 0L);
        this.write_merged = ParseUtil.parseLongOrDefault(splitstats[startIndex + 5], 0L);
        this.write_512bytes = ParseUtil.parseLongOrDefault(splitstats[startIndex + 6], 0L);
        this.write_waits_ms = ParseUtil.parseLongOrDefault(splitstats[startIndex + 7], 0L);
        this.in_flight = ParseUtil.parseLongOrDefault(splitstats[startIndex + 8], 0L);
        this.active_ms = ParseUtil.parseLongOrDefault(splitstats[startIndex + 9], 0L);
        this.waits_ms = ParseUtil.parseLongOrDefault(splitstats[startIndex + 10], 0L);
    }

}