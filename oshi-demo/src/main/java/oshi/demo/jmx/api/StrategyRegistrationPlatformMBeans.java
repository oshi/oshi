/*
 * Copyright 2022 The OSHI Project Contributors
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

public interface StrategyRegistrationPlatformMBeans {
    void registerMBeans(SystemInfo systemInfo, MBeanServer mBeanServer)
            throws NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException,
            MalformedObjectNameException, IntrospectionException, javax.management.IntrospectionException;
}
