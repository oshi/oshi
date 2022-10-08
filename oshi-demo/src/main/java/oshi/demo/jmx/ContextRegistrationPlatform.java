/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo.jmx;

import oshi.SystemInfo;
import oshi.demo.jmx.api.StrategyRegistrationPlatformMBeans;
import javax.management.MBeanServer;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.MBeanRegistrationException;
import java.beans.IntrospectionException;

public class ContextRegistrationPlatform {
    private StrategyRegistrationPlatformMBeans strategyRegistrationContext;

    public void setStrategyRegistrationContext(StrategyRegistrationPlatformMBeans platformMBeans) {
        this.strategyRegistrationContext = platformMBeans;
    }

    public void registerMBeans(SystemInfo sisInfo, MBeanServer mBeanServer)
            throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException,
            MBeanRegistrationException, IntrospectionException, javax.management.IntrospectionException {
        this.strategyRegistrationContext.registerMBeans(sisInfo, mBeanServer);
    }
}
