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
    public static void main(String[] args) throws IOException, MalformedObjectNameException, ReflectionException, InstanceNotFoundException, MBeanException, AttributeNotFoundException {

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
        PropertiesAvailable mBean =   MBeanServerInvocationHandler.newProxyInstance(mbsc,objectQuery,PropertiesAvailable.class,false);

        List<String> mBeanInfo = mBean.getProperties();

        System.out.println ( mbsc.getAttribute(objectQuery,mBeanInfo.get(1)));
        System.out.println ( mbsc.getAttribute(objectQuery,mBeanInfo.get(2)));
        System.out.println ( mbsc.getAttribute(objectQuery,mBeanInfo.get(3)));
        System.out.println ( mbsc.getAttribute(objectQuery,mBeanInfo.get(4)));

    }
}
