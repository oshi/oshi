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
import oshi.util.ParseUtil;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinBase;

/**
 * A CPU as defined in Windows registry.
 * 
 * @author dblock[at]dblock[dot]org
 * @author alessio.fachechi[at]gmail[dot]com
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
	private static long[] prevTicks = new long[4];
	private static long[] curTicks = getCurrentSystemCpuLoadTicks();
	private static long tickTime = System.currentTimeMillis();

	private String cpuVendor;
	private String cpuName;
	private String cpuIdentifier;
	private Long cpuVendorFreq;

	public CentralProcessor() {

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
	public float getLoad() {
		// Check if > ~ 0.95 seconds since last tick count.
		long now = System.currentTimeMillis();
		if (now - tickTime > 950) {
			// Enough time has elapsed. Copy latest ticks to earlier position
			System.arraycopy(curTicks, 0, prevTicks, 0, prevTicks.length);
			// Calculate new latest values
			curTicks = getCurrentSystemCpuLoadTicks();
			tickTime = now;
		}
		// Calculate total
		long total = 0;
		for (int i = 0; i < curTicks.length; i++) {
			total += (curTicks[i] - prevTicks[i]);
		}
		// Calculate idle from last field [3]
		long idle = curTicks[3] - prevTicks[3];
		// return
		if (total > 0 && idle >= 0) {
			return 100f * (total - idle) / total;
		}
		return 0f;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long[] getSystemCpuLoadTicks() {
		return getCurrentSystemCpuLoadTicks();
	}

	/**
	 * Gets system tick information from native call to GetSystemTimes().
	 * Returns an array with four elements representing clock ticks or
	 * milliseconds (platform dependent) spent in User (0), Nice (1), System
	 * (2), and Idle (3) states. By measuring the difference between ticks
	 * across a time interval, CPU load over that interval may be calculated.
	 * 
	 * @return An array of 4 long values representing time spent in User,
	 *         Nice(if applicable), System, and Idle states.
	 */
	private static long[] getCurrentSystemCpuLoadTicks() {
		WinBase.FILETIME lpIdleTime = new WinBase.FILETIME();
		WinBase.FILETIME lpKernelTime = new WinBase.FILETIME();
		WinBase.FILETIME lpUserTime = new WinBase.FILETIME();
		if (0 == Kernel32.INSTANCE.GetSystemTimes(lpIdleTime, lpKernelTime,
				lpUserTime))
			throw new LastErrorException("Error code: " + Native.getLastError());
		// Array order is user,nice,kernel,idle
		long[] ticks = new long[4];
		ticks[0] = lpUserTime.toLong() + Kernel32.WIN32_TIME_OFFSET;
		ticks[1] = 0L; // Windows is not 'nice'
		ticks[2] = lpKernelTime.toLong() - lpIdleTime.toLong();
		ticks[3] = lpIdleTime.toLong() + Kernel32.WIN32_TIME_OFFSET;
		return ticks;
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
		return getLoad();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getSystemLoadAverage() {
		return OS_MXBEAN.getSystemLoadAverage();
	}

	@Override
	public String toString() {
		return getName();
	}
}
