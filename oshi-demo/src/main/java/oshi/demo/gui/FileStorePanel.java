/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
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

/**
 * Displays used and free space on all filesystems.
 */
public class FileStorePanel extends OshiJPanel { // NOSONAR squid:S110

    private static final long serialVersionUID = 1L;

    private static final String USED = "Used";
    private static final String AVAILABLE = "Available";

    public FileStorePanel(SystemInfo si) {
        super();
        init(si.getOperatingSystem().getFileSystem());
    }

    private void init(FileSystem fs) {
        List<OSFileStore> fileStores = fs.getFileStores();
        @SuppressWarnings("unchecked")
        DefaultPieDataset<String>[] fsData = new DefaultPieDataset[fileStores.size()];
        JFreeChart[] fsCharts = new JFreeChart[fsData.length];

        JPanel fsPanel = new JPanel();
        fsPanel.setLayout(new GridBagLayout());
        GridBagConstraints fsConstraints = new GridBagConstraints();
        fsConstraints.weightx = 1d;
        fsConstraints.weighty = 1d;
        fsConstraints.fill = GridBagConstraints.BOTH;

        int modBase = (int) (fileStores.size() * (Config.GUI_HEIGHT + Config.GUI_WIDTH)
                / (Config.GUI_WIDTH * Math.sqrt(fileStores.size())));
        for (int i = 0; i < fileStores.size(); i++) {
            fsData[i] = new DefaultPieDataset<>();
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

    private static boolean updateDatasets(FileSystem fs, DefaultPieDataset<String>[] fsData, JFreeChart[] fsCharts) {
        List<OSFileStore> fileStores = fs.getFileStores();
        if (fileStores.size() != fsData.length) {
            return false;
        }
        int i = 0;
        for (OSFileStore store : fileStores) {
            fsCharts[i].setTitle(store.getName());
            List<TextTitle> subtitles = new ArrayList<>();
            if (SystemInfo.getCurrentPlatform().equals(PlatformEnum.WINDOWS)) {
                subtitles.add(new TextTitle(store.getLabel()));
            }
            long usable = store.getUsableSpace();
            long total = store.getTotalSpace();
            subtitles.add(new TextTitle(
                    "Available: " + FormatUtil.formatBytes(usable) + "/" + FormatUtil.formatBytes(total)));
            fsCharts[i].setSubtitles(subtitles);
            fsData[i].setValue(USED, (double) total - usable);
            fsData[i].setValue(AVAILABLE, usable);
            i++;
        }
        return true;
    }

    private static void configurePlot(JFreeChart chart) {
        @SuppressWarnings("unchecked")
        PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
        plot.setSectionPaint(USED, Color.red);
        plot.setSectionPaint(AVAILABLE, Color.green);
        plot.setExplodePercent(USED, 0.10);
        plot.setSimpleLabels(true);

        PieSectionLabelGenerator labelGenerator = new StandardPieSectionLabelGenerator("{0}: {1} ({2})",
                new DecimalFormat("0"), new DecimalFormat("0%"));
        plot.setLabelGenerator(labelGenerator);
    }

}
