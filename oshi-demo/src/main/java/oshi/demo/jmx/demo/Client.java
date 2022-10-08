/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo.jmx.demo;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Client {
    public static void main(String[] args) throws IOException, MalformedObjectNameException, ReflectionException,
            InstanceNotFoundException, MBeanException, AttributeNotFoundException {

        // The address of the connector server
        JMXServiceURL address = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:8888/server");
        // Map for custom properties key values
        Map environment = null;
        // Create the JMXCconnectorServer
        JMXConnector cntor = JMXConnectorFactory.connect(address, environment);
        // Obtain a "stub" for the remote MBeanServer
        MBeanServerConnection mbsc = cntor.getMBeanServerConnection();

        // Here we can obtain all the domains and there would be one calle oshi, meaning
        // that is the one that we registered into our MbeanServer releated with the
        // baseboard
        ObjectName objectQuery = new ObjectName("oshi:component=BaseBoard");
        PropertiesAvailable mBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, objectQuery,
                PropertiesAvailable.class, false);

        List<String> mBeanInfo = mBean.getProperties();

        System.out.println(mbsc.getAttribute(objectQuery, mBeanInfo.get(1)));
        System.out.println(mbsc.getAttribute(objectQuery, mBeanInfo.get(2)));
        System.out.println(mbsc.getAttribute(objectQuery, mBeanInfo.get(3)));
        System.out.println(mbsc.getAttribute(objectQuery, mBeanInfo.get(4)));

    }
}
