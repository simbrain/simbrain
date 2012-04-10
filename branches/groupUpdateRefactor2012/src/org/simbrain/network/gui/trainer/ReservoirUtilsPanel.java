package org.simbrain.network.gui.trainer;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle;
import javax.swing.border.TitledBorder;

import org.simbrain.network.gui.dialogs.RandomPanel;
import org.simbrain.network.gui.dialogs.network.ESNTrainingPanel;
import org.simbrain.network.subnetworks.EchoStateNetwork;
import org.simbrain.network.util.RandomSource;

public class ReservoirUtilsPanel extends JPanel {
	
	/** The title of the panel. */
	private TitledBorder title = BorderFactory.createTitledBorder("Noise");
	
	//TODO: change to more general ReservoirNetwork, when LSMs are added
	/** The esn being set. */
	private EchoStateNetwork esn;

	/** The noise generator. */
	private RandomSource randomSource;
	
	/** A panel for setting random values. */
	private RandomPanel randomPanel = new RandomPanel(true);
	
	/** A reference to the parent panel. */
	private JPanel trainerPanel;
	
	/** Enables noise. */
	private JButton enableNoise = new JButton();
	
	/** Flag for noise enabled/disabled */
	private boolean noiseEnabled;
	
	/** Commits changes made in the random panel. */
	private JButton noiseButton = new JButton("Apply");
	
	/** Contraints on the layout made accessable to all methods. */
	private GridBagConstraints gbc = new GridBagConstraints();
	
	/**
	 * The constructor, requiring both a reference to an esn and the parent
	 * trainer panel.
	 * @param trainerPanel the parent trainer panel
	 * @param esn the esn where state noise is being injected.
	 */
	public ReservoirUtilsPanel(JPanel trainerPanel, EchoStateNetwork esn) {
		this.trainerPanel = trainerPanel;
		this.esn = esn;
		this.setLayout(new GridBagLayout());
		this.setBorder(title);
		randomSource = esn.getNoiseGenerator();
		noiseEnabled = false;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		paintNoiseDisabled();	
		addActionListeners();
		resetConstraints();
	}
	
	private void addActionListeners() {
		enableNoise.addActionListener(new ActionListener (){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				if(noiseEnabled){
					noiseEnabled = false;
					paintNoiseDisabled();
					((ESNTrainingPanel) trainerPanel).getGenericParent().pack();
				} else {
					noiseEnabled = true;
					paintNoiseEnabled();
					((ESNTrainingPanel) trainerPanel).getGenericParent().pack();
				}
				
				esn.setNoise(noiseEnabled);
				
			}
			
		});
		
		noiseButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {	
				randomPanel.commitRandom(randomSource);
			}
			
		});
		
	}

	/**
	 * Paints the panel for the noise enabled state.
	 */
	private void paintNoiseEnabled(){
		clearAll();
		enableNoise.setText("On");
		enableNoise.setForeground(Color.green);
		this.add(enableNoise, gbc);
		gbc.gridy += 1;
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.gridheight = 6;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		this.add(randomPanel, gbc);
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.gridy = 20;
		gbc.gridx = 2; 
		gbc.weightx = 0;
		gbc.anchor = GridBagConstraints.NORTHEAST;
		this.add(noiseButton, gbc);
		this.repaint();
	}
	
	/**
	 * Paints the panel for the noise disabled state.
	 */
	private void paintNoiseDisabled () {
		clearAll();
		enableNoise.setText("Off"); 
		enableNoise.setForeground(Color.red);
		this.add(enableNoise, gbc);
		this.repaint();
	}
	
	/**
	 * Clears everything from the panel.
	 */
	private void clearAll() {
		this.removeAll();
		resetConstraints();
	}
	
	/** 
	 * Resets the gridbagconstraints on this panel to default, except that the
	 * anchor is set to NORTHWEST.
	 */
	private void resetConstraints() {
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.NORTHWEST;
	}
	
}
