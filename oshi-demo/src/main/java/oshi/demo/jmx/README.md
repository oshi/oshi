# How to use JMXOshiAgent

All you need to do is to call the next Factory method from the following class
  - Port: Specify the port desired in which you can run you server
  - Host: IP Address String formatted where the server is going to be found

```java
CreateJmxOshiAgent.createJmxOshiAgent(8888, "127.0.0.1");
```

As an alternative you can use the second factory method to pass the desired set of properties in form of `HashMap props`, this will allow you to configure you server as desired, from a custom implementation of `RMISocketFacotry` to the use of `TSL`. For more information about the different configuration around security please refer to [MX4J Security](http://mx4j.sourceforge.net/docs/ch03s10.html).

```java
HashMap props = ..
JMXOshiAgent oshiAgent = CreateJmxOshiAgent.createJmxOshiAgent(8888, "127.0.0.1", porps);
```
and all you have to do is

```java
oshiAgent.start();
```

## Client

For a client all we need is run the client in a different process as follows:

```java
JMXServiceURL address = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:8888/server");
```
where in this case `localhost` is where we deploy our host and the port is the one we had already defined.

```java
Map environment = null;
// Create the JMXCconnectorServer
JMXConnector cntor = JMXConnectorFactory.connect(address, environment);
// Obtain a "stub" for the remote MBeanServer
MBeanServerConnection mbsc = cntor.getMBeanServerConnection();
```
If we had set up our server with some props, we would need our client to have the same, also. If not, we can procced to use the `JMXConnectorFacory.connect()` and that will give us the remote `MBeanServerConnection` instance and with that we can just run the following code to retrieve the `Baseboard MBean`

```java
ObjectName objectQuery = new ObjectName("oshi:component=BaseBoard");
PropertiesAvailable mBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, objectQuery,
    PropertiesAvailable.class, false);

List<String> mBeanInfo = mBean.getProperties();

System.out.println(mbsc.getAttribute(objectQuery, mBeanInfo.get(1)));
System.out.println(mbsc.getAttribute(objectQuery, mBeanInfo.get(2)));
System.out.println(mbsc.getAttribute(objectQuery, mBeanInfo.get(3)));
System.out.println(mbsc.getAttribute(objectQuery, mBeanInfo.get(4)));
```
Note: Every MBean will implement the next interface `PropertiesAvailable` to offer the method `getProperties()` to get all the filed available in each `MBean`. Also this feature is not part of the core project, so that is why limited to only the `Baseboard MBean`
