/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import oshi.ffm.ForeignFunctions;

/**
 * Implementations of MacOS functions
 */
public final class MacSystemFunctions extends ForeignFunctions {

    private MacSystemFunctions() {
    }

    private static final SymbolLookup SYSTEM_LIBRARY = libraryLookup("System");

    public static final ValueLayout.OfLong SIZE_T = ValueLayout.JAVA_LONG;

    // int proc_listpids(uint32_t type, uint32_t typeinfo, void *buffer, int buffersize)
    private static final MethodHandle proc_listpids = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("proc_listpids"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT));

    public static int proc_listpids(int type, int typeinfo, MemorySegment pids, int bufferSize) throws Throwable {
        return (int) proc_listpids.invokeExact(type, typeinfo, pids, bufferSize);
    }

    // int proc_pidinfo(int pid, int flavor, uint64_t arg, void *buffer, int buffersize)
    private static final MethodHandle proc_pidinfo = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("proc_pidinfo"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT));

    public static int proc_pidinfo(int pid, int flavor, long arg, MemorySegment buffer, int bufferSize)
            throws Throwable {
        return (int) proc_pidinfo.invokeExact(pid, flavor, arg, buffer, bufferSize);
    }

    // int proc_pidpath(int pid, void * buffer, uint32_t buffersize)
    private static final MethodHandle proc_pidpath = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("proc_pidpath"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT));

    public static int proc_pidpath(int pid, MemorySegment buffer, int bufferSize) throws Throwable {
        return (int) proc_pidpath.invokeExact(pid, buffer, bufferSize);
    }

    // int proc_pid_rusage(int pid, int flavor, rusage_info_t *buffer)
    private static final MethodHandle proc_pid_rusage = LINKER.downcallHandle(
            SYSTEM_LIBRARY.findOrThrow("proc_pid_rusage"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS));

    public static int proc_pid_rusage(int pid, int flavor, MemorySegment buffer) throws Throwable {
        return (int) proc_pid_rusage.invokeExact(pid, flavor, buffer);
    }

    // int proc_pidfdinfo(int pid, int fd, int flavor, void * buffer, int buffersize)
    private static final MethodHandle proc_pidfdinfo = LINKER.downcallHandle(
            SYSTEM_LIBRARY.findOrThrow("proc_pidfdinfo"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT));

    public static int proc_pidfdinfo(int pid, int fd, int flavor, MemorySegment buffer, int bufferSize)
            throws Throwable {
        return (int) proc_pidfdinfo.invokeExact(pid, fd, flavor, buffer, bufferSize);
    }

    // struct passwd * q getpwuid(uid_t uid);
    private static final MethodHandle getpwuid = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("getpwuid"),
            FunctionDescriptor.of(ADDRESS, JAVA_INT));

    public static MemorySegment getpwuid(int uid) throws Throwable {
        MemorySegment result = (MemorySegment) getpwuid.invokeExact(uid);
        return result.equals(MemorySegment.NULL) ? null : result;
    }

    // struct group * getgrgid(gid_t gid);
    private static final MethodHandle getgrgid = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("getgrgid"),
            FunctionDescriptor.of(ADDRESS, JAVA_INT));

    public static MemorySegment getgrgid(int gid) throws Throwable {
        MemorySegment result = (MemorySegment) getgrgid.invokeExact(gid);
        return result.equals(MemorySegment.NULL) ? null : result;
    }

    // pid_t getpid(void);

    private static final MethodHandle getpid = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("getpid"),
            FunctionDescriptor.of(JAVA_INT));

    public static int getpid() throws Throwable {
        return (int) getpid.invokeExact();
    }

    // int sysctl(int *name, u_int namelen, void *oldp, size_t *oldlenp, void *newp, size_t newlen);

    private static final MethodHandle sysctl = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("sysctl"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, SIZE_T), CAPTURE_CALL_STATE);

    public static int sysctl(MemorySegment callState, MemorySegment name, int namelen, MemorySegment oldp,
            MemorySegment oldlenp, MemorySegment newp, long newlen) throws Throwable {
        return (int) sysctl.invokeExact(callState, name, namelen, oldp, oldlenp, newp, newlen);
    }

    // int sysctlbyname(const char *name, void *oldp, size_t *oldlenp, void *newp, size_t newlen);

    private static final MethodHandle sysctlbyname = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("sysctlbyname"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, SIZE_T), CAPTURE_CALL_STATE);

    public static int sysctlbyname(MemorySegment callState, MemorySegment name, MemorySegment oldp,
            MemorySegment oldlenp, MemorySegment newp, long newlen) throws Throwable {
        return (int) sysctlbyname.invokeExact(callState, name, oldp, oldlenp, newp, newlen);
    }

    // int getrlimit(int resource, struct rlimit *rlp);

    private static final MethodHandle getrlimit = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("getrlimit"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS));

    public static int getrlimit(int resource, MemorySegment rlp) throws Throwable {
        return (int) getrlimit.invokeExact(resource, rlp);
    }

    // mach_port_t mach_task_self(void)

    private static final MethodHandle mach_task_self = LINKER
            .downcallHandle(SYSTEM_LIBRARY.findOrThrow("mach_task_self"), FunctionDescriptor.of(JAVA_INT));

    public static int mach_task_self() throws Throwable {
        return (int) mach_task_self.invokeExact();
    }

    // kern_return_t mach_port_deallocate(ipc_space_t, mach_port_name_t);

    private static final MethodHandle mach_port_deallocate = LINKER.downcallHandle(
            SYSTEM_LIBRARY.findOrThrow("mach_port_deallocate"), FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT));

    public static int mach_port_deallocate(int task, int name) throws Throwable {
        return (int) mach_port_deallocate.invokeExact(task, name);
    }

    // int getfsstat(struct statfs *buf, int bufsize, int flags);

    private static final MethodHandle getfsstat64 = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("getfsstat64"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));

    public static int getfsstat64(MemorySegment buffer, int bufsize, int flags) {
        try {
            return (int) getfsstat64.invokeExact(buffer, bufsize, flags);
        } catch (Throwable e) {
            return -1;
        }
    }

    private static final MethodHandle mach_host_self_handle = LINKER
            .downcallHandle(SYSTEM_LIBRARY.findOrThrow("mach_host_self"), FunctionDescriptor.of(JAVA_INT));

    public static int mach_host_self() {
        try {
            return (int) mach_host_self_handle.invokeExact();
        } catch (Throwable e) {
            return -1;
        }
    }

    private static final MethodHandle host_page_size = LINKER.downcallHandle(
            SYSTEM_LIBRARY.findOrThrow("host_page_size"), FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS));

    public static int host_page_size(int hostPort, MemorySegment pPageSize) {
        try {
            return (int) host_page_size.invokeExact(hostPort, pPageSize);
        } catch (Throwable e) {
            return -1;
        }
    }

    private static final MethodHandle host_statistics = LINKER.downcallHandle(
            SYSTEM_LIBRARY.findOrThrow("host_statistics"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS), CAPTURE_CALL_STATE);

    public static int host_statistics(MemorySegment callState, int hostPort, int hostStat, MemorySegment stats,
            MemorySegment count) {
        try {
            return (int) host_statistics.invokeExact(callState, hostPort, hostStat, stats, count);
        } catch (Throwable e) {
            return -1;
        }
    }
}
