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
package oshi.software.os.mac.local;

import java.lang.management.ManagementFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.hardware.Processor;
import oshi.software.os.mac.local.SystemB.HostCpuLoadInfo;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;

import com.sun.jna.LastErrorException;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * A CPU.
 * 
 * @author alessandro[at]perucchi[dot]org
 * @author alessio.fachechi[at]gmail[dot]com
 * @author widdis[at]gmail[dot]com
 */
@SuppressWarnings("restriction")
public class CentralProcessor implements Processor {
	private static final java.lang.management.OperatingSystemMXBean OS_MXBEAN = ManagementFactory
			.getOperatingSystemMXBean();;
	private static boolean sunMXBean;
	static {
		try {
			Class.forName("com.sun.management.OperatingSystemMXBean");
			// Initialize CPU usage
			((com.sun.management.OperatingSystemMXBean) OS_MXBEAN)
					.getSystemCpuLoad();
			sunMXBean = true;
		} catch (ClassNotFoundException e) {
			sunMXBean = false;
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
		IntByReference size = new IntByReference(SystemB.INT_SIZE);
		Pointer p = new Memory(size.getValue());
		if (0 != SystemB.INSTANCE.sysctlbyname("hw.logicalcpu", p, size, null,
				0))
			throw new LastErrorException("Error code: " + Native.getLastError());
		numCPU = p.getInt(0);
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
	 */
	public CentralProcessor(int procNo) {
		if (procNo >= numCPU)
			throw new IllegalArgumentException("Processor number (" + procNo
					+ ") must be less than the number of CPUs: " + numCPU);
		this.processorNumber = procNo;
		updateProcessorTicks();
		System.arraycopy(allProcessorTicks[processorNumber], 0, curProcTicks,
				0, curProcTicks.length);
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
		if (this.cpuVendor == null) {
			int[] mib = { SystemB.CTL_MACHDEP, SystemB.MACHDEP_CPU,
					SystemB.MACHDEP_CPU_VENDOR };
			IntByReference size = new IntByReference();
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, null, size, null,
					0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			Pointer p = new Memory(size.getValue() + 1);
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, p, size, null, 0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			this.cpuVendor = p.getString(0);
		}
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
		if (this.cpuName == null) {
			int[] mib = { SystemB.CTL_MACHDEP, SystemB.MACHDEP_CPU,
					SystemB.MACHDEP_CPU_BRAND_STRING };
			IntByReference size = new IntByReference();
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, null, size, null,
					0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			Pointer p = new Memory(size.getValue() + 1);
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, p, size, null, 0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			this.cpuName = p.getString(0);
		}
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
			sb.append(" Family ").append(getFamily());
			sb.append(" Model ").append(getModel());
			sb.append(" Stepping ").append(getStepping());
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
		if (this.cpu64 == null) {
			int[] mib = { SystemB.CTL_HW, SystemB.HW_CPU64BIT_CAPABLE };
			IntByReference size = new IntByReference(SystemB.INT_SIZE);
			Pointer p = new Memory(size.getValue());
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, p, size, null, 0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			this.cpu64 = p.getInt(0) != 0;
		}
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
		if (this.cpuStepping == null) {
			int[] mib = { SystemB.CTL_MACHDEP, SystemB.MACHDEP_CPU,
					SystemB.MACHDEP_CPU_STEPPING };
			IntByReference size = new IntByReference(SystemB.INT_SIZE);
			Pointer p = new Memory(size.getValue());
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, p, size, null, 0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			this.cpuStepping = Integer.toString(p.getInt(0));
		}
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
		if (this.cpuModel == null) {
			int[] mib = { SystemB.CTL_MACHDEP, SystemB.MACHDEP_CPU,
					SystemB.MACHDEP_CPU_MODEL };
			IntByReference size = new IntByReference(SystemB.INT_SIZE);
			Pointer p = new Memory(size.getValue());
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, p, size, null, 0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			this.cpuModel = Integer.toString(p.getInt(0));
		}
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
		if (this.cpuFamily == null) {
			int[] mib = { SystemB.CTL_MACHDEP, SystemB.MACHDEP_CPU,
					SystemB.MACHDEP_CPU_FAMILY };
			IntByReference size = new IntByReference(SystemB.INT_SIZE);
			Pointer p = new Memory(size.getValue());
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, p, size, null, 0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			this.cpuFamily = Integer.toString(p.getInt(0));
		}
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
	public double getSystemCpuLoadBetweenTicks() {
		// Check if > ~ 0.95 seconds since last tick count.
		long now = System.currentTimeMillis();
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
	 * Updates system tick information from native host_statistics query. Array
	 * with four elements representing clock ticks or milliseconds (platform
	 * dependent) spent in User (0), Nice (1), System (2), and Idle (3) states.
	 * By measuring the difference between ticks across a time interval, CPU
	 * load over that interval may be calculated.
	 * 
	 * @return An array of 4 long values representing time spent in User,
	 *         Nice(if applicable), System, and Idle states.
	 */
	private static void updateSystemTicks() {
		int machPort = SystemB.INSTANCE.mach_host_self();
		HostCpuLoadInfo cpuLoadInfo = new HostCpuLoadInfo();
		if (0 != SystemB.INSTANCE.host_statistics(machPort,
				SystemB.HOST_CPU_LOAD_INFO, cpuLoadInfo, new IntByReference(
						cpuLoadInfo.size())))
			throw new LastErrorException("Error code: " + Native.getLastError());
		// Switch order to match linux
		curTicks[0] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_USER];
		curTicks[1] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_NICE];
		curTicks[2] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_SYSTEM];
		curTicks[3] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_IDLE];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getSystemCpuLoad() {
		if (sunMXBean) {
			return ((com.sun.management.OperatingSystemMXBean) OS_MXBEAN)
					.getSystemCpuLoad();
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
		if (now - procTickTime > 950) {
			// Enough time has elapsed. Update array in place
			updateProcessorTicks();
			// Copy arrays in place
			System.arraycopy(curProcTicks, 0, prevProcTicks, 0,
					curProcTicks.length);
			System.arraycopy(allProcessorTicks[processorNumber], 0,
					curProcTicks, 0, curProcTicks.length);
			procTickTime = now;
		}
		long total = 0;
		for (int i = 0; i < curProcTicks.length; i++) {
			total += (curProcTicks[i] - prevProcTicks[i]);
		}
		// Calculate idle from last field [3]
		long idle = curProcTicks[3] - prevProcTicks[3];
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
	 * cache when iterating over processors so that the host_processor_info
	 * query is only done once
	 */
	private static void updateProcessorTicks() {
		// Update no more frequently than 100ms so this is only triggered once
		// during iteration over Processors
		long now = System.currentTimeMillis();
		if (now - allProcTickTime < 100)
			return;

		int machPort = SystemB.INSTANCE.mach_host_self();

		IntByReference procCount = new IntByReference();
		PointerByReference procCpuLoadInfo = new PointerByReference();
		IntByReference procInfoCount = new IntByReference();
		if (0 != SystemB.INSTANCE.host_processor_info(machPort,
				SystemB.PROCESSOR_CPU_LOAD_INFO, procCount, procCpuLoadInfo,
				procInfoCount))
			throw new LastErrorException("Error code: " + Native.getLastError());

		int[] cpuTicks = procCpuLoadInfo.getValue().getIntArray(0,
				procInfoCount.getValue());
		for (int cpu = 0; cpu < procCount.getValue(); cpu++) {
			for (int j = 0; j < 4; j++) {
				int offset = cpu * SystemB.CPU_STATE_MAX;
				allProcessorTicks[cpu][0] = FormatUtil
						.getUnsignedInt(cpuTicks[offset
								+ SystemB.CPU_STATE_USER]);
				allProcessorTicks[cpu][1] = FormatUtil
						.getUnsignedInt(cpuTicks[offset
								+ SystemB.CPU_STATE_NICE]);
				allProcessorTicks[cpu][2] = FormatUtil
						.getUnsignedInt(cpuTicks[offset
								+ SystemB.CPU_STATE_SYSTEM]);
				allProcessorTicks[cpu][3] = FormatUtil
						.getUnsignedInt(cpuTicks[offset
								+ SystemB.CPU_STATE_IDLE]);
			}
		}
		allProcTickTime = now;
	}

	@Override
	public String toString() {
		return getName();
	}
}
