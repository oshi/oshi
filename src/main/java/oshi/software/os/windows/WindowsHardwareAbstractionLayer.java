/**
 * Oshi (https://github.com/dblock/oshi)
 * 
 * Copyright (c) 2010 - 2015 The Oshi Project Team
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.windows;

import java.util.ArrayList;
import java.util.List;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Memory;
import oshi.hardware.PowerSource;
import oshi.hardware.Processor;
import oshi.software.os.OSFileStore;
import oshi.software.os.windows.nt.CentralProcessor;
import oshi.software.os.windows.nt.GlobalMemory;
import oshi.software.os.windows.nt.WindowsFileSystem;
import oshi.software.os.windows.nt.WindowsPowerSource;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

public class WindowsHardwareAbstractionLayer implements
		HardwareAbstractionLayer {

	private Processor[] _processors = null;

	private Memory _memory = null;

	@Override
	public Memory getMemory() {
		if (this._memory == null) {
			this._memory = new GlobalMemory();
		}
		return this._memory;
	}

	@Override
	public Processor[] getProcessors() {

		if (this._processors == null) {
			final String cpuRegistryRoot = "HARDWARE\\DESCRIPTION\\System\\CentralProcessor";
			List<Processor> processors = new ArrayList<>();
			String[] processorIds = Advapi32Util.registryGetKeys(
					WinReg.HKEY_LOCAL_MACHINE, cpuRegistryRoot);
			int numCPU = 0;
			for (String processorId : processorIds) {
				String cpuRegistryPath = cpuRegistryRoot + "\\" + processorId;
				CentralProcessor cpu = new CentralProcessor(numCPU++);
				cpu.setIdentifier(Advapi32Util.registryGetStringValue(
						WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
						"Identifier"));
				cpu.setName(Advapi32Util.registryGetStringValue(
						WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
						"ProcessorNameString"));
				cpu.setVendor(Advapi32Util.registryGetStringValue(
						WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath,
						"VendorIdentifier"));
				processors.add(cpu);
			}
			this._processors = processors.toArray(new Processor[0]);
		}

		return this._processors;
	}

	@Override
	public PowerSource[] getPowerSources() {
		return WindowsPowerSource.getPowerSources();
	}

	@Override
	public OSFileStore[] getFileStores() {
		return WindowsFileSystem.getFileStores();
	}
}
