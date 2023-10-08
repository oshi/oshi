/*
 * Copyright 2022-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo.jmx;

import java.util.Map;

import oshi.demo.jmx.api.JMXOshiAgent;

public class CreateJmxOshiAgent {
    private static ContextRegistrationPlatform platform = new ContextRegistrationPlatform();

    public static JMXOshiAgent createJmxOshiAgent(Integer port, String host) throws Exception {
        return JMXOshiAgentServer.getInstance(host, port, null, platform);
    }

    public static JMXOshiAgent createJmxOshiAgent(Integer port, String host, Map<String, ?> properties)
            throws Exception {
        return JMXOshiAgentServer.getInstance(host, port, properties, platform);
    }
}
