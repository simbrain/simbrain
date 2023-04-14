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
package org.simbrain.network.gui.dialogs.group;

import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.TestInputPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.math.NumericMatrix;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.ShowHelpAction;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for editing neuron groups.
 *
 * @author Jeff Yoshimi
 * @author Yulin Li
 */
public final class NeuronGroupDialog extends StandardDialog {

    /**
     * Parent network panel.
     */
    private NetworkPanel networkPanel;

    /**
     * Neuron Group being created or edited.
     */
    private NeuronGroup neuronGroup;

    /**
     * If true this is a creation dialog. Otherwise it is an edit dialog.
     */
    private boolean isCreationDialog = false;

    /**
     * Neuron group summary.
     */
    private AnnotatedPropertyEditor mainPanel;

    /**
     * Special object to create new neuron groups.
     */
    private NeuronGroup.NeuronGroupCreator ngCreator;

    /**
     * For creating a new neuron group.
     *
     * @param np  parent panel
     */
    public NeuronGroupDialog(final NetworkPanel np) {
        networkPanel = np;
        isCreationDialog = true;
        init();
    }

    /**
     * For editing an existing neuron group.
     *
     * @param np Parent network panel
     * @param ng Neuron group being edited
     */
    public NeuronGroupDialog(final NetworkPanel np, final NeuronGroup ng) {
        networkPanel = np;
        neuronGroup = ng;
        isCreationDialog = false;
        init();
    }

    /**
     * Initialize the panel.
     */
    private void init() {

        if (isCreationDialog) {
            setTitle("Create Neuron Group");
        } else {
            setTitle("Edit " + neuronGroup.getLabel());
        }

        setMinimumSize(new Dimension(300, 200));

        if (!isCreationDialog) {
            mainPanel = new AnnotatedPropertyEditor(neuronGroup);
        } else {
            ngCreator =
                new NeuronGroup.NeuronGroupCreator(
                        networkPanel.getNetwork().getIdManager().getProposedId(NeuronGroup.class));
            mainPanel = new AnnotatedPropertyEditor(ngCreator);
        }

        setContentPane(mainPanel);

        if(!isCreationDialog) {
            // Input panel
            NumericMatrix matrix = new NumericMatrix() {

                @Override
                public void setData(double[][] data) {
                    neuronGroup.getInputManager().setData(data);
                }

                @Override
                public double[][] getData() {
                    return neuronGroup.getInputManager().getData();
                }
            };
            JPanel inputDataPanel = TestInputPanel.createTestInputPanel(networkPanel, neuronGroup.getNeuronList(), matrix);
            mainPanel.getTabbedPane().addTab("Input Data", inputDataPanel);
        }

        // Set up help button
        Action helpAction = new ShowHelpAction("Pages/Network/groups/NeuronGroup.html");
        addButton(new JButton(helpAction));

        if (!isCreationDialog) {
            // If editing, make this dialog based on a done button, rather than
            // ok and cancel. All edits are done with apply
            setAsDoneDialog();
        }
    }

    /**
     * Commit changes.
     */
    public void commitChanges() {

        mainPanel.commitChanges();

        if (isCreationDialog) {
            neuronGroup = ngCreator.create(networkPanel.getNetwork());
            networkPanel.getNetwork().addNetworkModelAsync(neuronGroup);
        } else {
            neuronGroup.applyLayout();
        }

        networkPanel.repaint();
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

}