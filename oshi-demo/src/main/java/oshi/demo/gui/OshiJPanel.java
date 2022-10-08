/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.demo.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Parent class combining code common to the other panels.
 */
public class OshiJPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    protected JLabel msgLabel = new JLabel();
    protected JPanel msgPanel = new JPanel();

    public OshiJPanel() {
        Dimension maxSize = getMaximumSize();
        if (maxSize != null) {
            setSize(maxSize);
        }
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        msgPanel.add(msgLabel);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(msgPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);
    }
}
