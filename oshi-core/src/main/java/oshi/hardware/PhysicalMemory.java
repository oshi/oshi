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
        sb.append("\n Capacity: " + FormatUtil.formatBytes(getCapacity()));
        sb.append("\n Clock speed: " + FormatUtil.formatHertz(getClockSpeed()));
    	sb.append("\n Manufacturer: " + getManufacturer());
        sb.append("\n Memory type: " + getMemoryType());
        return sb.toString();
    }
}
