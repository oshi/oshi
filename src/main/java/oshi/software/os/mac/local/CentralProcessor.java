/*
 * Copyright (c) Alessandro Perucchi, 2014
 * alessandro[at]perucchi[dot]org
 * Daniel Widdis, 2015
 * widdis[at]gmail[dot]com
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.mac.local;

import oshi.hardware.Processor;
import oshi.software.os.mac.local.SystemB.HostCpuLoadInfo;
import oshi.util.ParseUtil;

import com.sun.jna.LastErrorException;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A CPU.
 * 
 * @author alessandro[at]perucchi[dot]org
 * @author widdis[at]gmail[dot]com
 */
public class CentralProcessor implements Processor {
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
		if (_vendor == null) {
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
			_vendor = p.getString(0);
		}
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
		if (_name == null) {
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
			_name = p.getString(0);
		}
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
			sb.append(" Family ").append(getFamily());
			sb.append(" Model ").append(getModel());
			sb.append(" Stepping ").append(getStepping());
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
		if (_cpu64 == null) {
			int[] mib = { SystemB.CTL_HW, SystemB.HW_CPU64BIT_CAPABLE };
			IntByReference size = new IntByReference(SystemB.INT_SIZE);
			Pointer p = new Memory(size.getValue());
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, p, size, null, 0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			_cpu64 = p.getInt(0) != 0;
		}
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
		if (_stepping == null) {
			int[] mib = { SystemB.CTL_MACHDEP, SystemB.MACHDEP_CPU,
					SystemB.MACHDEP_CPU_STEPPING };
			IntByReference size = new IntByReference(SystemB.INT_SIZE);
			Pointer p = new Memory(size.getValue());
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, p, size, null, 0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			_stepping = Integer.toString(p.getInt(0));
		}
		return _stepping;
	}

	/**
	 * @param stepping
	 *            the stepping to set
	 */
	public void setStepping(String stepping) {
		_stepping = stepping;
	}

	/**
	 * @return the _model
	 */
	public String getModel() {
		if (_model == null) {
			int[] mib = { SystemB.CTL_MACHDEP, SystemB.MACHDEP_CPU,
					SystemB.MACHDEP_CPU_MODEL };
			IntByReference size = new IntByReference(SystemB.INT_SIZE);
			Pointer p = new Memory(size.getValue());
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, p, size, null, 0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			_model = Integer.toString(p.getInt(0));
		}
		return _model;
	}

	/**
	 * @param model
	 *            the model to set
	 */
	public void setModel(String model) {
		_model = model;
	}

	/**
	 * @return the _family
	 */
	public String getFamily() {
		if (_family == null) {
			int[] mib = { SystemB.CTL_MACHDEP, SystemB.MACHDEP_CPU,
					SystemB.MACHDEP_CPU_FAMILY };
			IntByReference size = new IntByReference(SystemB.INT_SIZE);
			Pointer p = new Memory(size.getValue());
			if (0 != SystemB.INSTANCE.sysctl(mib, mib.length, p, size, null, 0))
				throw new LastErrorException("Error code: "
						+ Native.getLastError());
			_family = Integer.toString(p.getInt(0));
		}
		return _family;
	}

	/**
	 * @param family
	 *            the family to set
	 */
	public void setFamily(String family) {
		_family = family;
	}

	/**
	 * {@inheritDoc}
	 */
	public float getLoad() {
		int[] prevTicks = getCpuTicks();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// Awake, O sleeper
		}
		int[] ticks = getCpuTicks();

		int total = 0;
		for (int i = 0; i < SystemB.CPU_STATE_MAX; i++) {
			total += (ticks[i] - prevTicks[i]);
		}
		int idle = ticks[SystemB.CPU_STATE_IDLE]
				- prevTicks[SystemB.CPU_STATE_IDLE];
		if (total > 0 && idle >= 0)
			return 100f * (total - idle) / total;
		else
			return 0f;
	}

	private int[] getCpuTicks() {
		// TODO: Consider PROCESSOR_CPU_LOAD_INFO to get value per-core
		int machPort = SystemB.INSTANCE.mach_host_self();
		int[] ticks = new int[SystemB.CPU_STATE_MAX];
		HostCpuLoadInfo cpuLoadInfo = new HostCpuLoadInfo();
		if (0 != SystemB.INSTANCE.host_statistics(machPort,
				SystemB.HOST_CPU_LOAD_INFO, cpuLoadInfo, new IntByReference(
						cpuLoadInfo.size())))
			throw new LastErrorException("Error code: " + Native.getLastError());
		for (int i = 0; i < SystemB.CPU_STATE_MAX; i++) {
			ticks[i] = cpuLoadInfo.cpu_ticks[i];
		}
		return ticks;
	}

	@Override
	public String toString() {
		return getName();
	}
}
