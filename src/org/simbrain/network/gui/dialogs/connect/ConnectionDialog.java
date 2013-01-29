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
import java.awt.FlowLayout;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.util.SynapseRouter;
import org.simbrain.util.ShowHelpAction;
import org.simbrain.util.StandardDialog;

/**
 *
 * Dialog wrapper for all connection panels.
 *
 * @author jyoshimi
 * @author ztosi
 *
 */
public class ConnectionDialog extends StandardDialog {

    /** The parent network panel. */
    private final NetworkPanel networkPanel;

    /** The connection panel wrapped in this dialog. */
    private AbstractConnectionPanel optionsPanel;

    /** The connection object associated with the connection panel. */
    private ConnectNeurons connection;

    /** The mapping of group names to available synapse groups. */
    private LinkedHashMap<String, SynapseGroup> groupingOptions =
    		new LinkedHashMap<String, SynapseGroup>();
    
    /** A combo-box showing the available group options. */
    private JComboBox synapseGroups;
    
    /**
     *
     * @param networkPanel
     */
    public ConnectionDialog(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
    }

    /**
     *
     * @param networkPanel
     * @param optionsPanel
     */
    public ConnectionDialog(final NetworkPanel networkPanel,
            AbstractConnectionPanel optionsPanel) {
        this.networkPanel = networkPanel;
        this.optionsPanel = optionsPanel;
        this.connection = optionsPanel.getConnection();
        fillFrame();
    }

    /**
     *
     * @param networkPanel
     * @param optionsPanel
     * @param connection
     */
    public ConnectionDialog(final NetworkPanel networkPanel,
            AbstractConnectionPanel optionsPanel, ConnectNeurons connection) {
        this.networkPanel = networkPanel;
        this.optionsPanel = optionsPanel;
        this.connection = connection;
        fillFrame();
    }

    /**
     * Fills the standard dialog with the connection panel and a help button.
     */
    public void fillFrame() {
        ShowHelpAction helpAction = new ShowHelpAction(
                "Pages/Network/connections.html");
        addButton(new JButton(helpAction));


        synapseGroups = new JComboBox(updateAvailableGroups());
        JPanel groupingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        groupingPanel.add(new JLabel("Add new synapses to group: "));
        groupingPanel.add(synapseGroups);
        optionsPanel.add(groupingPanel, BorderLayout.SOUTH);
        
        setContentPane(optionsPanel);
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        optionsPanel.commitChanges();
        
    	List<Synapse> synapses = connection.connectNeurons(networkPanel.getNetwork(),
                networkPanel.getSourceModelNeurons(),
                networkPanel.getSelectedModelNeurons());
        
    	SynapseGroup homeGroup = groupingOptions.get(synapseGroups.
    			getSelectedItem());
    	
        if (homeGroup != null) {
        	
        	for (Synapse s : synapses) {
        		SynapseRouter.addSynapseToGroup(s, homeGroup);  		
        	}
        	
        	if (((String) (synapseGroups.getSelectedItem())).
        			contentEquals("New Group")) {
        	networkPanel.getNetwork().addGroup(homeGroup);
        	} 
        }

    }

    /**
     * Updates the list of synapse groups available for the new connections to
     * be added to.
     * @return an array of the names of each available group.
     */
    private String [] updateAvailableGroups() {
    	groupingOptions.clear();
        groupingOptions.put("None", null);
        groupingOptions.put("New Group",
        		new SynapseGroup(networkPanel.getNetwork()));
  
        int count = 2;
        
        for (Group sg : networkPanel.getNetwork().getGroupList()) {
        	if (sg instanceof SynapseGroup) {
        		groupingOptions.put(sg.getLabel(), (SynapseGroup) sg);
        		count++;
        	}
        }
        
        String[] names = new String[count];
        names[0] = "None";
        names[1] = "New Group";
        for(int i = 2; i < count; i++) {
        	names[i] = (String)groupingOptions.keySet().toArray()[i];
        }
        
        
        return names;
  
    }
    
    public AbstractConnectionPanel getOptionsPanel() {
        return optionsPanel;
    }

    public void setOptionsPanel(AbstractConnectionPanel optionsPanel) {
        this.optionsPanel = optionsPanel;
    }

    public ConnectNeurons getConnection() {
        return connection;
    }

    public void setConnection(ConnectNeurons connection) {
        this.connection = connection;
    }
}