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

import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.TestInputPanel;
import org.simbrain.network.gui.trainer.DataPanel;
import org.simbrain.network.gui.trainer.IterativeControlsPanel;
import org.simbrain.network.subnetworks.BackpropNetwork;
import org.simbrain.network.trainers.BackpropTrainer;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.table.NumericTable;

/**
 * <b>BackpropDialog</b> is a dialog box for editing a Backprop network.
 */
public class BackpropEditorDialog extends StandardDialog {

    /** Network panel. */
    private NetworkPanel networkPanel;

    /** Main tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Reference to the backprop network being edited. */
    private BackpropNetwork backprop;

    /**
     * Default constructor.
     *
     * @param np parent panel
     * @param backprop edited network
     */
    public BackpropEditorDialog(final NetworkPanel np,
            final BackpropNetwork backprop) {
        networkPanel = np;
        this.backprop = backprop;
        init();
    }

    /**
     * This method initializes the components on the panel.
     */
    private void init() {
        setTitle("Edit Backprop Network");

        // Trainer tab
        BackpropTrainer trainer = new BackpropTrainer(backprop,
                backprop.getNeuronGroupsAsList());
        IterativeControlsPanel iterativeControls = new IterativeControlsPanel(
                networkPanel, trainer);
        tabbedPane.addTab("Train", iterativeControls);

        // Input data tab
        final DataPanel inputPanel = new DataPanel(backprop.getInputNeurons(),
                backprop.getTrainingSet().getInputDataMatrix(), 5, "Input");
        inputPanel.setFrame(this);
        tabbedPane.addTab("Input data", inputPanel);

        // Training data tab
        DataPanel trainingPanel = new DataPanel(backprop.getOutputNeurons(),
                backprop.getTrainingSet().getTargetDataMatrix(), 5, "Targets");
        trainingPanel.setFrame(this);
        tabbedPane.addTab("Target data", trainingPanel);

        // Testing tab
        final TestInputPanel testInputPanel = new TestInputPanel(networkPanel,
                backprop.getInputNeurons(), backprop.getTrainingSet()
                        .getInputData());
        tabbedPane.addTab("Test data", testInputPanel);

        // Finalize
        setContentPane(tabbedPane);

        // Listen for tab changed events. Load inputs to test tab
        // If inputs have been loaded
        ChangeListener changeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent
                        .getSource();
                int index = sourceTabbedPane.getSelectedIndex();
                if (index == 3) {
                    if (inputPanel.getTable().getData() != null) {
                        testInputPanel.setData(((NumericTable) inputPanel
                                .getTable().getData()).asDoubleArray());
                    }
                }
            }
        };
        tabbedPane.addChangeListener(changeListener);

    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
        // no implementation yet
        super.closeDialogOk();
    }
}
