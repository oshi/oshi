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

import com.sun.management.OperatingSystemMXBean;

/**
 * A CPU as defined in Linux /proc.
 * 
 * @author alessandro[at]perucchi[dot]org
 */
@SuppressWarnings("restriction")
public class CentralProcessor implements Processor {
	private static final OperatingSystemMXBean OS_MXBEAN;
	static {
		OS_MXBEAN = (com.sun.management.OperatingSystemMXBean) ManagementFactory
				.getOperatingSystemMXBean();
		// Initialize CPU usage
		OS_MXBEAN.getSystemCpuLoad();
	}

	private String _vendor;
	private String _name;
	private String _identifier = null;
	private String _stepping;
	private String _model;
	private String _family;
	private Long _freq = null;
	private Boolean _cpu64;

	/**
	 * Vendor identifier, eg. GenuineIntel.
	 * 
	 * @return Processor vendor.
	 */
	public String getVendor() {
		return _vendor;
	}

	/**
	 * Set processor vendor.
	 * 
	 * @param vendor
	 *            Vendor.
	 */
	public void setVendor(String vendor) {
		_vendor = vendor;
	}

	/**
	 * Name, eg. Intel(R) Core(TM)2 Duo CPU T7300 @ 2.00GHz
	 * 
	 * @return Processor name.
	 */
	public String getName() {
		return _name;
	}

	/**
	 * Set processor name.
	 * 
	 * @param name
	 *            Name.
	 */
	public void setName(String name) {
		_name = name;
	}

	/**
	 * Vendor frequency (in Hz), eg. for processor named Intel(R) Core(TM)2 Duo
	 * CPU T7300 @ 2.00GHz the vendor frequency is 2000000000.
	 * 
	 * @return Processor frequency or -1 if unknown.
	 * 
	 * @author alessio.fachechi[at]gmail[dot]com
	 */
	public long getVendorFreq() {
		if (_freq == null) {
			Pattern pattern = Pattern.compile("@ (.*)$");
			Matcher matcher = pattern.matcher(getName());

			if (matcher.find()) {
				String unit = matcher.group(1);
				_freq = ParseUtil.parseHertz(unit);
			} else {
				_freq = -1L;
			}
		}

		return _freq.longValue();
	}

	/**
	 * Set vendor frequency.
	 * 
	 * @param frequency
	 *            Frequency.
	 */
	public void setVendorFreq(long freq) {
		_freq = Long.valueOf(freq);
	}

	/**
	 * Identifier, eg. x86 Family 6 Model 15 Stepping 10.
	 * 
	 * @return Processor identifier.
	 */
	public String getIdentifier() {
		if (_identifier == null) {
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
			_identifier = sb.toString();
		}
		return _identifier;
	}

	/**
	 * Set processor identifier.
	 * 
	 * @param identifier
	 *            Identifier.
	 */
	public void setIdentifier(String identifier) {
		_identifier = identifier;
	}

	/**
	 * Is CPU 64bit?
	 * 
	 * @return True if cpu is 64bit.
	 */
	public boolean isCpu64bit() {
		return _cpu64.booleanValue();
	}

	/**
	 * Set flag is cpu is 64bit.
	 * 
	 * @param cpu64
	 *            True if cpu is 64.
	 */
	public void setCpu64(boolean cpu64) {
		_cpu64 = Boolean.valueOf(cpu64);
	}

	/**
	 * @return the _stepping
	 */
	public String getStepping() {
		return _stepping;
	}

	/**
	 * @param _stepping
	 *            the _stepping to set
	 */
	public void setStepping(String _stepping) {
		this._stepping = _stepping;
	}

	/**
	 * @return the _model
	 */
	public String getModel() {
		return _model;
	}

	/**
	 * @param _model
	 *            the _model to set
	 */
	public void setModel(String _model) {
		this._model = _model;
	}

	/**
	 * @return the _family
	 */
	public String getFamily() {
		return _family;
	}

	/**
	 * @param _family
	 *            the _family to set
	 */
	public void setFamily(String _family) {
		this._family = _family;
	}

	/**
	 * {@inheritDoc}
	 */
	@Deprecated
	public float getLoad() {
		long[] prevTicks = getCpuLoadTicks();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// Awake, O sleeper
		}
		long[] ticks = getCpuLoadTicks();
		long total = 0;
		for (int i = 0; i < ticks.length; i++) {
			total += (ticks[i] - prevTicks[i]);
		}
		long idle = ticks[ticks.length - 1] - prevTicks[ticks.length - 1];
		if (total > 0 && idle >= 0)
			return 100f * (total - idle) / total;
		else
			return 0f;
	}

	public long[] getCpuLoadTicks() {
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

	public double getSystemCPULoad() {
		return OS_MXBEAN.getSystemCpuLoad();
	}

	public double getSystemLoadAverage() {
		return OS_MXBEAN.getSystemLoadAverage();
	}

	public String toString() {
		return getName();
	}
}
