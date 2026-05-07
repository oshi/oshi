/*
 * Copyright 2022-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo.jmx.api;

import java.beans.IntrospectionException;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

import oshi.SystemInfo;

/**
 * Strategy for registering platform-specific MBeans.
 */
public interface StrategyRegistrationPlatformMBeans {
    /**
     * Registers MBeans for the given system info.
     *
     * @param systemInfo  the system info
     * @param mBeanServer the MBean server
     * @throws NotCompliantMBeanException              if MBean is not compliant
     * @throws InstanceAlreadyExistsException          if instance exists
     * @throws MBeanRegistrationException              if registration fails
     * @throws MalformedObjectNameException            if object name is malformed
     * @throws IntrospectionException                  if introspection fails
     * @throws javax.management.IntrospectionException if JMX introspection fails
     */
    void registerMBeans(SystemInfo systemInfo, MBeanServer mBeanServer)
            throws NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException,
            MalformedObjectNameException, IntrospectionException, javax.management.IntrospectionException;
}
