/**
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
package oshi.demo.gui;

import oshi.SystemInfo;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OperatingSystem;
import oshi.util.IPUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;

/**
 * Displays network parameters.
 *
 */
@SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
public class NetworkPanel extends OshiJPanel { // NOSONAR squid:S110
    private static final long serialVersionUID = 1L;

    private static final int INIT_HASH_SIZE = 1000;
    private static final String NETWORK_PARAMS = "Network Parameters";
    private static final String IP_CONNECTION = "IP Connection";
    private static final String[] COLUMNS =
        {
            "Type", "Local Address", "Local Port",
            "Foreign Address", "Foreign Port", "State",
            "Owner Process Id", "Receive Queue", "Transmit Queue"
        };
    private static final double[] COLUMN_WIDTH_PERCENT =
        {
            0.02, 0.1, 0.02,
            0.1, 0.02, 0.02,
            0.05, 0.05, 0.05
        };

    public NetworkPanel(SystemInfo si) {
        super();
        init(si);
    }

    private void init(SystemInfo si) {
        JLabel networkParamsLabel = new JLabel(NETWORK_PARAMS);
        networkParamsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(networkParamsLabel, BorderLayout.NORTH);
        JTextArea networkParamsArea = new JTextArea(0, 0);
        networkParamsArea.setText(buildNetworkParamsText(si.getOperatingSystem()));
        add(networkParamsArea);

        JLabel ipConnectionLabel = new JLabel(IP_CONNECTION);
        ipConnectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(ipConnectionLabel, BorderLayout.NORTH);
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        List<InternetProtocolStats.IPConnection> ipConnectionList = si.getOperatingSystem().getInternetProtocolStats().getConnections();
        TableModel model = new DefaultTableModel(parseIPConnection(ipConnectionList), COLUMNS);
        JTable ipConnectionTable = new JTable(model);
        JScrollPane scrollV = new JScrollPane(ipConnectionTable);
        scrollV.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        resizeColumns(ipConnectionTable.getColumnModel());
        add(scrollV);
    }

    private static String buildNetworkParamsText(OperatingSystem os) {
        NetworkParams params = os.getNetworkParams();
        StringBuilder sb = new StringBuilder("Host Name: ").append(params.getHostName());
        if (!params.getDomainName().isEmpty()) {
            sb.append("\nDomain Name: ").append(params.getDomainName());
        }
        if (params.getDnsServers().length > 0) {
            sb.append("\nDNS Server: ").append(IPUtil.toIPAddressStringByArray(params.getDnsServers()));
        }
        sb.append("\nIPv4 Default Gateway: ").append(params.getIpv4DefaultGateway());
        if (!params.getIpv6DefaultGateway().isEmpty()) {
            sb.append("\nIPv6 Default Gateway: ").append(params.getIpv6DefaultGateway());
        }
        sb.append("\nDNS Servers: ").append(IPUtil.toIPAddressStringByArray(params.getDnsServers()));

        return sb.toString();
    }

    private static Object[][] parseIPConnection(List<InternetProtocolStats.IPConnection> list) {
        Map<String, InternetProtocolStats.IPConnection> ipConnectionStringHashMap = new HashMap<>(INIT_HASH_SIZE);
        for (InternetProtocolStats.IPConnection ipConnection : list) {
            ipConnectionStringHashMap.put(ipConnection.getType(), ipConnection);
        }
        List<Entry<String, InternetProtocolStats.IPConnection>> ipConnectionList = new ArrayList<>(ipConnectionStringHashMap.entrySet());
        ipConnectionList.sort(Entry.comparingByKey());

        int i = 0;
        Object[][] ipConnectionArr = new Object[ipConnectionList.size()][COLUMNS.length];

        for (Entry<String, InternetProtocolStats.IPConnection> e : ipConnectionList) {
            InternetProtocolStats.IPConnection ipConnection = e.getValue();

            ipConnectionArr[i][0] = ipConnection.getType();
            ipConnectionArr[i][1] = IPUtil.toCompressedIPAddressStrByByteArray(ipConnection.getLocalAddress());
            ipConnectionArr[i][2] = ipConnection.getLocalPort();
            ipConnectionArr[i][3] = IPUtil.toCompressedIPAddressStrByByteArray(ipConnection.getForeignAddress());
            ipConnectionArr[i][4] = ipConnection.getForeignPort();
            ipConnectionArr[i][5] = ipConnection.getState().toString();
            ipConnectionArr[i][6] = ipConnection.getowningProcessId();
            ipConnectionArr[i][7] = ipConnection.getReceiveQueue();
            ipConnectionArr[i][8] = ipConnection.getTransmitQueue();

            i++;
        }

        return ipConnectionArr;
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
