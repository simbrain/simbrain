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

import java.awt.Frame;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 * <b>NeuronDialog</b> is a dialog box for setting the properties of a Neuron.
 */
@SuppressWarnings("serial")
public final class NeuronDialog extends StandardDialog {

    /** The neurons being modified. */
    private final List<Neuron> neuronList;

    /**
     * The main panel for editing neuron properties.
     */
    private NeuronPropertiesPanel neuronPropertiesPanel;

    /**
     * Help Button. Links to information about the currently selected neuron
     * update rule.
     */
    private final JButton helpButton = new JButton("Help");

    /** Show Help Action. The action executed by the help button */
    private ShowHelpAction helpAction;

    /**
     * Creates a neuron dialog from a collection of NeuronNodes. 
     * No frame available.
     *
     * @param selectedNeurons the neurons to edit
     * @return the dialog.
     */
    public static NeuronDialog createNeuronDialog(
            final Collection<NeuronNode> selectedNeurons) {
        NeuronDialog nd = new NeuronDialog(selectedNeurons);
        nd.neuronPropertiesPanel = NeuronPropertiesPanel
                .createNeuronPropertiesPanel(nd.neuronList, nd);
        nd.init();
        nd.addListeners();
        nd.updateHelp();
        return nd;
    }
    
    // Can't recall what having vs. not having a frame implies.  
    // If someone remembers, document it!  (JKY 1/16)
    
    
    /**
     * Creates a neuron dialog from a collection of NeuronNodes with a frame
     * specified.
     *
     * @param selectedNeurons neurons to edit.
     * @param parent the parent frame
     * @return the dialog.
     */
    public static NeuronDialog createNeuronDialog(
            final Collection<NeuronNode> selectedNeurons, final Frame parent) {
        NeuronDialog nd = new NeuronDialog(selectedNeurons, parent);
        nd.neuronPropertiesPanel = NeuronPropertiesPanel
                .createNeuronPropertiesPanel(nd.neuronList, nd);
        nd.init();
        nd.addListeners();
        nd.updateHelp();
        return nd;
    }
    
    /**
     * Create a neuron dialog for a list of logical neurons.
     *
     * @param neurons the neurons to edit
     * @return the dialog
     */
    public static NeuronDialog createNeuronDialog(
            final List<Neuron> neurons) {
        NeuronDialog nd = new NeuronDialog(neurons);
        nd.neuronPropertiesPanel = NeuronPropertiesPanel
                .createNeuronPropertiesPanel(nd.neuronList, nd);
        nd.init();
        nd.addListeners();
        nd.updateHelp();
        return nd;
    }

    /**
     * Construct the dialog object with no frame.
     *
     * @param selectedNeurons neurons to edit
     */
    private NeuronDialog(final Collection<NeuronNode> selectedNeurons) {
        neuronList = getNeuronList(selectedNeurons);
    }
    
    /**
     * Construct a dialog for a set of neurons.
     *
     * @param neurons
     */
    private NeuronDialog(final List<Neuron> neurons) {
        neuronList = neurons;
    }
    
    /**
     * Construct the dialog object with a frame.
     *
     * @param selectedNeurons neurons to edit
     * @param parent parent frame
     */
    private NeuronDialog(final Collection<NeuronNode> selectedNeurons,
            final Frame parent) {
        super(parent, "Neuron Dialog");
        neuronList = getNeuronList(selectedNeurons);
    }

    /**
     * Get the logical neurons from the NeuronNodes.
     *
     * @param selectedNeurons
     *            the selected gui neurons (pnodes) from which the neuron model
     *            objects will be extracted and then edited by this panel
     * @return the neuron model objects represented by the selected pnodes
     */
    private static List<Neuron> getNeuronList(
            final Collection<NeuronNode> selectedNeurons) {
        return selectedNeurons.stream().map(NeuronNode::getNeuron).collect(Collectors.toList());
    }

    /**
     * Initializes the components on the panel.
     */
    private void init() {
        setTitle("Neuron Dialog");
        JScrollPane scroller = new JScrollPane(neuronPropertiesPanel);
        scroller.setBorder(null);
        setContentPane(scroller);
        this.addButton(helpButton);
    }

    
    /**
     * Add listeners to the components of the dialog. Specifically alters the
     * destination of the help button to reflect the currently selected neuron
     * update rule.
     */
    private void addListeners() {
        neuronPropertiesPanel.getUpdateRulePanel().getCbNeuronType()
                .addActionListener(e -> SwingUtilities.invokeLater(() ->
                                updateHelp()));
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    /**
     * Set the help page based on the currently selected neuron type.
     */
    private void updateHelp() {
        if (neuronPropertiesPanel.getUpdateRulePanel().getCbNeuronType()
                .getSelectedItem() == SimbrainConstants.NULL_STRING) {
            helpAction = new ShowHelpAction("Pages/Network/neuron.html");
        } else {
            String name = (String) neuronPropertiesPanel.getUpdateRulePanel()
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
        neuronPropertiesPanel.commitChanges();

        // Notify the network that changes have been made
        neuronList.get(0).getNetwork().fireNeuronsUpdated(neuronList);
    }

}
