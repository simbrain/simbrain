package org.simbrain.network.gui.dialogs.network;

import java.awt.FlowLayout;

import javax.swing.JPanel;

import org.simbrain.network.gui.trainer.LMSOfflinePanel;
import org.simbrain.network.subnetworks.EchoStateNetwork;
import org.simbrain.network.trainers.LMSOffline;
import org.simbrain.network.trainers.Trainer;

/**
 * Panel for training ESN's.
 */
public class ESNTrainingPanel extends JPanel {
	
	
	/** Reference to echo state network to train. */
	private final EchoStateNetwork esn;
	
	/**
	 * Construct an ESN Training Panel.
	 *
	 * @param esn the underlying network
	 */
    public ESNTrainingPanel(final EchoStateNetwork esn) {
    	this.esn = esn;
        setLayout(new FlowLayout(FlowLayout.LEADING, 25, 5));
		final Trainer trainer = esn.getTrainer();
		LMSOfflinePanel offlinePanel = new LMSOfflinePanel((LMSOffline) trainer);
		add(offlinePanel);
	}
    



}
