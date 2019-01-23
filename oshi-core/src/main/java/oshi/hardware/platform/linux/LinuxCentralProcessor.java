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
package oshi.hardware.platform.linux;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.linux.Libc;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcUtil;

/**
 * A CPU as defined in Linux /proc.
 *
 * @author alessandro[at]perucchi[dot]org
 * @author alessio.fachechi[at]gmail[dot]com
 * @author widdis[at]gmail[dot]com
 */
public class LinuxCentralProcessor extends AbstractCentralProcessor {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(LinuxCentralProcessor.class);

    // See https://www.kernel.org/doc/Documentation/cpu-freq/user-guide.txt
    private static final String CPUFREQ_PATH = "/sys/devices/system/cpu/cpu";

    /**
     * Create a Processor
     */
    public LinuxCentralProcessor() {
        super();
        // Initialize class variables
        initVars();

        LOG.debug("Initialized Processor");
    }

    private void initVars() {
        String[] flags = new String[0];
        List<String> cpuInfo = FileUtil.readFile("/proc/cpuinfo");
        for (String line : cpuInfo) {
            String[] splitLine = ParseUtil.whitespacesColonWhitespace.split(line);
            if (splitLine.length < 2) {
                break;
            }
            switch (splitLine[0]) {
            case "vendor_id":
                setVendor(splitLine[1]);
                break;
            case "model name":
                setName(splitLine[1]);
                break;
            case "flags":
                flags = splitLine[1].toLowerCase().split(" ");
                boolean found = false;
                for (String flag : flags) {
                    if ("lm".equals(flag)) {
                        found = true;
                        break;
                    }
                }
                setCpu64(found);
                break;
            case "stepping":
                setStepping(splitLine[1]);
                break;
            case "model":
                setModel(splitLine[1]);
                break;
            case "cpu family":
                setFamily(splitLine[1]);
                break;
            default:
                // Do nothing
            }
        }
        setProcessorID(getProcessorID(getStepping(), getModel(), getFamily(), flags));
    }

