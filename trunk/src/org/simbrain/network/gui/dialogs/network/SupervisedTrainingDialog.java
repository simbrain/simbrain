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
import org.simbrain.network.trainers.Trainable;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.table.NumericTable;

/**
 * <b>SupervisedTrainingDialog</b> is the superclass of edit dialogs associated
 * with most supervised learning networks.
 */
public class SupervisedTrainingDialog extends StandardDialog {

    /** Network panel. */
    protected NetworkPanel networkPanel;

    /** Main tabbed pane. */
    protected JTabbedPane tabbedPane = new JTabbedPane();

    /** Reference to the trainable network being edited. */
    private Trainable trainable;

    /** Reference to input data panel. */
    private DataPanel inputPanel;

    /** Reference to training data panel. */
    private DataPanel trainingPanel;

    /** Reference to validate inputs panel */
    private TestInputPanel validateInputsPanel;

    /**
     * Default constructor.
     *
     * @param np parent panel
     * @param trainable edited network
     */
    public SupervisedTrainingDialog(final NetworkPanel np,
            final Trainable trainable) {
        networkPanel = np;
        this.trainable = trainable;
    }

    /**
     * This method initializes the components on the panel.
     */
    protected void initDefaultTabs() {

        // Set to modeless so the dialog can be left open
        setModalityType(ModalityType.MODELESS);

        // Input data tab
        inputPanel = new DataPanel(trainable.getInputNeurons(),
                trainable.getTrainingSet().getInputDataMatrix(), 5, "Input");
        inputPanel.setFrame(this);
        tabbedPane.addTab("Input data", inputPanel);

        // Training data tab
        trainingPanel = new DataPanel(trainable.getOutputNeurons(),
                trainable.getTrainingSet().getTargetDataMatrix(), 5, "Targets");
        trainingPanel.setFrame(this);
        tabbedPane.addTab("Target data", trainingPanel);

        // Testing tab
        validateInputsPanel = new TestInputPanel(networkPanel,
                trainable.getInputNeurons(), trainable.getTrainingSet()
                        .getInputDataMatrix());
        tabbedPane.addTab("Validate Input Data", validateInputsPanel);

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
                        validateInputsPanel.setData(((NumericTable) inputPanel
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
        super.closeDialogOk();
        inputPanel.commitChanges();
        trainingPanel.commitChanges();
        validateInputsPanel.commitChanges();
    }
}
