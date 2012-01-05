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
package org.simbrain.network.gui.dialogs.connect;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.gui.dialogs.synapse.SynapseDialog;
import org.simbrain.network.interfaces.Synapse;

/**
 * <b>AllToAllPanel</b> creates a dialog for setting preferences of all to all
 * neuron connections.
 * 
 * @author ztosi
 * @author jyoshimi
 * 
 */
public class AllToAllPanel extends AbstractConnectionPanel {

    /** Allow self connection check box. */
    private JCheckBox allowSelfConnect = new JCheckBox();

    /**
     * This method is the default constructor.
     * @param connection type
     */
    public AllToAllPanel(final AllToAll connection) {
        super(connection);
        initializeLayout();
    }
    
    /**
     * Initializes the custom layout for the all to all panel
     */
    public void initializeLayout(){ 	
    	
    	this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(10, 10, 0, 10);
        
        this.add(new JLabel("Excitatory/Inhibitory:"), gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        this.add(ratioSlider, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        this.add(new JLabel("% Excitatory"), gbc);
        
        gbc.gridx = 1;
        Dimension tRatioSize = tRatio.getPreferredSize();
        tRatioSize.width = 30;
        tRatio.setPreferredSize(tRatioSize);
        
        //the ratio text field gets its own panel to prevent distortion
        JPanel tRatioPanel = new JPanel();
        tRatioPanel.setLayout(new BorderLayout());
        tRatioPanel.add(tRatio, BorderLayout.WEST);
        this.add(tRatioPanel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        this.add(new JLabel("Allow Self-Connection: "), gbc);
        
        gbc.gridx = 1;
        this.add(allowSelfConnect, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        this.add(new JSeparator(JSeparator.HORIZONTAL), gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy = 5;
        this.add(new JLabel("Excitatory Synapse Type: "), gbc);
        
        gbc.gridx = 1;
        this.add(excitatorySynType, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 6;
        this.add(new JLabel("Inhibitory Synapse Type: "), gbc);
        
        gbc.gridx = 1;
        this.add(inhibitorySynType, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 3;
        this.add(new JSeparator(JSeparator.HORIZONTAL), gbc);
        
        gbc.gridy = 8;
        gbc.gridwidth = 1;
        this.add(new JLabel("Randomize Excitatory Weights: "), gbc);
        
        gbc.gridx = 1;
        this.add(randExcite, gbc);
        
        gbc.gridx = 2;
        this.add(randExButton, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 9;
        this.add(new JLabel("Randomize Inhibitory Weights: "), gbc);
        
        gbc.gridx = 1;
        this.add(randInhib, gbc);
        
        gbc.gridx = 2;
        this.add(randInButton, gbc);
        
    }

    /**
     * {@inheritDoc}
     */
    public void commitChanges() {
        connection.setPercentExcitatory(((Number)tRatio.getValue()).doubleValue() / 100);
        Synapse e = Synapse.getTemplateSynapse(excitatorySynType.getText());
    	connection.setBaseExcitatorySynapse(e);
    	Synapse i = Synapse.getTemplateSynapse(inhibitorySynType.getText());
    	connection.setBaseInhibitorySynapse(i);
    	if(randInhib.isSelected()) {
    		connection.setInhibitoryRand(inhibRS);
    	}
    	if(randExcite.isSelected()) {
    		connection.setExcitatoryRand(exciteRS);
    	}
    	((AllToAll) connection).setAllowSelfConnection(allowSelfConnect
                .isSelected());
    }
    

    /**
     * {@inheritDoc}
     */
    public void fillFieldValues() {
       
    }

}
