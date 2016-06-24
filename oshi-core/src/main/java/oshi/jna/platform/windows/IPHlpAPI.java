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
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.jna.platform.windows;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

/**
 * Windows IP Helper API. This class should be considered non-API as it may be
 * removed if/when its code is incorporated public into the JNA project.
 * 
 * @author widdis[at]gmail[dot]com
 */
public interface IPHlpAPI extends Library {
    IPHlpAPI INSTANCE = (IPHlpAPI) Native.loadLibrary("IPHlpAPI", IPHlpAPI.class);

    int MAX_INTERFACE_NAME_LEN = 256;
    int MAXLEN_IFDESCR = 256;
    int MAXLEN_PHYSADDR = 8;

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

    /**
     * The GetIfEntry function retrieves information for the specified interface
     * on the local computer
     * 
     * @param pIfRow
     *            A pointer to a MIB_IFROW structure that, on successful return,
     *            receives information for an interface on the local computer.
     *            On input, set the dwIndex member of MIB_IFROW to the index of
     *            the interface for which to retrieve information.
     * @return If the function succeeds, the return value is NO_ERROR.
     */
    int GetIfEntry(MIB_IFROW pIfRow);

}
