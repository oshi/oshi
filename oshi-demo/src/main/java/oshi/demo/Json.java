/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import oshi.SystemInfo;
import oshi.hardware.ComputerSystem;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.util.Util;

/**
 * Demonstrates the use of Jackson's ObjectMapper to create JSON from OSHI
 * objects
 */
public class Json {
    public static void main(String[] args) {
        // Jackson ObjectMapper
        ObjectMapper mapper = new ObjectMapper();

        // Fetch some OSHI objects
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();

        try {
            // Pretty print computer system
            System.out.println("JSON for ComputerSystem:");
            ComputerSystem cs = hal.getComputerSystem();
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(cs));

            // Print memory and then update it
            System.out.println("JSON for Memory:");
            GlobalMemory mem = hal.getMemory();
            System.out.println(mapper.writeValueAsString(mem));
            Util.sleep(1000);
            mem.updateAttributes();
            mem.getVirtualMemory().updateAttributes();
            System.out.println(mapper.writeValueAsString(mem));

        } catch (JsonProcessingException e) {
            // Rut roh...
            e.printStackTrace();
        }
    }
}
