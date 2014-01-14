/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui.dialogs.network;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.trainer.IterativeControlsPanel;
import org.simbrain.network.gui.trainer.subnetworkTrainingPanels.LMSOfflineControlPanel;
import org.simbrain.network.subnetworks.LMSNetwork;
import org.simbrain.network.trainers.LMSIterative;
import org.simbrain.network.trainers.LMSOffline;

/**
 * <b>LMSEditorDialog</b> is a dialog box for editing a LMS network.
 */
public class LMSEditorDialog extends SupervisedTrainingDialog {

    /** Reference to the lms network being edited. */
    private LMSNetwork lms;

    /**
     * Default constructor.
     *
     * @param np parent panel
     * @param lms edited network
     */
    public LMSEditorDialog(final NetworkPanel np, final LMSNetwork lms) {
        super(np, lms);
        this.lms = lms;
        init();
        initDefaultTabs();
    }

    /**
     * This method initializes the components on the panel.
     */
    private void init() {
        setTitle("Edit LMS Network");

        // Set up combo box
        String[] lmsTypes = { "Iterative", "Offline" };
        final JComboBox<String> selectType = new JComboBox<String>(lmsTypes);
        selectType.setPreferredSize(new Dimension(100, 100));
        Box cbHolder = Box.createHorizontalBox();
        cbHolder.add(Box.createHorizontalGlue());
        cbHolder.add(new JLabel("Select training type: "));
        cbHolder.add(selectType);
        cbHolder.add(Box.createHorizontalGlue());


        // Main vertical box
        Box trainerPanel = Box.createVerticalBox();
        trainerPanel.setOpaque(true);
        trainerPanel.add(cbHolder);
        final JPanel trainerContainer = new JPanel();
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        trainerPanel.add(separator);
        trainerPanel.add(trainerContainer);

        // Add to tabbed pane
        tabbedPane.addTab("Train", trainerPanel);
        updateComboBox(selectType, trainerContainer);

        selectType.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateComboBox(selectType, trainerContainer);
            }

        });

    }

    /**
     * Update trainer panel based on the combo box.
     *
     * @param selectType the combo box
     * @param trainerContainer the container
     */
    private void updateComboBox(JComboBox<String> selectType,
            JPanel trainerContainer) {
        trainerContainer.removeAll();
        if (selectType.getSelectedIndex() == 0) {
            LMSIterative trainer = new LMSIterative(lms);
            IterativeControlsPanel iterativeControls = new IterativeControlsPanel(
                    networkPanel, trainer);
            trainerContainer.add(iterativeControls);
        } else {
            LMSOffline trainer = new LMSOffline(lms);
            LMSOfflineControlPanel offlineControls = new LMSOfflineControlPanel(
                    networkPanel, trainer);
            trainerContainer.removeAll();
            trainerContainer.add(offlineControls);
        }
        repaint();
        pack();
        setLocationRelativeTo(null);
    }
}
