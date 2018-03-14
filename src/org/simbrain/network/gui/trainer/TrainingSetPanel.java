/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui.trainer;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.trainers.Trainable;
import org.simbrain.util.math.NumericMatrix;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Display input data and target data.
 *
 * @author jyoshimi
 */
public class TrainingSetPanel extends JPanel {

    /**
     * Representation of input data.
     */
    private DataPanel inputPanel;

    /**
     * Representation of target data.
     */
    private DataPanel targetPanel;

    /**
     * Parent frame.
     */
    private Window parentFrame;

    /**
     * Construct a new pane for displaying training sets.
     *
     * @param trainable                 the parent trainable object.
     * @param numVisibleColumnsPerTable number of columns to make visible in the
     *                                  input and target data tables.
     */
    public TrainingSetPanel(final Trainable trainable, final int numVisibleColumnsPerTable) {

        inputPanel = new DataPanel(trainable.getInputNeurons(), trainable.getTrainingSet().getInputDataMatrix(), numVisibleColumnsPerTable, "Input data");
        targetPanel = new DataPanel(trainable.getOutputNeurons(), trainable.getTrainingSet().getTargetDataMatrix(), numVisibleColumnsPerTable, "Target data");
        init();

    }

    /**
     * For data that is not embedded in traineable, so that all the data must be
     * specified.
     *
     * @param inputNeurons              the input neurons
     * @param inputData                 the input data
     * @param targetNeurons             the output neurons
     * @param targetData                target data
     * @param numVisibleColumnsPerTable number of columns to make visible in the
     *                                  input and target data tables.
     */
    public TrainingSetPanel(List<Neuron> inputNeurons, NumericMatrix inputData, List<Neuron> targetNeurons, NumericMatrix targetData, int numVisibleColumnsPerTable) {
        inputPanel = new DataPanel(inputNeurons, inputData, numVisibleColumnsPerTable, "Input data");
        targetPanel = new DataPanel(targetNeurons, targetData, numVisibleColumnsPerTable, "Target data");

        init();
    }

    /**
     * Initialize the panel.
     */
    private void init() {
        final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        inputPanel.setBorder(BorderFactory.createTitledBorder("Input data"));
        targetPanel.setBorder(BorderFactory.createTitledBorder("Target data"));
        // split.setDividerLocation(.5);
        split.setLeftComponent(inputPanel);
        split.setRightComponent(targetPanel);
        split.setResizeWeight(.5);
        split.setBorder(null);

        setLayout(new GridBagLayout());
        GridBagConstraints wholePanelConstraints = new GridBagConstraints();
        // wholePanelConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        wholePanelConstraints.fill = GridBagConstraints.BOTH;
        wholePanelConstraints.weightx = 0.5;
        wholePanelConstraints.weighty = 0.5;
        wholePanelConstraints.gridx = 0;
        wholePanelConstraints.gridy = 0;
        add(split, wholePanelConstraints);
    }

    /**
     * Create a panel for use in testing.
     */
    public TrainingSetPanel() {

        final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        // split.setDividerLocation(.5);
        JPanel filler1 = new JPanel();
        filler1.setBackground(Color.lightGray);
        JPanel filler2 = new JPanel();
        filler2.setBackground(Color.lightGray);
        split.setLeftComponent(filler1);
        split.setRightComponent(filler2);
        split.setResizeWeight(.5);
        split.setBorder(null);

        setLayout(new GridBagLayout());
        GridBagConstraints wholePanelConstraints = new GridBagConstraints();
        // wholePanelConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        wholePanelConstraints.fill = GridBagConstraints.BOTH;
        wholePanelConstraints.weightx = 0.5;
        wholePanelConstraints.weighty = 0.5;
        wholePanelConstraints.gridx = 0;
        wholePanelConstraints.gridy = 0;
        add(split, wholePanelConstraints);
    }

    /**
     * Hacked code for resizing sub-panels as this panel is resized.
     */
    private void resizePanels() {
        // inputPanel.setPreferredSize(new Dimension(getWidth() / 2 - 20,
        // inputPanel.getHeight()));
        // inputPanel.revalidate();
        // targetPanel.setPreferredSize(new Dimension(getWidth() / 2 - 20,
        // targetPanel.getHeight()));
        // targetPanel.revalidate();

    }

    /**
     * Set the frame. Used for dynamically resizing the internal frame as data
     * changes. See DataPanel.java.
     *
     * @param frame frame to set
     */
    public void setFrame(Window frame) {
        parentFrame = frame;
        //        parentFrame.setMaximizable(false);
        inputPanel.setFrame(frame);
        targetPanel.setFrame(frame);
    }

    public boolean commitChanges() {
        return inputPanel.commitChanges() && targetPanel.commitChanges();
    }


}
