/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.UsbDevice;

class FreeBsdUsbDeviceTest {

    /**
     * devinfo -v on a FreeBSD 15.0 Arm Morello board (cfarm240), trimmed: a BeagleBone Black on the XHCI controller's
     * root hub, listed once for each of its three interface drivers, followed by two PCI functions without a driver.
     */
    private static final List<String> MORELLO = """
                            pcib8 <PCI-PCI bridge> pnpinfo vendor=0x10b5 device=0x8749 subvendor=0x10b5 subdevice=0x8749 class=0x060400 at slot=20 function=0 dbsf=pci0:2:20:0
                              pci7 <PCI bus>
                                xhci0 <XHCI (generic) USB 3.0 controller> pnpinfo vendor=0x104c device=0x8241 subvendor=0x0000 subdevice=0x0000 class=0x0c0330 at slot=0 function=0 dbsf=pci0:8:0:0
                                  usbus0
                                    uhub0 <(0x104c) XHCI root HUB, class 9/0, rev 3.00/1.00, addr 1>
                                      urndis0 <BeagleBoard.org TIAM335xBeagleBoneBlack, class 0/0, rev 2.00/4.04, addr 1> pnpinfo vendor=0x1d6b product=0x0104 devclass=0x00 devsubclass=0x00 devproto=0x00 sernum="1234BBBK5678" release=0x0404 mode=host intclass=0x02 intsubclass=0x02 intprotocol=0xff at bus=0 hubaddr=1 port=4 devaddr=2 interface=0 ugen=ugen0.2
                                      cdce0 <BeagleBoard.org TIAM335xBeagleBoneBlack, class 0/0, rev 2.00/4.04, addr 1> pnpinfo vendor=0x1d6b product=0x0104 devclass=0x00 devsubclass=0x00 devproto=0x00 sernum="1234BBBK5678" release=0x0404 mode=host intclass=0x02 intsubclass=0x0d intprotocol=0x00 at bus=0 hubaddr=1 port=4 devaddr=2 interface=2 ugen=ugen0.2
                                      umodem0 <BeagleBoard.org TIAM335xBeagleBoneBlack, class 0/0, rev 2.00/4.04, addr 1> pnpinfo vendor=0x1d6b product=0x0104 devclass=0x00 devsubclass=0x00 devproto=0x00 sernum="1234BBBK5678" release=0x0404 mode=host intclass=0x02 intsubclass=0x02 intprotocol=0x01 ttyname=U0 ttyports=1 at bus=0 hubaddr=1 port=4 devaddr=2 interface=4 ugen=ugen0.2
                        unknown pnpinfo vendor=0x10b5 device=0x87d0 subvendor=0x10b5 subdevice=0x87d0 class=0x088000 at slot=0 function=1 dbsf=pci0:1:0:1
                        unknown pnpinfo vendor=0x10b5 device=0x87d0 subvendor=0x10b5 subdevice=0x87d0 class=0x088000 at slot=0 function=2 dbsf=pci0:1:0:2
            """
            .lines().toList();

    /** devinfo -v on a FreeBSD 16.0-CURRENT VM (cfarm430), trimmed: two UHCI controllers with only root hubs. */
    private static final List<String> VM = """
                    uhci0 <Intel 82801I (ICH9) USB controller> pnpinfo vendor=0x8086 device=0x2937 subvendor=0x1af4 subdevice=0x1100 class=0x0c0300 at slot=26 function=0 dbsf=pci0:0:26:0 handle=\\_SB_.PCI0.SD0_
                      usbus0
                        uhub0 <Intel UHCI root HUB, class 9/0, rev 1.00/1.00, addr 1>
                    uhci1 <Intel 82801I (ICH9) USB controller> pnpinfo vendor=0x8086 device=0x2938 subvendor=0x1af4 subdevice=0x1100 class=0x0c0300 at slot=26 function=1 dbsf=pci0:0:26:1 handle=\\_SB_.PCI0.SD1_
                      usbus1
                        uhub6 <Intel UHCI root HUB, class 9/0, rev 1.00/1.00, addr 1>
            """
            .lines().toList();

