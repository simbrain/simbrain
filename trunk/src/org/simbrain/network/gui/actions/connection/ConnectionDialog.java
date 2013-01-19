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
package org.simbrain.network.gui.actions.connection;

import javax.swing.JButton;

import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.connect.AbstractConnectionPanel;
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
@SuppressWarnings("serial")
public class ConnectionDialog extends StandardDialog{

	/** The parent network panel. */
	private final NetworkPanel networkPanel;
	
	/** The connection panel wrapped in this dialog. */
	private AbstractConnectionPanel optionsPanel;
	
	/** The connection object associated with the connection panel. */
	private ConnectNeurons connection;
	
	
	/**
	 * 
	 * @param networkPanel
	 */
	public ConnectionDialog(final NetworkPanel networkPanel){
		this.networkPanel = networkPanel;
	}
	
	/**
	 * 
	 * @param networkPanel
	 * @param optionsPanel
	 */
	public ConnectionDialog(final NetworkPanel networkPanel,
			AbstractConnectionPanel optionsPanel){
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
			AbstractConnectionPanel optionsPanel, ConnectNeurons connection){
		this.networkPanel = networkPanel;
		this.optionsPanel = optionsPanel;
		this.connection = connection;
		fillFrame();
	}
	
	/**
	 * Fills the standard dialog with the connection panel and a help button.
	 */
	public void fillFrame(){
		ShowHelpAction helpAction = new ShowHelpAction(
                "Pages/Network/connections.html");
        addButton(new JButton(helpAction));
        setContentPane(optionsPanel);
	}
	
	 @Override
     protected void closeDialogOk() {
         super.closeDialogOk();
         optionsPanel.commitChanges();
         connection.connectNeurons(networkPanel.getNetwork(),
                 networkPanel.getSourceModelNeurons(),
                 networkPanel.getSelectedModelNeurons());
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
