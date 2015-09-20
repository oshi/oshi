/**
 * Oshi (https://github.com/dblock/oshi)
 * 
 * Copyright (c) 2010 - 2015 The Oshi Project Team
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.linux.proc;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;

import oshi.hardware.Processor;
import oshi.software.os.linux.Libc;
import oshi.software.os.linux.Libc.Sysinfo;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

/**
 * A CPU as defined in Linux /proc.
 * 
 * @author alessandro[at]perucchi[dot]org
 * @author alessio.fachechi[at]gmail[dot]com
 * @author widdis[at]gmail[dot]com
 */
@SuppressWarnings("restriction")
public class CentralProcessor implements Processor {
	private static final Logger LOG = LoggerFactory.getLogger(CentralProcessor.class);

	// Determine whether MXBean supports Oracle JVM methods
	private static final java.lang.management.OperatingSystemMXBean OS_MXBEAN = ManagementFactory
			.getOperatingSystemMXBean();;
	private static boolean sunMXBean;

	static {
		try {
			Class.forName("com.sun.management.OperatingSystemMXBean");
			// Initialize CPU usage
			((com.sun.management.OperatingSystemMXBean) OS_MXBEAN).getSystemCpuLoad();
			sunMXBean = true;
			LOG.debug("Oracle MXBean detected.");
		} catch (ClassNotFoundException e) {
			sunMXBean = false;
			LOG.debug("Oracle MXBean not detected.");
		}
	}

	// Maintain two sets of previous ticks to be used for calculating usage
	// between them.
	// System ticks (static)
	private static long tickTime = System.currentTimeMillis();
	private static long[] prevTicks = new long[4];
	private static long[] curTicks = new long[4];

	static {
		updateSystemTicks();
		System.arraycopy(curTicks, 0, prevTicks, 0, curTicks.length);
	}

	// Maintain similar arrays for per-processor ticks (class variables)
	private long procTickTime = System.currentTimeMillis();
	private long[] prevProcTicks = new long[4];
	private long[] curProcTicks = new long[4];

	// Initialize numCPU
	private static int numCPU = 0;

	static {
		try {
			List<String> procCpu = FileUtil.readFile("/proc/cpuinfo");
			for (String cpu : procCpu) {
				if (cpu.startsWith("processor")) {
					numCPU++;
				}
			}
		} catch (IOException e) {
			LOG.error("Problem with /proc/cpuinfo: {}", e.getMessage());
		}
		// Force at least one processor
		if (numCPU < 1)
			numCPU = 1;
	}

	// Set up array to maintain current ticks for rapid reference. This array
	// will be updated in place and used as a cache to avoid rereading file
	// while iterating processors
	private static long[][] allProcessorTicks = new long[numCPU][4];
	private static long allProcTickTime = 0;

	private int processorNumber;
	private String cpuVendor;
	private String cpuName;
	private String cpuIdentifier = null;
	private String cpuStepping;
	private String cpuModel;
	private String cpuFamily;
	private Long cpuVendorFreq = null;
	private Boolean cpu64;

