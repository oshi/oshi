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
	 * 
	 * @return Processor frequency.
	 */
	long getVendorFreq();

	/**
	 * Set processor vendor frequency (in Hz).
	 * 
	 * @param freq
	 *            Frequency.
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
	 * Get total CPU load. Incurs a 1 second sleep delay while counting
	 * process/idle CPU ticks using {@link #getCpuLoadTicks()}.
	 * 
	 * @return CPU load in %
	 * 
	 * @deprecated As of release 1.3, replaced by {@link #getSystemCPULoad()}.
	 *             Users may also manually calculate using
	 *             {@link #getCpuLoadTicks()}.
	 */
	@Deprecated
	float getLoad();

	/**
	 * Get CPU Load tick counters. Returns an array with four elements
	 * representing clock ticks or milliseconds (platform dependent) spent in
	 * User (0), Nice (1), System (2), and Idle (3) states. By measuring the
	 * difference between ticks across a time interval, CPU load over that
	 * interval may be calculated.
	 * 
	 * @return An array of 4 long values representing time spent in User,
	 *         Nice(if applicable), System, and Idle states.
	 */
	long[] getCpuLoadTicks();

	/**
	 * Returns the "recent cpu usage" for the whole system from the
	 * {@link com.sun.management.OperatingSystemMXBean}. This value is a double
	 * in the [0.0,1.0] interval. A value of 0.0 means that all CPUs were idle
	 * during the recent period of time observed, while a value of 1.0 means
	 * that all CPUs were actively running 100% of the time during the recent
	 * period being observed. All values betweens 0.0 and 1.0 are possible
	 * depending of the activities going on in the system. If the system recent
	 * cpu usage is not available, the method returns a negative value.
	 * 
	 * Calling this method immediately upon instantiating the {@link Processor}
	 * may give unreliable results.
	 * 
	 * @return the "recent cpu usage" for the whole system; a negative value if
	 *         not available.
	 */
	@SuppressWarnings("restriction")
	double getSystemCPULoad();

	/**
	 * Returns the system load average for the last minute from the
	 * {@link java.lang.management.OperatingSystemMXBean}. The system load
	 * average is the sum of the number of runnable entities queued to the
	 * available processors and the number of runnable entities running on the
	 * available processors averaged over a period of time. The way in which the
	 * load average is calculated is operating system specific but is typically
	 * a damped time-dependent average.
	 * 
	 * If the load average is not available, a negative value is returned.
	 * 
	 * This method is designed to provide a hint about the system load and may
	 * be queried frequently. The load average may be unavailable on some
	 * platform where it is expensive to implement this method.
	 * 
	 * @return the system load average; or a negative value if not available.
	 */
	double getSystemLoadAverage();
}
