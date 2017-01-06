package oshi.software.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import oshi.software.os.NetworkParams;
import oshi.util.FileUtil;

/**
 * Common NetworkParams implementation.
 */
public abstract class AbstractNetworkParams implements NetworkParams {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractNetworkParams.class);
    private static final String NAMESERVER = "nameserver";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHostName() {
        try {
            String hn = InetAddress.getLocalHost().getHostName();
            int dot = hn.indexOf('.');
            if (dot == -1) {
                return hn;
            } else {
                return hn.substring(0, dot);
            }
        } catch (UnknownHostException e) {
            LOG.error("Unknown host exception when getting address of local host: " + e);
            return "";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDnsServers() {
        List<String> resolv = FileUtil.readFile("/etc/resolv.conf");
        String key = NAMESERVER;
        int maxNameServer = 3;
        List<String> servers = new ArrayList<>();
        for (int i = 0; i < resolv.size() && servers.size() < maxNameServer; i++) {
            String line = resolv.get(i);
            if (line.startsWith(key)) {
                String value = line.substring(key.length()).replaceFirst("^[ \t]+", "");
                if (value.length() != 0 && value.charAt(0) != '#' && value.charAt(0) != ';') {
                    String val = value.split("[ \t#;]", 2)[0];
                    servers.add(val);
                }
            }
        }
        return servers.toArray(new String[servers.size()]);
    }

    static protected String searchGateway(List<String> lines){
        for(String line: lines){
            String leftTrimmed = line.replaceFirst("^[ \t]+", "");
            if(leftTrimmed.startsWith("gateway:")){
                return leftTrimmed.split("[ \t]", 2)[1];
            }
        }
        return "";
    }
}
