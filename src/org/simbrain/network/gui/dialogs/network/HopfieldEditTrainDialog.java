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
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.TestInputPanel;
import org.simbrain.network.gui.trainer.DataPanel;
import org.simbrain.network.gui.trainer.SimpleTrainerControlPanel;
import org.simbrain.network.subnetworks.Hopfield;
import org.simbrain.network.trainers.HopfieldTrainer;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.table.NumericTable;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 * Dialog for setting the properties of Hopfield networks and training them.
 *
 * @author Jeff Yoshimi
 *
 */
public class HopfieldEditTrainDialog extends StandardDialog {

    /** The main panel. */
    final HopfieldPropertiesPanel hopfieldPropsPanel;

    /** Main tabbed pane. */
    protected JTabbedPane tabbedPane = new JTabbedPane();

    /** Reference to input data panel. */
    private DataPanel inputPanel;

    /** Reference to validate inputs panel */
    private TestInputPanel validateInputsPanel;

    /**
     * Construct the dialog.
     *
     * @param np parent network panel
     * @param hop the hopfield network
     */
    public HopfieldEditTrainDialog(NetworkPanel np, Hopfield hop) {

        // Set to modeless so the dialog can be left open
        setModalityType(ModalityType.MODELESS);

        // Set title
        setTitle("Edit / Train Hopfield Network");

        // Set up properties tab
        Box propsBox = Box.createVerticalBox();
        propsBox.setOpaque(true);
        propsBox.add(Box.createVerticalGlue());
        hopfieldPropsPanel = new HopfieldPropertiesPanel(np, hop);
        propsBox.add(hopfieldPropsPanel);
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        propsBox.add(separator);
        SimpleTrainerControlPanel controlPanel = new SimpleTrainerControlPanel(
                np, new HopfieldTrainer(hop));
        propsBox.add(controlPanel);
        tabbedPane.addTab("Properties", propsBox);


        // Input data tab
        inputPanel = new DataPanel(hop.getInputNeurons(),
                hop.getTrainingSet().getInputDataMatrix(), 5, "Input");
        inputPanel.setFrame(this);
        tabbedPane.addTab("Training data", inputPanel);

        // Testing tab
        validateInputsPanel = new TestInputPanel(np,
                hop.getInputNeurons(), hop.getTrainingSet()
                        .getInputDataMatrix());
        tabbedPane.addTab("Validate", validateInputsPanel);


        // Listen for tab changed events. Load inputs to test tab
        // If inputs have been loaded
        ChangeListener changeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent
                        .getSource();
                int index = sourceTabbedPane.getSelectedIndex();
                if (index == 2) {
                    if (inputPanel.getTable().getData() != null) {
                        validateInputsPanel.setData(((NumericTable) inputPanel
                                .getTable().getData()).asDoubleArray());
                    }
                }
            }
        };
        tabbedPane.addChangeListener(changeListener);

        // Set up help
        Action helpAction = new ShowHelpAction(hopfieldPropsPanel.getHelpPath());
        addButton(new JButton(helpAction));

        //  Finish configuration
        setContentPane(tabbedPane);

    }

    /**
     * Commit all changes made in the dialog to the model.
     */
    private void commitChanges() {
        hopfieldPropsPanel.commitChanges();
        inputPanel.commitChanges();
        validateInputsPanel.commitChanges();
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }
}
