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
import oshi.util.ParseUtil;

import com.sun.jna.LastErrorException;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.management.OperatingSystemMXBean;

/**
 * A CPU.
 * 
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 * @author alessio.fachechi[at]gmail[dot]com
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
	@Override
	public String getVendor() {
		if (this._vendor == null) {
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
			this._vendor = p.getString(0);
		}
		return this._vendor;
	}

	/**
	 * Set processor vendor.
	 * 
	 * @param vendor
	 *			Vendor.
	 */
	@Override
	public void setVendor(String vendor) {
		this._vendor = vendor;
	}

	/**
	 * Name, eg. Intel(R) Core(TM)2 Duo CPU T7300 @ 2.00GHz
	 * 
	 * @return Processor name.
	 */
	@Override
	public String getName() {
		if (this._name == null) {
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
			this._name = p.getString(0);
		}
		return this._name;
	}

	/**
	 * Set processor name.
	 * 
	 * @param name
	 *			Name.
	 */
	@Override
	public void setName(String name) {
		this._name = name;
	}

	/**
	 * Vendor frequency (in Hz), eg. for processor named Intel(R) Core(TM)2 Duo
	 * CPU T7300 @ 2.00GHz the vendor frequency is 2000000000.
	 * 
	 * @return Processor frequency or -1 if unknown.
	 */
	@Override
	public long getVendorFreq() {
		if (this._freq == null) {
			Pattern pattern = Pattern.compile("@ (.*)$");
			Matcher matcher = pattern.matcher(getName());

			if (matcher.find()) {
				String unit = matcher.group(1);
				_freq = ParseUtil.parseHertz(unit);
			} else {
				_freq = -1L;
			}
		}

		return this._freq.longValue();
	}

	/**
	 * Set vendor frequency.
	 * 
	 * @param freq
	 *			Frequency.
	 */
	@Override
	public void setVendorFreq(long freq) {
		this._freq = Long.valueOf(freq);
	}

	/**
	 * Identifier, eg. x86 Family 6 Model 15 Stepping 10.
	 * 
	 * @return Processor identifier.
	 */
	@Override
	public String getIdentifier() {
		if (this._identifier == null) {
			StringBuilder sb = new StringBuilder();
			if (getVendor().contentEquals("GenuineIntel"))
				sb.append(isCpu64bit() ? "Intel64" : "x86");
			else
				sb.append(getVendor());
			sb.append(" Family ").append(getFamily());
			sb.append(" Model ").append(getModel());
			sb.append(" Stepping ").append(getStepping());
			this._identifier = sb.toString();
		}
		return this._identifier;
	}

	/**
	 * Set processor identifier.
	 * 
	 * @param identifier
	 *			Identifier.
	 */
	@Override
	public void setIdentifier(String identifier) {
		this._identifier = identifier;
	}

	/**
	 * Is CPU 64bit?
	 * 
	 * @return True if cpu is 64bit.
	 */
	@Override
	public boolean isCpu64bit() {
		if (this._cpu64 == null) {
			int[] mib = { SystemB.CTL_HW, SystemB.HW_CPU64BIT_CAPABLE };
			IntByReference size = new IntByReference(SystemB.INT_SIZE);
			Pointer p = new Memory(size.getValue());
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, p, size, null, 0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			this._cpu64 = p.getInt(0) != 0;
		}
		return this._cpu64.booleanValue();
	}

	/**
	 * Set flag is cpu is 64bit.
	 * 
	 * @param cpu64
	 *			True if cpu is 64.
	 */
	@Override
	public void setCpu64(boolean cpu64) {
		this._cpu64 = Boolean.valueOf(cpu64);
	}

	/**
	 * @return the _stepping
	 */
	@Override
	public String getStepping() {
		if (this._stepping == null) {
			int[] mib = { SystemB.CTL_MACHDEP, SystemB.MACHDEP_CPU,
					SystemB.MACHDEP_CPU_STEPPING };
			IntByReference size = new IntByReference(SystemB.INT_SIZE);
			Pointer p = new Memory(size.getValue());
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, p, size, null, 0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			this._stepping = Integer.toString(p.getInt(0));
		}
		return this._stepping;
	}

	/**
	 * @param stepping
	 *			the stepping to set
	 */
	@Override
	public void setStepping(String stepping) {
		this._stepping = stepping;
	}

	/**
	 * @return the _model
	 */
	@Override
	public String getModel() {
		if (this._model == null) {
			int[] mib = { SystemB.CTL_MACHDEP, SystemB.MACHDEP_CPU,
					SystemB.MACHDEP_CPU_MODEL };
			IntByReference size = new IntByReference(SystemB.INT_SIZE);
			Pointer p = new Memory(size.getValue());
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, p, size, null, 0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			this._model = Integer.toString(p.getInt(0));
		}
		return this._model;
	}

	/**
	 * @param model
	 *			the model to set
	 */
	@Override
	public void setModel(String model) {
		this._model = model;
	}

	/**
	 * @return the _family
	 */
	@Override
	public String getFamily() {
		if (this._family == null) {
			int[] mib = { SystemB.CTL_MACHDEP, SystemB.MACHDEP_CPU,
					SystemB.MACHDEP_CPU_FAMILY };
			IntByReference size = new IntByReference(SystemB.INT_SIZE);
			Pointer p = new Memory(size.getValue());
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, p, size, null, 0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			this._family = Integer.toString(p.getInt(0));
		}
		return this._family;
	}

	/**
	 * @param family
	 *			the family to set
	 */
	@Override
	public void setFamily(String family) {
		this._family = family;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
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
		if (total > 0 && idle >= 0) {
			return 100f * (total - idle) / total;
		}
		return 0f;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long[] getCpuLoadTicks() {
		// TODO: Consider PROCESSOR_CPU_LOAD_INFO to get value per-core
		int machPort = SystemB.INSTANCE.mach_host_self();
		long[] ticks = new long[SystemB.CPU_STATE_MAX];
		HostCpuLoadInfo cpuLoadInfo = new HostCpuLoadInfo();
		if (0 != SystemB.INSTANCE.host_statistics(machPort,
				SystemB.HOST_CPU_LOAD_INFO, cpuLoadInfo, new IntByReference(
						cpuLoadInfo.size())))
			throw new LastErrorException("Error code: " + Native.getLastError());
		// Switch order to match linux
		ticks[0] = (long) cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_USER];
		ticks[1] = (long) cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_NICE];
		ticks[2] = (long) cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_SYSTEM];
		ticks[3] = (long) cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_IDLE];
		return ticks;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getSystemCPULoad() {
		return OS_MXBEAN.getSystemCpuLoad();
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
