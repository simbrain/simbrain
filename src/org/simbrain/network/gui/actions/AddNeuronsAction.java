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
package org.simbrain.network.gui.actions;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.AddNeuronsDialog;
import org.simbrain.network.gui.dialogs.layout.LayoutDialog;
import org.simbrain.network.gui.dialogs.neuron.NeuronDialog;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.neurons.LinearNeuron;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * Creates a group of neurons.
 */
public final class AddNeuronsAction
    extends AbstractAction {

    /** Network panel. */
    private final NetworkPanel networkPanel;

/*
    class ShowNeuronDialog extends AbstractAction{

    	private ArrayList<NeuronNode> nodes;
    	
    	public ShowNeuronDialog(ArrayList<NeuronNode> nodes){
    		super("Neuron Parameters");
    		this.nodes = nodes;
    	}
    	
		@Override
		public void actionPerformed(ActionEvent arg0) {
			NeuronDialog nDialog = new NeuronDialog(nodes){
				protected void closeDialogOk(){
					super.closeDialogOk();
			        commitChanges();
			        LayoutDialog dialog = new LayoutDialog(networkPanel);
			        dialog.pack();
			        dialog.setLocationRelativeTo(null);
			        dialog.setVisible(true);
				}
			};
			nDialog.pack();
			nDialog.setLocationRelativeTo(null);
			nDialog.setVisible(true);
		}
    	
    }
    
    private Action nDialog;
    */
    /**
     * Create a new neuron action with the specified network panel.
     *
     * @param networkPanel network panel, must not be null
     */
    public AddNeuronsAction(final NetworkPanel networkPanel) {
        super("Add Neurons...");

        if (networkPanel == null) {
            throw new IllegalArgumentException("networkPanel must not be null");
        }

        this.networkPanel = networkPanel;
        //putValue(SMALL_ICON, ResourceManager.getImageIcon("AddNeuron.png"));
        //putValue(SHORT_DESCRIPTION, "Add or \"put\" new node (p)");
        //networkPanel.getInputMap().put(KeyStroke.getKeyStroke('p'), this);
        //networkPanel.getActionMap().put(this, this);

    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
    	AddNeuronsDialog and = new AddNeuronsDialog(networkPanel);
        and.pack();
        and.setLocationRelativeTo(null);
        and.setVisible(true);
    }
  
}
