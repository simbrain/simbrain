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
package org.simbrain.network.gui.dialogs.synapse;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;

import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.nodes.SynapseNode;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 * The <b>SynapseDialog</b> is initialized with a list of synapses. When the
 * dialog is closed the synapses are changed based on the state of the dialog.
 */
public class SynapseDialog extends StandardDialog {

    /** The default serial version id. */
    private static final long serialVersionUID = 1L;

    /** Null string. */
    public static final String NULL_STRING = "...";

    /** Main panel. */
    private final Box mainPanel = Box.createVerticalBox();

    /**
     * Top panel. Contains fields for displaying/editing basic synapse
     * information.
     *
     * @see org.simbrain.network.gui.dialogs.synapse.BasicSynapseInfoPanel.java
     */
    private final BasicSynapseInfoPanel infoPanel;

    /**
     * Bottom panel. Contains fields for displaying/editing synapse update rule
     * parameters.
     */
    private final SynapseUpdateSettingsPanel updatePanel;

    /** The currently displayed spike responder panel. */
    private final SpikeResponderSettingsPanel responderPanel;

    /**
     * A boolean value to store the results of {@link #containsASpikeResponder(List)}
     * which determines whether or not a spike responder panel should be
     * displayed based on the update rules used by the presynaptic neurons to
     * the synapses being edited.
     */
    private final boolean usesSpikeResponders;

    /**
     * Help Button. Links to information about the currently selected synapse
     * update rule.
     */
    private final JButton helpButton = new JButton("Help");

    /** Show Help Action. The action executed by the help button */
    private ShowHelpAction helpAction;

    /** The synapses being modified. */
    private final ArrayList<Synapse> synapseList;

    /**
     * @param selectedSynapses the pnode_synapses being adjusted
     */
    public SynapseDialog(final Collection<SynapseNode> selectedSynapses) {
        this(getSynapses(selectedSynapses));
    }

    /**
     * @param synapseList the logical synapses being adjusted
     */
    public SynapseDialog(final List<Synapse> synapseList) {
        this.synapseList = (ArrayList<Synapse>) synapseList;
        infoPanel = new BasicSynapseInfoPanel(synapseList, this);
        updatePanel = new SynapseUpdateSettingsPanel(synapseList, this);
        responderPanel = new SpikeResponderSettingsPanel(synapseList, this);
        usesSpikeResponders = containsASpikeResponder(synapseList);
        initializeLayout();
        addListeners();
        updateHelp();
    }

    /**
     * Gets the logical synapses from a list of gui Synapse Nodes
     *
     * @param selectedSynapses the selected Synapse Node gui objects
     * @return the synapses contained within the slected synapse nodes
     */
    private static ArrayList<Synapse> getSynapses(
            final Collection<SynapseNode> selectedSynapses) {
        ArrayList<Synapse> sl = new ArrayList<Synapse>();
        for (SynapseNode s : selectedSynapses) {
            sl.add(s.getSynapse());
        }
        return sl;
    }

    /**
     * Initializes the components on the panel.
     */
    private void initializeLayout() {
        setTitle("Synapse Dialog");
        mainPanel.add(infoPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(updatePanel);
        if (usesSpikeResponders) {
            mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            mainPanel.add(responderPanel);
        }
        setContentPane(mainPanel);
        this.addButton(helpButton);
    }

    /**
     * Add listeners to the components of the dialog
     */
    private void addListeners() {
        updatePanel.getCbSynapseType().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {

                updateHelp();

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
     * Set the help page based on the currently selected synapse type.
     */
    public void updateHelp() {
        if (updatePanel.getCbSynapseType().getSelectedItem() == NULL_STRING) {
            helpAction = new ShowHelpAction("Pages/Network/synapse.html");
        } else {
            String name = (String) updatePanel.getCbSynapseType()
                    .getSelectedItem();
            helpAction = new ShowHelpAction("Pages/Network/synapse/" + name
                    + ".html");
        }
        helpButton.setAction(helpAction);
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {

        infoPanel.commitChanges(synapseList);

        // Now commit changes specific to the synapse type
        updatePanel.getSynapsePanel().commitChanges(synapseList);

        // If applicable commit changes for spike responders
        if (usesSpikeResponders) {
            responderPanel.getSpikeResponsePanel().commitChanges(synapseList);
        }

        // Notify the network that changes have been made
        synapseList.get(0).getNetwork().fireNetworkChanged();

    }

    /**
     * @return the list of synapses which are being edited by this dialog
     */
    public ArrayList<Synapse> getSynapseList() {
        return synapseList;
    }

    /**
     * Tests to make sure that at least one source neuron in the provided list
     * of synapses is a spiking neuron. . This is used to determine if a spike
     * responder panel should or shouldn't be displayed.
     *
     * @param synapses the synapses whose source neurons will be tested.
     * @return whether or not at least one of the synapses has a spike responder
     */
    public static boolean containsASpikeResponder(List<Synapse> synapses) {
        for (Synapse s : synapses) {
            NeuronUpdateRule nur = s.getSource().getUpdateRule();
            if (nur instanceof SpikingNeuronUpdateRule) {
                return true;
            }
        }
        return false;
    }

    // Old version of test that required all synapses were spike responders
    //    public static boolean spikeResponderTest(List<Synapse> synapses) {
    //        HashSet<SpikingNeuronUpdateRule> snurs = new HashSet<SpikingNeuronUpdateRule>();
    //        for (Synapse s : synapses) {
    //            NeuronUpdateRule nur = s.getSource().getUpdateRule();
    //            if (!snurs.contains(nur)) {
    //                if (!(nur instanceof SpikingNeuronUpdateRule)) {
    //                    return false;
    //                } else {
    //                    snurs.add((SpikingNeuronUpdateRule) nur);
    //                }
    //            }
    //        }
    //        return true;
    //    }

    // /**
    // * Test Main: For fast prototyping.
    // *
    // * @param args
    // */
    // public static void main(String[] args) {
    // Network net = new Network();
    // NetworkPanel np = new NetworkPanel(net);
    // Neuron n = new Neuron(net, new LinearRule());
    // NeuronNode nn = new NeuronNode(np, n);
    //
    // Synapse s = new Synapse(n, n);
    // ArrayList<SynapseNode> arr = new ArrayList<SynapseNode>();
    // arr.add(new SynapseNode(np, nn, nn, s));
    // SynapseDialog nd = new SynapseDialog(arr);
    //
    // nd.pack();
    // nd.setVisible(true);
    //
    // }

}
