/*
 * Copyright 2022-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo.jmx.strategiesplatform;

import java.beans.IntrospectionException;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import oshi.SystemInfo;
import oshi.demo.jmx.api.StrategyRegistrationPlatformMBeans;

/**
 * Windows-specific MBean registration strategy.
 */
public class WindowsStrategyRegistrattionPlatform implements StrategyRegistrationPlatformMBeans {

    /**
     * Default constructor.
     */
    public WindowsStrategyRegistrattionPlatform() {
    }

    @Override
    public void registerMBeans(SystemInfo systemInfo, MBeanServer mBeanServer)
            throws NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException,
            MalformedObjectNameException, IntrospectionException, javax.management.IntrospectionException {
        // here we can register all the MBeans reletad to windows. for this sample we
        // are only gonna register one MBean with two Attribute

        ObjectName objectName = new ObjectName("oshi:component=BaseBoard");
        oshi.demo.jmx.mbeans.Baseboard baseBoardMBean = new oshi.demo.jmx.mbeans.Baseboard(
                systemInfo.getHardware().getComputerSystem().getBaseboard());

        mBeanServer.registerMBean(baseBoardMBean, objectName);
    }
}
