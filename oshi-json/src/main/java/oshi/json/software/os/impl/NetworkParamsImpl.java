/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
