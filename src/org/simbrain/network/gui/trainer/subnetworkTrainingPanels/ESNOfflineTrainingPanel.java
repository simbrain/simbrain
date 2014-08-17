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
package org.simbrain.network.gui.trainer.subnetworkTrainingPanels;

import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.subnetworks.EchoStateNetwork;
import org.simbrain.network.trainers.LMSOffline;

/**
 * Panel for training ESN's.
 *
 * ESN classes like this have to do some tricky things because of how ESN's
 * work. The "visible training set" for the ESN is inputs and targets. But the
 * "real training set" that the learning rule is applied to is actually
 * harvested data from the reservoir network as inputs and the visible target
 * data as targets.
 *
 * This is similar to LMSOfflineTrainingPanel, but customized in light of the
 * above.
 */
public class ESNOfflineTrainingPanel extends JPanel {

    /** Reference to the controls panel. */
    private final LMSOfflineControlPanel controlPanel;

    /** Reservoir utils panel. */
    private final ReservoirUtilsPanel rUtilsPanel;

    /**
     * Construct an ESN Training Panel.
     *
     * @param panel the parent network panel
     * @param esn the underlying network
     */
    public ESNOfflineTrainingPanel(final NetworkPanel panel,
        final EchoStateNetwork esn, final Window frame) {
        // Initialize control panel with no trainer. It has to be set
        // After the apply button is pressed
        controlPanel = new LMSOfflineControlPanel(frame);

        // Set up main controls
        GridBagConstraints controlPanelConstraints = new GridBagConstraints();
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controls"));

        // Res Utils
        rUtilsPanel = new ReservoirUtilsPanel(esn, frame);
        controlPanelConstraints.weightx = 1;
        controlPanelConstraints.weighty = 1;
        controlPanelConstraints.gridx = 0;
        controlPanelConstraints.gridy = 4;
        controlPanel.add(rUtilsPanel, controlPanelConstraints);

        // Add the panel
        add(controlPanel);

        controlPanel.getApplyButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (controlPanel.getTrainer() == null) {
                    controlPanel.setTrainer((LMSOffline) esn.getTrainer());
                    controlPanel.addTrainerListeners();
                }
                controlPanel.runTrainer();
            }

        });

    }

}
