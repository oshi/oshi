package oshi.driver.windows.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory; // NOSONAR squid:S1191
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinPerf.PERF_COUNTER_BLOCK;
import com.sun.jna.platform.win32.WinPerf.PERF_COUNTER_DEFINITION;
import com.sun.jna.platform.win32.WinPerf.PERF_DATA_BLOCK;
import com.sun.jna.platform.win32.WinPerf.PERF_INSTANCE_DEFINITION;
import com.sun.jna.platform.win32.WinPerf.PERF_OBJECT_TYPE;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.ptr.IntByReference;

import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

public class HkeyPerformanceData {

    private static final Logger LOG = LoggerFactory.getLogger(HkeyPerformanceData.class);

    private static final String ENGLISH_COUNTER_KEY = "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Perflib\\009";

    private static final String COUNTER = "Counter";
    private static final String PROCESS = "Process";

    private static final String PRIORITY_BASE = "Priority Base";
    private static final String ELAPSED_TIME = "Elapsed Time";
    private static final String ID_PROCESS = "ID Process";
    private static final String CREATING_PROCESS_ID = "Creating Process ID";
    private static final String IO_READ_BYTES_SEC = "IO Read Bytes/sec";
    private static final String IO_WRITE_BYTES_SEC = "IO Write Bytes/sec";
    private static final String WORKING_SET_PRIVATE = "Working Set - Private";

    /*
     * Grow as needed but persist
     */
    private int perfDataBufferSize = 8192;
    /*
     * Process counter index in integer and string form
     */
    private int processIndex; // 6
    private String processIndexStr; // "6"
    /*
     * Registry counter data byte offsets
     */
    private int priorityBaseOffset; // 92
    private int elapsedTimeOffset; // 96
    private int idProcessOffset; // 104
    private int creatingProcessIdOffset; // 108
    private int ioReadOffset; // 160
    private int ioWriteOffset; // 168
    private int workingSetPrivateOffset; // 192

