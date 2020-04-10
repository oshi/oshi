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
import javax.swing.WindowConstants;

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
    private JMenuBar menuBar;

    private SystemInfo si = new SystemInfo();

    public static void main(String[] args) {
        OshiGui gui = new OshiGui();
        gui.init();
        SwingUtilities.invokeLater(gui::setVisible);
    }

    private void setVisible() {
        mainFrame.setVisible(true);
        jMenu.doClick();
    }

    private void init() {
        // Create the external frame
        mainFrame = new JFrame(Config.GUI_TITLE);
        mainFrame.setSize(Config.GUI_WIDTH, Config.GUI_HEIGHT);
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setResizable(true);
        mainFrame.setLocationByPlatform(true);
        mainFrame.setLayout(new BorderLayout());
        // Add a menu bar
        menuBar = new JMenuBar();
        mainFrame.setJMenuBar(menuBar);
        // Create the first menu option in this thread
        jMenu = getJMenu("OS & HW Info", 'O', "Hardware & OS Summary", new OsHwTextPanel(si));
        menuBar.add(jMenu);
        // Add later menu items in their own threads
        new Thread(new AddMenuBarTask("Memory", 'M', "Memory Summary", new MemoryPanel(si))).start();
        new Thread(new AddMenuBarTask("CPU", 'C', "CPU Usage", new ProcessorPanel(si))).start();
        new Thread(new AddMenuBarTask("FileStores", 'F', "FileStore Usage", new FileStorePanel(si))).start();
        new Thread(new AddMenuBarTask("Processes", 'P', "Processes", new ProcessPanel(si))).start();
        new Thread(new AddMenuBarTask("USB Devices", 'U', "USB Device list", new UsbPanel(si))).start();
    }

    private JButton getJMenu(String title, char mnemonic, String toolTip, OshiJPanel panel) {
        JButton button = new JButton(title);
        button.setMnemonic(mnemonic);
        button.setToolTipText(toolTip);
        button.addActionListener(e -> {
            Container contentPane = this.mainFrame.getContentPane();
            if (contentPane.getComponents().length <= 0 || contentPane.getComponent(0) != panel) {
                resetMainGui();
                this.mainFrame.getContentPane().add(panel);
                refreshMainGui();
            }
        });

        return button;
    }

    private void resetMainGui() {
        this.mainFrame.getContentPane().removeAll();
    }

    private void refreshMainGui() {
        this.mainFrame.revalidate();
        this.mainFrame.repaint();
    }

    /**
     * Runnable class to add a JMenu to the menubar.
     */
    private final class AddMenuBarTask implements Runnable {
        private String title;
        private char mnemonic;
        private String toolTip;
        private OshiJPanel panel;

        private AddMenuBarTask(String title, char mnemonic, String toolTip, OshiJPanel panel) {
            this.title = title;
            this.mnemonic = mnemonic;
            this.toolTip = toolTip;
            this.panel = panel;
        }

        @Override
        public void run() {
            menuBar.add(getJMenu(title, mnemonic, toolTip, panel));
        }
    }
}