	/**
	 * Create a Processor with the given number
	 * 
	 * @param procNo
	 *            The processor number
	 */
	public CentralProcessor(int procNo) {
		if (procNo >= numCPU)
			throw new IllegalArgumentException("Processor number (" + procNo
					+ ") must be less than the number of CPUs: " + numCPU);
		this.processorNumber = procNo;
		updateProcessorTicks();
		System.arraycopy(allProcessorTicks[processorNumber], 0, curProcTicks, 0, curProcTicks.length);
		LOG.debug("Initialized Processor {}", procNo);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getProcessorNumber() {
		return processorNumber;
	}

	/**
	 * Vendor identifier, eg. GenuineIntel.
	 * 
	 * @return Processor vendor.
	 */
	@Override
	public String getVendor() {
		return this.cpuVendor;
	}

	/**
	 * Set processor vendor.
	 * 
	 * @param vendor
	 *            Vendor.
	 */
	@Override
	public void setVendor(String vendor) {
		this.cpuVendor = vendor;
	}

	/**
	 * Name, eg. Intel(R) Core(TM)2 Duo CPU T7300 @ 2.00GHz
	 * 
	 * @return Processor name.
	 */
	@Override
	public String getName() {
		return this.cpuName;
	}

	/**
	 * Set processor name.
	 * 
	 * @param name
	 *            Name.
	 */
	@Override
	public void setName(String name) {
		this.cpuName = name;
	}

	/**
	 * Vendor frequency (in Hz), eg. for processor named Intel(R) Core(TM)2 Duo
	 * CPU T7300 @ 2.00GHz the vendor frequency is 2000000000.
	 * 
	 * @return Processor frequency or -1 if unknown.
	 */
	@Override
	public long getVendorFreq() {
		if (this.cpuVendorFreq == null) {
			Pattern pattern = Pattern.compile("@ (.*)$");
			Matcher matcher = pattern.matcher(getName());

			if (matcher.find()) {
				String unit = matcher.group(1);
				this.cpuVendorFreq = Long.valueOf(ParseUtil.parseHertz(unit));
			} else {
				this.cpuVendorFreq = Long.valueOf(-1L);
			}
		}

		return this.cpuVendorFreq.longValue();
	}

	/**
	 * Set vendor frequency.
	 * 
	 * @param freq
	 *            Frequency.
	 */
	@Override
	public void setVendorFreq(long freq) {
		this.cpuVendorFreq = Long.valueOf(freq);
	}

	/**
	 * Identifier, eg. x86 Family 6 Model 15 Stepping 10.
	 * 
	 * @return Processor identifier.
	 */
	@Override
	public String getIdentifier() {
		if (this.cpuIdentifier == null) {
			StringBuilder sb = new StringBuilder();
			if (getVendor().contentEquals("GenuineIntel"))
				sb.append(isCpu64bit() ? "Intel64" : "x86");
			else
				sb.append(getVendor());
			sb.append(" Family ");
			sb.append(getFamily());
			sb.append(" Model ");
			sb.append(getModel());
			sb.append(" Stepping ");
			sb.append(getStepping());
			this.cpuIdentifier = sb.toString();
		}
		return this.cpuIdentifier;
	}

	/**
	 * Set processor identifier.
	 * 
	 * @param identifier
	 *            Identifier.
	 */
	@Override
	public void setIdentifier(String identifier) {
		this.cpuIdentifier = identifier;
	}

	/**
	 * Is CPU 64bit?
	 * 
	 * @return True if cpu is 64bit.
	 */
	@Override
	public boolean isCpu64bit() {
		return this.cpu64.booleanValue();
	}

	/**
	 * Set flag is cpu is 64bit.
	 * 
	 * @param cpu64
	 *            True if cpu is 64.
	 */
	@Override
	public void setCpu64(boolean cpu64) {
		this.cpu64 = Boolean.valueOf(cpu64);
	}

	/**
	 * @return the stepping
	 */
	@Override
	public String getStepping() {
		return this.cpuStepping;
	}

	/**
	 * @param stepping
	 *            the stepping to set
	 */
	@Override
	public void setStepping(String stepping) {
		this.cpuStepping = stepping;
	}

	/**
	 * @return the model
	 */
	@Override
	public String getModel() {
		return this.cpuModel;
	}

	/**
	 * @param model
	 *            the model to set
	 */
	@Override
	public void setModel(String model) {
		this.cpuModel = model;
	}

	/**
	 * @return the family
	 */
	@Override
	public String getFamily() {
		return this.cpuFamily;
	}

	/**
	 * @param family
	 *            the family to set
	 */
	@Override
	public void setFamily(String family) {
		this.cpuFamily = family;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Deprecated
	public float getLoad() {
		// TODO Remove in 2.0
		return (float) getSystemCpuLoadBetweenTicks() * 100;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized double getSystemCpuLoadBetweenTicks() {
		// Check if > ~ 0.95 seconds since last tick count.
		long now = System.currentTimeMillis();
		LOG.trace("Current time: {}  Last tick time: {}", now, tickTime);
		boolean update = (now - tickTime > 950);
		if (update) {
			// Enough time has elapsed.
			// Update latest
			updateSystemTicks();
			tickTime = now;
		}
		// Calculate total
		long total = 0;
		for (int i = 0; i < curTicks.length; i++) {
			total += (curTicks[i] - prevTicks[i]);
		}
		// Calculate idle from last field [3]
		long idle = curTicks[3] - prevTicks[3];
		LOG.trace("Total ticks: {}  Idle ticks: {}", total, idle);

		// Copy latest ticks to earlier position for next call
		if (update) {
			System.arraycopy(curTicks, 0, prevTicks, 0, curTicks.length);
		}

		// return
		if (total > 0 && idle >= 0) {
			return (double) (total - idle) / total;
		}
		return 0d;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long[] getSystemCpuLoadTicks() {
		updateSystemTicks();
		// Make a copy
		long[] ticks = new long[curTicks.length];
		System.arraycopy(curTicks, 0, ticks, 0, curTicks.length);
		return ticks;
	}

	/**
	 * Updates system tick information from parsing /proc/stat. Array with four
	 * elements representing clock ticks or milliseconds (platform dependent)
	 * spent in User (0), Nice (1), System (2), and Idle (3) states. By
	 * measuring the difference between ticks across a time interval, CPU load
	 * over that interval may be calculated.
	 * 
	 * @return An array of 4 long values representing time spent in User,
	 *         Nice(if applicable), System, and Idle states.
	 */
	private static void updateSystemTicks() {
		LOG.trace("Updating System Ticks");
		// /proc/stat expected format
		// first line is overall user,nice,system,idle, etc.
		// cpu 3357 0 4313 1362393 ...
		String tickStr = "";
		try {
			List<String> procStat = FileUtil.readFile("/proc/stat");
			if (!procStat.isEmpty())
				tickStr = procStat.get(0);
		} catch (IOException e) {
			LOG.error("Problem with /proc/stat: {}", e.getMessage());
			return;
		}
		String[] tickArr = tickStr.split("\\s+");
		if (tickArr.length < 5)
			return;
		for (int i = 0; i < 4; i++) {
			curTicks[i] = Long.parseLong(tickArr[i + 1]);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getSystemCpuLoad() {
		if (sunMXBean) {
			return ((com.sun.management.OperatingSystemMXBean) OS_MXBEAN).getSystemCpuLoad();
		}
		return getSystemCpuLoadBetweenTicks();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getSystemLoadAverage() {
		return OS_MXBEAN.getSystemLoadAverage();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getProcessorCpuLoadBetweenTicks() {
		// Check if > ~ 0.95 seconds since last tick count.
		long now = System.currentTimeMillis();
		LOG.trace("Current time: {}  Last processor tick time: {}", now, procTickTime);
		if (now - procTickTime > 950) {
			// Enough time has elapsed. Update array in place
			updateProcessorTicks();
			// Copy arrays in place
			System.arraycopy(curProcTicks, 0, prevProcTicks, 0, curProcTicks.length);
			System.arraycopy(allProcessorTicks[processorNumber], 0, curProcTicks, 0, curProcTicks.length);
			procTickTime = now;
		}
		long total = 0;
		for (int i = 0; i < curProcTicks.length; i++) {
			total += (curProcTicks[i] - prevProcTicks[i]);
		}
		// Calculate idle from last field [3]
		long idle = curProcTicks[3] - prevProcTicks[3];
		LOG.trace("Total ticks: {}  Idle ticks: {}", total, idle);
		// update
		return (total > 0 && idle >= 0) ? (double) (total - idle) / total : 0d;
	}

	/**
	 * {@inheritDoc}
	 */
	public long[] getProcessorCpuLoadTicks() {
		updateProcessorTicks();
		return allProcessorTicks[processorNumber];
	}

	/**
	 * Updates the tick array for all processors if more than 100ms has elapsed
	 * since the last update. This permits using the allProcessorTicks as a
	 * cache when iterating over processors so that the /proc/stat file is only
	 * read once
	 */
	private static void updateProcessorTicks() {
		// Update no more frequently than 100ms so this is only triggered once
		// during iteration over Processors
		long now = System.currentTimeMillis();
		LOG.trace("Current time: {}  Last all processor tick time: {}", now, allProcTickTime);
		if (now - allProcTickTime < 100)
			return;

		// /proc/stat expected format
		// first line is overall user,nice,system,idle, etc.
		// cpu 3357 0 4313 1362393 ...
		// per-processor subsequent lines for cpu0, cpu1, etc.
		try {
			int cpu = 0;
			List<String> procStat = FileUtil.readFile("/proc/stat");
			for (String stat : procStat) {
				if (stat.startsWith("cpu") && !stat.startsWith("cpu ")) {
					String[] tickArr = stat.split("\\s+");
					if (tickArr.length < 5)
						break;
					for (int i = 0; i < 4; i++) {
						allProcessorTicks[cpu][i] = Long.parseLong(tickArr[i + 1]);
					}
					if (++cpu >= numCPU)
						break;
				}
			}
		} catch (IOException e) {
			LOG.error("Problem with /proc/stat: {}", e.getMessage());
		}
		allProcTickTime = now;
	}

	/**
	 * {@inheritDoc}
	 */
	public long getSystemUptime() {
		Sysinfo info = new Sysinfo();
		if (0 != Libc.INSTANCE.sysinfo(info)) {
			LOG.error("Failed to get system uptime. Error code: " + Native.getLastError());
			return 0L;
		}
		return info.uptime.longValue();
	}

	@Override
	public String toString() {
		return getName();
	}
}
