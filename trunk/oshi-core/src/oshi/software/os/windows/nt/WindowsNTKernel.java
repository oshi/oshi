/**
 * Copyright (c) Daniel Doubrovkine, 2010
 * dblock[at]dblock[dot]org
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://oshi.codeplex.com/license
 */
package oshi.software.os.windows.nt;

import oshi.hardware.Memory;
import oshi.software.os.Kernel;
import oshi.software.os.Process;

/**
 * Beginning with Windows XP, all modern versions of Windows are based on the Windows NT kernel.
 * @author dblock[at]dblock[dot]org
 */
public class WindowsNTKernel implements Kernel {

	@Override
	public Memory getMemory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Process[] getProcesses() {
		// TODO Auto-generated method stub
		return null;
	}

}
