/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo.jmx.demo;

import oshi.demo.jmx.CreateJmxOshiAgent;
import oshi.demo.jmx.api.JMXOshiAgent;

public class OshiJMXServer {
    public static void main(String[] args) throws Exception {

        JMXOshiAgent oshiAgent = CreateJmxOshiAgent.createJmxOshiAgent(8888, "127.0.0.1");
        oshiAgent.startAgent();
    }
}
