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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.gui.dialogs.RandomPanel;
import org.simbrain.network.gui.dialogs.synapse.SynapseDialog;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.util.RandomSource;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>AbstractNeuronPanel</b>.
 */
public abstract class AbstractConnectionPanel extends LabelledItemPanel {

    /** Main panel. */
    protected JPanel mainPanel = new JPanel();

    /** Reference to underlying connection object. */
    protected ConnectNeurons connection;
    
    /** Max ratio of excitatory/inhibitory connections. */
    private static final int RATIO_MAX = 100;
	
    /** Min ratio of excitatory/inhibitory connections. */
	private static final int RATIO_MIN = 0;
	
	/** Default starting ratio of excitatory/inhibitory. */
	private static final int RATIO_INIT = 50;
    
	/** A slider for setting the ratio of inhibitory to excitatory connections. */
    protected JSlider ratioSlider = new JSlider(JSlider.HORIZONTAL, RATIO_MIN,
    		RATIO_MAX, RATIO_INIT);
    
    /** A text field for setting the ratio of excitatory to inhibitory connections. */
    protected JFormattedTextField tRatio = new JFormattedTextField();
    
    /** A button opening a menu to select the desired type of excitatory synapse. */
    protected final JButton excitatorySynType = new JButton("ClampedSynapse");
    
    /** A button opening a menu to select the desired type of inhibitory synapse. */
    protected final JButton inhibitorySynType = new JButton("ClampedSynapse");
    
    /** A random panel to set the range and distribution of excitatory strengths. */
    protected RandomPanel exRandPanel = new RandomPanel(true);
    
    /** A random panel to set the range and distribution of inhibitory strengths. */
    protected RandomPanel inRandPanel = new RandomPanel(true);
    
    /** A button opening a menu to a random panel for excitatory connections. */
    protected final JButton randExButton = new JButton();
    
    /** A button opening a menu to a random panel for inhibitory connections. */
    protected final JButton randInButton = new JButton();
    
    /** A random source for inhibitory strengths. */
    protected final RandomSource inhibRS = new RandomSource();
    
    /** A random source for excitatory strengths. */
    protected final RandomSource exciteRS = new RandomSource();
    
    /** A checkbox for selecting whether or not inhibitory strengths are to be randomized. */
    protected JCheckBox randInhib = new JCheckBox();
    
    /** A checkbox for selecting whether or not excitatory strengths are to be randomized. */
    protected JCheckBox randExcite = new JCheckBox();
    
    /**
     * This method is the default constructor.
     */
    public AbstractConnectionPanel(final ConnectNeurons connection) {
        this.connection = connection;
        this.setLayout(new BorderLayout());
        this.add(mainPanel, BorderLayout.CENTER);
        initializeContent();
    }

    /**
     * Populate fields with current data.
     */
    public abstract void fillFieldValues();

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public abstract void commitChanges();

