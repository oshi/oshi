/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
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

import static oshi.util.Memoizer.memoize;

import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.Constants;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.Util;

/**
 * This class represents the entire Central Processing Unit (CPU) of a computer
 * system, which may contain one or more physical packages (sockets), one or
 * more physical processors (cores), and one or more logical processors (what
 * the Operating System sees, which may include hyperthreaded cores.)
 */
@ThreadSafe
public interface CentralProcessor {

    /**
     * The CPU's identifier strings ,including name, vendor, stepping, model, and
     * family information (also called the signature of a CPU)
     *
     * @return a {@link ProcessorIdentifier} object encapsulating CPU identifier
     *         information.
     */
    ProcessorIdentifier getProcessorIdentifier();

    /**
     * Maximum frequeny (in Hz), of the logical processors on this CPU.
     *
     * @return The max frequency or -1 if unknown.
     */
    long getMaxFreq();

    /**
     * Attempts to return the current frequency (in Hz), of the logical processors
     * on this CPU.
     * <p>
     * May not be implemented on all Operating Systems.
     * <p>
     * On Windows, returns an estimate based on the percent of maximum frequency. On
     * Windows systems with more than 64 logical processors, may only return
     * frequencies for the current processor group in the first portion of the
     * array.
     *
     * @return An array of processor frequencies for each logical processor on the
     *         system. Use the {@link #getLogicalProcessors()} to correlate these
     *         frequencies with physical packages and processors.
     */
    long[] getCurrentFreq();

    /**
     * Returns an {@code UnmodifiableList} of the CPU's logical processors. The list
     * will be sorted in order of increasing NUMA node number, and then processor
     * number. This order is consistent with other methods providing per-processor
     * results.
     *
     * @return An {@code UnmodifiabeList} of logical processors.
     */
    List<LogicalProcessor> getLogicalProcessors();

    /**
     * Returns the "recent cpu usage" for the whole system by counting ticks from
     * {@link #getSystemCpuLoadTicks()} between the user-provided value from a
     * previous call.
     *
     * @param oldTicks
     *            A tick array from a previous call to
     *            {@link #getSystemCpuLoadTicks()}
     * @return CPU load between 0 and 1 (100%)
     */
    double getSystemCpuLoadBetweenTicks(long[] oldTicks);

    /**
     * Get System-wide CPU Load tick counters. Returns an array with seven elements
     * representing milliseconds spent in User (0), Nice (1), System (2), Idle (3),
     * IOwait (4), Hardware interrupts (IRQ) (5), Software interrupts/DPC (SoftIRQ)
     * (6), or Steal (7) states. Use
     * {@link oshi.hardware.CentralProcessor.TickType#getIndex()} to retrieve the
     * appropriate index. By measuring the difference between ticks across a time
     * interval, CPU load over that interval may be calculated.
     * <p>
     * Note that while tick counters are in units of milliseconds, they may advance
     * in larger increments along with (platform dependent) clock ticks. For
     * example, by default Windows clock ticks are 1/64 of a second (about 15 or 16
     * milliseconds) and Linux ticks are distribution and configuration dependent
     * but usually 1/100 of a second (10 milliseconds).
     * <p>
     * Nice and IOWait information is not available on Windows, and IOwait and IRQ
     * information is not available on macOS, so these ticks will always be zero.
     * <p>
     * To calculate overall Idle time using this method, include both Idle and
     * IOWait ticks. Similarly, IRQ, SoftIRQ, and Steal ticks should be added to the
     * System value to get the total. System ticks also include time executing other
     * virtual hosts (steal).
     *
     * @return An array of 7 long values representing time spent in User, Nice,
     *         System, Idle, IOwait, IRQ, SoftIRQ, and Steal states.
     */
    long[] getSystemCpuLoadTicks();

    /**
     * Returns the system load average for the number of elements specified, up to
     * 3, representing 1, 5, and 15 minutes. The system load average is the sum of
     * the number of runnable entities queued to the available processors and the
     * number of runnable entities running on the available processors averaged over
     * a period of time. The way in which the load average is calculated is
     * operating system specific but is typically a damped time-dependent average.
     * If the load average is not available, a negative value is returned. This
     * method is designed to provide a hint about the system load and may be queried
     * frequently.
     * <p>
     * The load average may be unavailable on some platforms (e.g., Windows) where
     * it is expensive to implement this method.
     *
     * @param nelem
     *            Number of elements to return.
     * @return an array of the system load averages for 1, 5, and 15 minutes with
     *         the size of the array specified by nelem; or negative values if not
     *         available.
     */
    double[] getSystemLoadAverage(int nelem);

    /**
     * Returns the "recent cpu usage" for all logical processors by counting ticks
     * from {@link #getProcessorCpuLoadTicks()} between the user-provided value from
     * a previous call.
     *
     * @param oldTicks
     *            A tick array from a previous call to
     *            {@link #getProcessorCpuLoadTicks()}
     * @return array of CPU load between 0 and 1 (100%) for each logical processor
     */
    double[] getProcessorCpuLoadBetweenTicks(long[][] oldTicks);

