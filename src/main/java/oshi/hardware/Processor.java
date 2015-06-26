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
	 * Gets the processor number of this object, passed as an arg in the
	 * constructor.
	 * 
	 * @return The processor number
	 */
	int getProcessorNumber();

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
	 * Returns the "recent cpu usage" for the whole system by counting ticks
	 * from {@link #getSystemCpuLoadTicks()} between successive calls of this
	 * method, with a minimum interval slightly less than 1 second.
	 * 
	 * If less than one second has elapsed since the last call of this method,
	 * it will return a calculation based on the tick counts and times of the
	 * previous two calls. If at least a second has elapsed, it will return the
	 * average CPU load for the interval and update the "last called" times.
	 * 
	 * This method is intended to be used for periodic polling at intervals of 1
	 * second or longer.
	 * 
	 * @return CPU load in %
	 * 
	 * @deprecated Replaced in 1.3 by {@link #getSystemCpuLoadBetweenTicks()}
	 */
	@Deprecated
	float getLoad();

	/**
	 * Returns the "recent cpu usage" for the whole system by counting ticks
	 * from {@link #getSystemCpuLoadTicks()} between successive calls of this
	 * method, with a minimum interval slightly less than 1 second.
	 * 
	 * If less than one second has elapsed since the last call of this method,
	 * it will return a calculation based on the tick counts and times of the
	 * previous two calls. If at least a second has elapsed, it will return the
	 * average CPU load for the interval and update the "last called" times.
	 * 
	 * This method is intended to be used for periodic polling at intervals of 1
	 * second or longer.
	 * 
	 * @return CPU load between 0 and 1 (100%)
	 */
	double getSystemCpuLoadBetweenTicks();

	/**
	 * Get System-wide CPU Load tick counters. Returns an array with four
	 * elements representing clock ticks or milliseconds (platform dependent)
	 * spent in User (0), Nice (1), System (2), and Idle (3) states. By
	 * measuring the difference between ticks across a time interval, CPU load
	 * over that interval may be calculated.
	 * 
	 * @return An array of 4 long values representing time spent in User,
	 *         Nice(if applicable), System, and Idle states.
	 */
	long[] getSystemCpuLoadTicks();

	/**
	 * Returns the "recent cpu usage" for the whole system from
	 * {@link com.sun.management.OperatingSystemMXBean#getSystemCpuLoad()} if a
	 * user is running the Oracle JVM. This value is a double in the [0.0,1.0]
	 * interval. A value of 0.0 means that all CPUs were idle during the recent
	 * period of time observed, while a value of 1.0 means that all CPUs were
	 * actively running 100% of the time during the recent period being
	 * observed. All values between 0.0 and 1.0 are possible depending of the
	 * activities going on in the system. If the system recent cpu usage is not
	 * available, the method returns a negative value.
	 * 
	 * Calling this method immediately upon instantiating the {@link Processor}
	 * may give unreliable results.
	 * 
	 * If a user is not running the Oracle JVM, this method will default to the
	 * behavior and return value of {@link #getSystemCpuLoadBetweenTicks()}.
	 * 
	 * @return the "recent cpu usage" for the whole system; a negative value if
	 *         not available.
	 */
	@SuppressWarnings("restriction")
	double getSystemCpuLoad();

	/**
	 * Returns the system load average for the last minute from
	 * {@link java.lang.management.OperatingSystemMXBean#getSystemLoadAverage()}
	 * . The system load average is the sum of the number of runnable entities
	 * queued to the available processors and the number of runnable entities
	 * running on the available processors averaged over a period of time. The
	 * way in which the load average is calculated is operating system specific
	 * but is typically a damped time-dependent average.
	 * 
	 * If the load average is not available, a negative value is returned.
	 * 
	 * This method is designed to provide a hint about the system load and may
	 * be queried frequently. The load average may be unavailable on some
	 * platforms (e.g., Windows) where it is expensive to implement this method.
	 * 
	 * @return the system load average; or a negative value if not available.
	 */
	double getSystemLoadAverage();

	/**
	 * Returns the "recent cpu usage" for this processor by counting ticks for
	 * this processor from {@link #getProcessorCpuLoadTicks()} between
	 * successive calls of this method, with a minimum interval slightly less
	 * than 1 second.
	 * 
	 * If less than one second has elapsed since the last call of this method,
	 * it will return a calculation based on the tick counts and times of the
	 * previous two calls. If at least a second has elapsed, it will return the
	 * average CPU load for the interval and update the "last called" times.
	 * 
	 * This method is intended to be used for periodic polling (iterating over
	 * all processors) at intervals of 1 second or longer.
	 * 
	 * @return CPU load between 0 and 1 (100%)
	 */

	double getProcessorCpuLoadBetweenTicks();

	/**
	 * Get this Processor's CPU Load tick counters. Returns an array with four
	 * elements representing clock ticks or milliseconds (platform dependent)
	 * spent in User (0), Nice (1), System (2), and Idle (3) states. By
	 * measuring the difference between ticks across a time interval, CPU load
	 * over that interval may be calculated.
	 * 
	 * @return An array of 4 long values representing time spent in User,
	 *         Nice(if applicable), System, and Idle states.
	 */
	long[] getProcessorCpuLoadTicks();
}
