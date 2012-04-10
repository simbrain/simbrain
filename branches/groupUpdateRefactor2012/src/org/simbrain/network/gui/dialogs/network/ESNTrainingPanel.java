package org.simbrain.network.gui.dialogs.network;

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.simbrain.network.gui.trainer.LMSOfflinePanel;
import org.simbrain.network.gui.trainer.ReservoirUtilsPanel;
import org.simbrain.network.subnetworks.EchoStateNetwork;
import org.simbrain.network.trainers.LMSOffline;
import org.simbrain.network.trainers.Trainer;
import org.simbrain.util.genericframe.GenericFrame;

/**
 * Panel for training ESN's.
 */
public class ESNTrainingPanel extends JPanel {
	
	
	/** Reference to echo state network to train. */
	private final EchoStateNetwork esn;
	
	/** The associated trainer. */
	private final Trainer trainer;
	
	/** The parent frame. */
	private GenericFrame parent;
	
	/**
	 * Construct an ESN Training Panel.
	 *
	 * @param esn the underlying network
	 */
    public ESNTrainingPanel(final EchoStateNetwork esn) {
    	this.esn = esn;   
        trainer = esn.getTrainer();	
	}
    
    /**
     * Initializes the training panel.
     */
    public void init() {
    	LMSOfflinePanel offlinePanel = new LMSOfflinePanel((LMSOffline) trainer);
    	setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.weightx = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridwidth = 3;
		add(offlinePanel, constraints);
		constraints.gridy = 1;
		ReservoirUtilsPanel rUtilsPanel = new ReservoirUtilsPanel(this, esn);
		add(rUtilsPanel, constraints);
		parent.pack();
    }
    
    /**
     * A setter allowing this object to keep track of and alter the parent
     * frame.
     * @param parent the frame containing this panel.
     */
    public void setGenericParent(final GenericFrame parent) {
    	this.parent = parent;
    }
    
    /**
     * A getter for the parent frame, allowing other objects to alter the
     * parent frame of this panel.
     * @return the parent frame.
     */
    public GenericFrame getGenericParent() {
    	return parent;
    }


}
