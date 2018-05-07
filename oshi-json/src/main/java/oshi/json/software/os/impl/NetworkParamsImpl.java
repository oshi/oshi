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
package oshi.json.software.os.impl;

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import oshi.json.json.AbstractOshiJsonObject;
import oshi.json.json.NullAwareJsonObjectBuilder;
import oshi.json.software.os.NetworkParams;
import oshi.json.util.PropertiesUtil;

public class NetworkParamsImpl extends AbstractOshiJsonObject implements NetworkParams {

    private static final long serialVersionUID = 1L;

    private transient JsonBuilderFactory jsonFactory = Json.createBuilderFactory(null);

    private oshi.software.os.NetworkParams networkParams;

    /**
     * Creates a new platform-specific NetworkParams object wrapping the
     * provided argument
     *
     * @param networkParams
     *            a platform-specific NetworkParams object
     */
    public NetworkParamsImpl(oshi.software.os.NetworkParams networkParams) {
        this.networkParams = networkParams;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHostName() {
        return this.networkParams.getHostName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDomainName() {
        return this.networkParams.getDomainName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDnsServers() {
        return this.networkParams.getDnsServers();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpv4DefaultGateway() {
        return this.networkParams.getIpv4DefaultGateway();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpv6DefaultGateway() {
        return this.networkParams.getIpv6DefaultGateway();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject toJSON(Properties properties) {
        JsonObjectBuilder json = NullAwareJsonObjectBuilder.wrap(this.jsonFactory.createObjectBuilder());
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.networkParams.hostName")) {
            json.add("hostName", getHostName());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.networkParams.domainName")) {
            json.add("domainName", getDomainName());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.networkParams.dnsServers")) {
            JsonArrayBuilder nameServerArrayBuilder = this.jsonFactory.createArrayBuilder();
            for (String server : getDnsServers()) {
                nameServerArrayBuilder.add(server);
            }
            json.add("dnsServers", nameServerArrayBuilder.build());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.networkParams.ipv4DefaultGateway")) {
            json.add("ipv4DefaultGateway", getIpv4DefaultGateway());
        }
        if (PropertiesUtil.getBoolean(properties, "operatingSystem.networkParams.ipv6DefaultGateway")) {
            json.add("ipv6DefaultGateway", getIpv6DefaultGateway());
        }
        return json.build();
    }
}
