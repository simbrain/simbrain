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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.subnetworks.SimpleRecurrentNetwork;
import org.simbrain.network.trainers.IterableTrainer;
import org.simbrain.util.NumericMatrix;
import org.simbrain.util.genericframe.GenericFrame;

/**
 * Training panel for a simple recurrent network. Similar to the ESN Training
 * panel(s) in that certain events cause the logical training set (see
 * SimpleRecurrentNetwork.java) to be recreated, via call to
 * createLogicalInputData.getTrainer().
 */
public class SRNTrainingPanel extends JPanel {

    /** Reference to the controls panel. */
    private final SRNIterativeControls controlPanel;

    /** Reference to training set panel. */
    private final TrainingSetPanel trainingSetPanel;

    /** Reference to the network. */
    private final SimpleRecurrentNetwork srn;

    /** The parent frame. */
    private GenericFrame frame;

    /**
     * Construct an SRN Training Panel.
     *
     * @param panel the parent network panel
     * @param srn the underlying network
     */
    public SRNTrainingPanel(final NetworkPanel panel,
            final SimpleRecurrentNetwork srn) {
        this.srn = srn;

        controlPanel = new SRNIterativeControls(panel);

        // Set up main controls
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controls"));

        // Reference to the input data in the srn
        final NumericMatrix inputData = new NumericMatrix() {
            @Override
            public void setData(double[][] data) {
                srn.getTrainingSet().setInputData(data);
            }

            @Override
            public double[][] getData() {
                return srn.getTrainingSet().getInputData();
            }

        };
        // Reference to the target data in the esn
        final NumericMatrix targetData = new NumericMatrix() {
            @Override
            public void setData(double[][] data) {
                srn.getTrainingSet().setTargetData(data); //
            }

            @Override
            public double[][] getData() {
                return srn.getTrainingSet().getTargetData();
            }

        };

        // Training Set Panel
        trainingSetPanel = new TrainingSetPanel(srn.getInputNeurons(),
                inputData, srn.getOutputNeurons(), targetData, 3);

        // Set overall layout
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        // Control Panel
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 0;
        gbc.weighty = 0.5;
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(controlPanel, gbc);
        // Training Set
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1;
        gbc.weighty = 0.5;
        gbc.gridx = 1;
        gbc.gridy = 0;
        add(trainingSetPanel, gbc);
    }

    /**
     * Subclass of iterative controls panel that overrides initTrainer,
     * recreating the logical input data and resetting listeners. This happens
     * (or should happen) when input or target data are changed, and when
     * weights are changed (since this changes what the context nodes will do).
     */
    private class SRNIterativeControls extends IterativeControlsPanel {

        /**
         * Construct the control panel.
         *
         * @param networkPanel reference to network panel
         */
        public SRNIterativeControls(NetworkPanel networkPanel) {
            super(networkPanel);
        }

        @Override
        protected void initTrainer(boolean forceReinit) {
            //if (getTrainer() != null) {
            //    getTrainer().setUpdateCompleted(true);
            //}
            if (srn.getTrainingSet().getInputData() == null) {
                JOptionPane.showOptionDialog(null, "Input data not set",
                        "Warning", JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE, null, null, null);
            } else if (srn.getTrainingSet().getTargetData() == null) {
                JOptionPane.showOptionDialog(null, "Target data not set",
                        "Warning", JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE, null, null, null);
            } else {
                if ((getTrainer() == null) || (forceReinit)) {
                    IterableTrainer trainer = srn.getTrainer();
                    setTrainer(trainer);
                    addErrorListener();
                }
            }
        }

    }

    /**
     * Set parent frame.
     *
     * @param frame the frame containing this panel.
     */
    public void setFrame(final GenericFrame frame) {
        this.frame = frame;
        trainingSetPanel.setFrame(frame);
    }

}
