/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import oshi.SystemInfo;
import oshi.hardware.NetworkIF;
import oshi.software.os.NetworkParams;
import oshi.software.os.OperatingSystem;
import oshi.util.Constants;

/**
 * Displays a interface list, such as ifconfig.
 */
public class InterfacePanel extends OshiJPanel { // NOSONAR squid:S110

    private static final long serialVersionUID = 1L;

    private static final int INIT_HASH_SIZE = 100;
    private static final String IP_ADDRESS_SEPARATOR = "; ";

    private static final String PARAMS = "Network Parameters";
    private static final String INTERFACES = "Network Interfaces";
    private static final String[] COLUMNS = { "Name", "Index", "Speed", "IPv4 Address", "IPv6 address", "MAC address" };
    private static final double[] COLUMN_WIDTH_PERCENT = { 0.02, 0.02, 0.1, 0.25, 0.45, 0.15 };

    public InterfacePanel(SystemInfo si) {
        super();
        init(si);
    }

    private void init(SystemInfo si) {
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel paramsLabel = new JLabel(PARAMS);
        paramsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(paramsLabel);

        JTextArea paramsArea = new JTextArea(0, 0);
        paramsArea.setText(buildParamsText(si.getOperatingSystem()));
        add(paramsArea);

        JLabel interfaceLabel = new JLabel(INTERFACES);
        interfaceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(interfaceLabel);

        List<NetworkIF> networkIfList = si.getHardware().getNetworkIFs(true);

        TableModel model = new DefaultTableModel(parseInterfaces(networkIfList), COLUMNS);
        JTable intfTable = new JTable(model);
        JScrollPane scrollV = new JScrollPane(intfTable);
        scrollV.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        resizeColumns(intfTable.getColumnModel());
        add(scrollV);
    }

    private static String buildParamsText(OperatingSystem os) {
        NetworkParams params = os.getNetworkParams();
        StringBuilder sb = new StringBuilder("Host Name: ").append(params.getHostName());
        if (!params.getDomainName().isEmpty()) {
            sb.append("\nDomain Name: ").append(params.getDomainName());
        }
        sb.append("\nIPv4 Default Gateway: ").append(params.getIpv4DefaultGateway());
        if (!params.getIpv6DefaultGateway().isEmpty()) {
            sb.append("\nIPv6 Default Gateway: ").append(params.getIpv6DefaultGateway());
        }
        sb.append("\nDNS Servers: ").append(getIPAddressesString(params.getDnsServers()));
        return sb.toString();
    }

    private static Object[][] parseInterfaces(List<NetworkIF> list) {
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
            intfArr[i][5] = Constants.UNKNOWN.equals(intf.getMacaddr()) ? "" : intf.getMacaddr();

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
