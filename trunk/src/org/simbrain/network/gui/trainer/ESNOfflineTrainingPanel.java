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
package org.simbrain.network.gui.trainer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.subnetworks.EchoStateNetwork;
import org.simbrain.network.trainers.LMSOffline;
import org.simbrain.network.trainers.Trainer.DataNotInitializedException;
import org.simbrain.util.NumericMatrix;
import org.simbrain.util.genericframe.GenericFrame;

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

    /** Reference to training set panel. */
    private final TrainingSetPanel trainingSetPanel;

    /** Reservoir utils panel. */
    private final ReservoirUtilsPanel rUtilsPanel;

    /** The parent frame. */
    private GenericFrame frame;

    /**
     * Construct an ESN Training Panel.
     *
     * @param panel the parent network panel
     * @param esn the underlying network
     */
    public ESNOfflineTrainingPanel(final NetworkPanel panel,
            final EchoStateNetwork esn) {

        // Initialize control panel with no trainer. It has to be set
        // After the apply button is pressed
        controlPanel = new LMSOfflineControlPanel(panel);

        // Set up main controls
        GridBagConstraints controlPanelConstraints = new GridBagConstraints();
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controls"));

        // Reference to the input data in the esn
        final NumericMatrix inputData = new NumericMatrix() {
            @Override
            public void setData(double[][] data) {
                esn.setInputData(data);
            }

            @Override
            public double[][] getData() {
                return esn.getInputData();
            }

        };
        // Reference to the target data in the esn
        final NumericMatrix targetData = new NumericMatrix() {
            @Override
            public void setData(double[][] data) {
                esn.setTargetData(data); //
            }

            @Override
            public double[][] getData() {
                return esn.getTargetData();
            }

        };

        // Training Set Panel
        trainingSetPanel = new TrainingSetPanel(esn.getInputLayer()
                .getNeuronList(), inputData, esn.getOutputLayer()
                .getNeuronList(), targetData, 3);

        // Res Utils
        rUtilsPanel = new ReservoirUtilsPanel(esn);
        controlPanelConstraints.weightx = 1;
        controlPanelConstraints.weighty = 1;
        controlPanelConstraints.gridx = 0;
        controlPanelConstraints.gridy = 4;
        controlPanel.add(rUtilsPanel, controlPanelConstraints);

        // Set overall layout
        setLayout(new GridBagLayout());
        GridBagConstraints wholePanelConstraints = new GridBagConstraints();
        // Control Panel
        wholePanelConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        wholePanelConstraints.fill = GridBagConstraints.HORIZONTAL;
        wholePanelConstraints.insets = new Insets(10, 10, 10, 10);
        wholePanelConstraints.weightx = 0;
        wholePanelConstraints.weighty = 0.5;
        wholePanelConstraints.gridx = 0;
        wholePanelConstraints.gridy = 0;
        add(controlPanel, wholePanelConstraints);
        // Training Set
        wholePanelConstraints.anchor = GridBagConstraints.PAGE_START;
        wholePanelConstraints.fill = GridBagConstraints.BOTH;
        wholePanelConstraints.insets = new Insets(10, 10, 10, 10);
        wholePanelConstraints.weightx = 1;
        wholePanelConstraints.weighty = 0.5;
        wholePanelConstraints.gridx = 1;
        wholePanelConstraints.gridy = 0;
        add(trainingSetPanel, wholePanelConstraints);

        controlPanel.getApplyButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (inputData.getData() == null) {
                    JOptionPane.showOptionDialog(null, "Input data not set",
                            "Warning", JOptionPane.DEFAULT_OPTION,
                            JOptionPane.WARNING_MESSAGE, null, null, null);
                } else if (targetData.getData() == null) {
                    JOptionPane.showOptionDialog(null, "Target data not set",
                            "Warning", JOptionPane.DEFAULT_OPTION,
                            JOptionPane.WARNING_MESSAGE, null, null, null);
                } else {
                    if (controlPanel.getTrainer() == null) {
                        controlPanel.setTrainer((LMSOffline) esn.getTrainer());
                        controlPanel.addTrainerListeners();
                    }
                    controlPanel.runTrainer();
                }
            }
        });

    }

    /**
     * Set parent frame.
     *
     * @param frame the frame containing this panel.
     */
    public void setFrame(final GenericFrame frame) {
        this.frame = frame;
        trainingSetPanel.setFrame(frame);
        rUtilsPanel.setFrame(frame);
    }

}
