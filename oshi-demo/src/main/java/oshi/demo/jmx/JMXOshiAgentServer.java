/*
 * Copyright 2022-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo.jmx;

import oshi.SystemInfo;
import oshi.demo.jmx.api.JMXOshiAgent;
import oshi.demo.jmx.api.StrategyRegistrationPlatformMBeans;
import oshi.demo.jmx.strategiesplatform.WindowsStrategyRegistrattionPlatform;

import javax.management.ObjectName;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.MBeanRegistrationException;

import java.io.IOException;

import java.rmi.registry.LocateRegistry;
import java.util.Map;

public class JMXOshiAgentServer extends JMXOshiAgentMBeanServer implements JMXOshiAgent {
    private String host;
    private Integer port;
    private Map properties;
    private JMXServiceURL address;
    private JMXConnectorServer cntorServer;
    private ContextRegistrationPlatform contextRegistrationPlatform;
    private static JMXOshiAgentServer jmxOshiAgentServer;

    private JMXOshiAgentServer(Integer port, String host, Map properties, ContextRegistrationPlatform platform)
            throws Exception {

        this.port = port;
        this.host = host;
        this.properties = properties;
        this.contextRegistrationPlatform = platform;

        this.initializeMbeanServer();
        this.initializeMBeans();
    }

    /* A singleton Method just to retrieve the instance of JMSOshiAgentServer */
    protected static JMXOshiAgentServer getInstance(String host, Integer port, Map properties,
            ContextRegistrationPlatform platform) throws Exception {
        if (jmxOshiAgentServer == null) {
            jmxOshiAgentServer = new JMXOshiAgentServer(port, host, properties, platform);
        }
        return jmxOshiAgentServer;
    }

    /*
     * This method is for initializing the connector server and bound the MBeanServer with the JMXConnector
     */
    private void initializeMbeanServer() throws IOException, NotCompliantMBeanException, InstanceAlreadyExistsException,
            MBeanRegistrationException, MalformedObjectNameException {

        if (LocateRegistry.getRegistry(this.port) != null)
            LocateRegistry.createRegistry(this.port);

        address = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + this.host + ":" + this.port + "/server");
        cntorServer = JMXConnectorServerFactory.newJMXConnectorServer(address, this.properties, null);
        server = MBeanServerFactory.createMBeanServer();
        ObjectName cntorServerName = ObjectName.getInstance("connectors:protocol=rmi");
        server.registerMBean(cntorServer, cntorServerName);
    }

    /*
     * This method implement a strategy pattern to choose which platform is and execute the corresponding strategy for
     * registering the corresponding MBeans
     */
    private void initializeMBeans() throws Exception {

        StrategyRegistrationPlatformMBeans platformMBeans = null;
        SystemInfo si = new SystemInfo();

        switch (SystemInfo.getCurrentPlatform()) {
        case WINDOWS:
            platformMBeans = new WindowsStrategyRegistrattionPlatform();
            contextRegistrationPlatform.setStrategyRegistrationContext(platformMBeans);
            break;
        default:
            System.out.println("Couldn't Initialize server ");
            throw new Exception("Server could not be initialized");
        }

        platformMBeans.registerMBeans(si, this.server);
    }

    @Override
    public void startAgent() throws IOException {
        cntorServer.start();
        System.out.println("Server Started");
    }

    public void stopAgent() throws IOException {
        cntorServer.stop();
    }
}
