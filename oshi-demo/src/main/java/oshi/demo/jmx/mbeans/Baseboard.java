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
package oshi.demo.jmx.mbeans;

import oshi.demo.jmx.demo.PropertiesAvailable;

import javax.management.DynamicMBean;
import javax.management.MBeanInfo;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.AttributeList;
import javax.management.Attribute;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Baseboard implements DynamicMBean,PropertiesAvailable{

    private oshi.hardware.Baseboard  baseboard;
    private MBeanInfo dMBeanInfo = null;
    private List<String> propertiesAvailable = new ArrayList<>();
    private final String  PROPERTIES = "Properties";
    private final String  GET_PREFIX = "get";
    private void setUpMBean() throws IntrospectionException, javax.management.IntrospectionException {

        PropertyDescriptor[] methods = Introspector.getBeanInfo(baseboard.getClass()).getPropertyDescriptors();
        MBeanAttributeInfo[] attributeInfos = new MBeanAttributeInfo[methods.length];

        for(int i = 0; i < methods.length; i++){
            attributeInfos[i] = new MBeanAttributeInfo(methods[i].getName(),methods[i].getShortDescription(),methods[i].getReadMethod(),null);
            propertiesAvailable.add(methods[i].getName().substring(0,1).toUpperCase() + methods[i].getName().substring(1));
        }

        dMBeanInfo = new MBeanInfo(baseboard.getClass().getName(), null, attributeInfos, null, null,
            new MBeanNotificationInfo[0]);
    }

    public Baseboard(oshi.hardware.Baseboard baseboard) throws IntrospectionException, javax.management.IntrospectionException {
        this.baseboard = baseboard;
        this.setUpMBean();
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {

        if(attribute.equals( PROPERTIES)){
            return this.getProperties();
        }

        Method method = null;
        try {
            method = baseboard.getClass().getDeclaredMethod(GET_PREFIX + attribute, null);
            method.setAccessible(true);
            return method.invoke(baseboard,null);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public MBeanInfo getMBeanInfo() {
        return dMBeanInfo;
    }

    @Override
    public List<String> getProperties() {
        return propertiesAvailable;
    }
    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {

    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        return null;
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        return null;
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        return null;
    }

}
