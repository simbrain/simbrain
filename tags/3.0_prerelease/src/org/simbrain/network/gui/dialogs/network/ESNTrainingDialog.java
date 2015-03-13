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

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.TestInputPanel;
import org.simbrain.network.gui.trainer.DataPanel;
import org.simbrain.network.gui.trainer.subnetworkTrainingPanels.ESNOfflineTrainingPanel;
import org.simbrain.network.gui.trainer.subnetworkTrainingPanels.SOMTrainerControlsPanel;
import org.simbrain.network.subnetworks.EchoStateNetwork;
import org.simbrain.network.subnetworks.SOMNetwork;
import org.simbrain.network.trainers.SOMTrainer;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.table.NumericTable;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 * Dialog for training an ESN network.
 *
 */
public class ESNTrainingDialog extends StandardDialog {

    /** Main tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Reference to network panel. */
    private NetworkPanel panel;

    /** Reference to input data panel. */
    private DataPanel inputPanel;

    /** Reference to training data panel. */
    private DataPanel trainingPanel;

    /** Reference to validate inputs panel */
    private TestInputPanel validateInputsPanel;

    /**
     * Construct the dialog.
     *
     * @param np parent network panel
     * @param network the ESN network
     */
    public ESNTrainingDialog(NetworkPanel np, EchoStateNetwork network) {

        this.panel = np;

        // Set to modeless so the dialog can be left open
        setModalityType(ModalityType.MODELESS);

        // Set up training tab
        ESNOfflineTrainingPanel controlPanel = new ESNOfflineTrainingPanel(
                panel, network, this);
        tabbedPane.addTab("Train Network", controlPanel);

        // Input data tab
        inputPanel = new DataPanel(network.getInputLayer().getNeuronList(),
                network.getInputDataMatrix(), 5, "Input");
        inputPanel.setFrame(this);
        tabbedPane.addTab("Input data", inputPanel);

        // Training data tab
        trainingPanel = new DataPanel(network.getOutputLayer().getNeuronList(),
                network.getTargetDataMatrix(), 5, "Targets");
        trainingPanel.setFrame(this);
        tabbedPane.addTab("Target data", trainingPanel);

        // Testing tab
        validateInputsPanel =  TestInputPanel.createTestInputPanel(np,
                network.getInputLayer().getNeuronList(), network.getInputDataMatrix());
        tabbedPane.addTab("Validate Input Data", validateInputsPanel);

        // Listen for tab changed events. Load inputs to test tab
        // If inputs have been loaded
        ChangeListener changeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent
                        .getSource();
                int index = sourceTabbedPane.getSelectedIndex();
                // Just clicked out of training tab
                if (index == 1) {
                    // When entering training tab, commit table changes
                    inputPanel.commitChanges();
                    //somPropsPanel.commitChanges();
                } else if (index == 3) {
                    if (inputPanel.getTable().getData() != null) {
                        validateInputsPanel.setData(((NumericTable) inputPanel
                                .getTable().getData()).asDoubleArray());
                    }
                }
            }
        };
        tabbedPane.addChangeListener(changeListener);

        // Set up help
        Action helpAction = new ShowHelpAction(
                "Pages/Network/network/echostatenetwork.html");
        addButton(new JButton(helpAction));

        // Finish configuration
        setContentPane(tabbedPane);

    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        inputPanel.commitChanges();
        trainingPanel.commitChanges();
    }

}
