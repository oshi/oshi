/*
 * Copyright (c) 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
/**
 * [oshi-core API] Provides a cross-platform implementation to retrieve Operating System and Hardware Information, such
 * as OS version, memory, CPU, disk, devices, sensors, etc.
 *
 * <h2>Quick Start</h2>
 *
 * <pre>{@code
 * SystemInfoProvider si = SystemInfoFactory.create();
 * HardwareAbstractionLayer hal = si.getHardware();
 * OperatingSystem os = si.getOperatingSystem();
 *
 * // CPU usage (blocks for 1 second)
 * double cpuLoad = hal.getProcessor().getSystemCpuLoad(1000L);
 *
 * // Memory
 * GlobalMemory mem = hal.getMemory();
 * long availableBytes = mem.getAvailable();
 * }</pre>
 *
 * Re-using the same {@link oshi.SystemInfo} instance optimizes caching and CPU performance; creating a new instance
 * minimizes memory usage.
 *
 * <h2>Module Architecture</h2>
 *
 * OSHI is organized into three modules:
 * <ul>
 * <li><b>{@code oshi-common}</b> ({@code com.github.oshi.common}) - Interfaces, abstract base classes, POJOs, and
 * utilities. This module contains <i>no native code</i> and requires no native access permissions.</li>
 * <li><b>{@code oshi-core}</b> (this module) - Full OSHI implementation using
 * <a href="https://github.com/java-native-access/jna">JNA</a> for native access. Supports all platforms (Windows,
 * macOS, Linux, DragonFly BSD, FreeBSD, OpenBSD, Solaris, AIX). Entry point: {@link oshi.SystemInfo}.</li>
 * <li><b>{@code oshi-core-ffm}</b> - Alternative implementation using the Foreign Function and Memory (FFM) API (JDK
 * 25+). Currently supports Windows, macOS, Linux, and FreeBSD. Entry point: {@code oshi.ffm.SystemInfo}.</li>
 * </ul>
 *
 * <h2>Native Access and JEP 472</h2>
 *
 * Both {@code oshi-core} and {@code oshi-core-ffm} use native access to retrieve most system information.
 * <a href="https://openjdk.org/jeps/472">JEP 472</a> (JDK 24) causes the JVM to issue warnings when native code is
 * loaded, and a future JDK release will require the {@code --enable-native-access} flag to permit it.
 * <p>
 * Applications that cannot or prefer not to enable native access can depend on {@code oshi-common} alone. This module
 * provides the full OSHI API surface (interfaces and abstract base classes) along with utilities for parsing
 * {@code /proc} and other OS-provided text files, executing system commands, and formatting output. To use it:
 * <ol>
 * <li>Depend on the {@code oshi-common} artifact only (Maven: {@code com.github.oshi:oshi-common}).</li>
 * <li>Extend the abstract base classes in {@code oshi.hardware.common} and {@code oshi.software.common}. For hardware,
 * subclass {@link oshi.hardware.common.AbstractHardwareAbstractionLayer} and override its
 * {@code protected abstract create*()} factory methods to return your platform-specific implementations. For software
 * and individual components (e.g., {@link oshi.software.common.AbstractOperatingSystem},
 * {@link oshi.hardware.common.AbstractCentralProcessor}), override the {@code protected abstract query*()} methods with
 * platform-specific logic using command-line tools, {@code /proc} parsing, or other non-native techniques.</li>
 * </ol>
 * This approach trades some coverage (not all information is available without native access) for compatibility with
 * restricted JVM environments.
 */
package oshi;
