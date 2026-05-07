/*
 * Copyright 2022-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo.jmx;

import java.util.Map;

import oshi.demo.jmx.api.JMXOshiAgent;

/**
 * Factory for creating JMX OSHI agents.
 */
public class CreateJmxOshiAgent {

    /**
     * Private constructor.
     */
    private CreateJmxOshiAgent() {
    }

    private static ContextRegistrationPlatform platform = new ContextRegistrationPlatform();

    /**
     * Creates a JMX OSHI agent.
     *
     * @param port the port
     * @param host the host
     * @return the agent
     * @throws Exception if creation fails
     */
    public static JMXOshiAgent createJmxOshiAgent(Integer port, String host) throws Exception {
        return JMXOshiAgentServer.getInstance(host, port, null, platform);
    }

    /**
     * Creates a JMX OSHI agent with custom properties.
     *
     * @param port       the port
     * @param host       the host
     * @param properties the JMX properties
     * @return the agent
     * @throws Exception if creation fails
     */
    public static JMXOshiAgent createJmxOshiAgent(Integer port, String host, Map<String, ?> properties)
            throws Exception {
        return JMXOshiAgentServer.getInstance(host, port, properties, platform);
    }
}
