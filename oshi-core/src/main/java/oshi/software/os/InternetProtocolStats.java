/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.software.os;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;

/**
 * Includes key statistics of TCP and UDP protocols
 */
@ThreadSafe
public interface InternetProtocolStats {

    /**
     * Get the TCP stats for IPv4 connections.
     * <p>
     * On macOS connection information requires elevated permissions. Without
     * elevatd permissions, segment data is estimated.
     *
     * @return a {@link TcpStats} object encapsulating the stats.
     */
    TcpStats getTCPv4Stats();

    /**
     * Get the TCP stats for IPv6 connections, if available. If not available
     * separately, these may be 0 and included in IPv4 connections.
     *
     * @return a {@link TcpStats} object encapsulating the stats.
     */
    TcpStats getTCPv6Stats();

    /**
     * Get the UDP stats for IPv4 datagrams.
     *
     * @return a {@link UdpStats} object encapsulating the stats.
     */
    UdpStats getUDPv4Stats();

    /**
     * Get the UDP stats for IPv6 datagrams, if available. If not available
     * separately, these may be 0 and included in IPv4 datagrams.
     *
     * @return a {@link UdpStats} object encapsulating the stats.
     */
    UdpStats getUDPv6Stats();

    /**
     * Gets a list of TCP and UDP connections.
     *
     * @return A list of {@link IPConnection} objects for TCP and UDP connections.
     */
    List<IPConnection> getConnections();

    /**
     * Encapsulates statistics associated with a TCP connection.
     */
    @Immutable
    final class TcpStats {
        private final long connectionsEstablished;
        private final long connectionsActive;
        private final long connectionsPassive;
        private final long connectionFailures;
        private final long connectionsReset;
        private final long segmentsSent;
        private final long segmentsReceived;
        private final long segmentsRetransmitted;
        private final long inErrors;
        private final long outResets;

        public TcpStats(long connectionsEstablished, long connectionsActive, long connectionsPassive,
                long connectionFailures, long connectionsReset, long segmentsSent, long segmentsReceived,
                long segmentsRetransmitted, long inErrors, long outResets) {
            this.connectionsEstablished = connectionsEstablished;
            this.connectionsActive = connectionsActive;
            this.connectionsPassive = connectionsPassive;
            this.connectionFailures = connectionFailures;
            this.connectionsReset = connectionsReset;
            this.segmentsSent = segmentsSent;
            this.segmentsReceived = segmentsReceived;
            this.segmentsRetransmitted = segmentsRetransmitted;
            this.inErrors = inErrors;
            this.outResets = outResets;
        }

        /**
         * Connections Established is the number of TCP connections for which the
         * current state is either ESTABLISHED or CLOSE-WAIT
         *
         * @return the connectionsEstablished
         */
        public long getConnectionsEstablished() {
            return connectionsEstablished;
        }

        /**
         * Connections Active is the number of times TCP connections have made a direct
         * transition to the SYN-SENT state from the CLOSED state. In other words, it
         * shows a number of connections which are initiated by the local computer. The
         * value is a cumulative total.
         *
         * @return the connectionsActive
         */
        public long getConnectionsActive() {
            return connectionsActive;
        }

        /**
         * Connections Passive is the number of times TCP connections have made a direct
         * transition to the SYN-RCVD state from the LISTEN state. In other words, it
         * shows a number of connections to the local computer, which are initiated by
         * remote computers. The value is a cumulative total.
         *
         * @return the connectionsPassive
         */
        public long getConnectionsPassive() {
            return connectionsPassive;
        }

        /**
         * Connection Failures is the number of times TCP connections have made a direct
         * transition to the CLOSED state from the SYN-SENT state or the SYN-RCVD state,
         * plus the number of times TCP connections have made a direct transition to the
         * LISTEN state from the SYN-RCVD state.
         *
         * @return the connectionFailures
         */
        public long getConnectionFailures() {
            return connectionFailures;
        }

        /**
         * Connections Reset is the number of times TCP connections have made a direct
         * transition to the CLOSED state from either the ESTABLISHED state or the
         * CLOSE-WAIT state.
         *
         * @return the connectionsReset
         */
        public long getConnectionsReset() {
            return connectionsReset;
        }

