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
package oshi.demo;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;

import oshi.SystemInfo;
import oshi.demo.gui.Config;
import oshi.demo.gui.FileStorePanel;
import oshi.demo.gui.MemoryPanel;
import oshi.demo.gui.OsHwTextPanel;
import oshi.demo.gui.OshiJPanel;
import oshi.demo.gui.ProcessPanel;
import oshi.demo.gui.ProcessorPanel;
import oshi.demo.gui.UsbPanel;

/**
 * Basic Swing class to demonstrate potential uses for OSHI in a monitoring GUI.
 * Not ready for production use and intended as inspiration/examples.
 */
public class OshiGui {

    private JFrame mainFrame;
    private JButton jMenu;

    private SystemInfo si = new SystemInfo();

    public static void main(String[] args) {
        OshiGui gui = new OshiGui();
        gui.init();
        SwingUtilities.invokeLater(() -> gui.setVisible());
    }

    private void setVisible() {
        mainFrame.setVisible(true);
        jMenu.doClick();
    }

    private void init() {
        mainFrame = new JFrame(Config.GUI_TITLE);
        mainFrame.setSize(Config.GUI_WIDTH, Config.GUI_HEIGHT);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setResizable(true);
        mainFrame.setLocationByPlatform(true);
        mainFrame.setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        jMenu = getJMenu("OS & HW Info", 'O', "Hardware & OS Summary", getComputerSystemPanel());
        menuBar.add(jMenu);
        menuBar.add(getJMenu("Memory", 'M', "Memory Summary", getMemoryPanel()));
        menuBar.add(getJMenu("CPU", 'C', "CPU Usage", getProcessorPanel()));
        menuBar.add(getJMenu("FileStores", 'F', "FileStore Usage", getFileStorePanel()));
        menuBar.add(getJMenu("Processes", 'P', "Processes", getProcessPanel()));
        menuBar.add(getJMenu("USB Devices", 'U', "USB Device list", getUsbPanel()));

        mainFrame.setJMenuBar(menuBar);
    }

    private OsHwTextPanel getComputerSystemPanel() {
        return new OsHwTextPanel(si);
    }

    private MemoryPanel getMemoryPanel() {
        return new MemoryPanel(si.getHardware().getMemory());
    }

    private ProcessorPanel getProcessorPanel() {
        return new ProcessorPanel(si.getHardware().getProcessor());
    }

    private FileStorePanel getFileStorePanel() {
        return new FileStorePanel(si.getOperatingSystem().getFileSystem());
    }

    private ProcessPanel getProcessPanel() {
        return new ProcessPanel(si);
    }

    private UsbPanel getUsbPanel() {
        return new UsbPanel(si.getHardware());
    }

    private JButton getJMenu(String title, char mnemonic, String toolTip, OshiJPanel panel) {
        JButton button = new JButton(title);
        button.setMnemonic(mnemonic);
        button.setToolTipText(toolTip);
        button.addActionListener(e -> {
            Container contentPane = mainFrame.getContentPane();

            if (contentPane.getComponents().length <= 0 || contentPane.getComponent(0) != panel) {
                resetMainGui();
                mainFrame.getContentPane().add(panel);
                refreshMainGui();
            }
        });

        return button;
    }

    private void resetMainGui() {
        mainFrame.getContentPane().removeAll();
    }

    private void refreshMainGui() {
        mainFrame.revalidate();
        mainFrame.repaint();
    }
}
