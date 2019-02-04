/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware;

import java.io.Serializable;

/**
 * This class represents the entire Central Processing Unit (CPU) of a computer
 * system, which may contain one or more physical packages (sockets), one or
 * more physical processors (cores), and one or more logical processors (what
 * the Operating System sees, which may include hyperthreaded cores.)
 */
public interface CentralProcessor extends Serializable {

    /**
     * Processor vendor.
     *
     * @return vendor string.
     */
    String getVendor();

    /**
     * Name, eg. Intel(R) Core(TM)2 Duo CPU T7300 @ 2.00GHz
     *
     * @return Processor name.
     */
    String getName();

    /**
     * Vendor frequency (in Hz), eg. for processor named Intel(R) Core(TM)2 Duo
     * CPU T7300 @ 2.00GHz the vendor frequency is 2000000000.
     *
     * @return Processor frequency or -1 if unknown.
     */
    long getVendorFreq();

    /**
     * Maximum frequeny (in Hz), of the logical processors on this CPU.
     *
     * @return The max frequency or -1 if unknown.
     */
    long getMaxFreq();

    /**
     * Current frequeny (in Hz), of the logical processors on this CPU.
     *
     * @return An array of processor frequency or -1 if unknown.
     */
    long[] getCurrentFreq();

    /**
     * Gets the Processor ID. This is a hexidecimal string representing an
     * 8-byte value, normally obtained using the CPUID opcode with the EAX
     * register set to 1. The first four bytes are the resulting contents of the
     * EAX register, which is the Processor signature, represented in
     * human-readable form by {@link #getIdentifier()} . The remaining four
     * bytes are the contents of the EDX register, containing feature flags.
     * <p>
     * NOTE: The order of returned bytes is platform and software dependent.
     * Values may be in either Big Endian or Little Endian order.
     *
     * @return A string representing the Processor ID
     */
    String getProcessorID();

    /**
     * Identifier, eg. x86 Family 6 Model 15 Stepping 10.
     *
     * @return Processor identifier.
     */
    String getIdentifier();

    /**
     * Is CPU 64bit?
     *
     * @return True if cpu is 64bit.
     */
    boolean isCpu64bit();

    /**
     * @return the stepping
     */
    String getStepping();

    /**
     * @return the model
     */
    String getModel();

    /**
     * @return the family
     */
    String getFamily();

    /**
     * Returns an array of the CPU's logical processors. The array will be
     * sorted in order of increasing NUMA node number, and then processor
     * number. This order is consistent with other methods providing
     * per-processor results.
     * 
     * @return The logical processor array.
     */
    LogicalProcessor[] getLogicalProcessors();

    /**
     * Returns the "recent cpu usage" for the whole system by counting ticks
     * from {@link #getSystemCpuLoadTicks()} between the user-provided value
     * from a previous call.
     * 
     * @param oldTicks
     *            A tick array from a previous call to
     *            {@link #getSystemCpuLoadTicks()}
     * 
     * @return CPU load between 0 and 1 (100%)
     */
    double getSystemCpuLoadBetweenTicks(long[] oldTicks);

