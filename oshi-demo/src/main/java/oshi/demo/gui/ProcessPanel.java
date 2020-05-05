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

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import oshi.PlatformEnum;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;

/**
 * Displays a process list, such as ps or task manager. This performs more like
 * Windows Task Manger with current CPU as measured between polling intervals,
 * while PS uses a cumulative CPU value.
 */
public class ProcessPanel extends OshiJPanel { // NOSONAR squid:S110

    private static final long serialVersionUID = 1L;

    private static final String PROCESSES = "Processes";
    private static final String[] COLUMNS = { "PID", "PPID", "Threads", "% CPU", "Cumulative", "VSZ", "RSS", "% Memory",
            "Process Name" };
    private static final double[] COLUMN_WIDTH_PERCENT = { 0.07, 0.07, 0.07, 0.07, 0.09, 0.1, 0.1, 0.08, 0.35 };

    private transient Map<Integer, OSProcess> priorSnapshotMap = new HashMap<>();

    private transient ButtonGroup cpuOption = new ButtonGroup();
    private transient JRadioButton perProc = new JRadioButton("of one Processor");
    private transient JRadioButton perSystem = new JRadioButton("of System");

    private transient ButtonGroup sortOption = new ButtonGroup();
    private transient JRadioButton cpuButton = new JRadioButton("CPU %");
    private transient JRadioButton cumulativeCpuButton = new JRadioButton("Cumulative CPU");
    private transient JRadioButton memButton = new JRadioButton("Memory %");

    public ProcessPanel(SystemInfo si) {
        super();
        init(si);
    }

    private void init(SystemInfo si) {
        OperatingSystem os = si.getOperatingSystem();
        JLabel procLabel = new JLabel(PROCESSES);
        add(procLabel, BorderLayout.NORTH);

        JPanel settings = new JPanel();

        JLabel cpuChoice = new JLabel("          CPU %:");
        settings.add(cpuChoice);
        cpuOption.add(perProc);
        settings.add(perProc);
        cpuOption.add(perSystem);
        settings.add(perSystem);
        if (SystemInfo.getCurrentPlatformEnum().equals(PlatformEnum.WINDOWS)) {
            perSystem.setSelected(true);
        } else {
            perProc.setSelected(true);
        }

        JLabel sortChoice = new JLabel("          Sort by:");
        settings.add(sortChoice);
        sortOption.add(cpuButton);
        settings.add(cpuButton);
        sortOption.add(cumulativeCpuButton);
        settings.add(cumulativeCpuButton);
        sortOption.add(memButton);
        settings.add(memButton);
        cpuButton.setSelected(true);

        TableModel model = new DefaultTableModel(parseProcesses(os.getProcesses(0, null), si), COLUMNS);
        JTable procTable = new JTable(model);
        JScrollPane scrollV = new JScrollPane(procTable);
        scrollV.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        resizeColumns(procTable.getColumnModel());

        add(scrollV, BorderLayout.CENTER);
        add(settings, BorderLayout.SOUTH);

        Timer timer = new Timer(Config.REFRESH_SLOW, e -> {
            DefaultTableModel tableModel = (DefaultTableModel) procTable.getModel();
            Object[][] newData = parseProcesses(os.getProcesses(0, null), si);
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
            // Delete any extra rows
            for (int row = rowCount - 1; row >= newData.length; row--) {
                tableModel.removeRow(row);
            }
        });
        timer.start();
    }

    private Object[][] parseProcesses(List<OSProcess> list, SystemInfo si) {
        long totalMem = si.getHardware().getMemory().getTotal();
        int cpuCount = si.getHardware().getProcessor().getLogicalProcessorCount();
        // Build a map with a value for each process to control the sort
        Map<OSProcess, Double> processSortValueMap = new HashMap<>();
        for (OSProcess p : list) {
            int pid = p.getProcessID();
            // Ignore the Idle process on Windows
            if (pid > 0 || !SystemInfo.getCurrentPlatformEnum().equals(PlatformEnum.WINDOWS)) {
                // Set up for appropriate sort
                if (cpuButton.isSelected()) {
                    processSortValueMap.put(p, p.getProcessCpuLoadBetweenTicks(priorSnapshotMap.get(pid)));
                } else if (cumulativeCpuButton.isSelected()) {
                    processSortValueMap.put(p, p.getProcessCpuLoadCumulative());
                } else {
                    processSortValueMap.put(p, (double) p.getResidentSetSize());
                }
            }
        }
        // Now sort the list by the values
        List<Entry<OSProcess, Double>> procList = new ArrayList<>(processSortValueMap.entrySet());
        procList.sort(Entry.comparingByValue());
        // Insert into array in reverse order (lowest sort value last)
        int i = procList.size();
        Object[][] procArr = new Object[i][COLUMNS.length];
        // These are in descending CPU order
        for (Entry<OSProcess, Double> e : procList) {
            OSProcess p = e.getKey();
            // Matches order of COLUMNS field
            i--;
            int pid = p.getProcessID();
            procArr[i][0] = pid;
            procArr[i][1] = p.getParentProcessID();
            procArr[i][2] = p.getThreadCount();
            if (perProc.isSelected()) {
                procArr[i][3] = String.format("%.1f",
                        100d * p.getProcessCpuLoadBetweenTicks(priorSnapshotMap.get(pid)) / cpuCount);
                procArr[i][4] = String.format("%.1f", 100d * p.getProcessCpuLoadCumulative() / cpuCount);
            } else {
                procArr[i][3] = String.format("%.1f",
                        100d * p.getProcessCpuLoadBetweenTicks(priorSnapshotMap.get(pid)));
                procArr[i][4] = String.format("%.1f", 100d * p.getProcessCpuLoadCumulative());
            }
            procArr[i][5] = FormatUtil.formatBytes(p.getVirtualSize());
            procArr[i][6] = FormatUtil.formatBytes(p.getResidentSetSize());
            procArr[i][7] = String.format("%.1f", 100d * p.getResidentSetSize() / totalMem);
            procArr[i][8] = p.getName();
        }
        // Re-populate snapshot map
        priorSnapshotMap.clear();
        for (OSProcess p : list) {
            priorSnapshotMap.put(p.getProcessID(), p);
        }
        return procArr;
    }

    private static void resizeColumns(TableColumnModel tableColumnModel) {
        TableColumn column;
        int tW = tableColumnModel.getTotalColumnWidth();
        int cantCols = tableColumnModel.getColumnCount();
        for (int i = 0; i < cantCols; i++) {
            column = tableColumnModel.getColumn(i);
            int pWidth = (int) Math.round(COLUMN_WIDTH_PERCENT[i] * tW);
            column.setPreferredWidth(pWidth);
        }
    }
}
