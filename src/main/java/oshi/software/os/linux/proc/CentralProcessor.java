/**
 * Copyright (c) Alessandro Perucchi, 2014
 * alessandro[at]perucchi[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.linux.proc;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.hardware.Processor;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;

/**
 * A CPU as defined in Linux /proc.
 * 
 * @author alessandro[at]perucchi[dot]org
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
	public float getLoad() {
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
		in.close();
		ArrayList<Float> loads = new ArrayList<Float>();
		for (String load : result) {
			if (load.matches("-?\\d+(\\.\\d+)?")) {
				loads.add(Float.valueOf(load));
			}
		}
		// ((Total-PrevTotal)-(Idle-PrevIdle))/(Total-PrevTotal) - see
		// http://stackoverflow.com/a/23376195/4359897
		float totalCpuLoad = (loads.get(0) + loads.get(2)) * 100
				/ (loads.get(0) + loads.get(2) + loads.get(3));
		return FormatUtil.round(totalCpuLoad, 2);
	}

	@Override
	public String toString() {
		return getName();
	}

}