        /**
         * Segments Sent is the number of segments sent, including those on current
         * connections, but excluding those containing only retransmitted bytes.
         *
         * @return the segmentsSent
         */
        public long getSegmentsSent() {
            return segmentsSent;
        }

        /**
         * Segments Received is the number of segments received, including those
         * received in error. This count includes segments received on currently
         * established connections.
         *
         * @return the segmentsReceived
         */
        public long getSegmentsReceived() {
            return segmentsReceived;
        }

        /**
         * Segments Retransmitted is the number of segments retransmitted, that is,
         * segments transmitted containing one or more previously transmitted bytes.
         *
         * @return the segmentsRetransmitted
         */
        public long getSegmentsRetransmitted() {
            return segmentsRetransmitted;
        }

        /**
         * The number of errors received.
         *
         * @return the inErrors
         */
        public long getInErrors() {
            return inErrors;
        }

        /**
         * The number of segments transmitted with the reset flag set.
         *
         * @return the outResets
         */
        public long getOutResets() {
            return outResets;
        }

        @Override
        public String toString() {
            return "TcpStats [connectionsEstablished=" + connectionsEstablished + ", connectionsActive="
                    + connectionsActive + ", connectionsPassive=" + connectionsPassive + ", connectionFailures="
                    + connectionFailures + ", connectionsReset=" + connectionsReset + ", segmentsSent=" + segmentsSent
                    + ", segmentsReceived=" + segmentsReceived + ", segmentsRetransmitted=" + segmentsRetransmitted
                    + ", inErrors=" + inErrors + ", outResets=" + outResets + "]";
        }
    }

    /**
     * Encapsulates statistics associated with a UDP connection.
     */
    @Immutable
    final class UdpStats {
        private final long datagramsSent;
        private final long datagramsReceived;
        private final long datagramsNoPort;
        private final long datagramsReceivedErrors;

        public UdpStats(long datagramsSent, long datagramsReceived, long datagramsNoPort,
                long datagramsReceivedErrors) {
            this.datagramsSent = datagramsSent;
            this.datagramsReceived = datagramsReceived;
            this.datagramsNoPort = datagramsNoPort;
            this.datagramsReceivedErrors = datagramsReceivedErrors;
        }

        /**
         * Datagrams Sent is the number of UDP datagrams sent from the entity.
         *
         * @return the datagramsSent
         */
        public long getDatagramsSent() {
            return datagramsSent;
        }

        /**
         * Datagrams Received is the number of UDP datagrams delivered to UDP users.
         *
         * @return the datagramsReceived
         */
        public long getDatagramsReceived() {
            return datagramsReceived;
        }

        /**
         * Datagrams No Port is the number of received UDP datagrams for which there was
         * no application at the destination port.
         *
         * @return the datagramsNoPort
         */
        public long getDatagramsNoPort() {
            return datagramsNoPort;
        }

        /**
         * Datagrams Received Errors is the number of received UDP datagrams that could
         * not be delivered for reasons other than the lack of an application at the
         * destination port.
         *
         * @return the datagramsReceivedErrors
         */
        public long getDatagramsReceivedErrors() {
            return datagramsReceivedErrors;
        }

        @Override
        public String toString() {
            return "UdpStats [datagramsSent=" + datagramsSent + ", datagramsReceived=" + datagramsReceived
                    + ", datagramsNoPort=" + datagramsNoPort + ", datagramsReceivedErrors=" + datagramsReceivedErrors
                    + "]";
        }
    }

    /**
     * The TCP connection state as described in RFC 793.
     */
    enum TcpState {
        UNKNOWN, CLOSED, LISTEN, SYN_SENT, SYN_RECV, ESTABLISHED, FIN_WAIT_1, FIN_WAIT_2, CLOSE_WAIT, CLOSING, LAST_ACK,
        TIME_WAIT, NONE;
    }