    /**
     * Updates logical and physical processor counts from /proc/cpuinfo
     */
    @Override
    protected LogicalProcessor[] initProcessorCounts() {
        Set<String> processorIDs = new HashSet<>();
        Set<Integer> packageIDs = new HashSet<>();

        List<String> procCpu = FileUtil.readFile("/proc/cpuinfo");
        // Iterate once to count logical processors
        for (String cpu : procCpu) {
            if (cpu.startsWith("processor")) {
                this.logicalProcessorCount++;
            }
        }
        // Iterate again to populate
        LogicalProcessor[] logProcs = new LogicalProcessor[this.logicalProcessorCount];
        int currentProcessor = 0;
        for (String cpu : procCpu) {
            // Count logical processors
            if (cpu.startsWith("processor")) {
                currentProcessor = ParseUtil.parseLastInt(cpu, 0);
                logProcs[currentProcessor] = new LogicalProcessor();
                logProcs[currentProcessor].setProcessorNumber(currentProcessor);
            }
            // Count unique combinations of core id and physical id.
            if (cpu.startsWith("core id") || cpu.startsWith("cpu number")) {
                logProcs[currentProcessor].setPhysicalProcessorNumber(ParseUtil.parseLastInt(cpu, 0));
            } else if (cpu.startsWith("physical id")) {
                logProcs[currentProcessor].setPhysicalPackageNumber(ParseUtil.parseLastInt(cpu, 0));
            }
            if (logProcs[currentProcessor].getPhysicalProcessorNumber() >= 0
                    && logProcs[currentProcessor].getPhysicalPackageNumber() >= 0) {
                packageIDs.add(logProcs[currentProcessor].getPhysicalPackageNumber());
                processorIDs.add(logProcs[currentProcessor].getPhysicalProcessorNumber() + " "
                        + logProcs[currentProcessor].getPhysicalPackageNumber());
            }
        }
        // Force at least one processor
        if (this.logicalProcessorCount < 1) {
            LOG.error("Couldn't find logical processor count. Assuming 1.");
            this.logicalProcessorCount = 1;
            logProcs = new LogicalProcessor[1];
            logProcs[0] = new LogicalProcessor();
        }
        this.physicalProcessorCount = processorIDs.size();
        if (this.physicalProcessorCount < 1) {
            LOG.error("Couldn't find physical processor count. Assuming 1.");
            this.physicalProcessorCount = 1;
        }
        this.physicalPackageCount = packageIDs.size();
        if (this.physicalPackageCount < 1) {
            LOG.error("Couldn't find physical package count. Assuming 1.");
            this.physicalPackageCount = 1;
        }
        return logProcs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] querySystemCpuLoadTicks() {
        return ProcUtil.readSystemCpuLoadTicks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] queryCurrentFreq() {
        long[] freqs = new long[getLogicalProcessorCount()];
        // Attempt to fill array from cpu-freq source
        long max = 0L;
        for (int i = 0; i < freqs.length; i++) {
            freqs[i] = FileUtil.getLongFromFile(CPUFREQ_PATH + i + "/cpufreq/scaling_cur_freq");
            if (freqs[i] == 0) {
                freqs[i] = FileUtil.getLongFromFile(CPUFREQ_PATH + i + "/cpufreq/cpuinfo_cur_freq");
            }
            if (max < freqs[i]) {
                max = freqs[i];
            }
        }
        if (max > 0L) {
            // If successful, array is filled with values in KHz.
            for (int i = 0; i < freqs.length; i++) {
                freqs[i] *= 1000L;
            }
            return freqs;
        }
        // If unsuccessful, try from /proc/cpuinfo
        Arrays.fill(freqs, -1);
        List<String> cpuInfo = FileUtil.readFile("/proc/cpuinfo");
        int proc = 0;
        for (String s : cpuInfo) {
            if (s.toLowerCase().contains("cpu mhz")) {
                freqs[proc] = (long) (ParseUtil.parseLastDouble(s, 0d) * 1_000_000);
                if (++proc >= freqs.length) {
                    break;
                }
            }
        }
        return freqs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long queryMaxFreq() {
        long max = 0L;
        for (int i = 0; i < getLogicalProcessorCount(); i++) {
            long freq = FileUtil.getLongFromFile(CPUFREQ_PATH + i + "/cpufreq/scaling_max_freq");
            if (freq == 0) {
                freq = FileUtil.getLongFromFile(CPUFREQ_PATH + i + "/cpufreq/cpuinfo_max_freq");
            }
            if (max < freq) {
                max = freq;
            }
        }
        if (max > 0L) {
            // If successful, value is in KHz.
            return max * 1000L;
        }
        return -1L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        int retval = Libc.INSTANCE.getloadavg(average, nelem);
        if (retval < nelem) {
            for (int i = Math.max(retval, 0); i < average.length; i++) {
                average[i] = -1d;
            }
        }
        return average;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[this.logicalProcessorCount][TickType.values().length];
        // /proc/stat expected format
        // first line is overall user,nice,system,idle, etc.
        // cpu 3357 0 4313 1362393 ...
        // per-processor subsequent lines for cpu0, cpu1, etc.
        int cpu = 0;
        List<String> procStat = FileUtil.readFile("/proc/stat");
        for (String stat : procStat) {
            if (stat.startsWith("cpu") && !stat.startsWith("cpu ")) {
                // Split the line. Note the first (0) element is "cpu" so
                // remaining
                // elements are offset by 1 from the enum index
                String[] tickArr = ParseUtil.whitespaces.split(stat);
                if (tickArr.length <= TickType.IDLE.getIndex()) {
                    // If ticks don't at least go user/nice/system/idle, abort
                    return ticks;
                }
                // Note tickArr is offset by 1
                for (int i = 0; i < TickType.values().length; i++) {
                    ticks[cpu][i] = ParseUtil.parseLongOrDefault(tickArr[i + 1], 0L);
                }
                // Ignore guest or guest_nice, they are included in
                if (++cpu >= this.logicalProcessorCount) {
                    break;
                }
            }
        }
        return ticks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSystemUptime() {
        return (long) ProcUtil.getSystemUptimeSeconds();
    }

    /**
     * Fetches the ProcessorID from dmidecode (if possible with root
     * permissions), the cpuid command (if installed) or by encoding the
     * stepping, model, family, and feature flags.
     *
     * @param stepping
     * @param model
     * @param family
     * @param flags
     * @return The Processor ID string
     */
    private String getProcessorID(String stepping, String model, String family, String[] flags) {
        boolean procInfo = false;
        String marker = "Processor Information";
        for (String checkLine : ExecutingCommand.runNative("dmidecode -t 4")) {
            if (!procInfo && checkLine.contains(marker)) {
                marker = "ID:";
                procInfo = true;
            } else if (procInfo && checkLine.contains(marker)) {
                return checkLine.split(marker)[1].trim();
            }
        }
        // If we've gotten this far, dmidecode failed. Try cpuid.
        marker = "eax=";
        for (String checkLine : ExecutingCommand.runNative("cpuid -1r")) {
            if (checkLine.contains(marker) && checkLine.trim().startsWith("0x00000001")) {
                String eax = "";
                String edx = "";
                for (String register : ParseUtil.whitespaces.split(checkLine)) {
                    if (register.startsWith("eax=")) {
                        eax = ParseUtil.removeMatchingString(register, "eax=0x");
                    } else if (register.startsWith("edx=")) {
                        edx = ParseUtil.removeMatchingString(register, "edx=0x");
                    }
                }
                return edx + eax;
            }
        }
        // If we've gotten this far, dmidecode failed. Encode arguments
        return createProcessorID(stepping, model, family, flags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getContextSwitches() {
        List<String> procStat = FileUtil.readFile("/proc/stat");
        for (String stat : procStat) {
            if (stat.startsWith("ctxt ")) {
                String[] ctxtArr = ParseUtil.whitespaces.split(stat);
                if (ctxtArr.length == 2) {
                    return ParseUtil.parseLongOrDefault(ctxtArr[1], 0);
                }
            }
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getInterrupts() {
        List<String> procStat = FileUtil.readFile("/proc/stat");
        for (String stat : procStat) {
            if (stat.startsWith("intr ")) {
                String[] intrArr = ParseUtil.whitespaces.split(stat);
                if (intrArr.length > 2) {
                    return ParseUtil.parseLongOrDefault(intrArr[1], 0);
                }
            }
        }
        return -1;
    }
}
