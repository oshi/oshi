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
package oshi.hardware.platform.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory; // NOSONAR
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.PowrProf.POWER_INFORMATION_LEVEL;
import com.sun.jna.platform.win32.SetupApi;
import com.sun.jna.platform.win32.SetupApi.SP_DEVICE_INTERFACE_DATA;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.jna.platform.windows.PowrProf;
import oshi.jna.platform.windows.PowrProf.BATTERY_INFORMATION;
import oshi.jna.platform.windows.PowrProf.BATTERY_QUERY_INFORMATION;
import oshi.jna.platform.windows.PowrProf.SystemBatteryState;
import oshi.util.FormatUtil;

/**
 * A Power Source
 */
public class WindowsPowerSource extends AbstractPowerSource {

	private static final Logger LOG = LoggerFactory.getLogger(WindowsPowerSource.class);
	private static final GUID GUID_DEVCLASS_BATTERY = GUID.fromString("{72631E54-78A4-11D0-BCF7-00AA00B7B32A}");

	// Returned value includes GBS_HASBATTERY if the system has a
	// non-UPS battery, and GBS_ONBATTERY if the system is running on
	// a battery.
	private static final int GBS_HASBATTERY = 0x1; // dwResult & GBS_ONBATTERY means we have not yet found AC power.
	private static final int GBS_ONBATTERY = 0x2; // dwResult & GBS_HASBATTERY means we have found a non-UPS battery.

	private static final int BATTERY_SYSTEM_BATTERY = 0x80000000;
	private static final int BATTERY_IS_SHORT_TERM = 0x20000000;
	private static final int BATTERY_POWER_ON_LINE = 0x00000001;

	private static final int IOCTL_BATTERY_QUERY_TAG = 0x294040;
	private static final int IOCTL_BATTERY_QUERY_STATUS = 0x29404c;
	private static final int IOCTL_BATTERY_QUERY_INFORMATION = 0x294044;

	/**
	 * <p>
	 * Constructor for WindowsPowerSource.
	 * </p>
	 *
	 * @param newName              a {@link java.lang.String} object.
	 * @param newRemainingCapacity a double.
	 * @param newTimeRemaining     a double.
	 */
	public WindowsPowerSource(String newName, double newRemainingCapacity, double newTimeRemaining) {
		super(newName, newRemainingCapacity, newTimeRemaining);
		LOG.debug("Initialized WindowsPowerSource");
	}

	/**
	 * Gets Battery Information.
	 *
	 * @return An array of PowerSource objects representing batteries, etc.
	 */
	public static PowerSource[] getPowerSources() {
		// Windows provides a single unnamed battery
		WindowsPowerSource[] psArray = new WindowsPowerSource[1];
		psArray[0] = getPowerSource("System Battery");
		return psArray;
	}

