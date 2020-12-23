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
package oshi.jna.platform.windows;

import com.sun.jna.Memory;
import com.sun.jna.Native; // NOSONAR squid:S1191
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

public interface IPHlpAPI extends com.sun.jna.platform.win32.IPHlpAPI {
    IPHlpAPI INSTANCE = Native.load("IPHlpAPI", IPHlpAPI.class, W32APIOptions.DEFAULT_OPTIONS);

    int UDP_TABLE_OWNER_PID = 1;
    int TCP_TABLE_OWNER_PID_ALL = 5;

    /**
     * Contains information that describes an IPv4 TCP connection.
     */
    @FieldOrder({ "dwState", "dwLocalAddr", "dwLocalPort", "dwRemoteAddr", "dwRemotePort", "dwOwningPid" })
    class MIB_TCPROW_OWNER_PID extends Structure {
        public int dwState;
        public int dwLocalAddr;
        public int dwLocalPort;
        public int dwRemoteAddr;
        public int dwRemotePort;
        public int dwOwningPid;
    }

    /**
     * Contains a table of IPv4 TCP connections on the local computer.
     */
    @FieldOrder({ "dwNumEntries", "table" })
    class MIB_TCPTABLE_OWNER_PID extends Structure {
        public int dwNumEntries;
        public MIB_TCPROW_OWNER_PID[] table = new MIB_TCPROW_OWNER_PID[1];

        public MIB_TCPTABLE_OWNER_PID(Memory buf) {
            super(buf);
            // First element contains array size
            table = (MIB_TCPROW_OWNER_PID[]) new MIB_TCPROW_OWNER_PID().toArray(buf.getInt(0));
            read();
        }

    }

    /**
     * Contains information that describes an IPv6 TCP connection.
     */
    @FieldOrder({ "LocalAddr", "dwLocalScopeId", "dwLocalPort", "RemoteAddr", "dwRemoteScopeId", "dwRemotePort",
            "State", "dwOwningPid" })
    class MIB_TCP6ROW_OWNER_PID extends Structure {
        public byte[] LocalAddr = new byte[16];
        public int dwLocalScopeId;
        public int dwLocalPort;
        public byte[] RemoteAddr = new byte[16];
        public int dwRemoteScopeId;
        public int dwRemotePort;
        public int State;
        public int dwOwningPid;
    }

    /**
     * Contains a table of IPv6 TCP connections on the local computer.
     */
    @FieldOrder({ "dwNumEntries", "table" })
    class MIB_TCP6TABLE_OWNER_PID extends Structure {
        public int dwNumEntries;
        public MIB_TCP6ROW_OWNER_PID[] table = new MIB_TCP6ROW_OWNER_PID[1];

        public MIB_TCP6TABLE_OWNER_PID(Memory buf) {
            super(buf);
            // First element contains array size
            table = (MIB_TCP6ROW_OWNER_PID[]) new MIB_TCP6ROW_OWNER_PID().toArray(buf.getInt(0));
            read();
        }
    }

    /**
     * Contains information that describes an IPv6 UDP connection.
     */
    @FieldOrder({ "dwLocalAddr", "dwLocalPort", "dwOwningPid" })
    class MIB_UDPROW_OWNER_PID extends Structure {
        public int dwLocalAddr;
        public int dwLocalPort;
        public int dwOwningPid;
    }

    /**
     * Contains a table of IPv6 UDP connections on the local computer.
     */
    @FieldOrder({ "dwNumEntries", "table" })
    class MIB_UDPTABLE_OWNER_PID extends Structure {
        public int dwNumEntries;
        public MIB_UDPROW_OWNER_PID[] table = new MIB_UDPROW_OWNER_PID[1];

        public MIB_UDPTABLE_OWNER_PID(Memory buf) {
            super(buf);
            // First element contains array size
            table = (MIB_UDPROW_OWNER_PID[]) new MIB_UDPROW_OWNER_PID().toArray(buf.getInt(0));
            read();
        }
    }

    /**
     * Contains information that describes an IPv6 UDP connection.
     */
    @FieldOrder({ "ucLocalAddr", "dwLocalScopeId", "dwLocalPort", "dwOwningPid" })
    class MIB_UDP6ROW_OWNER_PID extends Structure {
        public byte[] ucLocalAddr = new byte[16];
        public int dwLocalScopeId;
        public int dwLocalPort;
        public int dwOwningPid;
    }

