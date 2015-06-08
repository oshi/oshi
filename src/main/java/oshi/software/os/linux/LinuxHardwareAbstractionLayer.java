package oshi.software.os.linux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Memory;
import oshi.hardware.PowerSource;
import oshi.hardware.Processor;
import oshi.software.os.linux.proc.CentralProcessor;
import oshi.software.os.linux.proc.GlobalMemory;
import oshi.software.os.linux.proc.LinuxPowerSource;
import oshi.util.FileUtil;

/**
 * @author alessandro[at]perucchi[dot]org
 */

public class LinuxHardwareAbstractionLayer implements HardwareAbstractionLayer {

	private static final String SEPARATOR = "\\s+:\\s";

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
			List<Processor> processors = new ArrayList<Processor>();
			List<String> cpuInfo = null;
			try {
				cpuInfo = FileUtil.readFile("/proc/cpuinfo");
			} catch (IOException e) {
				System.err.println("Problem with: /proc/cpuinfo");
				System.err.println(e.getMessage());
				return null;
			}
			CentralProcessor cpu = null;
			for (String toBeAnalyzed : cpuInfo) {
				if (toBeAnalyzed.equals("")) {
					if (cpu != null) {
						processors.add(cpu);
					}
					cpu = null;
					continue;
				}
				if (cpu == null) {
					cpu = new CentralProcessor();
				}
				if (toBeAnalyzed.startsWith("model name\t")) {
					cpu.setName(toBeAnalyzed.split(SEPARATOR)[1]);
					continue;
				}
				if (toBeAnalyzed.startsWith("flags\t")) {
					String[] flags = toBeAnalyzed.split(SEPARATOR)[1]
							.split(" ");
					boolean found = false;
					for (String flag : flags) {
						if (flag.equalsIgnoreCase("LM")) {
							found = true;
							break;
						}
					}
					cpu.setCpu64(found);
					continue;
				}
				if (toBeAnalyzed.startsWith("cpu family\t")) {
					cpu.setFamily(toBeAnalyzed.split(SEPARATOR)[1]);
					continue;
				}
				if (toBeAnalyzed.startsWith("model\t")) {
					cpu.setModel(toBeAnalyzed.split(SEPARATOR)[1]);
					continue;
				}
				if (toBeAnalyzed.startsWith("stepping\t")) {
					cpu.setStepping(toBeAnalyzed.split(SEPARATOR)[1]);
					continue;
				}
				if (toBeAnalyzed.startsWith("vendor_id")) {
					cpu.setVendor(toBeAnalyzed.split(SEPARATOR)[1]);
					continue;
				}
			}
			if (cpu != null) {
				processors.add(cpu);
			}
			_processors = processors.toArray(new Processor[0]);
		}

		return _processors;
	}

	public PowerSource[] getPowerSources() {
		return LinuxPowerSource.getPowerSources();
	}

}