	private static WindowsPowerSource getPowerSource(String name) {
		long maxCapacity = -1; // mWh
		long remainingCapacity = -1; // mWh
		long estimatedTimeRaw = -1; // Seconds
		long rate = 0; // mW, signed, charging or discharging
		// AcOnLine
		// BatteryPresent
		// Charging
		// Discharging
		// TODO: IOCTL_BATTERY_QUERY_INFORMATION
		// Capabilities -- units flag
		// Chemistry (NiMH etc.)
		// Design & FullCharge
		// CycleCount
		// TODO:
		// Manufacture Date
		// Manufacturer
		// SerialNumber
		// Temperature
		// #define FILE_DEVICE_BATTERY 0x00000029
		// #define METHOD_BUFFERED 0
		// #define FILE_READ_ACCESS 0x0001
		// CTL_CODE (((DeviceType) << 16) | ((Access) << 14) | ((Function) << 2) |
		// (Method))
		// IOCTL_BATTERY_QUERY_INFORMATION
		// #define IOCTL_BATTERY_QUERY_TAG CTL_CODE(FILE_DEVICE_BATTERY, 0x10,
		// METHOD_BUFFERED, FILE_READ_ACCESS)
		// #define IOCTL_BATTERY_QUERY_INFORMATION CTL_CODE(FILE_DEVICE_BATTERY, 0x11,
		// METHOD_BUFFERED, FILE_READ_ACCESS)
		// #define IOCTL_BATTERY_SET_INFORMATION CTL_CODE(FILE_DEVICE_BATTERY, 0x12,
		// METHOD_BUFFERED, FILE_WRITE_ACCESS)
		// #define IOCTL_BATTERY_QUERY_STATUS CTL_CODE(FILE_DEVICE_BATTERY, 0x13,
		// METHOD_BUFFERED, FILE_READ_ACCESS)

		// Kernel32.INSTANCE.DeviceIoControl(hBattery, dwIoControlCode, lpInBuffer,
		// nInBufferSize, lpOutBuffer, nOutBufferSize, lpBytesReturned, lpOverlapped)

		int size = new SystemBatteryState().size();
		Memory mem = new Memory(size);
		if (0 == PowrProf.INSTANCE.CallNtPowerInformation(POWER_INFORMATION_LEVEL.SystemBatteryState, null, 0, mem,
				size)) {
			SystemBatteryState batteryState = new SystemBatteryState(mem);
			if (batteryState.batteryPresent > 0) {
				int estimatedTime = -2; // -1 = unknown, -2 = unlimited
				if (batteryState.acOnLine == 0 && batteryState.charging == 0 && batteryState.discharging > 0) {
					estimatedTime = batteryState.estimatedTime;
				}
				maxCapacity = FormatUtil.getUnsignedInt(batteryState.maxCapacity);
				remainingCapacity = FormatUtil.getUnsignedInt(batteryState.remainingCapacity);
				estimatedTimeRaw = FormatUtil.getUnsignedInt(batteryState.estimatedTime);
				rate = batteryState.rate;
				return new WindowsPowerSource(name, (double) remainingCapacity / maxCapacity, estimatedTime);
			}
		}
		return new WindowsPowerSource("Unknown", 0d, -1d);
	}

