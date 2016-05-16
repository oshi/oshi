/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware;

import oshi.json.OshiJsonObject;
import oshi.software.os.OSProcess;

/**
 * The Central Processing Unit (CPU) or the processor is the portion of a
 * computer system that carries out the instructions of a computer program, and
 * is the primary element carrying out the computer's functions.
 * 
 * @author dblock[at]dblock[dot]org
 */
public interface CentralProcessor extends OshiJsonObject {
    /**
     * Processor vendor.
     * 
     * @return vendor string.
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
     * Vendor frequency (in Hz), eg. for processor named Intel(R) Core(TM)2 Duo
     * CPU T7300 @ 2.00GHz the vendor frequency is 2000000000.
     * 
     * @return Processor frequency or -1 if unknown.
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
     * method, with a minimum interval slightly less than 1 second. If less than
     * one second has elapsed since the last call of this method, it will return
     * a calculation based on the tick counts and times of the previous two
     * calls. If at least a second has elapsed, it will return the average CPU
     * load for the interval and update the "last called" times. This method is
     * intended to be used for periodic polling at intervals of 1 second or
     * longer.
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
     * The Idle time for this method includes system time spent idle waiting for
     * IO as reported by {@link #getSystemIOWaitTicks()}. The system time
     * includes system time spent servicing Hardware and Software IRQ requests
     * as reported by {@link #getSystemIrqTicks()} as well as executing other
     * virtual hosts (steal) or running a virtual cpu (guest).
     * 
     * @return An array of 4 long values representing time spent in User,
     *         Nice(if applicable), System, and Idle states.
     */
    long[] getSystemCpuLoadTicks();

    /**
     * Get System IOWait tick counters (if available on that Operating System).
     * Time spent waiting for IO to complete is included in the idle time
     * calculated by {@link #getSystemCpuLoadTicks()} but is provided separately
     * for more detail.
     * 
     * @return a long value representing time spent idle waiting for IO to
     *         complete.
     */
    long getSystemIOWaitTicks();

    /**
     * Get System IRQ tick counters (if available on that Operating System).
     * Time spent servicing hardware and software (Deferred Procedure Call)
     * interrupts is included in the system time calculated by
     * {@link #getSystemCpuLoadTicks()} but is provided separately for more
     * detail.
     * 
     * @return an array of two long values representing time spent servicing
     *         hardware interrupts and software interrupts (DPC)
     */
    long[] getSystemIrqTicks();

    /**
     * Returns the "recent cpu usage" for the whole system from
     * {@link com.sun.management.OperatingSystemMXBean#getSystemCpuLoad()} if a
     * user is running the Oracle JVM. This value is a double in the [0.0,1.0]
     * interval. A value of 0.0 means that all CPUs were idle during the recent
     * period of time observed, while a value of 1.0 means that all CPUs were
     * actively running 100% of the time during the recent period being
     * observed. All values between 0.0 and 1.0 are possible depending of the
     * activities going on in the system. If the system recent cpu usage is not
     * available, the method returns a negative value. Calling this method
     * immediately upon instantiating the {@link CentralProcessor} may give
     * unreliable results. If a user is not running the Oracle JVM, this method
     * will default to the behavior and return value of
     * {@link #getSystemCpuLoadBetweenTicks()}.
     * 
     * @return the "recent cpu usage" for the whole system; a negative value if
     *         not available.
     */
    @SuppressWarnings("restriction")
    double getSystemCpuLoad();

    /**
     * Returns the system load average for the last minute. This is equivalent
     * to calling {@link CentralProcessor#getSystemLoadAverage(int)} with an
     * argument of 1 and returning the first value, and is retained for
     * compatibility.
     * 
     * @return the system load average; or a negative value if not available.
     */
    double getSystemLoadAverage();

