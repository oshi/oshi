/*
 * Copyright 2022-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo.jmx.demo;

import oshi.demo.jmx.CreateJmxOshiAgent;
import oshi.demo.jmx.api.JMXOshiAgent;

/**
 * Demo JMX server for OSHI.
 */
public class OshiJMXServer {

    /**
     * Private constructor.
     */
    private OshiJMXServer() {
    }

    /**
     * Entry point.
     *
     * @param args command line arguments
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception {

        JMXOshiAgent oshiAgent = CreateJmxOshiAgent.createJmxOshiAgent(8888, "127.0.0.1");
        oshiAgent.startAgent();
    }
}
