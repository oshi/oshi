/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.driver.unix.aix.perfstat;

import static oshi.ffm.unix.aix.PerfstatFunctions.PERFSTAT_ID_T_SIZE;
import static oshi.ffm.unix.aix.PerfstatFunctions.PERFSTAT_PROTOCOL_T_SIZE;
import static oshi.ffm.unix.aix.PerfstatFunctions.perfstat_protocol;
import static oshi.ffm.unix.aix.PerfstatFunctions.protoName;
import static oshi.ffm.unix.aix.PerfstatFunctions.protoTcpAccepted;
import static oshi.ffm.unix.aix.PerfstatFunctions.protoTcpDropped;
import static oshi.ffm.unix.aix.PerfstatFunctions.protoTcpEstablished;
import static oshi.ffm.unix.aix.PerfstatFunctions.protoTcpIerrors;
import static oshi.ffm.unix.aix.PerfstatFunctions.protoTcpInitiated;
import static oshi.ffm.unix.aix.PerfstatFunctions.protoTcpIpackets;
import static oshi.ffm.unix.aix.PerfstatFunctions.protoTcpOpackets;
import static oshi.ffm.unix.aix.PerfstatFunctions.protoUdpIerrors;
import static oshi.ffm.unix.aix.PerfstatFunctions.protoUdpIpackets;
import static oshi.ffm.unix.aix.PerfstatFunctions.protoUdpNoSocket;
import static oshi.ffm.unix.aix.PerfstatFunctions.protoUdpOpackets;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * FFM-backed driver for {@code perfstat_protocol}, mirroring {@code oshi.driver.unix.aix.perfstat.PerfstatProtocolJNA}.
 * Only the TCP and UDP variants of the union are read — OSHI doesn't consume the other protocols.
 */
@ThreadSafe
public final class PerfstatProtocolFFM {

    private PerfstatProtocolFFM() {
    }

    /** POJO mirror of one protocol entry with TCP+UDP variants populated when applicable. */
    public static final class Protocol {
        public String name = "";
        // TCP stats (populated only when name equals "tcp")
        public long tcpIpackets;
        public long tcpIerrors;
        public long tcpOpackets;
        public long tcpInitiated;
        public long tcpAccepted;
        public long tcpEstablished;
        public long tcpDropped;
        // UDP stats (populated only when name equals "udp")
        public long udpIpackets;
        public long udpIerrors;
        public long udpOpackets;
        public long udpNoSocket;
    }

    /**
     * Queries {@code perfstat_protocol} for all protocols.
     *
     * @return one {@link Protocol} per entry, or an empty array on error
     */
    public static Protocol[] queryProtocols() {
        try (Arena arena = Arena.ofConfined()) {
            int count = perfstat_protocol(MemorySegment.NULL, MemorySegment.NULL, PERFSTAT_PROTOCOL_T_SIZE, 0);
            if (count <= 0) {
                return new Protocol[0];
            }
            MemorySegment buf = arena.allocate((long) PERFSTAT_PROTOCOL_T_SIZE * count);
            MemorySegment firstName = arena.allocate(PERFSTAT_ID_T_SIZE);
            int ret = perfstat_protocol(firstName, buf, PERFSTAT_PROTOCOL_T_SIZE, count);
            if (ret <= 0) {
                return new Protocol[0];
            }
            Protocol[] result = new Protocol[ret];
            for (int i = 0; i < ret; i++) {
                long off = (long) i * PERFSTAT_PROTOCOL_T_SIZE;
                Protocol p = new Protocol();
                p.name = protoName(buf, off);
                if ("tcp".equals(p.name)) {
                    p.tcpIpackets = protoTcpIpackets(buf, off);
                    p.tcpIerrors = protoTcpIerrors(buf, off);
                    p.tcpOpackets = protoTcpOpackets(buf, off);
                    p.tcpInitiated = protoTcpInitiated(buf, off);
                    p.tcpAccepted = protoTcpAccepted(buf, off);
                    p.tcpEstablished = protoTcpEstablished(buf, off);
                    p.tcpDropped = protoTcpDropped(buf, off);
                } else if ("udp".equals(p.name)) {
                    p.udpIpackets = protoUdpIpackets(buf, off);
                    p.udpIerrors = protoUdpIerrors(buf, off);
                    p.udpOpackets = protoUdpOpackets(buf, off);
                    p.udpNoSocket = protoUdpNoSocket(buf, off);
                }
                result[i] = p;
            }
            return result;
        } catch (Throwable t) {
            return new Protocol[0];
        }
    }
}
