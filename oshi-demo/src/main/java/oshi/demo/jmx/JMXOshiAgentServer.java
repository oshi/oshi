/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo.jmx;

import oshi.SystemInfo;
import oshi.demo.jmx.api.JMXOshiAgent;
import oshi.demo.jmx.api.StrategyRegistrationPlatformMBeans;
import oshi.demo.jmx.strategiesplatform.WindowsStrategyRegistrattionPlatform;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ObjectInstance;
import javax.management.MBeanServerFactory;
import javax.management.ReflectionException;
import javax.management.MBeanException;
import javax.management.QueryExp;
import javax.management.AttributeNotFoundException;
import javax.management.AttributeList;
import javax.management.Attribute;
import javax.management.InvalidAttributeValueException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.IntrospectionException;
import javax.management.MBeanInfo;
import javax.management.ListenerNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.OperationsException;
import javax.management.loading.ClassLoaderRepository;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.MBeanRegistrationException;

import java.io.IOException;
import java.io.ObjectInputStream;

import java.rmi.registry.LocateRegistry;
import java.util.Map;
import java.util.Set;

public class JMXOshiAgentServer implements JMXOshiAgent {
    private String host;
    private Integer port;
    private Map properties;

    private MBeanServer server;
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

        this.initilizeMbeanServer();
        this.initilizeMBeans();
    }

    /* A singlenton Method just to retrive the instance of JMSOshiAgentServer */
    protected static JMXOshiAgentServer getInstance(String host, Integer port, Map properties,
            ContextRegistrationPlatform platform) throws Exception {
        if (jmxOshiAgentServer == null) {
            jmxOshiAgentServer = new JMXOshiAgentServer(port, host, properties, platform);
        }
        return jmxOshiAgentServer;
    }

    /*
     * This method is for initilizing the connector server and bound the MBeanServer with the JMXConnector
     */
    private void initilizeMbeanServer() throws IOException, NotCompliantMBeanException, InstanceAlreadyExistsException,
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
     * This method implmenet an startegy pattern to choose wich platform is and execute the corresponding strategy for
     * registering the corresponding MBeans
     */
    private void initilizeMBeans() throws Exception {

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

    @Override
    public void stopAgent() throws IOException {
        cntorServer.stop();
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
        return null;
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
            NotCompliantMBeanException, InstanceNotFoundException {
        return null;
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
            NotCompliantMBeanException {
        return null;
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params,
            String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
            MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        return null;
    }

    @Override
    public ObjectInstance registerMBean(Object object, ObjectName name)
            throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        return server.registerMBean(object, name);
    }

    @Override
    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {

    }

    @Override
    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        return null;
    }

    @Override
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        return null;
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        return null;
    }

    @Override
    public boolean isRegistered(ObjectName name) {
        return false;
    }

    @Override
    public Integer getMBeanCount() {
        return null;
    }

    @Override
    public Object getAttribute(ObjectName name, String attribute)
            throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        return null;
    }

    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes)
            throws InstanceNotFoundException, ReflectionException {
        return null;
    }

    @Override
    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException,
            AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {

    }

    @Override
    public AttributeList setAttributes(ObjectName name, AttributeList attributes)
            throws InstanceNotFoundException, ReflectionException {
        return null;
    }

    @Override
    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature)
            throws InstanceNotFoundException, MBeanException, ReflectionException {
        return null;
    }

    @Override
    public String getDefaultDomain() {
        return null;
    }

    @Override
    public String[] getDomains() {
        return new String[0];
    }

    @Override
    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter,
            Object handback) throws InstanceNotFoundException {

    }

    @Override
    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter,
            Object handback) throws InstanceNotFoundException {

    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener)
            throws InstanceNotFoundException, ListenerNotFoundException {

    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter,
            Object handback) throws InstanceNotFoundException, ListenerNotFoundException {

    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener)
            throws InstanceNotFoundException, ListenerNotFoundException {

    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter,
            Object handback) throws InstanceNotFoundException, ListenerNotFoundException {

    }

    @Override
    public MBeanInfo getMBeanInfo(ObjectName name)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        return null;
    }

    @Override
    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
        return false;
    }

    @Override
    public Object instantiate(String className) throws ReflectionException, MBeanException {
        return null;
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName)
            throws ReflectionException, MBeanException, InstanceNotFoundException {
        return null;
    }

    @Override
    public Object instantiate(String className, Object[] params, String[] signature)
            throws ReflectionException, MBeanException {
        return null;
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature)
            throws ReflectionException, MBeanException, InstanceNotFoundException {
        return null;
    }

    @Override
    public ObjectInputStream deserialize(ObjectName name, byte[] data)
            throws InstanceNotFoundException, OperationsException {
        return null;
    }

    @Override
    public ObjectInputStream deserialize(String className, byte[] data)
            throws OperationsException, ReflectionException {
        return null;
    }

    @Override
    public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data)
            throws InstanceNotFoundException, OperationsException, ReflectionException {
        return null;
    }

    @Override
    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        return null;
    }

    @Override
    public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
        return null;
    }

    @Override
    public ClassLoaderRepository getClassLoaderRepository() {
        return null;
    }
}