    @Test
    void testParseUsbDevices() {
        List<UsbDevice> controllers = FreeBsdUsbDevice.parseUsbDevices(MORELLO);
        assertThat(controllers, hasSize(1));
        UsbDevice xhci = controllers.get(0);
        assertThat(xhci.getName(), is("XHCI (generic) USB 3.0 controller"));
        assertThat(xhci.getVendorId(), is("104c"));
        assertThat(xhci.getProductId(), is("8241"));
        // The usbus is skipped: the root hub hangs from the controller
        assertThat(xhci.getConnectedDevices(), hasSize(1));
        UsbDevice hub = xhci.getConnectedDevices().get(0);
        assertThat(hub.getName(), is("(0x104c) XHCI root HUB"));
        // One device, though three interface drivers list it
        assertThat(hub.getConnectedDevices(), hasSize(1));
        UsbDevice beagle = hub.getConnectedDevices().get(0);
        assertThat(beagle.getName(), is("BeagleBoard.org TIAM335xBeagleBoneBlack"));
        assertThat(beagle.getVendorId(), is("1d6b"));
        assertThat(beagle.getProductId(), is("0104"));
        assertThat(beagle.getSerialNumber(), is("1234BBBK5678"));
        assertThat(beagle.getUniqueDeviceId(), is("ugen0.2"));
        assertThat(beagle.getConnectedDevices(), is(empty()));
    }

    @Test
    void testParseUsbDevicesRootHubsOnly() {
        List<UsbDevice> controllers = FreeBsdUsbDevice.parseUsbDevices(VM);
        assertThat(controllers, hasSize(2));
        assertThat(controllers.get(0).getName(), is("Intel 82801I (ICH9) USB controller"));
        assertThat(controllers.get(1).getProductId(), is("2938"));
        UsbDevice hub = controllers.get(1).getConnectedDevices().get(0);
        assertThat(hub.getName(), is("Intel UHCI root HUB"));
        // A root hub has no pnpinfo of its own, so carries its controller's IDs
        assertThat(hub.getVendorId(), is("8086"));
        assertThat(hub.getConnectedDevices(), is(empty()));
    }

    @Test
    void testParseUsbDevicesSkipsChildDrivers() {
        // devinfo -v on cfarm430: QEMU's tablet, whose HID driver carries a HID bus and a mouse driver beneath it. The
        // mouse driver has a vendor and product of its own, but no ugen name
        List<String> tablet = """
                        ehci1 <Intel 82801I (ICH9) USB 2.0 controller> pnpinfo vendor=0x8086 device=0x293a subvendor=0x1af4 subdevice=0x1100 class=0x0c0320 at slot=29 function=7 dbsf=pci0:0:29:7 handle=\\_SB_.PCI0.SEF_
                          usbus7
                            uhub3 <Intel EHCI root HUB, class 9/0, rev 2.00/1.00, addr 1>
                              usbhid0 <QEMU QEMU USB Tablet, class 0/0, rev 2.00/0.00, addr 2> pnpinfo vendor=0x0627 product=0x0001 devclass=0x00 devsubclass=0x00 devproto=0x00 sernum="28754-0000:00:1d.7-1" release=0x0000 mode=host intclass=0x03 intsubclass=0x00 intprotocol=0x00 at bus=7 hubaddr=1 port=1 devaddr=2 interface=0 ugen=ugen7.2
                                hidbus0 <HID bus>
                                  hms0 <QEMU QEMU USB Tablet> pnpinfo page=0x0001 usage=0x0002 bus=0x03 vendor=0x0627 product=0x0001 version=0x0000 at index=0
                """
                .lines().toList();
        UsbDevice hub = FreeBsdUsbDevice.parseUsbDevices(tablet).get(0).getConnectedDevices().get(0);
        assertThat(hub.getConnectedDevices(), hasSize(1));
        UsbDevice usbTablet = hub.getConnectedDevices().get(0);
        assertThat(usbTablet.getName(), is("QEMU QEMU USB Tablet"));
        assertThat(usbTablet.getVendorId(), is("0627"));
        assertThat(usbTablet.getSerialNumber(), is("28754-0000:00:1d.7-1"));
        assertThat(usbTablet.getConnectedDevices(), is(empty()));
    }

    @Test
    void testParseUsbDevicesEmpty() {
        assertThat(FreeBsdUsbDevice.parseUsbDevices(Collections.emptyList()), is(empty()));
        // No USB controller at all
        assertThat(FreeBsdUsbDevice.parseUsbDevices(List.of("nexus0", "  acpi0 <BOCHS BXPC>")), is(empty()));
    }
}
