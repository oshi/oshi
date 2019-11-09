package oshi.jna.platform.windows;

import com.sun.jna.Native; // NOSONAR squid:S1192
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.W32APIOptions;

public interface Kernel32 extends com.sun.jna.platform.win32.Kernel32 {

    Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class, W32APIOptions.DEFAULT_OPTIONS);

    /**
     * Retrieves the process affinity mask for the specified process and the system
     * affinity mask for the system.
     *
     * @param hProcess
     *            A handle to the process whose affinity mask is desired.
     *            <p>
     *            This handle must have the {@link WinNT.PROCESS_QUERY_INFORMATION}
     *            or {@link WinNT.PROCESS_QUERY_LIMITED_INFORMATION} access right.
     * @param lpProcessAffinityMask
     *            A pointer to a variable that receives the affinity mask for the
     *            specified process.
     * @param lpSystemAffinityMask
     *            A pointer to a variable that receives the affinity mask for the
     *            system.
     * @return If the function succeeds, returns {@code true} and the function sets
     *         the variables pointed to by {@code lpProcessAffinityMask} and
     *         {@code lpSystemAffinityMask} to the appropriate affinity masks.
     *         <p>
     *         On a system with more than 64 processors, if the threads of the
     *         calling process are in a single processor group, the function sets
     *         the variables pointed to by {@code lpProcessAffinityMask} and
     *         {@code lpSystemAffinityMask} to the process affinity mask and the
     *         processor mask of active logical processors for that group. If the
     *         calling process contains threads in multiple groups, the function
     *         returns zero for both affinity masks.
     *         <p>
     *         If the function fails, the return value is {@code false}, and the
     *         values of the variables pointed to by {@code lpProcessAffinityMask}
     *         and {@code lpSystemAffinityMask} are undefined. To get extended error
     *         information, call {@link #GetLastError()}.
     */
    boolean GetProcessAffinityMask(HANDLE hProcess, ULONG_PTRByReference lpProcessAffinityMask,
            ULONG_PTRByReference lpSystemAffinityMask);
}
