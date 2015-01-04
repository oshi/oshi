/**
 * Copyright (c) Daniel Doubrovkine, 2010
 * dblock[at]dblock[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.windows.nt;

import oshi.hardware.Processor;

/**
 * A CPU as defined in Windows registry.
 * @author dblock[at]dblock[dot]org
 */
public class CentralProcessor implements Processor {
	private String _vendor;
	private String _name;
	private String _identifier;
	
	public CentralProcessor() {
		
	}
	
	/**
	 * Vendor identifier, eg. GenuineIntel.
	 * @return
	 *  Processor vendor. 
	 */
	public String getVendor() {
		return _vendor;
	}
	
	/**
	 * Set processor vendor.
	 * @param vendor
	 *  Vendor.
	 */
	public void setVendor(String vendor) {
		_vendor = vendor;
	}

	/**
	 * Name, eg. Intel(R) Core(TM)2 Duo CPU T7300 @ 2.00GHz
	 * @return
	 *  Processor name. 
	 */
	public String getName() {
		return _name;
	}
	
	/**
	 * Set processor name.
	 * @param name
	 *  Name.
	 */
	public void setName(String name) {
		_name = name;
	}
	
	/**
	 * Identifier, eg. x86 Family 6 Model 15 Stepping 10.
	 * @return
	 *  Processor identifier. 
	 */
	public String getIdentifier() {
		return _identifier;
	}
	
	/**
	 * Set processor identifier.
	 * @param identifier
	 *  Identifier.
	 */
	public void setIdentifier(String identifier) {
		_identifier = identifier;
	}

	public boolean isCpu64bit() {
		throw new UnsupportedOperationException();
	}

	public void setCpu64(boolean cpu64) {
		throw new UnsupportedOperationException();
	}

	public String getStepping() {
		throw new UnsupportedOperationException();
	}

	public void setStepping(String _stepping) {
		throw new UnsupportedOperationException();
	}

	public String getModel() {
		throw new UnsupportedOperationException();
	}

	public void setModel(String _model) {
		throw new UnsupportedOperationException();
	}

	public String getFamily() {
		throw new UnsupportedOperationException();
	}

	public void setFamily(String _family) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return _name;
	}
}