    /**
     * Get System-wide CPU Load tick counters. Returns an array with seven
     * elements representing milliseconds spent in User (0), Nice (1), System
     * (2), Idle (3), IOwait (4), Hardware interrupts (IRQ) (5), Software
     * interrupts/DPC (SoftIRQ) (6), or Steal (7) states. Use
     * {@link TickType#getIndex()} to retrieve the appropriate index. By
     * measuring the difference between ticks across a time interval, CPU load
     * over that interval may be calculated.
     * <p>
     * Note that while tick counters are in units of milliseconds, they may
     * advance in larger increments (platform dependent). For example, by
     * default Windows clock ticks are 1/64 of a second (about 15 or 16
     * milliseconds) and Linux ticks are distribution and configuration
     * dependent but usually 1/100 of a second (10 milliseconds).
     * <p>
     * Nice and IOWait information is not available on Windows, and IOwait and
     * IRQ information is not available on macOS, so these ticks will always be
     * zero.
     * <p>
     * To calculate overall Idle time using this method, include both Idle and
     * IOWait ticks. Similarly, IRQ, SoftIRQ, and Steal ticks should be added to
     * the System value to get the total. System ticks also include time
     * executing other virtual hosts (steal).
     *
     * @return An array of 7 long values representing time spent in User, Nice,
     *         System, Idle, IOwait, IRQ, SoftIRQ, and Steal states.
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
     * activities going on in the system.
     * <P>
     * If the system recent cpu usage is not available, the method returns a
     * negative value. Calling this method immediately upon instantiating the
     * {@link CentralProcessor} may give unreliable results. Calling this method
     * too frequently may return {@link Double#NaN}. If a user is not running
     * the Oracle JVM, this method will return a negative value.
     *
     * @return the "recent cpu usage" for the whole system; a negative value if
     *         not available.
     */
    @SuppressWarnings("restriction")
    double getSystemCpuLoad();

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
     * ticks from {@link #getProcessorCpuLoadTicks()} between the user-provided
     * value from a previous call.
     *
     * @param oldTicks
     *            A tick array from a previous call to
     *            {@link #getProcessorCpuLoadTicks()}
     * @return array of CPU load between 0 and 1 (100%) for each logical
     *         processor
     */
    double[] getProcessorCpuLoadBetweenTicks(long[][] oldTicks);

    /**
     * Get Processor CPU Load tick counters. Returns a two dimensional array,
     * with {@link #getLogicalProcessorCount()} arrays, each containing seven
     * elements representing milliseconds spent in User (0), Nice (1), System
     * (2), Idle (3), IOwait (4), Hardware interrupts (IRQ) (5), Software
     * interrupts/DPC (SoftIRQ) (6), or Steal (7) states. Use
     * {@link TickType#getIndex()} to retrieve the appropriate index. By
     * measuring the difference between ticks across a time interval, CPU load
     * over that interval may be calculated.
     * <p>
     * Note that while tick counters are in units of milliseconds, they may
     * advance in larger increments (platform dependent). For example, by
     * default Windows clock ticks are 1/64 of a second (about 15 or 16
     * milliseconds) and Linux ticks are distribution and configuration
     * dependent but usually 1/100 of a second (10 milliseconds).
     * <p>
     * Nice and IOwait per processor information is not available on Windows,
     * and IOwait and IRQ information is not available on macOS, so these ticks
     * will always be zero.
     * <p>
     * To calculate overall Idle time using this method, include both Idle and
     * IOWait ticks. Similarly, IRQ, SoftIRQ and Steal ticks should be added to
     * the System value to get the total. System ticks also include time
     * executing other virtual hosts (steal).
     *
     * @return A 2D array of logicalProcessorCount x 7 long values representing
     *         time spent in User, Nice, System, Idle, IOwait, IRQ, SoftIRQ, and
     *         Steal states.
     */
    long[][] getProcessorCpuLoadTicks();

    /**
     * Get the System uptime (time since boot).
     *
     * @return Number of seconds since boot.
     */
    long getSystemUptime();

    /**
     * Get the number of logical CPUs available for processing. This value may
     * be higher than physical CPUs if hyperthreading is enabled.
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
     * Get the number of packages/sockets in the system. A single package may
     * contain multiple cores.
     *
     * @return The number of physical packages available.
     */
    int getPhysicalPackageCount();

    /**
     * Get the number of context switches which have occurred
     *
     * @return The number of context switches
     */
    long getContextSwitches();

    /**
     * Get the number of interrupts which have occurred
     *
     * @return The number of interrupts
     */
    long getInterrupts();

    /**
     * Update the values for the next call to the getters on this class.
     */
    void updateAttributes();

    /**
     * Index of CPU tick counters in the {@link #getSystemCpuLoadTicks()} and
     * {@link #getProcessorCpuLoadTicks()} arrays.
     */
    enum TickType {
        /**
         * CPU utilization that occurred while executing at the user level
         * (application).
         */
        USER(0),
        /**
         * CPU utilization that occurred while executing at the user level with
         * nice priority.
         */
        NICE(1),
        /**
         * CPU utilization that occurred while executing at the system level
         * (kernel).
         */
        SYSTEM(2),
        /**
         * Time that the CPU or CPUs were idle and the system did not have an
         * outstanding disk I/O request.
         */
        IDLE(3),
        /**
         * Time that the CPU or CPUs were idle during which the system had an
         * outstanding disk I/O request.
         */
        IOWAIT(4),
        /**
         * Time that the CPU used to service hardware IRQs
         */
        IRQ(5),
        /**
         * Time that the CPU used to service soft IRQs
         */
        SOFTIRQ(6),
        /**
         * Time which the hypervisor dedicated for other guests in the system.
         * Only supported on Linux.
         */
        STEAL(7);

        private int index;

        TickType(int value) {
            this.index = value;
        }

        /**
         * @return The integer index of this ENUM in the processor tick arrays,
         *         which matches the output of Linux /proc/cpuinfo
         */
        public int getIndex() {
            return index;
        }
    }

    /**
     * A class representing a Logical Processor and its replationship to
     * physical processors, physical packages, and logical groupings such as
     * NUMA Nodes and Processor groups, useful for identifying processor
     * topology.
     */
    class LogicalProcessor implements Serializable {
        private static final long serialVersionUID = 1L;

        private int processorNumber;
        private int physicalProcessorNumber;
        private int physicalPackageNumber;
        private int numaNode;
        private int processorGroup;

        /**
         * @param processorNumber
         *            the Processor number
         * @param physicalProcessorNumber
         *            the core number
         * @param physicalPackageNumber
         *            the package/socket number
         */
        public LogicalProcessor(int processorNumber, int physicalProcessorNumber, int physicalPackageNumber) {
            this(processorNumber, physicalProcessorNumber, physicalPackageNumber, 0, 0);
        }

        /**
         * @param processorNumber
         *            the Processor number
         * @param physicalProcessorNumber
         *            the core number
         * @param physicalPackageNumber
         *            the package/socket number
         * @param numaNode
         *            the NUMA node number
         */
        public LogicalProcessor(int processorNumber, int physicalProcessorNumber, int physicalPackageNumber,
                int numaNode) {
            this(processorNumber, physicalProcessorNumber, physicalPackageNumber, numaNode, 0);
        }

        /**
         * @param processorNumber
         *            the Processor number
         * @param physicalProcessorNumber
         *            the core number
         * @param physicalPackageNumber
         *            the package/socket number
         * @param numaNode
         *            the NUMA node number
         * @param processorGroup
         *            the Processor Group number
         */
        public LogicalProcessor(int processorNumber, int physicalProcessorNumber, int physicalPackageNumber,
                int numaNode, int processorGroup) {
            this.processorNumber = processorNumber;
            this.physicalProcessorNumber = physicalProcessorNumber;
            this.physicalPackageNumber = physicalPackageNumber;
            this.numaNode = numaNode;
            this.processorGroup = processorGroup;
        }

        /**
         * The Logical Processor number as seen by the Operating System. Used
         * for assigning process affinity and reporting CPU usage and other
         * statistics.
         * 
         * @return the processorNumber
         */
        public int getProcessorNumber() {
            return processorNumber;
        }

        /**
         * The physical processor (core) id number assigned to this logical
         * processor. Hyperthreaded logical processors which share the same
         * physical processor will have the same number.
         * 
         * @return the physicalProcessorNumber
         */
        public int getPhysicalProcessorNumber() {
            return physicalProcessorNumber;
        }

        /**
         * The physical package (socket) id number assigned to this logical
         * processor. Multicore CPU packages may have multiple physical
         * processors which share the same number.
         * 
         * @return the physicalPackageNumber
         */
        public int getPhysicalPackageNumber() {
            return physicalPackageNumber;
        }

        /**
         * The NUMA node. If the operating system supports Non-Uniform Memory
         * Access this identifies the node number. Set to 0 if the operating
         * system does not support NUMA. Not supported on macOS or FreeBSD.
         * 
         * @return the NUMA Node number
         */
        public int getNumaNode() {
            return numaNode;
        }

        /**
         * The Processor Group. Only applies to Windows systems with more than
         * 64 logical processors. Set to 0 for other operating systems or
         * Windows systems with 64 or fewer logical processors.
         * 
         * @return the processorGroup
         */
        public int getProcessorGroup() {
            return processorGroup;
        }

    }
}