	static int GetBatteryState() {
		// enumerate the batteries and ask each one for information.
		// Ported from:
		// https://docs.microsoft.com/en-us/windows/win32/power/enumerating-battery-devices
		int dwResult = GBS_ONBATTERY;

		HANDLE hdev = SetupApi.INSTANCE.SetupDiGetClassDevs(GUID_DEVCLASS_BATTERY, null, null,
				SetupApi.DIGCF_PRESENT | SetupApi.DIGCF_DEVICEINTERFACE);
		if (WinBase.INVALID_HANDLE_VALUE != hdev) {
			// Limit search to 100 batteries max
			for (int idev = 0; idev < 100; idev++) {
				System.out.println("idev: " + idev);
				SP_DEVICE_INTERFACE_DATA did = new SP_DEVICE_INTERFACE_DATA();
				did.cbSize = did.size();

				if (SetupApi.INSTANCE.SetupDiEnumDeviceInterfaces(hdev, null, GUID_DEVCLASS_BATTERY, idev, did)) {
					IntByReference cbRequired = new IntByReference(0);
					SetupApi.INSTANCE.SetupDiGetDeviceInterfaceDetail(hdev, did, null, 0, cbRequired, null);
					if (WinError.ERROR_INSUFFICIENT_BUFFER == Kernel32.INSTANCE.GetLastError()) {
						// PSP_DEVICE_INTERFACE_DETAIL_DATA: int size + TCHAR array
						cbRequired.setValue(cbRequired.getValue());
						Memory pdidd = new Memory(cbRequired.getValue());
						int cbSize = (int) pdidd.size();
						pdidd.setInt(0, Integer.BYTES + 2); // pdidd->cbSize = sizeof(*pdidd);

						if (SetupApi.INSTANCE.SetupDiGetDeviceInterfaceDetail(hdev, did, pdidd, cbSize, cbRequired,
								null)) {
							// Enumerated a battery. Ask it for information.
							System.out.println("Battery: \"" + pdidd.getWideString(Integer.BYTES) + "\"");
							HANDLE hBattery = Kernel32.INSTANCE.CreateFile(pdidd.getWideString(Integer.BYTES), // pdidd->DevicePath
									WinNT.GENERIC_READ | WinNT.GENERIC_WRITE,
									WinNT.FILE_SHARE_READ | WinNT.FILE_SHARE_WRITE, null, WinNT.OPEN_EXISTING,
									WinNT.FILE_ATTRIBUTE_NORMAL, null);
							System.out.println("hBattery: " + Pointer.nativeValue(hBattery.getPointer()));
							System.out.println(
									"hInvalid: " + Pointer.nativeValue(WinBase.INVALID_HANDLE_VALUE.getPointer()));
							if (!WinBase.INVALID_HANDLE_VALUE.equals(hBattery)) {
								// Ask the battery for its tag.
								BATTERY_QUERY_INFORMATION bqi = new PowrProf.BATTERY_QUERY_INFORMATION();
								IntByReference dwWait = new IntByReference(0);
								IntByReference dwTag = new IntByReference(-999);
								IntByReference dwOut = new IntByReference();

								boolean ret = Kernel32.INSTANCE.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_TAG,
										dwWait.getPointer(), Integer.BYTES, dwTag.getPointer(), Integer.BYTES, dwOut,
										null);
								System.out.println("IOCTL Success: " + ret + ", LastError="
										+ Kernel32.INSTANCE.GetLastError() + ", dwOut=" + dwOut.getValue());
								System.out.println("Tag:" + dwTag.getValue());
								bqi.BatteryTag = dwTag.getValue();

								if (Kernel32.INSTANCE.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_TAG,
										dwWait.getPointer(), Integer.BYTES, dwTag.getPointer(), Integer.BYTES, dwOut,
										null) && bqi.BatteryTag > 0) {
									// With the tag, you can query the battery info.
									BATTERY_INFORMATION bi = new BATTERY_INFORMATION();
									bqi.InformationLevel = PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryInformation
											.ordinal();
									System.out.println(bqi.toString());
									
									ret = Kernel32.INSTANCE.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_INFORMATION,
											bqi.getPointer(), bqi.size(), bi.getPointer(), bi.size(), dwOut, null);
									System.out.println("IOCTL Success: " + ret + ", LastError="
											+ Kernel32.INSTANCE.GetLastError() + ", dwOut=" + dwOut.getValue());

									if (Kernel32.INSTANCE.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_INFORMATION,
											bqi.getPointer(), bqi.size(), bi.getPointer(), bi.size(), dwOut, null)) {
										// Only non-UPS system batteries count
										System.out.println(bqi.toString());
										System.out.println(bi.toString());
										if (0 != (bi.Capabilities & BATTERY_SYSTEM_BATTERY)) {
											if (0 == (bi.Capabilities & BATTERY_IS_SHORT_TERM)) {
												dwResult |= GBS_HASBATTERY;
											}

											// Query the battery status.
											PowrProf.BATTERY_WAIT_STATUS bws = new PowrProf.BATTERY_WAIT_STATUS();
											bws.BatteryTag = bqi.BatteryTag;

											PowrProf.BATTERY_STATUS bs = new PowrProf.BATTERY_STATUS();

											if (Kernel32.INSTANCE.DeviceIoControl(hBattery, IOCTL_BATTERY_QUERY_STATUS,
													bws.getPointer(), bws.size(), bs.getPointer(), bs.size(), dwOut,
													null)) {
												System.out.println(bs.toString());
												if (0 != (bs.PowerState & BATTERY_POWER_ON_LINE)) {
													dwResult &= ~GBS_ONBATTERY;
												}
											}
										}
									}
								}
								Kernel32.INSTANCE.CloseHandle(hBattery);
							}
						}
					}
				} else if (WinError.ERROR_NO_MORE_ITEMS == Kernel32.INSTANCE.GetLastError()) {
					break; // Enumeration failed - perhaps we're out of items
				}
			}
			SetupApi.INSTANCE.SetupDiDestroyDeviceInfoList(hdev);
		}

		// Final cleanup: If we didn't find a battery, then presume that we
		// are on AC power.

		if (0 == (dwResult & GBS_HASBATTERY)) {
			dwResult &= ~GBS_ONBATTERY;
		}

		return dwResult;
	}

	/** {@inheritDoc} */
	@Override
	public void updateAttributes() {
		PowerSource ps = getPowerSource(this.name);
		this.remainingCapacity = ps.getRemainingCapacity();
		this.timeRemaining = ps.getTimeRemaining();
	}

	public static void main(String[] args) {
		System.out.println("RESULT: " + WindowsPowerSource.GetBatteryState());
	}
}
