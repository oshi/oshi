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
package oshi.hardware;

import oshi.util.FormatUtil;

/**
 * The PhysicalMemory class represents a physical memory device located on a
 * computer system and available to the operating system.
 */
public class PhysicalMemory {
    /*
     * The bank or slots label
     * 
     * @return the bank label
     */
    private final String bankLabel;
    
    /*
     * The capacity of memory bank in bytes
     * 
     * @return the capacity
     */
    private final long capacity;
    
    /*
     * The configured memory clock speed in  mega Hertz
     * 
     * @return the clock speed
     */
    private final long clockSpeed;
    
    /*
     * The manufacturer of the physical memory
     * 
     * @return the manufacturer
     */
    private final String manufacturer;
    
    /*
     * The type of physical memory
     * 
     * @return the memory type
     */
    private final String memoryType;
    
    public PhysicalMemory(String bankLabel,long capacity,long clockSpeed,String manufacturer,String memoryType){
	this.bankLabel = bankLabel;
	this.capacity = capacity;
	this.clockSpeed = clockSpeed;
	this.manufacturer = manufacturer;
	this.memoryType = memoryType;
    }
    
    public String getBankLabel() {
        return bankLabel;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getClockSpeed() {
        return clockSpeed * 1000000L;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getMemoryType() {
        return memoryType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" Bank label: " + getBankLabel());
        sb.append(", Capacity: " + FormatUtil.formatBytes(getCapacity()));
        sb.append(", Clock speed: " + FormatUtil.formatHertz(getClockSpeed()));
    	sb.append(", Manufacturer: " + getManufacturer());
        sb.append(", Memory type: " + getMemoryType());
        return sb.toString();
    }
}