    public HkeyPerformanceData() throws InstantiationException {
        // Get the title indices
        int priorityBaseIndex = 0;
        int elapsedTimeIndex = 0;
        int idProcessIndex = 0;
        int creatingProcessIdIndex = 0;
        int ioReadIndex = 0;
        int ioWriteIndex = 0;
        int workingSetPrivateIndex = 0;

        try {

            // Look up list of english names and ids
            String[] counters = Advapi32Util.registryGetStringArray(WinReg.HKEY_LOCAL_MACHINE, ENGLISH_COUNTER_KEY,
                    COUNTER);
            // Array contains alternating index/name pairs
            // "1", "1847", "2", "System", "4", "Memory", ...
            // Get position of name in the array (odd index), return parsed value of
            // previous even index
            for (int i = 1; i < counters.length; i += 2) {
                if (counters[i].equals(PROCESS)) {
                    this.processIndex = Integer.parseInt(counters[i - 1]);
                } else if (counters[i].equals(PRIORITY_BASE)) {
                    priorityBaseIndex = Integer.parseInt(counters[i - 1]);
                } else if (counters[i].equals(ELAPSED_TIME)) {
                    elapsedTimeIndex = Integer.parseInt(counters[i - 1]);
                } else if (counters[i].equals(ID_PROCESS)) {
                    idProcessIndex = Integer.parseInt(counters[i - 1]);
                } else if (counters[i].equals(CREATING_PROCESS_ID)) {
                    creatingProcessIdIndex = Integer.parseInt(counters[i - 1]);
                } else if (counters[i].equals(IO_READ_BYTES_SEC)) {
                    ioReadIndex = Integer.parseInt(counters[i - 1]);
                } else if (counters[i].equals(IO_WRITE_BYTES_SEC)) {
                    ioWriteIndex = Integer.parseInt(counters[i - 1]);
                } else if (counters[i].equals(WORKING_SET_PRIVATE)) {
                    workingSetPrivateIndex = Integer.parseInt(counters[i - 1]);
                }
            }
        } catch (NumberFormatException e) {
            // Unexpected but handle anyway
            throw new InstantiationException("Failed to parse counter index/name array.");
        } catch (Win32Exception e) {
            throw new InstantiationException("Unable to locate English counter names in registry Perflib 009.");
        }
        // If any of the indices are 0, we failed
        if (this.processIndex == 0 || priorityBaseIndex == 0 || elapsedTimeIndex == 0 || idProcessIndex == 0
                || creatingProcessIdIndex == 0 || ioReadIndex == 0 || ioWriteIndex == 0
                || workingSetPrivateIndex == 0) {
            throw new InstantiationException("Failed to parse counter index/name array.");
        }
        this.processIndexStr = Integer.toString(this.processIndex);

        // now load the Process registry to match up the offsets
        // Sequentially increase the buffer until everything fits.
        // Save this buffer size for later use
        IntByReference lpcbData = new IntByReference(this.perfDataBufferSize);
        Pointer pPerfData = new Memory(this.perfDataBufferSize);
        int ret = Advapi32.INSTANCE.RegQueryValueEx(WinReg.HKEY_PERFORMANCE_DATA, this.processIndexStr, 0, null,
                pPerfData, lpcbData);
        if (ret != WinError.ERROR_SUCCESS && ret != WinError.ERROR_MORE_DATA) {
            throw new InstantiationException("Error " + ret + " reading HKEY_PERFORMANCE_DATA from the registry.");
        }
        while (ret == WinError.ERROR_MORE_DATA) {
            this.perfDataBufferSize += 4096;
            lpcbData.setValue(this.perfDataBufferSize);
            pPerfData = new Memory(this.perfDataBufferSize);
            ret = Advapi32.INSTANCE.RegQueryValueEx(WinReg.HKEY_PERFORMANCE_DATA, this.processIndexStr, 0, null,
                    pPerfData, lpcbData);
        }

        PERF_DATA_BLOCK perfData = new PERF_DATA_BLOCK(pPerfData.share(0));

        // See format at
        // https://msdn.microsoft.com/en-us/library/windows/desktop/aa373105(v=vs.85).aspx
        // [ ] Object Type
        // [ ][ ][ ] Multiple counter definitions
        // Then multiple:
        // [ ] Instance Definition
        // [ ] Instance name
        // [ ] Counter Block
        // [ ][ ][ ] Counter data for each definition above

        long perfObjectOffset = perfData.HeaderLength;

        // Iterate object types. For Process should only be one here
        for (int obj = 0; obj < perfData.NumObjectTypes; obj++) {
            PERF_OBJECT_TYPE perfObject = new PERF_OBJECT_TYPE(pPerfData.share(perfObjectOffset));
            // Identify where counter definitions start
            long perfCounterOffset = perfObjectOffset + perfObject.HeaderLength;
            // If this isn't the Process object, ignore
            if (perfObject.ObjectNameTitleIndex == this.processIndex) {
                for (int counter = 0; counter < perfObject.NumCounters; counter++) {
                    PERF_COUNTER_DEFINITION perfCounter = new PERF_COUNTER_DEFINITION(
                            pPerfData.share(perfCounterOffset));
                    if (perfCounter.CounterNameTitleIndex == priorityBaseIndex) {
                        this.priorityBaseOffset = perfCounter.CounterOffset;
                    } else if (perfCounter.CounterNameTitleIndex == elapsedTimeIndex) {
                        this.elapsedTimeOffset = perfCounter.CounterOffset;
                    } else if (perfCounter.CounterNameTitleIndex == creatingProcessIdIndex) {
                        this.creatingProcessIdOffset = perfCounter.CounterOffset;
                    } else if (perfCounter.CounterNameTitleIndex == idProcessIndex) {
                        this.idProcessOffset = perfCounter.CounterOffset;
                    } else if (perfCounter.CounterNameTitleIndex == ioReadIndex) {
                        this.ioReadOffset = perfCounter.CounterOffset;
                    } else if (perfCounter.CounterNameTitleIndex == ioWriteIndex) {
                        this.ioWriteOffset = perfCounter.CounterOffset;
                    } else if (perfCounter.CounterNameTitleIndex == workingSetPrivateIndex) {
                        this.workingSetPrivateOffset = perfCounter.CounterOffset;
                    }
                    // Increment for next Counter
                    perfCounterOffset += perfCounter.ByteLength;
                }
                // We're done, break the loop
                break;
            }
            // Increment for next object (should never need this)
            perfObjectOffset += perfObject.TotalByteLength;
        }
    }

