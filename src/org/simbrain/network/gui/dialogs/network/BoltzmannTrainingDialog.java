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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.TestInputPanel;
import org.simbrain.network.gui.trainer.DataPanel;
import org.simbrain.network.gui.trainer.subnetworkTrainingPanels.BoltzmannTrainerControlsPanel;
import org.simbrain.network.gui.trainer.subnetworkTrainingPanels.BoltzmannTrainerControlsPanel;
import org.simbrain.network.subnetworks.BoltzmannMachine;
import org.simbrain.network.trainers.BoltzmannTrainer;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.table.NumericTable;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 * Dialog for training a SOM network.
 *
 * @author Jeff Yoshimi
 *
 */
public class BoltzmannTrainingDialog extends StandardDialog {

    /** Main tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Panel for setting properties of the SOM network. */
   // private BoltzmanPropertiesPanel somPropsPanel;

    /** Reference to network panel. */
    private NetworkPanel panel;

    /** Reference to the Boltzman Network. */
    private BoltzmannMachine network;

    /** Reference to input data panel. */
    private DataPanel inputPanel;

    /** Reference to validate inputs panel */
    private TestInputPanel validateInputsPanel;

    /**
     * Construct the dialog.
     *
     * @param np parent network panel
     * @param network the Boltzmann network
     */
    public BoltzmannTrainingDialog(NetworkPanel np, BoltzmannMachine network) {

        this.panel = np;
        this.network = network;

        // Set to modeless so the dialog can be left open
        setModalityType(ModalityType.MODELESS);

        // Set up properties tab
//        somPropsPanel = new SOMPropertiesPanel(np, network.getSom());
//        tabbedPane.addTab("Network Properties", somPropsPanel);

        // Set up training tab
        final BoltzmannTrainerControlsPanel controlPanel = new BoltzmannTrainerControlsPanel(panel,
                new BoltzmannTrainer(network), network);
        tabbedPane.addTab("Train Network", controlPanel);

        // Input data tab
        inputPanel = new DataPanel(network.getInputNeurons(),
                network.getTrainingSet().getInputDataMatrix(), 5, "Input");
        inputPanel.setFrame(this);
        tabbedPane.addTab("Training data", inputPanel);

        // Testing tab
        validateInputsPanel =  TestInputPanel.createTestInputPanel(np,
                network.getInputNeurons(), network.getTrainingSet()
                        .getInputDataMatrix());
        tabbedPane.addTab("Validate", validateInputsPanel);

        // Listen for tab changed events. Load inputs to test tab
        // If inputs have been loaded
        ChangeListener changeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent
                        .getSource();
                int index = sourceTabbedPane.getSelectedIndex();
                // Just clicked out of Properties tab
                if (index == 1) {
                    // When entering training tab, commit table changes
                    inputPanel.commitChanges();
//                    somPropsPanel.commitChanges();
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
                "Pages/Network/network/som.html");
        addButton(new JButton(helpAction));

        // Finish configuration
        setContentPane(tabbedPane);

        // Stop trainer from running any time the window is closed
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                controlPanel.getTrainer().setUpdateCompleted(true);
            }
        });
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
//        somPropsPanel.commitChanges();
        inputPanel.commitChanges();
    }

}
