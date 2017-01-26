/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
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
package oshi.software.os.mac;

import java.util.List;

import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;

public class MacNetworkParams extends AbstractNetworkParams{
    private final String IPV6_ROUTE_HEADER = "Internet6:";

    private final String DEFAULT_GATEWAY = "default";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDomainName() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDnsServers() {
        return new String[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpv4DefaultGateway(){
        return searchGateway(ExecutingCommand.runNative("route -n get default"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpv6DefaultGateway() {
        List<String> lines = ExecutingCommand.runNative("netstat -nr");
        boolean v6Table = false;
        for(String line: lines){
            if(v6Table && line.startsWith(DEFAULT_GATEWAY)){
                String[] fields = line.split("[ \t#;]");
                if(fields[2].contains("G")) {
                    return fields[1].split("%")[0];
                }
            } else if(line.startsWith(IPV6_ROUTE_HEADER)){
                v6Table = true;
            }
        }
        return "";
    }
}