    public Map<Integer, OSProcess> buildProcessMapFromRegistry(OperatingSystem os, Collection<Integer> pids) {
        Map<Integer, OSProcess> processMap = new HashMap<>();
        // Grab the PERF_DATA_BLOCK from the registry.
        // Sequentially increase the buffer until everything fits.
        IntByReference lpcbData = new IntByReference(this.perfDataBufferSize);
        Pointer pPerfData = new Memory(this.perfDataBufferSize);
        int ret = Advapi32.INSTANCE.RegQueryValueEx(WinReg.HKEY_PERFORMANCE_DATA, this.processIndexStr, 0, null,
                pPerfData, lpcbData);
        if (ret != WinError.ERROR_SUCCESS && ret != WinError.ERROR_MORE_DATA) {
            LOG.error("Error {} reading HKEY_PERFORMANCE_DATA from the registry.", ret);
            return processMap;
        }
        while (ret == WinError.ERROR_MORE_DATA) {
            this.perfDataBufferSize += 4096;
            lpcbData.setValue(this.perfDataBufferSize);
            pPerfData = new Memory(this.perfDataBufferSize);
            ret = Advapi32.INSTANCE.RegQueryValueEx(WinReg.HKEY_PERFORMANCE_DATA, this.processIndexStr, 0, null,
                    pPerfData, lpcbData);
        }

        PERF_DATA_BLOCK perfData = new PERF_DATA_BLOCK(pPerfData.share(0));
        long perfTime100nSec = perfData.PerfTime100nSec.getValue(); // 1601
        long now = System.currentTimeMillis(); // 1970 epoch

        // See format at
        // https://msdn.microsoft.com/en-us/library/windows/desktop/aa373105(v=vs.85).aspx
        // [ ] Object Type
        // [ ][ ][ ] Multiple counter definitions
        // Then multiple:
        // [ ] Instance Definition
        // [ ] Instance name
        // [ ] Counter Block
        // [ ][ ][ ] Counter data for each definition above
        long perfObjectOffset = perfData.HeaderLength;

        // Iterate object types. For Process should only be one here
        for (int obj = 0; obj < perfData.NumObjectTypes; obj++) {
            PERF_OBJECT_TYPE perfObject = new PERF_OBJECT_TYPE(pPerfData.share(perfObjectOffset));
            // If this isn't the Process object, ignore
            if (perfObject.ObjectNameTitleIndex == this.processIndex) {
                // Skip over counter definitions
                // There will be many of these, this points to the first one
                long perfInstanceOffset = perfObjectOffset + perfObject.DefinitionLength;

                // We need this for every process, initialize outside loop to
                // save overhead
                PERF_COUNTER_BLOCK perfCounterBlock = null;
                // Iterate instances.
                // The last instance is _Total so subtract 1 from max
                for (int inst = 0; inst < perfObject.NumInstances - 1; inst++) {
                    PERF_INSTANCE_DEFINITION perfInstance = new PERF_INSTANCE_DEFINITION(
                            pPerfData.share(perfInstanceOffset));
                    long perfCounterBlockOffset = perfInstanceOffset + perfInstance.ByteLength;

                    int pid = pPerfData.getInt(perfCounterBlockOffset + this.idProcessOffset);
                    if (pids == null || pids.contains(pid)) {
                        OSProcess proc = new OSProcess(os);
                        processMap.put(pid, proc);

                        proc.setProcessID(pid);
                        proc.setName(pPerfData.getWideString(perfInstanceOffset + perfInstance.NameOffset));
                        long upTime = (perfTime100nSec
                                - pPerfData.getLong(perfCounterBlockOffset + this.elapsedTimeOffset)) / 10_000L;
                        proc.setUpTime(upTime < 1L ? 1L : upTime);
                        proc.setStartTime(now - upTime);
                        proc.setBytesRead(pPerfData.getLong(perfCounterBlockOffset + this.ioReadOffset));
                        proc.setBytesWritten(pPerfData.getLong(perfCounterBlockOffset + this.ioWriteOffset));
                        proc.setResidentSetSize(
                                pPerfData.getLong(perfCounterBlockOffset + this.workingSetPrivateOffset));
                        proc.setParentProcessID(
                                pPerfData.getInt(perfCounterBlockOffset + this.creatingProcessIdOffset));
                        proc.setPriority(pPerfData.getInt(perfCounterBlockOffset + this.priorityBaseOffset));
                    }

                    // Increment to next instance
                    perfCounterBlock = new PERF_COUNTER_BLOCK(pPerfData.share(perfCounterBlockOffset));
                    perfInstanceOffset = perfCounterBlockOffset + perfCounterBlock.ByteLength;
                }
                // We've found the process object and are done, no need to look at any other
                // objects (shouldn't be any). Break the loop
                break;
            }
            // Increment for next object (should never need this)
            perfObjectOffset += perfObject.TotalByteLength;
        }
        return processMap;
    }
}