/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.CgroupInfo;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.linux.ProcPath;
import oshi.util.linux.SysPath;

/**
 * Linux implementation of {@link CgroupInfo} supporting both cgroup v2 and v1.
 * <p>
 * This implementation detects the cgroup version and reads resource limits and usage from the appropriate cgroup
 * filesystem paths. Limit values are memoized while usage values are read fresh on each call.
 * <p>
 * Container detection ({@link #isContainerized()}) checks for known container markers such as {@code /.dockerenv}, and
 * cgroup paths containing {@code /docker/}, {@code /kubepods/}, {@code /lxc/}, etc.
 */
@ThreadSafe
public class LinuxCgroupInfo implements CgroupInfo {

    private static final long NANOSECONDS_PER_MICROSECOND = 1000L;

    // Kernel reports cgroup v1 "no limit" as a value near Long.MAX_VALUE, rounded down to page size.
    // Use a generous guard band to handle page sizes up to 64KB.
    private static final long V1_NO_LIMIT_THRESHOLD = UNLIMITED_MEMORY - 0x1_0000;

    private static final String[] CONTAINER_MARKERS = { "/docker/", "docker-", "/kubepods/", "kubepods.slice", "/lxc/",
            "/containerd/", "cri-containerd-", "/crio-", "/buildkit/", "/libpod-", "/podman-" };

    private final Supplier<Integer> versionSupplier = memoize(this::detectVersion);
    private final Supplier<String> cgroupPathSupplier = memoize(this::parseCgroupPath);
    private final Supplier<Boolean> containerizedSupplier = memoize(this::detectContainerized);
    private final Supplier<List<String>> selfCgroupSupplier = memoize(() -> FileUtil.readFile(ProcPath.SELF_CGROUP));
    private final Supplier<Long> cpuQuotaSupplier = memoize(this::readCpuQuota, defaultExpiration());
    private final Supplier<Long> cpuPeriodSupplier = memoize(this::readCpuPeriod, defaultExpiration());
    private final Supplier<Long> memoryLimitSupplier = memoize(this::readMemoryLimit, defaultExpiration());
    private final Supplier<Long> pidLimitSupplier = memoize(this::readPidLimit, defaultExpiration());
    private final Map<String, String> v1ControllerPathCache = new ConcurrentHashMap<>();

    /**
     * Constructs a new LinuxCgroupInfo instance.
     */
    public LinuxCgroupInfo() {
    }

    @Override
    public boolean isContainerized() {
        return containerizedSupplier.get();
    }

