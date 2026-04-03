/*
 * Copyright 2025-2026 The OSHI Project Contributors
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

    public static int getfsstat64(MemorySegment buffer, int bufsize, int flags) throws Throwable {
        return (int) getfsstat64.invokeExact(buffer, bufsize, flags);
    }

    // void setutxent(void);

    private static final MethodHandle setutxent = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("setutxent"),
            FunctionDescriptor.ofVoid());

    public static void setutxent() throws Throwable {
        setutxent.invokeExact();
    }

    // struct utmpx * getutxent(void);

    private static final MethodHandle getutxent = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("getutxent"),
            FunctionDescriptor.of(ADDRESS));

    public static MemorySegment getutxent() throws Throwable {
        MemorySegment result = (MemorySegment) getutxent.invokeExact();
        return result.equals(MemorySegment.NULL) ? null : result;
    }

    // void endutxent(void);

    private static final MethodHandle endutxent = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("endutxent"),
            FunctionDescriptor.ofVoid());

    public static void endutxent() throws Throwable {
        endutxent.invokeExact();
    }

    // int gethostname(char *name, size_t namelen);

    private static final MethodHandle gethostname = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("gethostname"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG));

    public static int gethostname(MemorySegment name, long namelen) throws Throwable {
        return (int) gethostname.invokeExact(name, namelen);
    }

    // int getaddrinfo(const char *node, const char *service, const struct addrinfo *hints, struct addrinfo **res);

    private static final MethodHandle getaddrinfo = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("getaddrinfo"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    public static int getaddrinfo(MemorySegment node, MemorySegment service, MemorySegment hints, MemorySegment res)
            throws Throwable {
        return (int) getaddrinfo.invokeExact(node, service, hints, res);
    }

    // void freeaddrinfo(struct addrinfo *res);

    private static final MethodHandle freeaddrinfo = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("freeaddrinfo"),
            FunctionDescriptor.ofVoid(ADDRESS));

    public static void freeaddrinfo(MemorySegment res) throws Throwable {
        freeaddrinfo.invokeExact(res);
    }

    // const char * gai_strerror(int ecode);

    private static final MethodHandle gai_strerror = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("gai_strerror"),
            FunctionDescriptor.of(ADDRESS, JAVA_INT));

    public static String gai_strerror(int ecode) throws Throwable {
        MemorySegment result = (MemorySegment) gai_strerror.invokeExact(ecode);
        return result.equals(MemorySegment.NULL) ? "" : result.reinterpret(256).getString(0);
    }

    private static final MethodHandle mach_host_self_handle = LINKER
            .downcallHandle(SYSTEM_LIBRARY.findOrThrow("mach_host_self"), FunctionDescriptor.of(JAVA_INT));

    public static int mach_host_self() throws Throwable {
        return (int) mach_host_self_handle.invokeExact();
    }

    private static final MethodHandle host_page_size = LINKER.downcallHandle(
            SYSTEM_LIBRARY.findOrThrow("host_page_size"), FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS));

    public static int host_page_size(int hostPort, MemorySegment pPageSize) throws Throwable {
        return (int) host_page_size.invokeExact(hostPort, pPageSize);
    }

    private static final MethodHandle host_statistics = LINKER.downcallHandle(
            SYSTEM_LIBRARY.findOrThrow("host_statistics"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS), CAPTURE_CALL_STATE);

    public static int host_statistics(MemorySegment callState, int hostPort, int hostStat, MemorySegment stats,
            MemorySegment count) throws Throwable {
        return (int) host_statistics.invokeExact(callState, hostPort, hostStat, stats, count);
    }

    // kern_return_t host_processor_info(host_t host, processor_flavor_t flavor, natural_t *out_processor_count,
    // processor_info_array_t *out_processor_info, mach_msg_type_number_t
    // *out_processor_infoCnt);

    private static final MethodHandle host_processor_info = LINKER.downcallHandle(
            SYSTEM_LIBRARY.findOrThrow("host_processor_info"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS), CAPTURE_CALL_STATE);

    public static int host_processor_info(MemorySegment callState, int hostPort, int flavor,
            MemorySegment processorCount, MemorySegment processorInfo, MemorySegment processorInfoCount)
            throws Throwable {
        return (int) host_processor_info.invokeExact(callState, hostPort, flavor, processorCount, processorInfo,
                processorInfoCount);
    }

    // int getloadavg(double loadavg[], int nelem);

    private static final MethodHandle getloadavg = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("getloadavg"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

    public static int getloadavg(MemorySegment loadavg, int nelem) throws Throwable {
        return (int) getloadavg.invokeExact(loadavg, nelem);
    }

    // kern_return_t vm_deallocate(vm_map_t target_task, vm_address_t address, vm_size_t size);

    private static final MethodHandle vm_deallocate = LINKER.downcallHandle(SYSTEM_LIBRARY.findOrThrow("vm_deallocate"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_LONG, JAVA_LONG));

    public static int vm_deallocate(int targetTask, long address, long size) throws Throwable {
        return (int) vm_deallocate.invokeExact(targetTask, address, size);
    }
}