    /**
     * Get Processor CPU Load tick counters. Returns a two dimensional array, with
     * {@link #getLogicalProcessorCount()} arrays, each containing seven elements
     * representing milliseconds spent in User (0), Nice (1), System (2), Idle (3),
     * IOwait (4), Hardware interrupts (IRQ) (5), Software interrupts/DPC (SoftIRQ)
     * (6), or Steal (7) states. Use
     * {@link oshi.hardware.CentralProcessor.TickType#getIndex()} to retrieve the
     * appropriate index. By measuring the difference between ticks across a time
     * interval, CPU load over that interval may be calculated.
     * <p>
     * Note that while tick counters are in units of milliseconds, they may advance
     * in larger increments along with (platform dependent) clock ticks. For
     * example, by default Windows clock ticks are 1/64 of a second (about 15 or 16
     * milliseconds) and Linux ticks are distribution and configuration dependent
     * but usually 1/100 of a second (10 milliseconds).
     * <p>
     * Nice and IOwait per processor information is not available on Windows, and
     * IOwait and IRQ information is not available on macOS, so these ticks will
     * always be zero.
     * <p>
     * To calculate overall Idle time using this method, include both Idle and
     * IOWait ticks. Similarly, IRQ, SoftIRQ and Steal ticks should be added to the
     * System value to get the total. System ticks also include time executing other
     * virtual hosts (steal).
     *
     * @return A 2D array of logicalProcessorCount x 7 long values representing time
     *         spent in User, Nice, System, Idle, IOwait, IRQ, SoftIRQ, and Steal
     *         states.
     */
    long[][] getProcessorCpuLoadTicks();

    /**
     * Get the number of logical CPUs available for processing. This value may be
     * higher than physical CPUs if hyperthreading is enabled.
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
         * CPU utilization that occurred while executing at the user level with nice
         * priority.
         */
        NICE(1),
        /**
         * CPU utilization that occurred while executing at the system level (kernel).
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
         * Time which the hypervisor dedicated for other guests in the system. Only
         * supported on Linux.
         */
        STEAL(7);

        private int index;

        TickType(int value) {
            this.index = value;
        }

