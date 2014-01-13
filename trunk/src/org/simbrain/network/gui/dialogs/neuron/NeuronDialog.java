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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.nodes.NeuronNode;
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

    /**
     * A data panel containing both the basic neuron info and neuron update
     * settings.
     */
    private final CombinedNeuronInfoPanel neuronDataPanel;

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
        neuronDataPanel = new CombinedNeuronInfoPanel(neuronList, this, false);
        init();
        addListeners();
        updateHelp();
    }

    /**
     * Get the logical neurons from the NeuronNodes.
     * @param selectedNeurons the selected gui neurons (pnodes) from which
     * the neuron model objects will be extracted and then edited by this panel
     * @return the neuron model objects represented by the selected pnodes
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
        setContentPane(neuronDataPanel);
        this.addButton(helpButton);
    }

    /**
     * Add listeners to the components of the dialog. Specifically alters the
     * destination of the help button to reflect the currently selected
     * neuron update rule.
     */
    private void addListeners() {
        neuronDataPanel.getUpdateInfoPanel().getCbNeuronType()
        .addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updateHelp();
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
        if (neuronDataPanel.getUpdateInfoPanel()
                .getCbNeuronType().getSelectedItem() == NULL_STRING)
        {
            helpAction = new ShowHelpAction("Pages/Network/neuron.html");
        } else {
            String name = (String) neuronDataPanel.getUpdateInfoPanel()
                    .getCbNeuronType().getSelectedItem();
            helpAction = new ShowHelpAction("Pages/Network/neuron/" + name
                    + ".html");
        }
        helpButton.setAction(helpAction);
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        neuronDataPanel.commitChanges();

        // Notify the network that changes have been made
        neuronList.get(0).getNetwork().fireNetworkChanged();
    }

//    /**
//     * Test Main: For fast prototyping
//     *
//     * @param args
//     */
//    public static void main(String[] args) {
//
//        Neuron n = new Neuron(new Network(), new LinearRule());
//        ArrayList<NeuronNode> arr = new ArrayList<NeuronNode>();
//        arr.add(new NeuronNode(new NetworkPanel(n.getNetwork()), n));
//        NeuronDialog nd = new NeuronDialog(arr);
//
//        nd.pack();
//        nd.setVisible(true);
//
//    }
}
