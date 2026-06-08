/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.aix.perfstat;

import static oshi.ffm.unix.aix.PerfstatFunctions.PERFSTAT_ID_T_SIZE;
import static oshi.ffm.unix.aix.PerfstatFunctions.PERFSTAT_NETINTERFACE_T_SIZE;
import static oshi.ffm.unix.aix.PerfstatFunctions.netIfBitrate;
import static oshi.ffm.unix.aix.PerfstatFunctions.netIfCollisions;
import static oshi.ffm.unix.aix.PerfstatFunctions.netIfDescription;
import static oshi.ffm.unix.aix.PerfstatFunctions.netIfIbytes;
import static oshi.ffm.unix.aix.PerfstatFunctions.netIfIerrors;
import static oshi.ffm.unix.aix.PerfstatFunctions.netIfIfIqdrops;
import static oshi.ffm.unix.aix.PerfstatFunctions.netIfIpackets;
import static oshi.ffm.unix.aix.PerfstatFunctions.netIfMtu;
import static oshi.ffm.unix.aix.PerfstatFunctions.netIfName;
import static oshi.ffm.unix.aix.PerfstatFunctions.netIfObytes;
import static oshi.ffm.unix.aix.PerfstatFunctions.netIfOerrors;
import static oshi.ffm.unix.aix.PerfstatFunctions.netIfOpackets;
import static oshi.ffm.unix.aix.PerfstatFunctions.netIfType;
import static oshi.ffm.unix.aix.PerfstatFunctions.perfstat_netinterface;

import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.ForeignFunctions;

/**
 * FFM-backed driver for {@code perfstat_netinterface}, mirroring
 * {@code oshi.driver.unix.aix.perfstat.PerfstatNetInterfaceJNA}.
 */
@ThreadSafe
public final class PerfstatNetInterfaceFFM {

    private static final Logger LOG = LoggerFactory.getLogger(PerfstatNetInterfaceFFM.class);

    private PerfstatNetInterfaceFFM() {
    }

    /** POJO mirror of the {@code perfstat_netinterface_t} fields OSHI consumes. */
    public static final class NetInterface {
        public String name = "";
        public String description = "";
        public byte type;
        public long mtu;
        public long ipackets;
        public long ibytes;
        public long ierrors;
        public long opackets;
        public long obytes;
        public long oerrors;
        public long collisions;
        public long bitrate;
        public long if_iqdrops;
    }

    /**
     * Queries {@code perfstat_netinterface} for all network interfaces.
     *
     * @return one {@link NetInterface} per interface, or an empty array on error
     */
    public static NetInterface[] queryNetInterfaces() {
        return ForeignFunctions.callInArenaOrDefault(arena -> {
            int count = perfstat_netinterface(MemorySegment.NULL, MemorySegment.NULL, PERFSTAT_NETINTERFACE_T_SIZE, 0);
            if (count <= 0) {
                return new NetInterface[0];
            }
            MemorySegment buf = arena.allocate((long) PERFSTAT_NETINTERFACE_T_SIZE * count);
            MemorySegment firstName = arena.allocate(PERFSTAT_ID_T_SIZE);
            int ret = perfstat_netinterface(firstName, buf, PERFSTAT_NETINTERFACE_T_SIZE, count);
            if (ret <= 0) {
                return new NetInterface[0];
            }
            NetInterface[] result = new NetInterface[ret];
            for (int i = 0; i < ret; i++) {
                long off = (long) i * PERFSTAT_NETINTERFACE_T_SIZE;
                NetInterface n = new NetInterface();
                n.name = netIfName(buf, off);
                n.description = netIfDescription(buf, off);
                n.type = netIfType(buf, off);
                n.mtu = netIfMtu(buf, off);
                n.ipackets = netIfIpackets(buf, off);
                n.ibytes = netIfIbytes(buf, off);
                n.ierrors = netIfIerrors(buf, off);
                n.opackets = netIfOpackets(buf, off);
                n.obytes = netIfObytes(buf, off);
                n.oerrors = netIfOerrors(buf, off);
                n.collisions = netIfCollisions(buf, off);
                n.bitrate = netIfBitrate(buf, off);
                n.if_iqdrops = netIfIfIqdrops(buf, off);
                result[i] = n;
            }
            return result;
        }, LOG, Level.TRACE, "Failed to query network interfaces", new NetInterface[0]);
    }
}
