/**
 * 
 */
package oshi.software.os.mac;

import java.util.ArrayList;
import java.util.List;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Memory;
import oshi.hardware.Processor;
import oshi.software.os.mac.local.CentralProcessor;
import oshi.software.os.mac.local.GlobalMemory;
import oshi.util.ExecutingCommand;

/**
 * @author alessandro[at]perucchi[dot]org
 */

public class MacHardwareAbstractionLayer implements HardwareAbstractionLayer {

	private Processor[] _processors;
	private Memory _memory;

	/*
	 * (non-Javadoc)
	 * 
	 * @see oshi.hardware.HardwareAbstractionLayer#getProcessors()
	 */
	public Processor[] getProcessors() {
		if (_processors == null) {
			List<Processor> processors = new ArrayList<Processor>();
			int nbCPU = new Integer(
					ExecutingCommand.getFirstAnswer("sysctl -n hw.availcpu"));
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

}
