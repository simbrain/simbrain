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

import java.awt.event.ActionEvent;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.TestInputPanel;
import org.simbrain.network.gui.trainer.DataPanel;
import org.simbrain.network.gui.trainer.subnetworkTrainingPanels.SOMTrainerControlsPanel;
import org.simbrain.network.subnetworks.SOMNetwork;
import org.simbrain.network.trainers.SOMTrainer;
import org.simbrain.network.trainers.Trainer.DataNotInitializedException;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.table.NumericTable;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 * Dialog for training a SOM network.
 *
 * @author Jeff Yoshimi
 *
 */
public class SOMTrainingDialog extends StandardDialog {

    /** Main tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Panel for setting properties of the SOM network. */
    private SOMPropertiesPanel somPropsPanel;

    /** Reference to network panel. */
    private NetworkPanel panel;

    /** Reference to the SOM Network. */
    private SOMNetwork network;

    /**
     * Construct the dialog.
     *
     * @param np parent network panel
     * @param network the SOM network
     */
    public SOMTrainingDialog(NetworkPanel np, SOMNetwork network) {

        this.panel = np;
        this.network = network;

        // Set to modeless so the dialog can be left open
        setModalityType(ModalityType.MODELESS);

        // Set up properties tab
        somPropsPanel = new SOMPropertiesPanel(np, network.getSom());
        tabbedPane.addTab("Network Properties", somPropsPanel);

        // Set up training tab
        SOMTrainerControlsPanel controlPanel = new SOMTrainerControlsPanel(panel,
                new SOMTrainer(network), network);
        tabbedPane.addTab("Train Network", controlPanel);

        // Input data tab
        final DataPanel inputPanel = new DataPanel(network.getInputNeurons(),
                network.getTrainingSet().getInputDataMatrix(), 5, "Input");
        inputPanel.setFrame(this);
        tabbedPane.addTab("Training data", inputPanel);

        // Testing tab
        final TestInputPanel testInputPanel = new TestInputPanel(np,
                network.getInputNeurons(), network.getTrainingSet()
                        .getInputData());
        tabbedPane.addTab("Test data", testInputPanel);

        // Listen for tab changed events. Load inputs to test tab
        // If inputs have been loaded
        ChangeListener changeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent
                        .getSource();
                int index = sourceTabbedPane.getSelectedIndex();
                // Just clicked out of Properties tab
                if (index == 2) {
                    somPropsPanel.commitChanges();
                }
                // Just clicked out of input tab
                if (index == 3) {
                    if (inputPanel.getTable().getData() != null) {
                        testInputPanel.setData(((NumericTable) inputPanel
                                .getTable().getData()).asDoubleArray());
                    }
                }
            }
        };
        tabbedPane.addChangeListener(changeListener);

        // Set up help
        Action helpAction = new ShowHelpAction(
                "Pages/Network/network/SOMnetwork.html");
        addButton(new JButton(helpAction));

        // Finish configuration
        setContentPane(tabbedPane);

    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        somPropsPanel.commitChanges();
    }

  
}