    /**
     * Add notes or other text to bottom of panel.  Can be html formatted.
     *
     * @param text Text to be added
     */
    public void addBottomText(final String text) {
        JPanel labelPanel = new JPanel();
        JLabel theLabel = new JLabel(text);
        labelPanel.add(theLabel);
        this.add(labelPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Initializes the ratio field, sliders, change listeners, action listeners,
     * and random buttons/checkboxes.
     */
    public void initializeContent(){
    	tRatio.setValue(((Number)(ConnectNeurons.getDefaultRatio() * 100)).intValue());
    	initializeRatioSlider();
    	initializeChangeListeners();
    	initializeActionListeners();
    	randExButton.setIcon(ResourceManager.getImageIcon("ExRand.png"));
    	randInButton.setIcon(ResourceManager.getImageIcon("InRand.png"));
    	randExButton.setEnabled(false);
    	randInButton.setEnabled(false);
    }
    
    /**
     * Initializes the values of the GUI ratio slider.
     */
    public void initializeRatioSlider(){
    	ratioSlider.setMajorTickSpacing(10);
        ratioSlider.setMinorTickSpacing(2);
        ratioSlider.setPaintTicks(true);
        
        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put(new Integer(0), new JLabel("0/100"));
        labelTable.put(new Integer(25), new JLabel("25/75"));
        labelTable.put(new Integer(50), new JLabel("50/50"));
        labelTable.put(new Integer(75), new JLabel("75/25"));
        labelTable.put(new Integer(100), new JLabel("100/0"));
        ratioSlider.setLabelTable(labelTable);
        ratioSlider.setPaintLabels(true);
    }
    
    /**
     * Initializes the change listeners relating to the ratio text field and
     * ratio slider (reciprocal listeners).
     */
    public void initializeChangeListeners(){
    	
    	tRatio.addPropertyChangeListener(new PropertyChangeListener(){

			public void propertyChange(PropertyChangeEvent arg0) {
				if(arg0.getSource() == tRatio) {
					ratioSlider.setValue(((Number)tRatio.getValue()).intValue());
				}
			}
        });
    	
    	ratioSlider.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider)e.getSource();
				if(!source.getValueIsAdjusting() && source == ratioSlider){
					tRatio.setValue(new Integer(ratioSlider.getValue()));
				}	
			}
    		
    	});
    	
    }
    
    /**
     * Initializes the action listeners relating to buttons and checkboxes.
     */
    public void initializeActionListeners(){
    	
    	excitatorySynType.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ArrayList<Synapse> excitatoryList = new ArrayList<Synapse>();
                excitatoryList.add(connection.getBaseExcitatorySynapse());
                SynapseDialog dialog = new SynapseDialog(excitatoryList);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                Synapse excitatorySynapse = dialog.getSynapseList().get(0);
                connection.setBaseExcitatorySynapse(excitatorySynapse);
                excitatorySynType.setText(excitatorySynapse.getType());
            }

        });
  	  
  	  	inhibitorySynType.addActionListener(new ActionListener() {
  		  public void actionPerformed(ActionEvent e) {
                ArrayList<Synapse> inhibitoryList = new ArrayList<Synapse>();
                inhibitoryList.add(connection.getBaseInhibitorySynapse());
                SynapseDialog dialog = new SynapseDialog(inhibitoryList);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                Synapse inhibitorySynapse = dialog.getSynapseList().get(0);
                connection.setBaseInhibitorySynapse(inhibitorySynapse);
                inhibitorySynType.setText(inhibitorySynapse.getType());
            }
        });
  	  
  	  	randInhib.addActionListener(new ActionListener(){
  	  		public void actionPerformed(ActionEvent arg0) {
  	  			if(arg0.getSource() == randInhib){
  	  				if(randInhib.isSelected()){
  	  					randInButton.setEnabled(true);
  	  					connection.setEnableInRand(true);
  	  				} else {
  	  					randInButton.setEnabled(false);
  	  					connection.setEnableInRand(false);
  	  				}
  	  			}
  	  		}
  	  	});
	 
		randExcite.addActionListener(new ActionListener(){
	 		public void actionPerformed(ActionEvent arg0) {
	 			if(arg0.getSource() == randExcite){
	 				if(randExcite.isSelected()){
	 					randExButton.setEnabled(true);
	 					connection.setEnableExRand(true);
	 				} else {
	 					randExButton.setEnabled(false);
	 					connection.setEnableExRand(false);
	 				}
	 			}
	 		}
	 	 });
	 
		 randExButton.addActionListener(new ActionListener(){
			
			@SuppressWarnings("serial")
			 public void actionPerformed(ActionEvent arg0) {
				 StandardDialog exRSD = new StandardDialog(){	 
					 @Override
					 protected void closeDialogOk(){
						 super.closeDialogOk();
						 exRandPanel.commitRandom(exciteRS);
					 }
				 };
				 exRSD.setContentPane(exRandPanel);
				 exRSD.pack();
				 exRSD.setLocationRelativeTo(null);
				 exRSD.setVisible(true);
			 }
			 
		 });
	 
		 randInButton.addActionListener(new ActionListener() {
	
			 @SuppressWarnings("serial")
			 public void actionPerformed(ActionEvent arg0) {
				 StandardDialog inRSD = new StandardDialog(){	 
					 @Override
					 protected void closeDialogOk(){
						 super.closeDialogOk();
						 inRandPanel.commitRandom(inhibRS);
					 }
				 };
				 inRSD.setContentPane(inRandPanel);
				 inRSD.pack();
				 inRSD.setLocationRelativeTo(null);
				 inRSD.setVisible(true);
			 }
			 
		 });
    }
    
}
