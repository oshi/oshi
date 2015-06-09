package oshi.software.os.windows;

import java.util.ArrayList;
import java.util.List;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Memory;
import oshi.hardware.PowerSource;
import oshi.hardware.Processor;
import oshi.software.os.windows.nt.CentralProcessor;
import oshi.software.os.windows.nt.GlobalMemory;
import oshi.software.os.windows.nt.WindowsPowerSource;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

public class WindowsHardwareAbstractionLayer implements
		HardwareAbstractionLayer {

	private Processor[] _processors = null;

	private Memory _memory = null;

	public Memory getMemory() {
		if (_memory == null) {
			_memory = new GlobalMemory();
		}
		return _memory;
	}

	public Processor[] getProcessors() {

		if (_processors == null) {
			final String cpuRegistryRoot = "HARDWARE\\DESCRIPTION\\System\\CentralProcessor";
			List<Processor> processors = new ArrayList<Processor>();
			String[] processorIds = Advapi32Util.registryGetKeys(
					WinReg.HKEY_LOCAL_MACHINE, cpuRegistryRoot);
			for (String processorId : processorIds) {
				String cpuRegistryPath = cpuRegistryRoot + "\\" + processorId;
				CentralProcessor cpu = new CentralProcessor();
				cpu.setIdentifier(Advapi32Util.registryGetStringValue(
						WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
						"Identifier"));
				cpu.setName(Advapi32Util.registryGetStringValue(
						WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
						"ProcessorNameString"));
				cpu.setVendor(Advapi32Util.registryGetStringValue(
						WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
						"VendorIdentifier"));
				processors.add(cpu);
			}
			_processors = processors.toArray(new Processor[0]);
		}

		return _processors;
	}

	public PowerSource[] getPowerSources() {
		return WindowsPowerSource.getPowerSources();
	}

}
