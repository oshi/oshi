/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.NetworkInterface;

import org.junit.jupiter.api.Test;

import oshi.hardware.NetworkIF.IfOperStatus;

class NetworkIFTest {

    private static final NetworkIF MINIMAL = new NetworkIF() {
        @Override
        public NetworkInterface queryNetworkInterface() {
            return null;
        }

        @Override
        public String getName() {
            return "eth0";
        }

        @Override
        public int getIndex() {
            return 0;
        }

        @Override
        public String getDisplayName() {
            return "eth0";
        }

        @Override
        public long getMTU() {
            return 1500L;
        }

        @Override
        public String getMacaddr() {
            return "00:00:00:00:00:00";
        }

        @Override
        public String[] getIPv4addr() {
            return new String[0];
        }

        @Override
        public Short[] getSubnetMasks() {
            return new Short[0];
        }

        @Override
        public String[] getIPv6addr() {
            return new String[0];
        }

        @Override
        public Short[] getPrefixLengths() {
            return new Short[0];
        }

        @Override
        public long getBytesRecv() {
            return 0L;
        }

        @Override
        public long getBytesSent() {
            return 0L;
        }

        @Override
        public long getPacketsRecv() {
            return 0L;
        }

        @Override
        public long getPacketsSent() {
            return 0L;
        }

        @Override
        public long getInErrors() {
            return 0L;
        }

        @Override
        public long getOutErrors() {
            return 0L;
        }

        @Override
        public long getInDrops() {
            return 0L;
        }

        @Override
        public long getCollisions() {
            return 0L;
        }

        @Override
        public long getSpeed() {
            return 0L;
        }

        @Override
        public long getTimeStamp() {
            return 0L;
        }

        @Override
        public boolean isKnownVmMacAddr() {
            return false;
        }

        @Override
        public boolean updateAttributes() {
            return false;
        }
    };

    private static final NetworkIF OVERRIDING = new NetworkIF() {
        @Override
        public NetworkInterface queryNetworkInterface() {
            return null;
        }

        @Override
        public String getName() {
            return "eth1";
        }

        @Override
        public int getIndex() {
            return 1;
        }

        @Override
        public String getDisplayName() {
            return "eth1";
        }

        @Override
        public long getMTU() {
            return 1500L;
        }

        @Override
        public String getMacaddr() {
            return "00:00:00:00:00:01";
        }

        @Override
        public String[] getIPv4addr() {
            return new String[0];
        }

        @Override
        public Short[] getSubnetMasks() {
            return new Short[0];
        }

        @Override
        public String[] getIPv6addr() {
            return new String[0];
        }

        @Override
        public Short[] getPrefixLengths() {
            return new Short[0];
        }

        @Override
        public long getBytesRecv() {
            return 0L;
        }

        @Override
        public long getBytesSent() {
            return 0L;
        }

        @Override
        public long getPacketsRecv() {
            return 0L;
        }

        @Override
        public long getPacketsSent() {
            return 0L;
        }

        @Override
        public long getInErrors() {
            return 0L;
        }

        @Override
        public long getOutErrors() {
            return 0L;
        }

        @Override
        public long getInDrops() {
            return 0L;
        }

        @Override
        public long getCollisions() {
            return 0L;
        }

        @Override
        public long getSpeed() {
            return 0L;
        }

        @Override
        public long getTimeStamp() {
            return 0L;
        }

        @Override
        public boolean isKnownVmMacAddr() {
            return false;
        }

        @Override
        public boolean updateAttributes() {
            return false;
        }

        @Override
        public String getIfAlias() {
            return "wan0";
        }

        @Override
        public IfOperStatus getIfOperStatus() {
            return IfOperStatus.UP;
        }

        @Override
        public int getIfType() {
            return 6;
        }

        @Override
        public int getNdisPhysicalMediumType() {
            return 14;
        }

        @Override
        public boolean isConnectorPresent() {
            return true;
        }
    };

    @Test
    void testDefaultGetIfAlias() {
        assertThat(MINIMAL.getIfAlias(), is(""));
    }

    @Test
    void testDefaultGetIfOperStatus() {
        assertThat(MINIMAL.getIfOperStatus(), is(IfOperStatus.UNKNOWN));
    }

    @Test
    void testDefaultGetIfType() {
        assertThat(MINIMAL.getIfType(), is(0));
    }

    @Test
    void testDefaultGetNdisPhysicalMediumType() {
        assertThat(MINIMAL.getNdisPhysicalMediumType(), is(0));
    }

    @Test
    void testDefaultIsConnectorPresent() {
        assertThat(MINIMAL.isConnectorPresent(), is(false));
    }

    @Test
    void testOverriddenDefaults() {
        assertThat(OVERRIDING.getIfAlias(), is("wan0"));
        assertThat(OVERRIDING.getIfOperStatus(), is(IfOperStatus.UP));
        assertThat(OVERRIDING.getIfType(), is(6));
        assertThat(OVERRIDING.getNdisPhysicalMediumType(), is(14));
        assertThat(OVERRIDING.isConnectorPresent(), is(true));
    }

    @Test
    void testIfOperStatusByValue() {
        assertThat(IfOperStatus.byValue(1), is(IfOperStatus.UP));
        assertThat(IfOperStatus.byValue(2), is(IfOperStatus.DOWN));
        assertThat(IfOperStatus.byValue(99), is(IfOperStatus.UNKNOWN));
        assertThat(IfOperStatus.UP.getValue(), is(1));
    }
}
