/*
 * MIT License
 *
 * Copyright (c) 2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.demo.jmx.strategiesplatform;

import oshi.SystemInfo;
import oshi.demo.jmx.api.StrategyRegistrationPlatformMBeans;

import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import java.beans.IntrospectionException;

public class WindowsStrategyRegistrattionPlatform implements StrategyRegistrationPlatformMBeans {
    @Override
    public void registerMBeans(SystemInfo systemInfo, MBeanServer mBeanServer) throws NotCompliantMBeanException,
        InstanceAlreadyExistsException, MBeanRegistrationException, MalformedObjectNameException, IntrospectionException, javax.management.IntrospectionException {
        // here we can register all the MBeans reletad to windows. for this sample we
        // are only gonna register one MBean with two Attribute

        ObjectName objectName = new ObjectName("oshi:component=BaseBoard");
        oshi.demo.jmx.mbeans.Baseboard baseBoardMBean = new oshi.demo.jmx.mbeans.Baseboard(systemInfo.getHardware().getComputerSystem().getBaseboard());

        mBeanServer.registerMBean(baseBoardMBean, objectName);
    }
}