        /**
         * @return The integer index of this ENUM in the processor tick arrays, which
         *         matches the output of Linux /proc/cpuinfo
         */
        public int getIndex() {
            return index;
        }
    }

    /**
     * A class representing a Logical Processor and its replationship to physical
     * processors, physical packages, and logical groupings such as NUMA Nodes and
     * Processor groups, useful for identifying processor topology.
     */
    @Immutable
    class LogicalProcessor {
        private final int processorNumber;
        private final int physicalProcessorNumber;
        private final int physicalPackageNumber;
        private final int numaNode;
        private final int processorGroup;

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
         * The Logical Processor number as seen by the Operating System. Used for
         * assigning process affinity and reporting CPU usage and other statistics.
         *
         * @return the processorNumber
         */
        public int getProcessorNumber() {
            return processorNumber;
        }

        /**
         * The physical processor (core) id number assigned to this logical processor.
         * Hyperthreaded logical processors which share the same physical processor will
         * have the same number.
         *
         * @return the physicalProcessorNumber
         */
        public int getPhysicalProcessorNumber() {
            return physicalProcessorNumber;
        }

        /**
         * The physical package (socket) id number assigned to this logical processor.
         * Multicore CPU packages may have multiple physical processors which share the
         * same number.
         *
         * @return the physicalPackageNumber
         */
        public int getPhysicalPackageNumber() {
            return physicalPackageNumber;
        }

        /**
         * The NUMA node. If the operating system supports Non-Uniform Memory Access
         * this identifies the node number. Set to 0 if the operating system does not
         * support NUMA. Not supported on macOS or FreeBSD.
         *
         * @return the NUMA Node number
         */
        public int getNumaNode() {
            return numaNode;
        }

        /**
         * The Processor Group. Only applies to Windows systems with more than 64
         * logical processors. Set to 0 for other operating systems or Windows systems
         * with 64 or fewer logical processors.
         *
         * @return the processorGroup
         */
        public int getProcessorGroup() {
            return processorGroup;
        }
    }

    /**
     * A class encapsulating ghe CPU's identifier strings ,including name, vendor,
     * stepping, model, and family information (also called the signature of a CPU)
     */
    @Immutable
    final class ProcessorIdentifier {
        private static final String OSHI_ARCHITECTURE_PROPERTIES = "oshi.architecture.properties";

        // Provided in constructor
        private final String cpuVendor;
        private final String cpuName;
        private final String cpuFamily;
        private final String cpuModel;
        private final String cpuStepping;
        private final String processorID;
        private final String cpuIdentifier;
        private final boolean cpu64bit;
        private final long cpuVendorFreq;

        private final Supplier<String> microArchictecture = memoize(this::queryMicroarchitecture);

        public ProcessorIdentifier(String cpuVendor, String cpuName, String cpuFamily, String cpuModel,
                String cpuStepping, String processorID, boolean cpu64bit) {
            this.cpuVendor = cpuVendor;
            this.cpuName = cpuName;
            this.cpuFamily = cpuFamily;
            this.cpuModel = cpuModel;
            this.cpuStepping = cpuStepping;
            this.processorID = processorID;
            this.cpu64bit = cpu64bit;

            // Build Identifier
            StringBuilder sb = new StringBuilder();
            if (cpuVendor.contentEquals("GenuineIntel")) {
                sb.append(cpu64bit ? "Intel64" : "x86");
            } else {
                sb.append(cpuVendor);
            }
            sb.append(" Family ").append(cpuFamily);
            sb.append(" Model ").append(cpuModel);
            sb.append(" Stepping ").append(cpuStepping);
            this.cpuIdentifier = sb.toString();

            // Parse Freq from name string
            Pattern pattern = Pattern.compile("@ (.*)$");
            Matcher matcher = pattern.matcher(cpuName);
            if (matcher.find()) {
                String unit = matcher.group(1);
                this.cpuVendorFreq = ParseUtil.parseHertz(unit);
            } else {
                this.cpuVendorFreq = -1L;
            }
        }

        /**
         * Processor vendor.
         *
         * @return vendor string.
         */
        public String getVendor() {
            return cpuVendor;
        }

        /**
         * Name, eg. Intel(R) Core(TM)2 Duo CPU T7300 @ 2.00GHz
         *
         * @return Processor name.
         */
        public String getName() {
            return cpuName;
        }

        /**
         * Gets the family. For non-Intel/AMD processors, returns the comparable value,
         * such as the Architecture.
         *
         * @return the family
         */
        public String getFamily() {
            return cpuFamily;
        }

        /**
         * Gets the model. For non-Intel/AMD processors, returns the comparable value,
         * such as the Partnum.
         *
         * @return the model
         */
        public String getModel() {
            return cpuModel;
        }

        /**
         * Gets the stepping. For non-Intel/AMD processors, returns the comparable
         * value, such as the rnpn composite of Variant and Revision.
         *
         * @return the stepping
         */
        public String getStepping() {
            return cpuStepping;
        }

        /**
         * Gets the Processor ID. This is a hexidecimal string representing an 8-byte
         * value, normally obtained using the CPUID opcode with the EAX register set to
         * 1. The first four bytes are the resulting contents of the EAX register, which
         * is the Processor signature, represented in human-readable form by
         * {@link #getIdentifier()} . The remaining four bytes are the contents of the
         * EDX register, containing feature flags.
         * <p>
         * For processors that do not support the CPUID opcode this field is populated
         * with a comparable hex string. For example, ARM Processors will fill the first
         * 32 bytes with the MIDR.
         * <p>
         * NOTE: The order of returned bytes is platform and software dependent. Values
         * may be in either Big Endian or Little Endian order.
         * <p>
         * NOTE: If OSHI is unable to determine the ProcessorID from native sources, it
         * will attempt to reconstruct one from available information in the processor
         * identifier.
         *
         * @return A string representing the Processor ID
         */
        public String getProcessorID() {
            return processorID;
        }

        /**
         * Identifier, eg. x86 Family 6 Model 15 Stepping 10. For non-Intel/AMD
         * processors, this string is populated with comparable values.
         *
         * @return Processor identifier.
         */
        public String getIdentifier() {
            return cpuIdentifier;
        }

        /**
         * Is CPU 64bit?
         *
         * @return True if cpu is 64bit.
         */
        public boolean isCpu64bit() {
            return cpu64bit;
        }

        /**
         * Vendor frequency (in Hz), eg. for processor named Intel(R) Core(TM)2 Duo CPU
         * T7300 @ 2.00GHz the vendor frequency is 2000000000.
         *
         * @return Processor frequency or -1 if unknown.
         */
        public long getVendorFreq() {
            return cpuVendorFreq;
        }

        /**
         * Returns the processor's microarchitecture, if known.
         *
         * @return A string containing the microarchitecture if known.
         *         {@link Constants#UNKNOWN} otherwise.
         */
        public String getMicroarchitecture() {
            return microArchictecture.get();
        }

        private String queryMicroarchitecture() {
            Properties archProps = FileUtil.readPropertiesFromFilename(OSHI_ARCHITECTURE_PROPERTIES);
            // Intel is default, no prefix
            StringBuilder sb = new StringBuilder();
            // AMD and ARM properties have prefix
            if (this.cpuVendor.contains("AMD")) {
                sb.append("amd.");
            } else if (this.getVendor().contains("ARM")) {
                sb.append("arm.");
            }
            sb.append(this.cpuFamily);
            // Check for match with only family
            String arch = archProps.getProperty(sb.toString());

            if (Util.isBlank(arch)) {
                // Append model
                sb.append('.').append(this.cpuModel);
            }
            arch = archProps.getProperty(sb.toString());

            if (Util.isBlank(arch)) {
                // Append stepping
                sb.append('.').append(this.cpuStepping);
            }
            arch = archProps.getProperty(sb.toString());

            return Util.isBlank(arch) ? Constants.UNKNOWN : arch;
        }

        @Override
        public String toString() {
            return getIdentifier();
        }
    }
}