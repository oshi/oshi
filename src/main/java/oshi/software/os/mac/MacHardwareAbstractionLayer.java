/*
 * Copyright (c) Alessandro Perucchi, 2014
 * alessandro[at]perucchi[dot]org
 * Daniel Widdis, 2015
 * widdis[at]gmail[dot]com
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.mac;

import java.util.ArrayList;
import java.util.List;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Memory;
import oshi.hardware.PowerSource;
import oshi.hardware.Processor;
import oshi.software.os.mac.local.CentralProcessor;
import oshi.software.os.mac.local.GlobalMemory;
import oshi.software.os.mac.local.MacPowerSource;
import oshi.software.os.mac.local.SystemB;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;

/**
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */
public class MacHardwareAbstractionLayer implements HardwareAbstractionLayer {

	private Processor[] _processors;
	private Memory _memory;
	private PowerSource[] _powerSources;

	/*
	 * (non-Javadoc)
	 * 
	 * @see oshi.hardware.HardwareAbstractionLayer#getProcessors()
	 */
	public Processor[] getProcessors() {
		if (_processors == null) {
			int nbCPU = 1;
			List<Processor> processors = new ArrayList<Processor>();
			int[] mib = { SystemB.CTL_HW, SystemB.HW_LOGICALCPU };
			com.sun.jna.Memory pNbCPU = new com.sun.jna.Memory(SystemB.INT_SIZE);
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, pNbCPU,
					new IntByReference(SystemB.INT_SIZE), null, 0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			nbCPU = pNbCPU.getInt(0);
			for (int i = 0; i < nbCPU; i++)
				processors.add(new CentralProcessor());

			_processors = processors.toArray(new Processor[0]);
		}
		return _processors;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see oshi.hardware.HardwareAbstractionLayer#getMemory()
	 */
	public Memory getMemory() {
		if (_memory == null) {
			_memory = new GlobalMemory();
		}
		return _memory;
	}

	@Override
	public PowerSource[] getPowerSources() {
		if (_powerSources == null) {
			_powerSources = MacPowerSource.getPowerSources();
		}
		return _powerSources;
	}

}
