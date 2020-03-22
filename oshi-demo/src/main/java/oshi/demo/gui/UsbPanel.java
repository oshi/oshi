/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
package oshi.demo.gui;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.text.DefaultCaret;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.UsbDevice;

/**
 * Shows USB devices. Simply prints OSHI's output in a scrollable pane.
 */
public class UsbPanel extends OshiJPanel { // NOSONAR squid:S110

    private static final long serialVersionUID = 1L;

    private static final String USB_DEVICES = "USB Devices";

    public UsbPanel(SystemInfo si) {
        super();
        init(si.getHardware());
    }

    private void init(HardwareAbstractionLayer hal) {

        JLabel usb = new JLabel(USB_DEVICES);
        add(usb, BorderLayout.NORTH);
        JTextArea usbArea = new JTextArea(60, 20);
        JScrollPane scrollV = new JScrollPane(usbArea);
        scrollV.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        DefaultCaret caret = (DefaultCaret) usbArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        usbArea.setText(getUsbString(hal));
        add(scrollV, BorderLayout.CENTER);

        Timer timer = new Timer(Config.REFRESH_SLOW, e -> usbArea.setText(getUsbString(hal)));
        timer.start();
    }

    private static String getUsbString(HardwareAbstractionLayer hal) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (UsbDevice usbDevice : hal.getUsbDevices(true)) {
            if (first) {
                first = false;
            } else {
                sb.append('\n');
            }
            sb.append(String.valueOf(usbDevice));
        }
        return sb.toString();
    }

}
