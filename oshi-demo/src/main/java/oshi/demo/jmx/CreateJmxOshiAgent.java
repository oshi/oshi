/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo.jmx;

import oshi.demo.jmx.api.JMXOshiAgent;

import java.util.HashMap;

public class CreateJmxOshiAgent {
    private static ContextRegistrationPlatform platform = new ContextRegistrationPlatform();

    public static JMXOshiAgent createJmxOshiAgent(Integer port, String host) throws Exception {
        return JMXOshiAgentServer.getInstance(host, port, null, platform);
    }

    public static JMXOshiAgent createJmxOshiAgent(Integer port, String host, HashMap properties) throws Exception {
        return JMXOshiAgentServer.getInstance(host, port, properties, platform);
    }
}
