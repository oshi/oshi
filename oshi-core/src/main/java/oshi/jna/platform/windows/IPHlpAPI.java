/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
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
package oshi.jna.platform.windows;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.WinDef.UCHAR;
import com.sun.jna.platform.win32.WinDef.UINT;
import com.sun.jna.platform.win32.WinDef.ULONG;
import com.sun.jna.platform.win32.WinDef.ULONGByReference;

/**
 * Windows IP Helper API. This class should be considered non-API as it may be
 * removed if/when its code is incorporated public into the JNA project.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface IPHlpAPI extends Library {
    IPHlpAPI INSTANCE = Native.loadLibrary("IPHlpAPI", IPHlpAPI.class);

    int IF_MAX_STRING_SIZE = 256;
    int IF_MAX_PHYS_ADDRESS_LENGTH = 32;
    int MAX_INTERFACE_NAME_LEN = 256;
    int MAXLEN_IFDESCR = 256;
    int MAXLEN_PHYSADDR = 8;
    int MAX_HOSTNAME_LEN = 128;
    int MAX_DOMAIN_NAME_LEN = 128;
    int MAX_SCOPE_ID_LEN = 256;

    int ERROR_BUFFER_OVERFLOW = 0x6f;

    class MIB_IFROW extends Structure {
        public char[] wszName = new char[MAX_INTERFACE_NAME_LEN];
        public int dwIndex;
        public int dwType;
        public int dwMtu;
        public int dwSpeed;
        public int dwPhysAddrLen;
        public byte[] bPhysAddr = new byte[MAXLEN_PHYSADDR];
        public int dwAdminStatus;
        public int dwOperStatus;
        public int dwLastChange;
        public int dwInOctets;
        public int dwInUcastPkts;
        public int dwInNUcastPkts;
        public int dwInDiscards;
        public int dwInErrors;
        public int dwInUnknownProtos;
        public int dwOutOctets;
        public int dwOutUcastPkts;
        public int dwOutNUcastPkts;
        public int dwOutDiscards;
        public int dwOutErrors;
        public int dwOutQLen;
        public int dwDescrLen;
        public byte[] bDescr = new byte[MAXLEN_IFDESCR];

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "wszName", "dwIndex", "dwType", "dwMtu", "dwSpeed", "dwPhysAddrLen",
                    "bPhysAddr", "dwAdminStatus", "dwOperStatus", "dwLastChange", "dwInOctets", "dwInUcastPkts",
                    "dwInNUcastPkts", "dwInDiscards", "dwInErrors", "dwInUnknownProtos", "dwOutOctets",
                    "dwOutUcastPkts", "dwOutNUcastPkts", "dwOutDiscards", "dwOutErrors", "dwOutQLen", "dwDescrLen",
                    "bDescr" });
        }
    }

    class MIB_IFROW2 extends Structure {
        public long InterfaceLuid; // 64-bit union
        public ULONG InterfaceIndex;
        public GUID InterfaceGuid;
        public char[] Alias = new char[IF_MAX_STRING_SIZE + 1];
        public char[] Description = new char[IF_MAX_STRING_SIZE + 1];
        public ULONG PhysicalAddressLength;
        public UCHAR[] PhysicalAddress = new UCHAR[IF_MAX_PHYS_ADDRESS_LENGTH];
        public UCHAR[] PermanentPhysicalAddress = new UCHAR[IF_MAX_PHYS_ADDRESS_LENGTH];
        public ULONG Mtu;
        public ULONG Type;
        // enums
        public int TunnelType;
        public int MediaType;
        public int PhysicalMediumType;
        public int AccessType;
        public int DirectionType;
        // 8-bit structure
        public byte InterfaceAndOperStatusFlags;
        // enums
        public int OperStatus;
        public int AdminStatus;
        public int MediaConnectState;
        public GUID NetworkGuid;
        public int ConnectionType;
        public long TransmitLinkSpeed;
        public long ReceiveLinkSpeed;
        public long InOctets;
        public long InUcastPkts;
        public long InNUcastPkts;
        public long InDiscards;
        public long InErrors;
        public long InUnknownProtos;
        public long InUcastOctets;
        public long InMulticastOctets;
        public long InBroadcastOctets;
        public long OutOctets;
        public long OutUcastPkts;
        public long OutNUcastPkts;
        public long OutDiscards;
        public long OutErrors;
        public long OutUcastOctets;
        public long OutMulticastOctets;
        public long OutBroadcastOctets;
        public long OutQLen;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "InterfaceLuid", "InterfaceIndex", "InterfaceGuid", "Alias",
                    "Description", "PhysicalAddressLength", "PhysicalAddress", "PermanentPhysicalAddress", "Mtu",
                    "Type", "TunnelType", "MediaType", "PhysicalMediumType", "AccessType", "DirectionType",
                    "InterfaceAndOperStatusFlags", "OperStatus", "AdminStatus", "MediaConnectState", "NetworkGuid",
                    "ConnectionType", "TransmitLinkSpeed", "ReceiveLinkSpeed", "InOctets", "InUcastPkts",
                    "InNUcastPkts", "InDiscards", "InErrors", "InUnknownProtos", "InUcastOctets", "InMulticastOctets",
                    "InBroadcastOctets", "OutOctets", "OutUcastPkts", "OutNUcastPkts", "OutDiscards", "OutErrors",
                    "OutUcastOctets", "OutMulticastOctets", "OutBroadcastOctets", "OutQLen" });
        }
    }

    class IP_ADDRESS_STRING extends Structure {
        public byte[] String = new byte[16];

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "String" });
        }
    }

    class IP_ADDR_STRING extends Structure {
        public ByReference Next;
        public IP_ADDRESS_STRING IpAddress;
        public IP_ADDRESS_STRING IpMask;
        public int Context;

        public static class ByReference extends IP_ADDR_STRING implements Structure.ByReference {
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "Next", "IpAddress", "IpMask", "Context" });
        }
    }

    class FIXED_INFO extends Structure {
        public byte[] HostName = new byte[MAX_HOSTNAME_LEN + 4];
        public byte[] DomainName = new byte[MAX_DOMAIN_NAME_LEN + 4];
        public IP_ADDR_STRING.ByReference CurrentDnsServer; // IP_ADDR_STRING
        public IP_ADDR_STRING DnsServerList;
        public UINT NodeType;
        public byte[] ScopeId = new byte[MAX_SCOPE_ID_LEN + 4];
        public UINT EnableRouting;
        public UINT EnableProxy;
        public UINT EnableDns;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "HostName", "DomainName", "CurrentDnsServer", "DnsServerList",
                    "NodeType", "ScopeId", "EnableRouting", "EnableProxy", "EnableDns" });
        }

        public FIXED_INFO(Pointer p) {
            super(p);
        }
    }

    /**
     * The GetIfEntry function retrieves information for the specified interface
     * on the local computer.
     *
     * The dwIndex member in the MIB_IFROW structure pointed to by the pIfRow
     * parameter must be initialized to a valid network interface index
     * retrieved by a previous call to the GetIfTable, GetIfTable2, or
     * GetIfTable2Ex function. The GetIfEntry function will fail if the dwIndex
     * member of the MIB_IFROW pointed to by the pIfRow parameter does not match
     * an existing interface index on the local computer.
     *
     * @param pIfRow
     *            A pointer to a MIB_IFROW structure that, on successful return,
     *            receives information for an interface on the local computer.
     *            On input, set the dwIndex member of MIB_IFROW to the index of
     *            the interface for which to retrieve information.
     * @return If the function succeeds, the return value is NO_ERROR.
     */
    int GetIfEntry(MIB_IFROW pIfRow);

    /**
     * The GetIfEntry2 function retrieves information for the specified
     * interface on the local computer.
     *
     * On input, at least one of the following members in the MIB_IF_ROW2
     * structure passed in the Row parameter must be initialized: InterfaceLuid
     * or InterfaceIndex. The fields are used in the order listed above. So if
     * the InterfaceLuid is specified, then this member is used to determine the
     * interface. If no value was set for the InterfaceLuid member (the value of
     * this member was set to zero), then the InterfaceIndex member is next used
     * to determine the interface. On output, the remaining fields of the
     * MIB_IF_ROW2 structure pointed to by the Row parameter are filled in.
     *
     * @param pIfRow2
     *            A pointer to a MIB_IF_ROW2 structure that, on successful
     *            return, receives information for an interface on the local
     *            computer. On input, the InterfaceLuid or the InterfaceIndex
     *            member of the MIB_IF_ROW2 must be set to the interface for
     *            which to retrieve information.
     * @return If the function succeeds, the return value is NO_ERROR.
     */
    int GetIfEntry2(MIB_IFROW2 pIfRow2);

    /**
     * The GetNetworkParams function retrieves network parameters for the local
     * computer.
     *
     * @param pFixedInfo
     *            A pointer to a buffer that contains a FIXED_INFO structure
     *            that receives the network parameters for the local computer,
     *            if the function was successful. This buffer must be allocated
     *            by the caller prior to calling the GetNetworkParams function.
     * @param pOutBufLen
     *            A pointer to a ULONG variable that specifies the size of the
     *            FIXED_INFO structure. If this size is insufficient to hold the
     *            information, GetNetworkParams fills in this variable with the
     *            required size, and returns an error code of
     *            ERROR_BUFFER_OVERFLOW.
     * @return If the function succeeds, the return value is ERROR_SUCCESS.
     */
    int GetNetworkParams(FIXED_INFO pFixedInfo, ULONGByReference pOutBufLen);
}
