/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.jna.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.win32.ShellAPI;

public interface Winioctl extends com.sun.jna.platform.win32.Winioctl {

    int IOCTL_STORAGE_QUERY_PROPERTY = 0x2D1400;
    int IOCTL_USB_GET_ROOT_HUB_NAME = 0x220408;
    int IOCTL_USB_GET_NODE_INFORMATION = 0x220408;
    int IOCTL_USB_GET_NODE_CONNECTION_INFORMATION = 0x22040c;
    int IOCTL_USB_GET_NODE_CONNECTION_NAME = 0x220414;
    int IOCTL_USB_GET_NODE_CONNECTION_DRIVERKEY_NAME = 0x220420;

    int StorageDeviceProperty = 0;
    int PropertyStandardQuery = 0;

    @FieldOrder({ "ActualLength", "RootHubName" })
    class USB_ROOT_HUB_NAME extends Structure {
        public int ActualLength;
        public char[] RootHubName = new char[1];

        public USB_ROOT_HUB_NAME() {
            super();
            this.ActualLength = this.size();
            write();
        }

        public void resizeBuffer() {
            // Resize char array to account for int
            if (this.ActualLength > this.size()) {
                this.RootHubName = new char[(this.ActualLength - Integer.BYTES) / Native.WCHAR_SIZE];
                allocateMemory();
            }
        }
    }

    @FieldOrder({ "NodeType", "HubInformation" })
    class USB_NODE_INFORMATION extends Structure {
        public int NodeType; // UsbHub, UsbMIParent
        // We only use this for hub info, so simplifying union
        public USB_HUB_INFORMATION HubInformation;
    }

    @FieldOrder({ "HubDescriptor", "HubIsBusPowered" })
    class USB_HUB_INFORMATION extends Structure {
        public USB_HUB_DESCRIPTOR HubDescriptor;
        public byte HubIsBusPowered;
    }

    @FieldOrder({ "bDescriptorLength", "bDescriptorType", "bNumberOfPorts", "wHubCharacteristics",
            "bPowerOnToPowerGood", "bHubControlCurrent", "bRemoveAndPowerMask" })
    class USB_HUB_DESCRIPTOR extends Structure {
        public byte bDescriptorLength;
        public byte bDescriptorType;
        public byte bNumberOfPorts;
        public short wHubCharacteristics;
        public byte bPowerOnToPowerGood;
        public byte bHubControlCurrent;
        public byte[] bRemoveAndPowerMask = new byte[64];
    }

    @FieldOrder({ "ConnectionIndex", "DeviceDescriptor", "CurrentConfigurationValue", "LowSpeed", "DeviceIsHub",
            "DeviceAddress", "NumberOfOpenPipes", "ConnectionStatus" })
    class USB_NODE_CONNECTION_INFORMATION extends Structure {
        public int ConnectionIndex;
        public USB_DEVICE_DESCRIPTOR DeviceDescriptor;
        public byte CurrentConfigurationValue;
        public byte LowSpeed;
        public byte DeviceIsHub;
        public short DeviceAddress;
        public int NumberOfOpenPipes;
        public int ConnectionStatus; // USB_CONNECTION_STATUS
        // ignore USB_PIPE_INFO PipeList[0]

        public USB_NODE_CONNECTION_INFORMATION() {
            super(ShellAPI.STRUCTURE_ALIGNMENT);
        }

        public USB_NODE_CONNECTION_INFORMATION(int port) {
            this();
            this.ConnectionIndex = port;
            write();
        }
    }

    @FieldOrder({ "bLength", "bDescriptorType", "bcdUSB", "bDeviceClass", "bDeviceSubClass", "bDeviceProtocol",
            "bMaxPacketSize0", "idVendor", "idProduct", "bcdDevice", "iManufacturer", "iProduct", "iSerialNumber",
            "bNumConfigurations" })
    class USB_DEVICE_DESCRIPTOR extends Structure {
        public byte bLength;
        public byte bDescriptorType;
        public short bcdUSB;
        public byte bDeviceClass;
        public byte bDeviceSubClass;
        public byte bDeviceProtocol;
        public byte bMaxPacketSize0;
        public short idVendor;
        public short idProduct;
        public short bcdDevice;
        public byte iManufacturer;
        public byte iProduct;
        public byte iSerialNumber;
        public byte bNumConfigurations;
    }

    @FieldOrder({ "ConnectionIndex", "ActualLength", "DriverKeyName" })
    class USB_NODE_CONNECTION_DRIVERKEY_NAME extends Structure {
        public int ConnectionIndex;
        public int ActualLength;
        public char[] DriverKeyName = new char[1];

        public USB_NODE_CONNECTION_DRIVERKEY_NAME(int port) {
            super();
            this.ConnectionIndex = port;
            write();
        }

        public void resizeBuffer() {
            // Resize char array to account for int
            if (this.ActualLength > this.size()) {
                this.DriverKeyName = new char[(this.ActualLength - 2 * Integer.BYTES) / Native.WCHAR_SIZE];
                allocateMemory();
                write();
            }
        }
    }

    @FieldOrder({ "ConnectionIndex", "ActualLength", "NodeName" })
    class USB_NODE_CONNECTION_NAME extends Structure {
        public int ConnectionIndex;
        public int ActualLength;
        public char[] NodeName = new char[1];

        public USB_NODE_CONNECTION_NAME(int port) {
            super();
            this.ConnectionIndex = port;
            write();
        }

        public void resizeBuffer() {
            // Resize char array to account for int
            if (this.ActualLength > this.size()) {
                this.NodeName = new char[(this.ActualLength - 2 * Integer.BYTES) / Native.WCHAR_SIZE];
                allocateMemory();
                write();
            }
        }
    }

    // delete the below maybe

    @FieldOrder({ "PropertyId", "QueryType", "AdditionalParameters" })
    class STORAGE_PROPERTY_QUERY extends Structure {
        public int PropertyId;
        public int QueryType;
        public byte[] AdditionalParameters = new byte[1];
    }

    @FieldOrder({ "Version", "Size", "DeviceType", "DeviceTypeModifier", "RemovableMedia", "CommandQueueing",
            "VendorIdOffset", "ProductIdOffset", "ProductRevisionOffset", "SerialNumberOffset", "BusType",
            "RawPropertiesLength", "RawDeviceProperties" })
    class STORAGE_DEVICE_DESCRIPTOR extends Structure {
        public int Version; // Size of the base structure
        public int Size; // Total size of data
        public byte DeviceType;
        public byte DeviceTypeModifier;
        public byte RemovableMedia;
        public byte CommandQueueing;
        public int VendorIdOffset;
        public int ProductIdOffset;
        public int ProductRevisionOffset;
        public int SerialNumberOffset;
        public int BusType;
        public int RawPropertiesLength;
        public byte[] RawDeviceProperties = new byte[1];

        public STORAGE_DEVICE_DESCRIPTOR(Pointer p) {
            super(p);
            read();
        }
    }
}
