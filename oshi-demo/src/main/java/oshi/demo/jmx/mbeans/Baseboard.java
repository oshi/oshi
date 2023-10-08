/*
 * Copyright 2022-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo.jmx.mbeans;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.ReflectionException;

import oshi.demo.jmx.demo.PropertiesAvailable;

public class Baseboard implements DynamicMBean, PropertiesAvailable {

    private oshi.hardware.Baseboard baseboard;
    private MBeanInfo dMBeanInfo = null;
    private List<String> propertiesAvailable = new ArrayList<>();
    private final String PROPERTIES = "Properties";

    private void setUpMBean() throws IntrospectionException, javax.management.IntrospectionException {

        PropertyDescriptor[] methods = Introspector.getBeanInfo(baseboard.getClass()).getPropertyDescriptors();
        MBeanAttributeInfo[] attributeInfos = new MBeanAttributeInfo[methods.length];

        for (int i = 0; i < methods.length; i++) {
            attributeInfos[i] = new MBeanAttributeInfo(methods[i].getName(), methods[i].getShortDescription(),
                    methods[i].getReadMethod(), null);
            propertiesAvailable.add(
                    methods[i].getName().substring(0, 1).toUpperCase(Locale.ROOT) + methods[i].getName().substring(1));
        }

        dMBeanInfo = new MBeanInfo(baseboard.getClass().getName(), null, attributeInfos, null, null,
                new MBeanNotificationInfo[0]);
    }

    public Baseboard(oshi.hardware.Baseboard baseboard)
            throws IntrospectionException, javax.management.IntrospectionException {
        this.baseboard = baseboard;
        this.setUpMBean();
    }

    @Override
    public Object getAttribute(String attribute) {
        switch (attribute) {
        case PROPERTIES:
            return this.getProperties();
        case "Manufacturer":
            return baseboard.getManufacturer();
        case "Model":
            return baseboard.getModel();
        case "Version":
            return baseboard.getVersion();
        case "SerialNumber":
            return baseboard.getSerialNumber();
        default:
            throw new IllegalArgumentException("No attribute " + attribute);
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
    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {

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
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        return null;
    }

}
