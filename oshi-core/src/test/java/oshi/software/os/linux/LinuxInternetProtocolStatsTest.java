package oshi.software.os.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import oshi.util.ProcUtil;

@DisabledOnOs(OS.WINDOWS)
public class LinuxInternetProtocolStatsTest {

    @Test
    void testRawNetNetstat() {
        String resource =
            LinuxInternetProtocolStats.class.getResource("sample-proc-net-netstat.txt").getFile();

        Map<String, Map<String, Long>> results = ProcUtil.getMapFromHeaderProc(resource);

        assertThat(results.keySet(), containsInAnyOrder("TcpExt", "IpExt", "MPTcpExt"));
        assertThat(results.get("TcpExt").get("SyncookiesSent"), is(6L));
        assertThat(results.get("TcpExt").get("TCPAODroppedIcmps"), is(3L));
        assertThat(results.get("IpExt").get("InNoRoutes"), is(55L));
        assertThat(results.get("MPTcpExt").get("MPCurrEstab"), is(1L));
    }

    @Test
    void testRawNetNetstatWithLimitedKeys() {
        String resource =
            LinuxInternetProtocolStats.class.getResource("sample-proc-net-netstat.txt").getFile();

        Map<String, Map<String, Long>> results = ProcUtil.getMapFromHeaderProc(resource, "IpExt");

        assertThat(results.keySet(), contains("IpExt"));
        assertThat(results.get("IpExt").get("InNoRoutes"), is(55L));
    }

    @Test
    void testRawNetSnmp() {
        String resource =
            LinuxInternetProtocolStats.class.getResource("sample-proc-net-snmp.txt").getFile();

        Map<String, Map<String, Long>> results = ProcUtil.getMapFromHeaderProc(resource);

        assertThat(results.keySet(), containsInAnyOrder("Ip", "Icmp", "IcmpMsg", "Tcp", "Udp", "UdpLite"));
        assertThat(results.get("Tcp").get("ActiveOpens"), is(1892L));
        assertThat(results.get("Ip").get("OutTransmits"), is(66296L));
        assertThat(results.get("Icmp").get("InMsgs"), is(184L));
    }

}
