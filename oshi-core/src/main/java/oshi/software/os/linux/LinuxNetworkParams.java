package oshi.software.os.linux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import oshi.software.os.NetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;

public class LinuxNetworkParams implements NetworkParams{

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(LinuxNetworkParams.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHostName() {
        return FileUtil.getStringFromFile("/proc/sys/kernel/hostname");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDomainName() {
        return ExecutingCommand.getFirstAnswer("dnsdomainname");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDnsServers() {
        List<String> resolv = FileUtil.readFile("/etc/resolv.conf");
        String key = "nameserver";
        int maxNameServer = 3;
        List<String> servers = new ArrayList<>();
        for(String line: resolv){
            if(!line.startsWith(key)) continue;

            String value = line.substring(key.length())
                .replaceFirst("^[ \t]+", "");
            if(value.length() == 0 ||
                value.charAt(0) == '#' || value.charAt(0) == ';') continue;
            String val = value.split("[ \t#;]", 2)[0];
            servers.add(val);
            if(servers.size() >= maxNameServer) break;
        }
        return servers.toArray(new String[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getV4DefaultGateway() {
        List<String> routes = ExecutingCommand.runNative("route -A inet -n");
        if(routes.size() <= 2){
            return "";
        }

        String gateway = "";
        int minMetric = Integer.MAX_VALUE;

        for(int i = 2; i < routes.size(); i++){
            String[] fields = routes.get(i).split("\\s+");
            if(fields.length > 4 && fields[0].equals("0.0.0.0")) {
                try{
                    boolean isGateway = fields[3].indexOf('G') != -1;
                    int metric = Integer.parseInt(fields[4]);
                    if(isGateway && (metric < minMetric)){
                        minMetric = metric;
                        gateway = fields[1];
                    }
                } catch (NumberFormatException ignored){}
            }
        }
        return gateway;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getV6DefaultGateway() {
        List<String> routes = ExecutingCommand.runNative("route -A inet6 -n");
        if(routes.size() <= 2){
            return "";
        }

        String gateway = "";
        int minMetric = Integer.MAX_VALUE;

        for(int i = 2; i < routes.size(); i++){
            String[] fields = routes.get(i).split("\\s+");
            if(fields.length > 3 && fields[0].equals("::/0")) {
                try{
                    boolean isGateway = fields[2].indexOf('G') != -1;
                    int metric = Integer.parseInt(fields[3]);
                    if(isGateway && (metric < minMetric)){
                        minMetric = metric;
                        gateway = fields[1];
                    }
                } catch (NumberFormatException ignored){}
            }
        }
        return gateway;
    }
}
