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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystem.ProcessSort;
import oshi.util.FormatUtil;

public class ProcessPanel extends OshiJPanel { // NOSONAR squid:S110

    private static final long serialVersionUID = 1L;

    private static final String PROCESSES = "Processes";
    private static final String[] COLUMNS = { "PID", "% CPU", "% Memory", "VSZ", "RSS", "Name" };

    private Map<Integer, OSProcess> priorSnapshotMap = new HashMap<>();

    public ProcessPanel(SystemInfo si) {
        super();
        init(si);
    }

    private void init(SystemInfo si) {
        OperatingSystem os = si.getOperatingSystem();
        GlobalMemory mem = si.getHardware().getMemory();
        JLabel procLabel = new JLabel(PROCESSES);
        add(procLabel, BorderLayout.NORTH);

        TableModel model = new DefaultTableModel(parseProcesses(os.getProcesses(0, ProcessSort.CPU), mem), COLUMNS);
        JTable procTable = new JTable(model);
        JScrollPane scrollV = new JScrollPane(procTable);
        scrollV.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(scrollV, BorderLayout.CENTER);

        Timer timer = new Timer(Config.REFRESH_SLOW, e -> {
            DefaultTableModel tableModel = (DefaultTableModel) procTable.getModel();
            Object[][] newData = parseProcesses(os.getProcesses(0, ProcessSort.CPU), mem);
            int rowCount = tableModel.getRowCount();
            for (int row = 0; row < newData.length; row++) {
                if (row < rowCount) {
                    // Overwrite row
                    for (int col = 0; col < newData[row].length; col++) {
                        tableModel.setValueAt(newData[row][col], row, col);
                    }
                } else {
                    // Add row
                    tableModel.addRow(newData[row]);
                }
            }
            // Delelte any extra rows
            for (int row = rowCount - 1; row >= newData.length; row--) {
                tableModel.removeRow(row);
            }
        });
        timer.start();
    }

    private Object[][] parseProcesses(OSProcess[] procs, GlobalMemory mem) {
        // Build a sorted (by CPU) map
        Map<OSProcess, Double> processCpuMap = new HashMap<>();
        for (OSProcess p : procs) {
            // Get previous update. OK to return null for next method call
            OSProcess priorSnapshot = priorSnapshotMap.get(p.getProcessID());
            processCpuMap.put(p, p.getProcessCpuLoadBetweenTicks(priorSnapshot));
        }
        // Now sort
        List<Entry<OSProcess, Double>> procList = new ArrayList<>(processCpuMap.entrySet());
        procList.sort(Entry.comparingByValue());
        // Insert into array in reverse order (lowest CPU last)
        // Simultaneously re-populate snapshot map
        int i = procs.length;
        Object[][] procArr = new Object[procs.length][6];
        priorSnapshotMap.clear();
        // These are in descending CPU order
        for (Entry<OSProcess, Double> e : procList) {
            OSProcess p = e.getKey();
            priorSnapshotMap.put(p.getProcessID(), p);
            // Matches order of COLUMNS field
            i--;
            procArr[i][0] = p.getProcessID();
            procArr[i][1] = String.format("%.1f", 100d * e.getValue());
            procArr[i][2] = String.format("%.1f", 100d * p.getResidentSetSize() / mem.getTotal());
            procArr[i][3] = FormatUtil.formatBytes(p.getVirtualSize());
            procArr[i][4] = FormatUtil.formatBytes(p.getResidentSetSize());
            procArr[i][5] = p.getName();
        }
        return procArr;
    }
}
