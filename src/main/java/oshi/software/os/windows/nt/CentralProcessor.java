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
package oshi.software.os.windows.nt;

import java.lang.management.ManagementFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.hardware.Processor;
import oshi.software.os.windows.nt.Pdh.PdhFmtCounterValue;
import oshi.util.ParseUtil;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinBase.SYSTEM_INFO;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * A CPU as defined in Windows registry.
 * 
 * @author dblock[at]dblock[dot]org
 * @author alessio.fachechi[at]gmail[dot]com
 * @author widdis[at]gmail[dot]com
 */
@SuppressWarnings("restriction")
public class CentralProcessor implements Processor {
	private static final java.lang.management.OperatingSystemMXBean OS_MXBEAN = ManagementFactory
			.getOperatingSystemMXBean();
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

	// Initialize numCPU and open a Performance Data Helper Thread for
	// monitoring each processor
	private static PointerByReference phQuery = new PointerByReference();
	private static final IntByReference zero = new IntByReference(0);
	private static final int numCPU;
	static {
		// Get number of processors
		SYSTEM_INFO sysinfo = new SYSTEM_INFO();
		Kernel32.INSTANCE.GetSystemInfo(sysinfo);
		numCPU = sysinfo.dwNumberOfProcessors.intValue();

		// Set up query for this processor
		int ret = Pdh.INSTANCE.PdhOpenQuery(null, zero, phQuery);
		if (ret != 0)
			throw new LastErrorException("Cannot open PDH query. Error code: "
					+ String.format("0x%08X", ret));

		// Set up hook to close the query on shutdown
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				Pdh.INSTANCE.PdhCloseQuery(phQuery.getValue());
			}
		});
	}
	// Set up a counter for each processor
	private static PointerByReference[] phUserCounters = new PointerByReference[numCPU];
	private static PointerByReference[] phIdleCounters = new PointerByReference[numCPU];
	static {
		for (int p = 0; p < numCPU; p++) {
			// Options are (only need 2 to calculate all)
			// "\Processor(0)\% processor time"
			// "\Processor(0)\% idle time" (1 - processor)
			// "\Processor(0)\% privileged time" (subset of processor)
			// "\Processor(0)\% user time" (other subset of processor)
			// Note need to make \ = \\ for Java Strings and %% for format
			String counterPath = String.format("\\Processor(%d)\\%% user time",
					p);
			phUserCounters[p] = new PointerByReference();
			int ret = Pdh.INSTANCE.PdhAddEnglishCounterA(phQuery.getValue(),
					counterPath, zero, phUserCounters[p]);
			if (ret != 0)
				throw new LastErrorException(
						"Cannot add PDH Counter for % user time for processor "
								+ p + ". Error code: "
								+ String.format("0x%08X", ret));
			counterPath = String.format("\\Processor(%d)\\%% idle time", p);
			phIdleCounters[p] = new PointerByReference();
			ret = Pdh.INSTANCE.PdhAddEnglishCounterA(phQuery.getValue(),
					counterPath, zero, phIdleCounters[p]);
			if (ret != 0)
				throw new LastErrorException(
						"Cannot add PDH Counter for % idle time for processor "
								+ p + ". Error code: "
								+ String.format("0x%08X", ret));
		}
		// Initialize by collecting data the first time
		int ret = Pdh.INSTANCE.PdhCollectQueryData(phQuery.getValue());
		if (ret != 0)
			throw new LastErrorException(
					"Cannot collect PDH query data. Error code: "
							+ String.format("0x%08X", ret));
	}
	// Set up array to maintain current ticks for rapid reference. This array
	// will be updated in place and used to increment ticks based on processor
	// data helper which only gives % between reads
	private static long[][] allProcessorTicks = new long[numCPU][4];
	private static long allProcTickTime = System.currentTimeMillis() - 100;

	private int processorNumber;
	private String cpuVendor;
	private String cpuName;
	private String cpuIdentifier;
	private Long cpuVendorFreq;

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
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCpu64bit() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setCpu64(boolean cpu64) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getStepping() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setStepping(String stepping) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getModel() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setModel(String model) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFamily() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setFamily(String family) {
		throw new UnsupportedOperationException();
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
	 * Updates system tick information from native call to GetSystemTimes().
	 * Array with four elements representing clock ticks or milliseconds
	 * (platform dependent) spent in User (0), Nice (1), System (2), and Idle
	 * (3) states. By measuring the difference between ticks across a time
	 * interval, CPU load over that interval may be calculated.
	 */
	private static void updateSystemTicks() {
		WinBase.FILETIME lpIdleTime = new WinBase.FILETIME();
		WinBase.FILETIME lpKernelTime = new WinBase.FILETIME();
		WinBase.FILETIME lpUserTime = new WinBase.FILETIME();
		if (0 == Kernel32.INSTANCE.GetSystemTimes(lpIdleTime, lpKernelTime,
				lpUserTime))
			throw new LastErrorException("Error code: " + Native.getLastError());
		// Array order is user,nice,kernel,idle
		curTicks[0] = lpUserTime.toLong() + Kernel32.WIN32_TIME_OFFSET;
		curTicks[1] = 0L; // Windows is not 'nice'
		curTicks[2] = lpKernelTime.toLong() - lpIdleTime.toLong();
		curTicks[3] = lpIdleTime.toLong() + Kernel32.WIN32_TIME_OFFSET;
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
		// Expected to be -1 for Windows
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
	 * cache when iterating over processors since pdh query updates all counters
	 * at once so we can't separate individual processors
	 */
	private void updateProcessorTicks() {
		// Update no more frequently than 100ms so this is only triggered once
		// during iteration over Processors
		long now = System.currentTimeMillis();
		if (now - allProcTickTime < 100)
			return;

		// This call updates all process counters to % used since last call
		int ret = Pdh.INSTANCE.PdhCollectQueryData(phQuery.getValue());
		if (ret != 0)
			throw new LastErrorException(
					"Cannot collect PDH query data. Error code: "
							+ String.format("0x%08X", ret));
		// Multiply % usage times elapsed MS to recreate ticks, then increment
		long elapsed = now - allProcTickTime;
		for (int cpu = 0; cpu < numCPU; cpu++) {
			PdhFmtCounterValue phUserCounterValue = new PdhFmtCounterValue();
			ret = Pdh.INSTANCE.PdhGetFormattedCounterValue(
					phUserCounters[cpu].getValue(), Pdh.PDH_FMT_LARGE
							| Pdh.PDH_FMT_1000, null, phUserCounterValue);
			if (ret != 0)
				throw new LastErrorException(
						"Cannot get PDH User % counter value. Error code: "
								+ String.format("0x%08X", ret));

			PdhFmtCounterValue phIdleCounterValue = new PdhFmtCounterValue();
			ret = Pdh.INSTANCE.PdhGetFormattedCounterValue(
					phIdleCounters[cpu].getValue(), Pdh.PDH_FMT_LARGE
							| Pdh.PDH_FMT_1000, null, phIdleCounterValue);
			if (ret != 0)
				throw new LastErrorException(
						"Cannot get PDH Idle % counter value. Error code: "
								+ String.format("0x%08X", ret));

			// Returns results in 1000's of percent, e.g. 5% is 5000
			// Multiply by elapsed to get total ms and Divide by 100 * 1000
			// Putting division at end avoids need to cast division to double
			long user = elapsed * phUserCounterValue.value.largeValue / 100000;
			long idle = elapsed * phIdleCounterValue.value.largeValue / 100000;
			// Elasped is only since last read, so increment previous value
			allProcessorTicks[cpu][0] += user;
			// allProcessorTicks[cpu][1] is ignored, Windows is not nice
			allProcessorTicks[cpu][2] += (elapsed - user - idle); // u+i+sys=100%
			allProcessorTicks[cpu][3] += idle;
		}
		allProcTickTime = now;
	}

	@Override
	public String toString() {
		return getName();
	}
}
