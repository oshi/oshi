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

import oshi.SystemInfo;
import oshi.hardware.NetworkIF;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

/**
 * Displays a process list, such as ps or task manager. This performs more like
 * Windows Task Manger with current CPU as measured between polling intervals,
 * while PS uses a cumulative CPU value.
 * @author Constantine
 */
@SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
public class InterfacePanel extends OshiJPanel { // NOSONAR squid:S110

    private static final long serialVersionUID = 1L;

    private static final int INIT_HASH_SIZE = 100;
    private static final String IP_ADDRESS_SEPARATOR = ";";

    private static final String INTERFACE = "Interfaces";
    private static final String[] COLUMNS = { "Name", "Index", "Speed", "IPv4 Address", "IPv6 address"};
    private static final double[] COLUMN_WIDTH_PERCENT = { 0.01, 0.01, 0.01, 0.1, 0.6};

    public InterfacePanel(SystemInfo si) {
        super();
        init(si);
    }

    private void init(SystemInfo si) {
        List<NetworkIF> networkIfList = si.getHardware().getNetworkIFs(true);
        JLabel interfaceLabel = new JLabel(INTERFACE);
        add(interfaceLabel, BorderLayout.NORTH);

        TableModel model = new DefaultTableModel(parseInterfaces(networkIfList), COLUMNS);
        JTable intfTable = new JTable(model);
        JScrollPane scrollV = new JScrollPane(intfTable);
        scrollV.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        resizeColumns(intfTable.getColumnModel());
        add(scrollV, BorderLayout.CENTER);
    }

    private Object[][] parseInterfaces(List<NetworkIF> list) {
        Map<NetworkIF, Integer> intfSortValueMap = new HashMap<>(INIT_HASH_SIZE);
        for (NetworkIF intf : list) {
            intfSortValueMap.put(intf, intf.getIndex());
        }
        List<Entry<NetworkIF, Integer>> intfList = new ArrayList<>(intfSortValueMap.entrySet());
        intfList.sort(Entry.comparingByValue());

        int i = 0;
        Object[][] intfArr = new Object[intfList.size()][COLUMNS.length];

        for (Entry<NetworkIF, Integer> e : intfList) {
            NetworkIF intf = e.getKey();

            intfArr[i][0] = intf.getName();
            intfArr[i][1] = intf.getIndex();
            intfArr[i][2] = intf.getSpeed();
            intfArr[i][3] = getIPAddressesString(intf.getIPv4addr());
            intfArr[i][4] = getIPAddressesString(intf.getIPv6addr());

            i++;
        }

        return intfArr;
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

    private static String getIPAddressesString(String[] ipAddressArr) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (String ipAddress : ipAddressArr) {
            if (first) {
                first = false;
            } else {
                sb.append(IP_ADDRESS_SEPARATOR);
            }
            sb.append(ipAddress);
        }

        return sb.toString();
    }
}
