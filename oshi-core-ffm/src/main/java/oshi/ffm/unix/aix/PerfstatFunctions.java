/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.unix.aix;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import oshi.ffm.ForeignFunctions;

/**
 * FFM bindings for the AIX {@code libperfstat} library, mapped to the surface OSHI actually consumes.
 * <p>
 * Struct sizes and field offsets are taken from JNA's {@code com.sun.jna.platform.unix.aix.Perfstat} (which has been
 * reviewed against AIX's {@code <libperfstat.h>}); see the JNA file for full field documentation.
 * <p>
 * The library lives at {@code /usr/lib/libperfstat.a(shr_64.o)} (or {@code (shr.o)} for the 32-bit variant). Loading
 * requires the AIX-specific {@code RTLD_MEMBER} flag, which FFM's {@link SymbolLookup#libraryLookup} can't pass; the
 * dlopen call is wrapped behind {@link SharedObjectLoader}, mirroring JNA's contrib/platform
 * {@code SharedObjectLoader}.
 */
public final class PerfstatFunctions extends ForeignFunctions {

    private PerfstatFunctions() {
    }

    /** Identifier length for {@code perfstat_id_t} and the leading {@code name} field of most perfstat structs. */
    public static final int IDENTIFIER_LENGTH = 64;

    // Struct sizes (sizeof) as computed by JNA on 64-bit AIX. These are passed to each perfstat call as the
    // sizeof_struct argument; perfstat strides the output array by this amount.
    public static final int PERFSTAT_ID_T_SIZE = 64;
    public static final int PERFSTAT_CPU_TOTAL_T_SIZE = 696;
    public static final int PERFSTAT_CPU_T_SIZE = 616;
    public static final int PERFSTAT_MEMORY_TOTAL_T_SIZE = 352;
    public static final int PERFSTAT_PROCESS_T_SIZE = 288;
    public static final int PERFSTAT_DISK_T_SIZE = 496;
    public static final int PERFSTAT_PARTITION_CONFIG_T_SIZE = 800;
    public static final int PERFSTAT_NETINTERFACE_T_SIZE = 240;
    public static final int PERFSTAT_PROTOCOL_T_SIZE = 728;

    // libperfstat is loaded via the AIX-specific SharedObjectLoader, which knows about
    // RTLD_MEMBER + the archive-member syntax. The lookup is cached for the JVM lifetime.
    private static final SymbolLookup LIBPERFSTAT = SharedObjectLoader.loadPerfstat();

    // ---- Method handles. All perfstat_* APIs share the signature
    // ---- int perfstat_xxx(perfstat_id_t *name, perfstat_xxx_t *userbuff, int sizeof_struct, int desired_number);

