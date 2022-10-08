/*
 * Copyright 2019-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

/**
 * Demonstrates the use of Jackson's ObjectMapper to create JSON from OSHI objects
 */
public class Json {
    /**
     * <p>
     * main.
     * </p>
     *
     * @param args an array of {@link java.lang.String} objects.
     */
    public static void main(String[] args) {
        // Jackson ObjectMapper
        ObjectMapper mapper = new ObjectMapper();

        // Fetch some OSHI objects
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();

        try {
            // Pretty print computer system
            System.out.println("JSON for CPU:");
            CentralProcessor cpu = hal.getProcessor();
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(cpu));

            // Print memory
            System.out.println("JSON for Memory:");
            GlobalMemory mem = hal.getMemory();
            System.out.println(mapper.writeValueAsString(mem));

        } catch (JsonProcessingException e) {
            System.out.println("Exception encountered: " + e.getMessage());
        }
    }
}
