/*
 * Copyright 2022-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo.jmx;

import java.beans.IntrospectionException;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

import oshi.SystemInfo;
import oshi.demo.jmx.api.StrategyRegistrationPlatformMBeans;

/**
 * Registers platform MBeans using a configured strategy.
 */
public class ContextRegistrationPlatform {

    /**
     * Default constructor.
     */
    public ContextRegistrationPlatform() {
    }

    private StrategyRegistrationPlatformMBeans strategyRegistrationContext;

    /**
     * Sets the platform MBean registration strategy.
     *
     * @param platformMBeans the strategy to use
     */
    public void setStrategyRegistrationContext(StrategyRegistrationPlatformMBeans platformMBeans) {
        this.strategyRegistrationContext = platformMBeans;
    }

    /**
     * Registers MBeans using the configured strategy.
     *
     * @param sisInfo     the system info
     * @param mBeanServer the MBean server
     * @throws javax.management.MalformedObjectNameException   if object name is malformed
     * @throws javax.management.NotCompliantMBeanException     if MBean is not compliant
     * @throws javax.management.InstanceAlreadyExistsException if instance exists
     * @throws javax.management.MBeanRegistrationException     if registration fails
     * @throws java.beans.IntrospectionException               if introspection fails
     * @throws javax.management.IntrospectionException         if JMX introspection fails
     */
    public void registerMBeans(SystemInfo sisInfo, MBeanServer mBeanServer)
            throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException,
            MBeanRegistrationException, IntrospectionException, javax.management.IntrospectionException {
        this.strategyRegistrationContext.registerMBeans(sisInfo, mBeanServer);
    }
}