    /**
     * Returns the system load average for the number of elements specified, up
     * to 3, representing 1, 5, and 15 minutes. The system load average is the
     * sum of the number of runnable entities queued to the available processors
     * and the number of runnable entities running on the available processors
     * averaged over a period of time. The way in which the load average is
     * calculated is operating system specific but is typically a damped
     * time-dependent average. If the load average is not available, a negative
     * value is returned. This method is designed to provide a hint about the
     * system load and may be queried frequently. The load average may be
     * unavailable on some platforms (e.g., Windows) where it is expensive to
     * implement this method.
     * 
     * @param nelem
     *            Number of elements to return.
     * @return an array of the system load averages for 1, 5, and 15 minutes
     *         with the size of the array specified by nelem; or negative values
     *         if not available.
     */
    double[] getSystemLoadAverage(int nelem);

    /**
     * Returns the "recent cpu usage" for all logical processors by counting
     * ticks for the processors from {@link #getProcessorCpuLoadTicks()} between
     * successive calls of this method, with a minimum interval slightly less
     * than 1 second. If less than one second has elapsed since the last call of
     * this method, it will return a calculation based on the tick counts and
     * times of the previous two calls. If at least a second has elapsed, it
     * will return the average CPU load for the interval and update the
     * "last called" times. This method is intended to be used for periodic
     * polling (iterating over all processors) at intervals of 1 second or
     * longer.
     * 
     * @return array of CPU load between 0 and 1 (100%) for each logical
     *         processor
     */
    double[] getProcessorCpuLoadBetweenTicks();

    /**
     * Get Processor CPU Load tick counters. Returns a two dimensional array,
     * with {@link #getLogicalProcessorCount()} arrays, each containing four
     * elements representing clock ticks or milliseconds (platform dependent)
     * spent in User (0), Nice (1), System (2), and Idle (3) states. By
     * measuring the difference between ticks across a time interval, CPU load
     * over that interval may be calculated.
     * 
     * The Idle time for this method includes processor time spent idle waiting
     * for IO as reported by {@link #getSystemIOWaitTicks()}. The system time
     * includes processor time spent servicing Hardware IRQ requests as reported
     * by {@link #getSystemIrqTicks()} as well as Software IRQ requests
     * (softirq), executing other virtual hosts (steal) or running a virtual cpu
     * (guest).
     * 
     * @return A 2D array of logicalProcessorCount x 4 long values representing
     *         time spent in User, Nice(if applicable), System, and Idle states.
     */
    long[][] getProcessorCpuLoadTicks();

    /**
     * Get the System uptime (time since boot).
     * 
     * @return Number of seconds since boot.
     */
    long getSystemUptime();

    /**
     * Get the System/CPU Serial Number, if available. On Linux, this requires
     * either root permissions, or installation of the (deprecated) HAL library
     * (lshal command).
     * 
     * @return the System/CPU Serial Number, if available, otherwise returns
     *         "unknown"
     */
    String getSystemSerialNumber();

    /**
     * Get the number of logical CPUs available for processing.
     * 
     * @return The number of logical CPUs available.
     */
    int getLogicalProcessorCount();

    /**
     * Get the number of physical CPUs/cores available for processing.
     * 
     * @return The number of physical CPUs available.
     */
    int getPhysicalProcessorCount();

    /**
     * Gets currently running processes
     * 
     * @return An array of {@link oshi.software.os.OSProcess} objects for
     *         currently running processes
     */
    OSProcess[] getProcesses();

    /**
     * Gets information on a currently running process
     * 
     * @param pid
     *            A process ID
     * @return An {@link oshi.software.os.OSProcess} object for the specified
     *         process id if it is running; null otherwise currently running
     *         processes
     */
    OSProcess getProcess(int pid);

    /**
     * Get the number of processes currently running
     * 
     * @return The number of processes running
     */
    int getProcessCount();

    /**
     * Get the number of threads currently running
     * 
     * @return The number of threads running
     */
    int getThreadCount();
}
