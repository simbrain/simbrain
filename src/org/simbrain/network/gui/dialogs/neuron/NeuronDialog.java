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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.JButton;

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
    private Box mainPanel = Box.createVerticalBox();

    /** 
     *  Top panel. Contains fields for displaying/editing basic neuron
     *  information.
     *  @see org.simbrain.network.gui.dialogs.neuron.BasicNeuronInfoPanel.java
     */
    private BasicNeuronInfoPanel topPanel;
    
    /** 
     * Bottom panel. Contains fields for displaying/editing neuron update
     * rule parameters.
     * @see org.simbrain.network.gui.dialogs.neuron.NeuronUpdateSettingsPanel.java
     */
    private NeuronUpdateSettingsPanel bottomPanel;
    
    /** 
     *  Help Button. Links to information about the currently selected neuron
     *  update rule.
     */
    private JButton helpButton = new JButton("Help");

    /** Show Help Action. The action executed by the help button */
    private ShowHelpAction helpAction;

    /** The neurons being modified. */
    private ArrayList<Neuron> neuronList = new ArrayList<Neuron>();

    /** The pnodes which refer to them. */
    private ArrayList<NeuronNode> selectionList;
    	
    /**
     * @param selectedNeurons the pnode_neurons being adjusted
     */
    public NeuronDialog(final Collection<NeuronNode> selectedNeurons) {
        selectionList = new ArrayList<NeuronNode>(selectedNeurons);
        setNeuronList();
        init();
        addListeners();
    }

    /**
     * Get the logical neurons from the NeuronNodes.
     */
    private void setNeuronList() {
        neuronList.clear();

        for (NeuronNode n : selectionList) {
            neuronList.add(n.getNeuron());
        }
    }

    /**
     * Initializes the components on the panel.
     */
    private void init() {
        
    	setTitle("Neuron Dialog");
        mainPanel = Box.createVerticalBox();
        
        // Initialize the two main panels
        topPanel = new BasicNeuronInfoPanel(neuronList); // Basic Neuron Info       
        bottomPanel = new NeuronUpdateSettingsPanel(neuronList); // Update info        
    
        mainPanel.add(topPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(bottomPanel);
        setContentPane(mainPanel);
        
        updateHelp();    
        helpButton.setAction(helpAction);
        this.addButton(helpButton);

    }
       
    /**
     * Add listeners to the components of the dialog
     */
    private void addListeners(){
 	
    	// Alert the dialog if the top panel changes, resize accordingly
    	topPanel.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				mainPanel.repaint();
				pack();			
			}
    		
    	});
    	
    	// Alert the dialog if the bottom panel changes, resize accordingly
    	bottomPanel.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				mainPanel.repaint();
				pack();				
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
            name = name.substring(0, 1).toLowerCase().concat(name
            		.substring(1));
            helpAction = new ShowHelpAction("Pages/Network/neuron/" + name
                    + ".html");
        }
        helpButton.setAction(helpAction);
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
    	
    	topPanel.commitChanges();
    	
        // Now commit changes specific to the neuron type
        bottomPanel.getNeuronPanel().commitChanges(neuronList);
    	
        // Notify the network that changes have been made
        neuronList.get(0).getNetwork().fireNetworkChanged();
        
    }

    /**
     * Test Main: For fast prototyping
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
