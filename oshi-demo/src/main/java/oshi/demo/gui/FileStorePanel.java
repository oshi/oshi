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
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DefaultPieDataset;

import oshi.PlatformEnum;
import oshi.SystemInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.FormatUtil;

public class FileStorePanel extends OshiJPanel { // NOSONAR squid:S110

    private static final long serialVersionUID = 1L;

    private static final String USED = "Used";
    private static final String AVAILABLE = "Available";

    public FileStorePanel(FileSystem fileSystem) {
        super();
        init(fileSystem);
    }

    private void init(FileSystem fs) {
        OSFileStore[] fileStores = fs.getFileStores();
        DefaultPieDataset[] fsData = new DefaultPieDataset[fileStores.length];
        JFreeChart[] fsCharts = new JFreeChart[fsData.length];

        JPanel fsPanel = new JPanel();
        fsPanel.setLayout(new GridBagLayout());
        GridBagConstraints fsConstraints = new GridBagConstraints();
        fsConstraints.weightx = 1d;
        fsConstraints.weighty = 1d;
        fsConstraints.fill = GridBagConstraints.BOTH;

        int modBase = (int) (fileStores.length * (Config.GUI_HEIGHT + Config.GUI_WIDTH)
                / (Config.GUI_WIDTH * Math.sqrt(fileStores.length)));
        for (int i = 0; i < fileStores.length; i++) {
            fsData[i] = new DefaultPieDataset();
            fsCharts[i] = ChartFactory.createPieChart(null, fsData[i], true, true, false);
            configurePlot(fsCharts[i]);
            fsConstraints.gridx = i % modBase;
            fsConstraints.gridy = i / modBase;
            fsPanel.add(new ChartPanel(fsCharts[i]), fsConstraints);
        }
        updateDatasets(fs, fsData, fsCharts);

        add(fsPanel, BorderLayout.CENTER);

        Timer timer = new Timer(Config.REFRESH_SLOWER, e -> {
            if (!updateDatasets(fs, fsData, fsCharts)) {
                ((Timer) e.getSource()).stop();
                fsPanel.removeAll();
                init(fs);
                fsPanel.revalidate();
                fsPanel.repaint();
            }
        });
        timer.start();
    }

    private boolean updateDatasets(FileSystem fs, DefaultPieDataset[] fsData, JFreeChart[] fsCharts) {
        OSFileStore[] fileStores = fs.getFileStores();
        if (fileStores.length != fsData.length) {
            return false;
        }
        for (int i = 0; i < fsData.length; i++) {
            fsCharts[i].setTitle(fileStores[i].getName());
            List<TextTitle> subtitles = new ArrayList<>();
            if (SystemInfo.getCurrentPlatformEnum().equals(PlatformEnum.WINDOWS)) {
                subtitles.add(new TextTitle(fileStores[i].getLabel()));
            }
            long usable = fileStores[i].getUsableSpace();
            long total = fileStores[i].getTotalSpace();
            subtitles.add(new TextTitle(
                    "Available: " + FormatUtil.formatBytes(usable) + "/" + FormatUtil.formatBytes(total)));
            fsCharts[i].setSubtitles(subtitles);
            fsData[i].setValue(USED, (double) total - usable);
            fsData[i].setValue(AVAILABLE, usable);
        }
        return true;
    }

    private void configurePlot(JFreeChart chart) {
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSectionPaint(USED, Color.red);
        plot.setSectionPaint(AVAILABLE, Color.green);
        plot.setExplodePercent(USED, 0.10);
        plot.setSimpleLabels(true);

        PieSectionLabelGenerator labelGenerator = new StandardPieSectionLabelGenerator("{0}: {1} ({2})",
                new DecimalFormat("0"), new DecimalFormat("0%"));
        plot.setLabelGenerator(labelGenerator);
    }

}
