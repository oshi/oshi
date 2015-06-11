/**
 * Copyright (c) Daniel Doubrovkine, 2010
 * dblock[at]dblock[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.windows.nt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.hardware.Processor;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * A CPU as defined in Windows registry.
 * 
 * @author dblock[at]dblock[dot]org
 */
public class CentralProcessor implements Processor {
	private String _vendor;
	private String _name;
	private String _identifier;
	private Long _freq;

	public CentralProcessor() {

	}

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
	 * {@inheritDoc}
	 */
	public boolean isCpu64bit() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setCpu64(boolean cpu64) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getStepping() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setStepping(String _stepping) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getModel() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setModel(String _model) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getFamily() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFamily(String _family) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	public float getLoad() {
		// this always return whole number value on windows machines
		String result = ExecutingCommand.getAnswerAt(
				"wmic /locale:ms_409 cpu get loadpercentage", 2);

		if (result == null || result.length() <= 0) {
			throw new RuntimeException(
					"CPU load could not be obtained from system");
		}

		try {
			return Integer.valueOf(result.trim());
		} catch (NumberFormatException e) {
			System.err.println("Cannot parse CPU load value: " + result.trim());
			throw new RuntimeException("Cannot parse load value: "
					+ result.trim());
		}
	}

	@Override
	public String toString() {
		return _name;
	}
}
