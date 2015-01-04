package oshi.software.os.linux;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Memory;
import oshi.hardware.Processor;
import oshi.software.os.linux.proc.CentralProcessor;
import oshi.software.os.linux.proc.GlobalMemory;
import oshi.util.ExecutingCommand;
import oshi.util.FormatUtil;

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

	public float getProcessorLoad() {
		Scanner in = null;
		try {
			in = new Scanner(new FileReader("/proc/stat"));
		} catch (FileNotFoundException e) {
			System.err.println("Problem with: /proc/stat");
			System.err.println(e.getMessage());
			return -1;
		}
		in.useDelimiter("\n");
		String[] result = in.next().split(" ");
		ArrayList<Float> loads = new ArrayList<Float>();
		for (String load : result) {
			if (load.matches("-?\\d+(\\.\\d+)?")) {
				loads.add(Float.valueOf(load));
			}
		}
		// ((Total-PrevTotal)-(Idle-PrevIdle))/(Total-PrevTotal) - see http://stackoverflow.com/a/23376195/4359897
		float totalCpuLoad = (loads.get(0) + loads.get(2))*100 / (loads.get(0) + loads.get(2) + loads.get(3));
		return FormatUtil.round(totalCpuLoad, 2);
	}

	public Processor[] getProcessors() {

		if (_processors == null) {
			List<Processor> processors = new ArrayList<Processor>();
			Scanner in = null;
			try {
				in = new Scanner(new FileReader("/proc/cpuinfo"));
			} catch (FileNotFoundException e) {
				System.err.println("Problem with: /proc/cpuinfo");
				System.err.println(e.getMessage());
				return null;
			}
			in.useDelimiter("\n");
			CentralProcessor cpu = null;
			while (in.hasNext()) {
				String toBeAnalyzed = in.next();
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
					cpu.setName(toBeAnalyzed.split(SEPARATOR)[1]); // model
																	// name
					continue;
				}
				if (toBeAnalyzed.startsWith("flags\t")) {
					String[] flags=toBeAnalyzed.split(SEPARATOR)[1].split(" ");
					boolean found=false;
					for (String flag: flags) {
						if (flag.equalsIgnoreCase("LM")) {
							found=true;
							break;
						}
					}
					cpu.setCpu64(found);
					continue;
				}
				if (toBeAnalyzed.startsWith("cpu family\t")) {
					cpu.setFamily(toBeAnalyzed.split(SEPARATOR)[1]); // model
																	// name
					continue;
				}
				if (toBeAnalyzed.startsWith("model\t")) {
					cpu.setModel(toBeAnalyzed.split(SEPARATOR)[1]); // model
																	// name
					continue;
				}
				if (toBeAnalyzed.startsWith("stepping\t")) {
					cpu.setStepping(toBeAnalyzed.split(SEPARATOR)[1]); // model
																	// name
					continue;
				}
				if (toBeAnalyzed.startsWith("vendor_id")) {
					cpu.setVendor(toBeAnalyzed.split(SEPARATOR)[1]); // vendor_id
					continue;
				}
			}
			in.close();
			if (cpu != null) {
				processors.add(cpu);
			}
			_processors = processors.toArray(new Processor[0]);
		}

		return _processors;
	}

}