    /**
     * Contains a table of IPv6 UDP connections on the local computer.
     */
    @FieldOrder({ "dwNumEntries", "table" })
    class MIB_UDP6TABLE_OWNER_PID extends Structure {
        public int dwNumEntries;
        public MIB_UDP6ROW_OWNER_PID[] table = new MIB_UDP6ROW_OWNER_PID[1];

        public MIB_UDP6TABLE_OWNER_PID(Memory buf) {
            super(buf);
            // First element contains array size
            table = (MIB_UDP6ROW_OWNER_PID[]) new MIB_UDP6ROW_OWNER_PID().toArray(buf.getInt(0));
            read();
        }
    }

    /**
     * Retrieves a table that contains a list of TCP endpoints available to the
     * application.
     *
     * @param pTcpTable
     *            A pointer to the table structure that contains the filtered TCP
     *            endpoints available to the application.
     * @param pdwSize
     *            The estimated size of the structure returned in pTcpTable, in
     *            bytes. If this value is set too small,
     *            {@code ERROR_INSUFFICIENT_BUFFER} is returned by this function,
     *            and this field will contain the correct size of the structure.
     * @param bOrder
     *            A value that specifies whether the TCP connection table should be
     *            sorted. If this parameter is set to TRUE, the TCP endpoints in the
     *            table are sorted in ascending order, starting with the lowest
     *            local IP address. If this parameter is set to FALSE, the TCP
     *            endpoints in the table appear in the order in which they were
     *            retrieved. The following values are compared (as listed) when
     *            ordering the TCP endpoints: Local IP address, Local scope ID
     *            (applicable when the ulAf parameter is set to AF_INET6), Local TCP
     *            port, Remote IP address, Remote scope ID (applicable when the ulAf
     *            parameter is set to AF_INET6), Remote TCP port.
     * @param ulAf
     *            The version of IP used by the TCP endpoints.
     * @param TableClass
     *            The type of the TCP table structure to retrieve. This parameter
     *            can be one of the values from the {@code TCP_TABLE_CLASS}
     *            enumeration.
     * @param Reserved
     *            Reserved. This value must be zero.
     * @return If the function succeeds, the return value is {@code NO_ERROR}. If
     *         the function fails, the return value is an error code.
     */
    int GetExtendedTcpTable(Pointer pTcpTable, IntByReference pdwSize, boolean bOrder, int ulAf, int TableClass,
            int Reserved);

    /**
     * Retrieves a table that contains a list of UDP endpoints available to the
     * application.
     *
     * @param pUdpTable
     *            A pointer to the table structure that contains the filtered UDP
     *            endpoints available to the application.
     * @param pdwSize
     *            The estimated size of the structure returned in pTcpTable, in
     *            bytes. If this value is set too small,
     *            {@code ERROR_INSUFFICIENT_BUFFER} is returned by this function,
     *            and this field will contain the correct size of the structure.
     * @param bOrder
     *            A value that specifies whether the TCP connection table should be
     *            sorted. If this parameter is set to TRUE, the TCP endpoints in the
     *            table are sorted in ascending order, starting with the lowest
     *            local IP address. If this parameter is set to FALSE, the TCP
     *            endpoints in the table appear in the order in which they were
     *            retrieved. The following values are compared (as listed) when
     *            ordering the TCP endpoints: Local IP address, Local scope ID
     *            (applicable when the ulAf parameter is set to {@code AF_INET6}),
     *            Local TCP port, Remote IP address, Remote scope ID (applicable
     *            when the ulAf parameter is set to {@code AF_INET6}), Remote TCP
     *            port.
     * @param ulAf
     *            The version of IP used by the UDP endpoints.
     * @param TableClass
     *            The type of the TCP table structure to retrieve. This parameter
     *            can be one of the values from the {@code TCP_TABLE_CLASS}
     *            enumeration.
     * @param Reserved
     *            Reserved. This value must be zero.
     * @return If the function succeeds, the return value is {@code NO_ERROR}. If
     *         the function fails, the return value is an error code.
     */
    int GetExtendedUdpTable(Pointer pUdpTable, IntByReference pdwSize, boolean bOrder, int ulAf, int TableClass,
            int Reserved);
}