    private static final FunctionDescriptor PERFSTAT_FD = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT,
            JAVA_INT);

    private static final MethodHandle perfstat_cpu_total = LINKER
            .downcallHandle(LIBPERFSTAT.findOrThrow("perfstat_cpu_total"), PERFSTAT_FD);
    private static final MethodHandle perfstat_cpu = LINKER.downcallHandle(LIBPERFSTAT.findOrThrow("perfstat_cpu"),
            PERFSTAT_FD);
    private static final MethodHandle perfstat_memory_total = LINKER
            .downcallHandle(LIBPERFSTAT.findOrThrow("perfstat_memory_total"), PERFSTAT_FD);
    private static final MethodHandle perfstat_process = LINKER
            .downcallHandle(LIBPERFSTAT.findOrThrow("perfstat_process"), PERFSTAT_FD);
    private static final MethodHandle perfstat_disk = LINKER.downcallHandle(LIBPERFSTAT.findOrThrow("perfstat_disk"),
            PERFSTAT_FD);
    private static final MethodHandle perfstat_partition_config = LINKER
            .downcallHandle(LIBPERFSTAT.findOrThrow("perfstat_partition_config"), PERFSTAT_FD);
    private static final MethodHandle perfstat_netinterface = LINKER
            .downcallHandle(LIBPERFSTAT.findOrThrow("perfstat_netinterface"), PERFSTAT_FD);
    private static final MethodHandle perfstat_protocol = LINKER
            .downcallHandle(LIBPERFSTAT.findOrThrow("perfstat_protocol"), PERFSTAT_FD);

    public static int perfstat_cpu_total(MemorySegment name, MemorySegment userbuff, int sizeofStruct, int desired)
            throws Throwable {
        return (int) perfstat_cpu_total.invokeExact(name, userbuff, sizeofStruct, desired);
    }

    public static int perfstat_cpu(MemorySegment name, MemorySegment userbuff, int sizeofStruct, int desired)
            throws Throwable {
        return (int) perfstat_cpu.invokeExact(name, userbuff, sizeofStruct, desired);
    }

    public static int perfstat_memory_total(MemorySegment name, MemorySegment userbuff, int sizeofStruct, int desired)
            throws Throwable {
        return (int) perfstat_memory_total.invokeExact(name, userbuff, sizeofStruct, desired);
    }

    public static int perfstat_process(MemorySegment name, MemorySegment userbuff, int sizeofStruct, int desired)
            throws Throwable {
        return (int) perfstat_process.invokeExact(name, userbuff, sizeofStruct, desired);
    }

    public static int perfstat_disk(MemorySegment name, MemorySegment userbuff, int sizeofStruct, int desired)
            throws Throwable {
        return (int) perfstat_disk.invokeExact(name, userbuff, sizeofStruct, desired);
    }

    public static int perfstat_partition_config(MemorySegment name, MemorySegment userbuff, int sizeofStruct,
            int desired) throws Throwable {
        return (int) perfstat_partition_config.invokeExact(name, userbuff, sizeofStruct, desired);
    }

    public static int perfstat_netinterface(MemorySegment name, MemorySegment userbuff, int sizeofStruct, int desired)
            throws Throwable {
        return (int) perfstat_netinterface.invokeExact(name, userbuff, sizeofStruct, desired);
    }

    public static int perfstat_protocol(MemorySegment name, MemorySegment userbuff, int sizeofStruct, int desired)
            throws Throwable {
        return (int) perfstat_protocol.invokeExact(name, userbuff, sizeofStruct, desired);
    }

    /**
     * Copy {@code IDENTIFIER_LENGTH} bytes from {@code seg + offset} into a Java string, stopping at the first NUL.
     *
     * @param seg    a perfstat struct segment
     * @param offset byte offset of the leading-name field within {@code seg}
     * @return the decoded ASCII name
     */
    public static String readName(MemorySegment seg, long offset) {
        byte[] bytes = new byte[IDENTIFIER_LENGTH];
        for (int i = 0; i < IDENTIFIER_LENGTH; i++) {
            byte b = seg.get(JAVA_BYTE, offset + i);
            if (b == 0) {
                return new String(bytes, 0, i, java.nio.charset.StandardCharsets.US_ASCII);
            }
            bytes[i] = b;
        }
        return new String(bytes, java.nio.charset.StandardCharsets.US_ASCII);
    }

    // ===========================================================================================
    // Field accessors for perfstat_cpu_total_t. Offsets verified via JNA's @FieldOrder reflection.
    // ===========================================================================================

    /** {@code ncpus}: number of active logical processors. */
    public static int cpuTotalNcpus(MemorySegment seg) {
        return seg.get(JAVA_INT, 0);
    }

    /** {@code processorHZ}: processor clock speed in Hz. */
    public static long cpuTotalProcessorHZ(MemorySegment seg) {
        return seg.get(JAVA_LONG, 72);
    }

    /** {@code user}: raw clock ticks spent in user mode (total). */
    public static long cpuTotalUser(MemorySegment seg) {
        return seg.get(JAVA_LONG, 80);
    }

    /** {@code sys}: raw clock ticks spent in system mode (total). */
    public static long cpuTotalSys(MemorySegment seg) {
        return seg.get(JAVA_LONG, 88);
    }

    /** {@code idle}: raw clock ticks spent idle (total). */
    public static long cpuTotalIdle(MemorySegment seg) {
        return seg.get(JAVA_LONG, 96);
    }

    /** {@code wait}: raw clock ticks spent waiting for I/O (total). */
    public static long cpuTotalWait(MemorySegment seg) {
        return seg.get(JAVA_LONG, 104);
    }

    /** {@code pswitch}: number of process switches. */
    public static long cpuTotalPswitch(MemorySegment seg) {
        return seg.get(JAVA_LONG, 112);
    }

    /** {@code devintrs}: number of device interrupts (total). */
    public static long cpuTotalDevintrs(MemorySegment seg) {
        return seg.get(JAVA_LONG, 176);
    }

    /** {@code softintrs}: number of software interrupts (total). */
    public static long cpuTotalSoftintrs(MemorySegment seg) {
        return seg.get(JAVA_LONG, 184);
    }

    /** {@code loadavg[i]}: 1/5/15-minute load averages (scaled by 1&lt;&lt;SBITS). */
    public static long cpuTotalLoadavg(MemorySegment seg, int i) {
        return seg.get(JAVA_LONG, 200L + i * 8L);
    }

    /** {@code idle_stolen_purr}: idle cycles stolen by the hypervisor. */
    public static long cpuTotalIdleStolenPurr(MemorySegment seg) {
        return seg.get(JAVA_LONG, 560);
    }

    /** {@code busy_stolen_purr}: busy cycles stolen by the hypervisor. */
    public static long cpuTotalBusyStolenPurr(MemorySegment seg) {
        return seg.get(JAVA_LONG, 576);
    }

    // ===========================================================================================
    // Field accessors for perfstat_cpu_t (per-CPU element of the perfstat_cpu array).
    // ===========================================================================================

    /** {@code name[64]}: logical-processor name ({@code cpu0}, {@code cpu1}, …). */
    public static String cpuName(MemorySegment seg, long elementOffset) {
        return readName(seg, elementOffset);
    }

    /** {@code user}: raw clock ticks spent in user mode (this CPU). */
    public static long cpuUser(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 64);
    }

    /** {@code sys}: raw clock ticks spent in system mode (this CPU). */
    public static long cpuSys(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 72);
    }

    /** {@code idle}: raw clock ticks spent idle (this CPU). */
    public static long cpuIdle(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 80);
    }

    /** {@code wait}: raw clock ticks waiting for I/O (this CPU). */
    public static long cpuWait(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 88);
    }

    /** {@code devintrs}: device interrupts (this CPU). Offset within JNA layout: 384. */
    public static long cpuDevintrs(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 384);
    }

    /** {@code softintrs}: software interrupts (this CPU). Offset within JNA layout: 392. */
    public static long cpuSoftintrs(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 392);
    }

    /** {@code idle_stolen_purr}: idle cycles stolen (this CPU). Offset within JNA layout: 432. */
    public static long cpuIdleStolenPurr(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 432);
    }

    /** {@code busy_stolen_purr}: busy cycles stolen (this CPU). Offset within JNA layout: 448. */
    public static long cpuBusyStolenPurr(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 448);
    }

    // ===========================================================================================
    // Field accessors for perfstat_memory_total_t.
    // ===========================================================================================

    /** {@code virt_total}: total virtual memory (in 4 KB pages). */
    public static long memVirtTotal(MemorySegment seg) {
        return seg.get(JAVA_LONG, 0);
    }

    /** {@code real_total}: total real memory (in 4 KB pages). */
    public static long memRealTotal(MemorySegment seg) {
        return seg.get(JAVA_LONG, 8);
    }

    /** {@code pgspins}: number of page-ins from paging space. */
    public static long memPgspins(MemorySegment seg) {
        return seg.get(JAVA_LONG, 72);
    }

    /** {@code pgspouts}: number of page-outs to paging space. */
    public static long memPgspouts(MemorySegment seg) {
        return seg.get(JAVA_LONG, 80);
    }

    /** {@code pgsp_total}: total paging space (in 4 KB pages). */
    public static long memPgspTotal(MemorySegment seg) {
        return seg.get(JAVA_LONG, 120);
    }

    /** {@code pgsp_free}: free paging space (in 4 KB pages). */
    public static long memPgspFree(MemorySegment seg) {
        return seg.get(JAVA_LONG, 128);
    }

    /** {@code virt_active}: active virtual pages (accessed). */
    public static long memVirtActive(MemorySegment seg) {
        return seg.get(JAVA_LONG, 168);
    }

    /** {@code real_avail}: real memory available without paging out working segments (in 4 KB pages). */
    public static long memRealAvail(MemorySegment seg) {
        return seg.get(JAVA_LONG, 328);
    }

    // ===========================================================================================
    // Field accessors for perfstat_process_t (per-process element).
    // ===========================================================================================

    /** {@code pid}: process ID. */
    public static long procPid(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 8);
    }

    /** {@code num_threads}: thread count. */
    public static long procNumThreads(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 88);
    }

    /** {@code proc_real_mem_data}: real memory used for data (KB). */
    public static long procRealMemData(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 120);
    }

    /** {@code proc_real_mem_text}: real memory used for text (KB). */
    public static long procRealMemText(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 128);
    }

    /** {@code real_inuse}: real memory in use (KB). */
    public static long procRealInuse(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 168);
    }

    /** {@code ucpu_time}: user-mode CPU time (milliseconds when filled by perfstat_process). */
    public static double procUcpuTime(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_DOUBLE, elementOffset + 232);
    }

    /** {@code scpu_time}: system-mode CPU time (milliseconds when filled by perfstat_process). */
    public static double procScpuTime(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_DOUBLE, elementOffset + 240);
    }

    // ===========================================================================================
    // Field accessors for perfstat_disk_t (per-disk element).
    // ===========================================================================================

    /** {@code name[64]}: disk name. */
    public static String diskName(MemorySegment seg, long elementOffset) {
        return readName(seg, elementOffset);
    }

    /** {@code description[64]}: disk description (from ODM). */
    public static String diskDescription(MemorySegment seg, long elementOffset) {
        return readName(seg, elementOffset + 64);
    }

    /** {@code size}: disk size (in MB). */
    public static long diskSize(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 192);
    }

    /** {@code bsize}: disk block size (in bytes). */
    public static long diskBsize(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 208);
    }

    /** {@code xfers}: number of transfers to/from disk. */
    public static long diskXfers(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 224);
    }

    /** {@code wblks}: blocks written to disk. */
    public static long diskWblks(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 232);
    }

    /** {@code rblks}: blocks read from disk. */
    public static long diskRblks(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 240);
    }

    /** {@code qdepth}: instantaneous service-queue depth. */
    public static long diskQdepth(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 248);
    }

    /** {@code time}: time the disk is active. */
    public static long diskTime(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 256);
    }

    // ===========================================================================================
    // Field accessors for perfstat_partition_config_t (singleton; passed by reference, count = 1).
    // ===========================================================================================

    /** {@code conf}: partition properties (32-bit union, see {@code perfstat_partition_type_t}). */
    public static int configConf(MemorySegment seg) {
        return seg.get(JAVA_INT, 136);
    }

    /** {@code machineID[64]}: machine ID. */
    public static String configMachineID(MemorySegment seg) {
        return readName(seg, 276);
    }

    /** {@code processorMHz}: processor clock speed in MHz. */
    public static double configProcessorMHz(MemorySegment seg) {
        return seg.get(JAVA_DOUBLE, 344);
    }

    /** {@code OSBuild[64]}: OS build string. */
    public static String configOSBuild(MemorySegment seg) {
        return readName(seg, 512);
    }

    /** {@code smtthreads}: number of SMT threads. */
    public static int configSmtthreads(MemorySegment seg) {
        return seg.get(JAVA_INT, 580);
    }

    /** {@code vcpus.max}: max virtual CPUs. vcpus is a perfstat_value_t starting at offset 632; max is at +8. */
    public static long configVcpusMax(MemorySegment seg) {
        return seg.get(JAVA_LONG, 632 + 8);
    }

    // ===========================================================================================
    // Field accessors for perfstat_netinterface_t (per-interface element).
    // ===========================================================================================

    /** {@code name[64]}: interface name. */
    public static String netIfName(MemorySegment seg, long elementOffset) {
        return readName(seg, elementOffset);
    }

    /** {@code description[64]}: interface description (from ODM). */
    public static String netIfDescription(MemorySegment seg, long elementOffset) {
        return readName(seg, elementOffset + 64);
    }

    /** {@code type}: interface type byte (see {@code <net/if_types.h>}). */
    public static byte netIfType(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_BYTE, elementOffset + 128);
    }

    /** {@code mtu}: network frame size. */
    public static long netIfMtu(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 136);
    }

    /** {@code ipackets}: packets received. */
    public static long netIfIpackets(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 144);
    }

    /** {@code ibytes}: bytes received. */
    public static long netIfIbytes(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 152);
    }

    /** {@code ierrors}: input errors. */
    public static long netIfIerrors(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 160);
    }

    /** {@code opackets}: packets sent. */
    public static long netIfOpackets(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 168);
    }

    /** {@code obytes}: bytes sent. */
    public static long netIfObytes(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 176);
    }

    /** {@code oerrors}: output errors. */
    public static long netIfOerrors(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 184);
    }

    /** {@code collisions}: collisions on CSMA interface. */
    public static long netIfCollisions(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 192);
    }

    /** {@code bitrate}: adapter rating (bits per second). */
    public static long netIfBitrate(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 200);
    }

    /** {@code if_iqdrops}: dropped on input on this interface. */
    public static long netIfIfIqdrops(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 224);
    }

    // ===========================================================================================
    // Field accessors for perfstat_protocol_t (per-protocol element).
    //
    // The struct is: { byte name[64]; union u { tcp{...}, udp{...}, ... } u; long version; }.
    // Within the union, the tcp and udp variants live at the same offset (64 within the struct);
    // both start with ipackets at +0, ierrors at +8, opackets at +16, and diverge from there.
    // ===========================================================================================

    /** {@code name[64]}: protocol name ({@code ip}, {@code tcp}, {@code udp}, …). */
    public static String protoName(MemorySegment seg, long elementOffset) {
        return readName(seg, elementOffset);
    }

    /** {@code u.tcp.ipackets}: TCP input packets. */
    public static long protoTcpIpackets(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 64);
    }

    /** {@code u.tcp.ierrors}: TCP input errors. */
    public static long protoTcpIerrors(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 72);
    }

    /** {@code u.tcp.opackets}: TCP output packets. */
    public static long protoTcpOpackets(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 80);
    }

    /** {@code u.tcp.initiated}: TCP connections initiated. */
    public static long protoTcpInitiated(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 88);
    }

    /** {@code u.tcp.accepted}: TCP connections accepted. */
    public static long protoTcpAccepted(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 96);
    }

    /** {@code u.tcp.established}: TCP connections established. */
    public static long protoTcpEstablished(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 104);
    }

    /** {@code u.tcp.dropped}: TCP connections dropped. */
    public static long protoTcpDropped(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 112);
    }

    /** {@code u.udp.ipackets}: UDP input packets. */
    public static long protoUdpIpackets(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 64);
    }

    /** {@code u.udp.ierrors}: UDP input errors. */
    public static long protoUdpIerrors(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 72);
    }

    /** {@code u.udp.opackets}: UDP output packets. */
    public static long protoUdpOpackets(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 80);
    }

    /** {@code u.udp.no_socket}: UDP packets dropped due to no socket. */
    public static long protoUdpNoSocket(MemorySegment seg, long elementOffset) {
        return seg.get(JAVA_LONG, elementOffset + 88);
    }
}