    /**
     * Encapsulates information associated with an IP connection.
     */
    @Immutable
    final class IPConnection {
        private final String type;
        private final byte[] localAddress;
        private final int localPort;
        private final byte[] foreignAddress;
        private final int foreignPort;
        private final TcpState state;
        private final int transmitQueue;
        private final int receiveQueue;
        private int owningProcessId;

        public IPConnection(String type, byte[] localAddress, int localPort, byte[] foreignAddress, int foreignPort,
                TcpState state, int transmitQueue, int receiveQueue, int owningProcessId) {
            this.type = type;
            this.localAddress = Arrays.copyOf(localAddress, localAddress.length);
            this.localPort = localPort;
            this.foreignAddress = Arrays.copyOf(foreignAddress, foreignAddress.length);
            this.foreignPort = foreignPort;
            this.state = state;
            this.transmitQueue = transmitQueue;
            this.receiveQueue = receiveQueue;
            this.owningProcessId = owningProcessId;
        }

        /**
         * Returns the connection protocol type, e.g., tcp4, tcp6, udp4, udp6
         *
         * @return The protocol type
         */
        public String getType() {
            return type;
        }

        /**
         * Gets the local address. For IPv4 addresses this is a 4-byte array. For IPv6
         * addresses this is a 16-byte array.
         * <p>
         * On Unix operating systems, the 16-bit value may be truncated, giving only the
         * high order bytes. IPv6 addresses ending in zeroes should be considered
         * suspect.
         *
         * @return The local address, or an empty array if the listener can accept a
         *         connection on any interface.
         */
        public byte[] getLocalAddress() {
            return Arrays.copyOf(localAddress, localAddress.length);
        }

        /**
         * Gets the local port.
         *
         * @return The local port, or 0 if unknown, or any port.
         */
        public int getLocalPort() {
            return localPort;
        }

        /**
         * Gets the foreign/remote address. For IPv4 addresses this is a 4-byte array.
         * For IPv6 addresses this is a 16-byte array.
         * <p>
         * On Unix operating systems, this value may be truncated. IPv6 addresses ending
         * in zeroes should be considered suspect.
         *
         * @return The foreign/remote address, or an empty array if unknown. An empty
         *         array will also result if
         */
        public byte[] getForeignAddress() {
            return Arrays.copyOf(foreignAddress, foreignAddress.length);
        }

        /**
         * Gets the foreign/remote port.
         *
         * @return The foreign/remote port, or 0 if unknown.
         */
        public int getForeignPort() {
            return foreignPort;
        }

        /**
         * Gets the connection state (TCP connections only).
         *
         * @return The connection state if known or relevant, null otherwise.
         */
        public TcpState getState() {
            return state;
        }

        /**
         * Gets the size of the transmit queue. Not available on Windows.
         *
         * @return The size of the transmit queue, or 0 if unknown.
         */
        public int getTransmitQueue() {
            return transmitQueue;
        }

        /**
         * Gets the size of the receive queue. Not available on Windows.
         *
         * @return The size of the receive queue, or 0 if unknown.
         */
        public int getReceiveQueue() {
            return receiveQueue;
        }

        /**
         * Gets the id of the process which holds this connection.
         *
         * @return The process id of the process which holds this connection if known,
         *         -1 otherwise.
         */
        public int getowningProcessId() {
            return owningProcessId;
        }

        @Override
        public String toString() {
            String localIp = "*";
            try {
                localIp = InetAddress.getByAddress(localAddress).toString();
            } catch (UnknownHostException e) { // NOSONAR squid:S108
            }
            String foreignIp = "*";
            try {
                foreignIp = InetAddress.getByAddress(foreignAddress).toString();
            } catch (UnknownHostException e) { // NOSONAR squid:S108
            }
            return "IPConnection [type=" + type + ", localAddress=" + localIp + ", localPort=" + localPort
                    + ", foreignAddress=" + foreignIp + ", foreignPort=" + foreignPort + ", state=" + state
                    + ", transmitQueue=" + transmitQueue + ", receiveQueue=" + receiveQueue + ", owningProcessId="
                    + owningProcessId + "]";
        }
    }
}
