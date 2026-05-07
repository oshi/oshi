/*
 * Copyright 2022-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo.jmx.api;

import java.io.IOException;

import javax.management.MBeanServer;

/**
 * Interface for the JMX OSHI agent.
 */
public interface JMXOshiAgent extends MBeanServer {
    /**
     * Starts the JMX agent.
     *
     * @throws IOException if an I/O error occurs
     */
    public void startAgent() throws IOException;

    /**
     * Stops the JMX agent.
     *
     * @throws IOException if an I/O error occurs
     */
    public void stopAgent() throws IOException;

}
