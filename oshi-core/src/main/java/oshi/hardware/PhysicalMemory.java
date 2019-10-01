package oshi.hardware;

import java.util.HashMap;
import java.util.Map;

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
     * The capacity of memory bank
     * 
     * @return the capacity
     */
    private final long capacity;
    
    /*
     * The configured memory clock speed in Hertz
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
    
    /*
     * Mapping from integer memory type to actual String value
     */
    private static Map<Integer,String> memoryTypeMapping;
    
    public PhysicalMemory(String bankLabel,long capacity,long clockSpeed,String manufacturer,int memoryType){
	this.bankLabel = bankLabel;
	this.capacity = capacity;
	this.clockSpeed = clockSpeed;
	this.manufacturer = manufacturer;
	memoryTypeMapping = new HashMap<>();
	memoryTypeMapping.put(0, "Unknown");
	memoryTypeMapping.put(1, "Other");
	memoryTypeMapping.put(2, "DRAM");
	memoryTypeMapping.put(3, "Synchronous DRAM");
	memoryTypeMapping.put(4, "Cache DRAM");
	memoryTypeMapping.put(5, "EDO");
	memoryTypeMapping.put(6, "EDRAM");
	memoryTypeMapping.put(7, "VRAM");
	memoryTypeMapping.put(8, "SRAM");
	memoryTypeMapping.put(9, "RAM");
	memoryTypeMapping.put(10, "ROM");
	memoryTypeMapping.put(11, "Flash");
	memoryTypeMapping.put(12, "EEPROM");
	memoryTypeMapping.put(13, "FEPROM");
	memoryTypeMapping.put(14, "EPROM");
	memoryTypeMapping.put(15, "CDRAM");
	memoryTypeMapping.put(16, "3DRAM");
	memoryTypeMapping.put(17, "SDRAM");
	memoryTypeMapping.put(18, "SGRAM");
	memoryTypeMapping.put(19, "RDRAM");
	memoryTypeMapping.put(20, "DDR");
	memoryTypeMapping.put(21, "DDR2");
	memoryTypeMapping.put(22, "DDR2-FB-DIMM");
	memoryTypeMapping.put(24, "DDR3");
	memoryTypeMapping.put(25, "FBD2");
	this.memoryType = convertMemoryTypeToString(memoryType);
    }
    
    public String getBankLabel() {
        return bankLabel;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getClockSpeed() {
        return clockSpeed;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getMemoryType() {
        return memoryType;
    }
    
    public static String convertMemoryTypeToString(int memoryType) {
	if(memoryTypeMapping.get(memoryType) != null)
	    return memoryTypeMapping.get(memoryType);
	return "Unknown";
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
