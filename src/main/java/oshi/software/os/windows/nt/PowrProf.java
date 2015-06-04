/*
 * Copyright (c) Daniel Widdis, 2015
 * widdis[at]gmail[dot]com
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.windows.nt;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Power profile stats
 * 
 * @author widdis[at]gmail[dot]com
 */
public interface PowrProf extends Library {
	PowrProf INSTANCE = (PowrProf) Native.loadLibrary("PowrProf",
			PowrProf.class);

	public static int SYSTEM_BATTERY_STATE = 5;

	public static class SystemBatteryState extends Structure {
		public byte acOnLine; // boolean
		public byte batteryPresent; // boolean
		public byte charging; // boolean
		public byte discharging; // boolean
		public byte[] spare1 = new byte[4]; // unused
		public int maxCapacity; // unsigned 32 bit
		public int remainingCapacity; // unsigned 32 bit
		public int rate; // signed 32 bit
		public int estimatedTime; // signed 32 bit
		public int defaultAlert1; // unsigned 32 bit
		public int defaultAlert2; // unsigned 32 bit
	}

	int CallNtPowerInformation(int informationLevel, Pointer lpInputBuffer,
			NativeLong nInputBufferSize, Structure lpOutputBuffer,
			NativeLong nOutputBufferSize);
}