    private boolean detectContainerized() {
        // Check for known container indicator files
        if (new File("/.dockerenv").exists() || new File("/run/.containerenv").exists()) {
            return true;
        }
        // Check cgroup path for container-specific markers
        String cgroupPath = cgroupPathSupplier.get();
        if (!cgroupPath.isEmpty()) {
            for (String marker : CONTAINER_MARKERS) {
                if (cgroupPath.contains(marker)) {
                    return true;
                }
            }
        }
        // Also check v1 cgroup entries for container markers
        List<String> selfCgroup = selfCgroupSupplier.get();
        for (String line : selfCgroup) {
            for (String marker : CONTAINER_MARKERS) {
                if (line.contains(marker)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int getVersion() {
        return versionSupplier.get();
    }

    @Override
    public long getCpuQuota() {
        return cpuQuotaSupplier.get();
    }

    @Override
    public long getCpuPeriod() {
        return cpuPeriodSupplier.get();
    }

    @Override
    public long getCpuUsage() {
        int version = getVersion();
        if (version == 2) {
            return readCpuUsageV2();
        } else if (version == 1) {
            return readCpuUsageV1();
        }
        return 0L;
    }

    @Override
    public long getMemoryLimit() {
        return memoryLimitSupplier.get();
    }

    @Override
    public long getMemoryUsage() {
        int version = getVersion();
        if (version == 2) {
            return readMemoryUsageV2();
        } else if (version == 1) {
            return readMemoryUsageV1();
        }
        return 0L;
    }

    @Override
    public long getPidLimit() {
        return pidLimitSupplier.get();
    }

    @Override
    public long getPidCurrent() {
        int version = getVersion();
        if (version == 2) {
            return readPidCurrentV2();
        } else if (version == 1) {
            return readPidCurrentV1();
        }
        return 0L;
    }

    private int detectVersion() {
        List<String> filesystems = FileUtil.readFile(ProcPath.FILESYSTEMS);
        boolean hasCgroup2 = filesystems.stream().anyMatch(line -> line.contains("cgroup2"));

        List<String> selfCgroup = selfCgroupSupplier.get();
        if (selfCgroup.isEmpty()) {
            return 0;
        }

        // Check for v2: single entry with format "0::/{path}"
        if (hasCgroup2 && selfCgroup.size() == 1 && selfCgroup.get(0).startsWith("0::")) {
            return 2;
        }

        // Multiple entries indicate v1 or hybrid mode, fall back to v1
        return 1;
    }

    private String parseCgroupPath() {
        List<String> selfCgroup = selfCgroupSupplier.get();
        if (selfCgroup.isEmpty()) {
            return "";
        }

        int version = getVersion();
        if (version == 2) {
            // v2 format: "0::/{path}"
            String line = selfCgroup.get(0);
            if (line.startsWith("0::")) {
                String path = line.substring(3);
                return path.isEmpty() ? "/" : path;
            }
        }

        // For v1, return empty - we'll use controller-specific paths
        return "";
    }

    private String getV2CgroupBase() {
        String cgroupPath = cgroupPathSupplier.get();
        if (cgroupPath.isEmpty() || cgroupPath.equals("/")) {
            return SysPath.CGROUP;
        }
        // Remove leading slash for path concatenation
        if (cgroupPath.startsWith("/")) {
            cgroupPath = cgroupPath.substring(1);
        }
        return SysPath.CGROUP + cgroupPath + "/";
    }

    private String getV1ControllerPath(String controller) {
        return v1ControllerPathCache.computeIfAbsent(controller, this::resolveV1ControllerPath);
    }

    private String resolveV1ControllerPath(String controller) {
        List<String> selfCgroup = selfCgroupSupplier.get();
        for (String line : selfCgroup) {
            // v1 format: "hierarchy-id:controllers:path"
            String[] parts = line.split(":");
            if (parts.length >= 3) {
                String controllers = parts[1];
                if (controllers.isEmpty()) {
                    continue;
                }
                String path = parts[2];
                // Exact match: split comma-separated controllers
                for (String c : controllers.split(",")) {
                    if (c.equals(controller)) {
                        if (path.startsWith("/")) {
                            path = path.substring(1);
                        }
                        // Use the full controllers string as the mount name (e.g., "cpu,cpuacct")
                        return SysPath.CGROUP + controllers + "/" + path + "/";
                    }
                }
            }
        }
        return SysPath.CGROUP + controller + "/";
    }

    private long readCpuQuota() {
        int version = getVersion();
        if (version == 2) {
            return readCpuQuotaV2();
        } else if (version == 1) {
            return readCpuQuotaV1();
        }
        return UNLIMITED;
    }

    private long readCpuQuotaV2() {
        return readCpuQuotaV2(getV2CgroupBase());
    }

    // package-private for testing
    long readCpuQuotaV2(String basePath) {
        String cpuMax = FileUtil.getStringFromFile(basePath + "cpu.max");
        if (cpuMax.isEmpty()) {
            return UNLIMITED;
        }
        String[] parts = cpuMax.split("\\s+");
        if (parts.length >= 1) {
            if ("max".equalsIgnoreCase(parts[0])) {
                return UNLIMITED;
            }
            return ParseUtil.parseLongOrDefault(parts[0], UNLIMITED);
        }
        return UNLIMITED;
    }

    private long readCpuQuotaV1() {
        return readCpuQuotaV1(getV1ControllerPath("cpu"));
    }

    // package-private for testing
    long readCpuQuotaV1(String controllerBase) {
        long quota = FileUtil.getLongFromFile(controllerBase + "cpu.cfs_quota_us");
        return quota == 0 ? UNLIMITED : quota;
    }

    private long readCpuPeriod() {
        int version = getVersion();
        if (version == 2) {
            return readCpuPeriodV2();
        } else if (version == 1) {
            return readCpuPeriodV1();
        }
        return DEFAULT_CPU_PERIOD;
    }

    private long readCpuPeriodV2() {
        return readCpuPeriodV2(getV2CgroupBase());
    }

    // package-private for testing
    long readCpuPeriodV2(String basePath) {
        String cpuMax = FileUtil.getStringFromFile(basePath + "cpu.max");
        if (cpuMax.isEmpty()) {
            return DEFAULT_CPU_PERIOD;
        }
        String[] parts = cpuMax.split("\\s+");
        if (parts.length >= 2) {
            return ParseUtil.parseLongOrDefault(parts[1], DEFAULT_CPU_PERIOD);
        }
        return DEFAULT_CPU_PERIOD;
    }

    private long readCpuPeriodV1() {
        return readCpuPeriodV1(getV1ControllerPath("cpu"));
    }

    // package-private for testing
    long readCpuPeriodV1(String controllerBase) {
        long period = FileUtil.getLongFromFile(controllerBase + "cpu.cfs_period_us");
        return period == 0 ? DEFAULT_CPU_PERIOD : period;
    }

    long readCpuUsageV2() {
        return readCpuUsageV2(getV2CgroupBase());
    }

    // package-private for testing
    long readCpuUsageV2(String basePath) {
        List<String> lines = FileUtil.readFile(basePath + "cpu.stat");
        for (String line : lines) {
            if (line.startsWith("usage_usec")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    long usec = ParseUtil.parseLongOrDefault(parts[1], 0L);
                    return usec * NANOSECONDS_PER_MICROSECOND;
                }
            }
        }
        return 0L;
    }

    long readCpuUsageV1() {
        return readCpuUsageV1(getV1ControllerPath("cpuacct"));
    }

    // package-private for testing
    long readCpuUsageV1(String controllerBase) {
        return FileUtil.getLongFromFile(controllerBase + "cpuacct.usage");
    }

    private long readMemoryLimit() {
        int version = getVersion();
        if (version == 2) {
            return readMemoryLimitV2();
        } else if (version == 1) {
            return readMemoryLimitV1();
        }
        return UNLIMITED_MEMORY;
    }

    private long readMemoryLimitV2() {
        return readMemoryLimitV2(getV2CgroupBase());
    }

    // package-private for testing
    long readMemoryLimitV2(String basePath) {
        String memMax = FileUtil.getStringFromFile(basePath + "memory.max");
        if (memMax.isEmpty() || "max".equalsIgnoreCase(memMax.trim())) {
            return UNLIMITED_MEMORY;
        }
        return ParseUtil.parseLongOrDefault(memMax.trim(), UNLIMITED_MEMORY);
    }

    private long readMemoryLimitV1() {
        return readMemoryLimitV1(getV1ControllerPath("memory"));
    }

    // package-private for testing
    long readMemoryLimitV1(String controllerBase) {
        long limit = FileUtil.getLongFromFile(controllerBase + "memory.limit_in_bytes");
        if (limit == 0 || limit > V1_NO_LIMIT_THRESHOLD) {
            return UNLIMITED_MEMORY;
        }
        return limit;
    }

    long readMemoryUsageV2() {
        return readMemoryUsageV2(getV2CgroupBase());
    }

    // package-private for testing
    long readMemoryUsageV2(String basePath) {
        String memCurrent = FileUtil.getStringFromFile(basePath + "memory.current");
        return ParseUtil.parseLongOrDefault(memCurrent.trim(), 0L);
    }

    long readMemoryUsageV1() {
        return readMemoryUsageV1(getV1ControllerPath("memory"));
    }

    // package-private for testing
    long readMemoryUsageV1(String controllerBase) {
        return FileUtil.getLongFromFile(controllerBase + "memory.usage_in_bytes");
    }

    private long readPidLimit() {
        int version = getVersion();
        if (version == 2) {
            return readPidLimitV2();
        } else if (version == 1) {
            return readPidLimitV1();
        }
        return UNLIMITED;
    }

    private long readPidLimitV2() {
        return readPidLimitV2(getV2CgroupBase());
    }

    // package-private for testing
    long readPidLimitV2(String basePath) {
        String pidsMax = FileUtil.getStringFromFile(basePath + "pids.max");
        if (pidsMax.isEmpty() || "max".equalsIgnoreCase(pidsMax.trim())) {
            return UNLIMITED;
        }
        return ParseUtil.parseLongOrDefault(pidsMax.trim(), UNLIMITED);
    }

    private long readPidLimitV1() {
        return readPidLimitV1(getV1ControllerPath("pids"));
    }

    // package-private for testing
    long readPidLimitV1(String controllerBase) {
        String pidsMax = FileUtil.getStringFromFile(controllerBase + "pids.max");
        if (pidsMax.isEmpty() || "max".equalsIgnoreCase(pidsMax.trim())) {
            return UNLIMITED;
        }
        long limit = ParseUtil.parseLongOrDefault(pidsMax.trim(), UNLIMITED);
        return limit == 0 ? UNLIMITED : limit;
    }

    long readPidCurrentV2() {
        return readPidCurrentV2(getV2CgroupBase());
    }

    // package-private for testing
    long readPidCurrentV2(String basePath) {
        String pidsCurrent = FileUtil.getStringFromFile(basePath + "pids.current");
        return ParseUtil.parseLongOrDefault(pidsCurrent.trim(), 0L);
    }

    long readPidCurrentV1() {
        return readPidCurrentV1(getV1ControllerPath("pids"));
    }

    // package-private for testing
    long readPidCurrentV1(String controllerBase) {
        return FileUtil.getLongFromFile(controllerBase + "pids.current");
    }
}
