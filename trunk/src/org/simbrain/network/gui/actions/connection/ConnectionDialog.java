package org.simbrain.network.gui.actions.connection;

import javax.swing.JButton;

import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.connect.AbstractConnectionPanel;
import org.simbrain.util.ShowHelpAction;
import org.simbrain.util.StandardDialog;

public class ConnectionDialog extends StandardDialog{

	private final NetworkPanel networkPanel;
	
	private AbstractConnectionPanel optionsPanel;
	
	private ConnectNeurons connection;
	
	
	public ConnectionDialog(final NetworkPanel networkPanel){
		this.networkPanel = networkPanel;
	}
	
	public ConnectionDialog(final NetworkPanel networkPanel,
			AbstractConnectionPanel optionsPanel){
		this.networkPanel = networkPanel;
		this.optionsPanel = optionsPanel;
		this.connection = optionsPanel.getConnection();
		fillFrame();
	}
	
	public ConnectionDialog(final NetworkPanel networkPanel,
			AbstractConnectionPanel optionsPanel, ConnectNeurons connection){
		this.networkPanel = networkPanel;
		this.optionsPanel = optionsPanel;
		this.connection = connection;
		fillFrame();
	}
	
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
