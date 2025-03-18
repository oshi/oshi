package oshi.software.os.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class LinuxInternetProtocolStatsTest {

    @Test
    void testRawNetNetstat() {
        URL url = LinuxInternetProtocolStats.class.getResource("sample-proc-net-netstat.txt");
        assertThat(url, is(notNullValue()));

        Map<String, Map<String, Long>> results = LinuxInternetProtocolStats.processNetSnmpOrNetstat(url.getFile());

        assertThat(results.keySet(), containsInAnyOrder("TcpExt", "IpExt", "MPTcpExt"));
        assertThat(results.get("TcpExt").get("SyncookiesSent"), is(6L));
        assertThat(results.get("TcpExt").get("TCPAODroppedIcmps"), is(3L));
        assertThat(results.get("IpExt").get("InNoRoutes"), is(55L));
        assertThat(results.get("MPTcpExt").get("MPCurrEstab"), is(1L));
    }

    @Test
    void testRawNetSnmp() {
        URL url = LinuxInternetProtocolStats.class.getResource("sample-proc-net-snmp.txt");
        assertThat(url, is(notNullValue()));

        Map<String, Map<String, Long>> results = LinuxInternetProtocolStats.processNetSnmpOrNetstat(url.getFile());

        assertThat(results.keySet(), containsInAnyOrder("Ip", "Icmp", "IcmpMsg", "Tcp", "Udp", "UdpLite"));
        assertThat(results.get("Tcp").get("ActiveOpens"), is(1892L));
        assertThat(results.get("Ip").get("OutTransmits"), is(66296L));
        assertThat(results.get("Icmp").get("InMsgs"), is(184L));
    }

    @Test
    void testRawNetSnmp6() {
        URL url = LinuxInternetProtocolStats.class.getResource("sample-proc-net-snmp6.txt");
        assertThat(url, is(notNullValue()));

        Map<String, Map<String, Long>> results = LinuxInternetProtocolStats.processNetSnmp6(url.getFile());

        assertThat(results.keySet(), containsInAnyOrder("Ip6", "Icmp6", "Udp6", "UdpLite6"));
        assertThat(results.get("Udp6").get("InDatagrams"), is(8021L));
        assertThat(results.get("Icmp6").get("OutType143"), is(368L));
        assertThat(results.get("UdpLite6").get("MemErrors"), is(1L));
    }
}
