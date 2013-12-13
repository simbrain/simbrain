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
package org.simbrain.network.gui.dialogs.neuron;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.util.ShowHelpAction;
import org.simbrain.util.StandardDialog;

/**
 * <b>NeuronDialog</b> is a dialog box for setting the properties of a Neuron.
 *
 */
public class NeuronDialog extends StandardDialog {

    /** The default serial version id. */
    private static final long serialVersionUID = 1L;

    /** Null string. */
    public static final String NULL_STRING = "...";

    /** Main panel. */
    private final Box mainPanel = Box.createVerticalBox();

    /**
     * Top panel. Contains fields for displaying/editing basic neuron
     * information.
     *
     * @see org.simbrain.network.gui.dialogs.neuron.BasicNeuronInfoPanel.java
     */
    private final BasicNeuronInfoPanel topPanel;

    /**
     * Bottom panel. Contains fields for displaying/editing neuron update rule
     * parameters.
     *
     * @see org.simbrain.network.gui.dialogs.neuron.NeuronUpdateSettingsPanel.java
     */
    private final NeuronUpdateSettingsPanel bottomPanel;

    /**
     * Help Button. Links to information about the currently selected neuron
     * update rule.
     */
    private final JButton helpButton = new JButton("Help");

    /** Show Help Action. The action executed by the help button */
    private ShowHelpAction helpAction;

    /** The neurons being modified. */
    private final ArrayList<Neuron> neuronList;

    /**
     * @param selectedNeurons the pnode_neurons being adjusted
     */
    public NeuronDialog(final Collection<NeuronNode> selectedNeurons) {
        neuronList = getNeuronList(selectedNeurons);
        topPanel = new BasicNeuronInfoPanel(neuronList, this);
        bottomPanel = new NeuronUpdateSettingsPanel(neuronList, this, false);
        bottomPanel.getNeuronPanel().setReplace(false);
        init();
        addListeners();
        updateHelp();
    }

    /**
     * Get the logical neurons from the NeuronNodes.
     */
    private static ArrayList<Neuron> getNeuronList(
            final Collection<NeuronNode> selectedNeurons) {
        ArrayList<Neuron> nl = new ArrayList<Neuron>();
        for (NeuronNode n : selectedNeurons) {
            nl.add(n.getNeuron());
        }
        return nl;
    }

    /**
     * Initializes the components on the panel.
     */
    private void init() {
        setTitle("Neuron Dialog");
        mainPanel.add(topPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(bottomPanel);
        setContentPane(mainPanel);
        this.addButton(helpButton);
    }

    /**
     * Add listeners to the components of the dialog
     */
    private void addListeners() {
        bottomPanel.getCbNeuronType().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updateHelp();
                        AbstractNeuronPanel np = bottomPanel.getNeuronPanel();
                        topPanel.getExtraDataPanel().fillDefaultValues(
                                np.getPrototypeRule());
                    }
                });
            }
        });

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    /**
     * Set the help page based on the currently selected neuron type.
     */
    private void updateHelp() {
        if (bottomPanel.getCbNeuronType().getSelectedItem() == NULL_STRING) {
            helpAction = new ShowHelpAction("Pages/Network/neuron.html");
        } else {
            String name = (String) bottomPanel.getCbNeuronType()
                    .getSelectedItem();
            helpAction = new ShowHelpAction("Pages/Network/neuron/" + name
                    + ".html");
        }
        helpButton.setAction(helpAction);
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {

        // Commit changes specific to the neuron type
        // This must be the first change committed, as other neuron panels
        // make assumptions about the type of the neuron update rule being
        // edited that can result in ClassCastExceptions otherwise.
        bottomPanel.commitChanges();

        topPanel.commitChanges();

        // Notify the network that changes have been made
        neuronList.get(0).getNetwork().fireNetworkChanged();

    }

    /**
     * Test Main: For fast prototyping
     *
     * @param args
     */
    public static void main(String[] args) {

        Neuron n = new Neuron(new Network(), new LinearRule());
        ArrayList<NeuronNode> arr = new ArrayList<NeuronNode>();
        arr.add(new NeuronNode(new NetworkPanel(n.getNetwork()), n));
        NeuronDialog nd = new NeuronDialog(arr);

        nd.pack();
        nd.setVisible(true);

    }

}
