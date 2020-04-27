/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
}
