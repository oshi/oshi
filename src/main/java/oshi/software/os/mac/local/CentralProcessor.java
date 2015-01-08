/**
 * Copyright (c) Alessandro Perucchi, 2014
 * alessandro[at]perucchi[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.mac.local;

import oshi.hardware.Processor;
import oshi.util.ExecutingCommand;

import java.util.ArrayList;

/**
 * A CPU.
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
	private Boolean _cpu64;

	/**
	 * Vendor identifier, eg. GenuineIntel.
	 * 
	 * @return Processor vendor.
	 */
	public String getVendor() {
		if (_vendor==null)
			_vendor=ExecutingCommand.getFirstAnswer("sysctl -n machdep.cpu.vendor");
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
		if (_name==null)
			_name=ExecutingCommand.getFirstAnswer("sysctl -n machdep.cpu.brand_string");
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
		if (_cpu64 == null) {
			_cpu64 = ExecutingCommand.getFirstAnswer(
					"sysctl -n hw.cpu64bit_capable").equals("1") ? true : false;
		}
		return _cpu64;
	}

	/**
	 * Set flag is cpu is 64bit.
	 * 
	 * @param cpu64
	 *            True if cpu is 64.
	 */
	public void setCpu64(boolean cpu64) {
		_cpu64 = cpu64;
	}

	/**
	 * @return the _stepping
	 */
	public String getStepping() {
		if (_stepping == null)
			_stepping = ExecutingCommand
					.getFirstAnswer("sysctl -n machdep.cpu.stepping");
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
		if (_model == null) {
			_model = ExecutingCommand
					.getFirstAnswer("sysctl -n machdep.cpu.model");
			;
		}
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
		if (_family == null) {
			_family = ExecutingCommand
					.getFirstAnswer("sysctl -n machdep.cpu.family");
		}
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
		ArrayList<String> topResult = ExecutingCommand.runNative("top -l 1 -R -F -n1"); // cpu load is in [3]
		String[] idle = topResult.get(3).split(" "); // idle value is in [6]
		return 100 - Float.valueOf(idle[6].replace("%", ""));
	}

	@Override
	public String toString() {
		return getName();
	}

}
