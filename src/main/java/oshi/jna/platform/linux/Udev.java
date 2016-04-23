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
 * enrico[dot]bianchi[at]gmail[dot]com
 *    com.github.dblock - initial API and implementation and/or initial documentation
 */
package oshi.jna.platform.linux;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

/**
 * Interface for talking with Udev.
 *
 * @author enrico[dot]bianchi[at]gmail[dot]com
 */
public interface Udev extends Library {

    public static final NativeLibrary JNA_NATIVE_LIB = NativeLibrary.getInstance("udev");
    public static final Udev INSTANCE = (Udev) Native.loadLibrary("udev", Udev.class);

    public static final class BlockDevStats extends Structure {

        public BlockDevStats(long read_ops,
                long read_merged,
                long read_512bytes,
                long read_waits_ms,
                long write_ops,
                long write_merged,
                long write_512bytes,
                long write_waits_ms,
                long in_flight,
                long active_ms,
                long waits_ms) {
            this.read_ops = read_ops;
            this.read_merged = read_merged;
            this.read_512bytes = read_512bytes;
            this.read_waits_ms = read_waits_ms;
            this.write_ops = write_ops;
            this.write_merged = write_merged;
            this.write_512bytes = write_512bytes;
            this.write_waits_ms = write_waits_ms;
            this.in_flight = in_flight;
            this.active_ms = active_ms;
            this.waits_ms = waits_ms;
        }

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
        protected List getFieldOrder() {
            return Arrays.asList(new String[]{"read_ops", "read_merged", "read_512bytes",
                "read_waits_ms", "write_ops", "write_merged", "write_512bytes", "write_waits_ms",
                "in_flight", "active_ms", "waits_ms"});
        }
    };

    public static class UdevHandle extends PointerType {

        public UdevHandle(Pointer address) {
            super(address);
        }

        public UdevHandle() {
            super();
        }
    };

    public static final class UdevDevice extends PointerType {

        public UdevDevice(Pointer address) {
            super(address);
        }

        public UdevDevice() {
            super();
        }
    };

    public static class UdevEnumerate extends PointerType {

        public UdevEnumerate(Pointer address) {
            super(address);
        }

        public UdevEnumerate() {
            super();
        }
    };

    public static class UdevListEntry extends PointerType {

        public UdevListEntry(Pointer address) {
            super(address);
        }

        public UdevListEntry() {
            super();
        }
    };

    public Udev.UdevHandle udev_new();

    void udev_unref(Udev.UdevHandle udev);

    void udev_device_unref(Udev.UdevDevice udev_device);

    void udev_enumerate_unref(Udev.UdevEnumerate udev_enumerate);

    public Udev.UdevEnumerate udev_enumerate_new(Udev.UdevHandle udev);

    public Udev.UdevDevice udev_device_get_parent_with_subsystem_devtype(Udev.UdevDevice udev_device, String subsystem, String devtype);

    public Udev.UdevDevice udev_device_new_from_syspath(Udev.UdevHandle udev, String syspath);

    public Udev.UdevListEntry udev_list_entry_get_next(Udev.UdevListEntry list_entry);

    public String udev_device_get_sysattr_value(final Udev.UdevDevice udev_device, final String sysattr);

    public int udev_enumerate_add_match_subsystem(Udev.UdevEnumerate udev_enumerate, String subsystem);

    public int udev_enumerate_scan_devices(Udev.UdevEnumerate udev_enumerate);

    public Udev.UdevListEntry udev_enumerate_get_list_entry(Udev.UdevEnumerate udev_enumerate);

    public String udev_list_entry_get_name(Udev.UdevListEntry list_entry);

    public String udev_device_get_devtype(Udev.UdevDevice udev_device);

    public String udev_device_get_devnode(Udev.UdevDevice udev_device);

    public String udev_device_get_property_value(Udev.UdevDevice udev_device, String key);
}
