package oshi.software.os.windows;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oshi.jna.platform.windows.IPHlpAPI;
import oshi.jna.platform.windows.IPHlpAPI.FIXED_INFO;
import oshi.jna.platform.windows.Kernel32;
import oshi.software.os.NetworkParams;
import oshi.util.platform.windows.WmiUtil;

public class WindowsNetworkParams implements NetworkParams {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsNetworkParams.class);

    private static final WmiUtil.ValueType[] GATEWAY_TYPES = {WmiUtil.ValueType.STRING, WmiUtil.ValueType.UINT16};

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHostName() {
        char[] buffer = new char[256];
        IntByReference bufferSize = new IntByReference(buffer.length);
        if (!Kernel32.INSTANCE.GetComputerNameEx(Kernel32.ComputerNameDnsHostname, buffer, bufferSize)) {
            LOG.error("Failed to get host name. Error code: {}", Kernel32.INSTANCE.GetLastError());
            return "";
        }
        return new String(buffer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDomainName() {
        char[] buffer = new char[256];
        IntByReference bufferSize = new IntByReference(buffer.length);
        if (!Kernel32.INSTANCE.GetComputerNameEx(Kernel32.ComputerNameDnsDomain, buffer, bufferSize)) {
            LOG.error("Failed to get dns domain name. Error code: {}", Kernel32.INSTANCE.GetLastError());
            return "";
        }
        return new String(buffer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDnsServers() {
        // this may be done by iterating WMI instances ROOT\CIMV2\Win32_NetworkAdapterConfiguration
        // then sort by IPConnectionMetric, but current JNA release does not have string array support
        // for Variant (it's merged but not release yet).
        WinDef.ULONGByReference bufferSize = new WinDef.ULONGByReference();
        int ret = IPHlpAPI.INSTANCE.GetNetworkParams(null, bufferSize);
        if (ret != IPHlpAPI.ERROR_BUFFER_OVERFLOW) {
            LOG.error("Failed to get network parameters buffer size. Error code: {}", ret);
            return new String[0];
        }

        FIXED_INFO buffer = new FIXED_INFO(new Memory(bufferSize.getValue().longValue()));
        ret = IPHlpAPI.INSTANCE.GetNetworkParams(buffer, bufferSize);
        if (ret != 0) {
            LOG.error("Failed to get network parameters. Error code: {}", ret);
            return new String[0];
        }

        List<String> list = new ArrayList<>();
        IPHlpAPI.IP_ADDR_STRING dns = buffer.DnsServerList;
        while(dns != null) {
            String addr = new String(dns.IpAddress.String);
            int nullPos = addr.indexOf(0);
            if(nullPos != -1) {
                addr = addr.substring(0, nullPos);
            }
            list.add(addr);
            dns = dns.Next;
        }

        return list.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpv4DefaultGateway() {
        String dest = "0.0.0.0/0";
        return getNextHop(dest);
    }

    private String getNextHop(String dest) {
        Map<String, List<Object>> vals = WmiUtil.selectObjectsFrom("ROOT\\StandardCimv2", "MSFT_NetRoute",
            "NextHop,RouteMetric", "WHERE DestinationPrefix=\"" + dest + "\"", GATEWAY_TYPES);
        List<Object> metrics = vals.get("RouteMetric");
        if (vals.get("RouteMetric").size() == 0) {
            return "";
        } else {
            int index = 0;
            Long min = Long.MAX_VALUE;
            for (int i = 0; i < metrics.size(); i++) {
                Long metric = (Long) metrics.get(i);
                if (metric < min) {
                    min = metric;
                    index = i;
                }
            }
            return (String) vals.get("NextHop").get(index);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpv6DefaultGateway() {
        String dest = "::/0";
        return getNextHop(dest);
    }
}
