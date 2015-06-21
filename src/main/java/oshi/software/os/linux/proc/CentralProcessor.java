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

import oshi.hardware.Processor;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

/**
 * A CPU as defined in Linux /proc.
 * 
 * @author alessandro[at]perucchi[dot]org
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
	private String cpuIdentifier = null;
	private String cpuStepping;
	private String cpuModel;
	private String cpuFamily;
	private Long cpuVendorFreq = null;
	private Boolean cpu64;

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
	 * Gets system tick information from parsing /proc/stat. Returns an array
	 * with four elements representing clock ticks or milliseconds (platform
	 * dependent) spent in User (0), Nice (1), System (2), and Idle (3) states.
	 * By measuring the difference between ticks across a time interval, CPU
	 * load over that interval may be calculated.
	 * 
	 * @return An array of 4 long values representing time spent in User,
	 *         Nice(if applicable), System, and Idle states.
	 */
	private static long[] getCurrentSystemCpuLoadTicks() {
		// /proc/stat expected format
		// first line is overall user,nice,system,idle, etc.
		// cpu 3357 0 4313 1362393 ...
		// TODO: per-processor subsequent lines for cpu0, cpu1, etc.
		long[] ticks = new long[4];
		String tickStr = "";
		try {
			List<String> procStat = FileUtil.readFile("/proc/stat");
			if (!procStat.isEmpty())
				tickStr = procStat.get(0);
		} catch (IOException e) {
			System.err.println("Problem with: /proc/stat");
			System.err.println(e.getMessage());
			return ticks;
		}
		String[] tickArr = tickStr.split("\\s+");
		if (tickStr.length() < 5)
			return ticks;
		for (int i = 0; i < 4; i++) {
			ticks[i] = Long.parseLong(tickArr[i + 1]);
		}
		return ticks;
	}

	@Override
	public double getSystemCpuLoad() {
		if (sunMXBean) {
			return ((com.sun.management.OperatingSystemMXBean) OS_MXBEAN)
					.getSystemCpuLoad();
		}
		return getLoad();
	}

	@Override
	public double getSystemLoadAverage() {
		return OS_MXBEAN.getSystemLoadAverage();
	}

	@Override
	public String toString() {
		return getName();
	}
}
