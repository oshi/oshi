/**
 * Copyright (c) Daniel Doubrovkine, 2010
 * dblock[at]dblock[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.hardware;

/**
 * The Central Processing Unit (CPU) or the processor is the portion of a
 * computer system that carries out the instructions of a computer program, and
 * is the primary element carrying out the computer's functions.
 * 
 * @author dblock[at]dblock[dot]org
 */
public interface Processor {
	/**
	 * Processor vendor.
	 * 
	 * @return String.
	 */
	String getVendor();

	/**
	 * Set processor vendor.
	 * 
	 * @param vendor
	 *            Vendor.
	 */
	void setVendor(String vendor);

	/**
	 * Name, eg. Intel(R) Core(TM)2 Duo CPU T7300 @ 2.00GHz
	 * 
	 * @return Processor name.
	 */
	String getName();

	/**
	 * Set processor name.
	 * 
	 * @param name
	 *            Name.
	 */
	void setName(String name);
	
	/**
	 * Vendor frequency (in Hz).
	 * @return Processor frequency.
	 */
	long getVendorFreq();
	
	/**
	 * Set processor vendor frequency (in Hz).
	 * @param freq Frequency.
	 */
	void setVendorFreq(long freq);

	/**
	 * Identifier, eg. x86 Family 6 Model 15 Stepping 10.
	 * 
	 * @return Processor identifier.
	 */
	String getIdentifier();

	/**
	 * Set processor identifier.
	 * 
	 * @param identifier
	 *            Identifier.
	 */
	void setIdentifier(String identifier);

	/**
	 * Is CPU 64bit?
	 * 
	 * @return True if cpu is 64bit.
	 */
	boolean isCpu64bit();

	/**
	 * Set flag is cpu is 64bit.
	 * 
	 * @param cpu64
	 *            True if cpu is 64.
	 */
	void setCpu64(boolean cpu64);

	/**
	 * @return the _stepping
	 */
	String getStepping();

	/**
	 * @param _stepping
	 *            the _stepping to set
	 */
	void setStepping(String _stepping);

	/**
	 * @return the _model
	 */
	String getModel();

	/**
	 * @param _model
	 *            the _model to set
	 */
	void setModel(String _model);

	/**
	 * @return the _family
	 */
	String getFamily();

	/**
	 * @param _family
	 *            the _family to set
	 */
	void setFamily(String _family);

	/**
	 * Get total CPU load
	 * 
	 * @return CPU load in %
	 */
	float getLoad();
}
