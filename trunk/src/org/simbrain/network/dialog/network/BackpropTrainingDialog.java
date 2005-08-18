/*
 * Part of HDV (High-Dimensional-Visualizer), a tool for visualizing high
 * dimensional datasets.
 * 
 * Copyright (C) 2004 Scott Hotton <http://www.math.smith.edu/~zeno/> and 
 * Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network.dialog.network;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.NetworkThread;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simnet.networks.Backprop;


/**
 * <b>DialogNetwork</b> is a dialog box for setting the properties of the 
 * Network GUI.
 */
public class BackpropTrainingDialog extends StandardDialog implements ActionListener,WindowListener {

	private LabelledItemPanel mainPanel = new LabelledItemPanel();	
	private JButton jbInputsFile = new JButton("None selected");
	private JButton jbOutputsFile = new JButton("None selected");
	private JTextField tfEpochs = new JTextField();
	private JTextField tfEta = new JTextField();
	private JTextField tfMu = new JTextField();
	private JTextField tfErrorInterval = new JTextField();
	private JButton jbRandomize = new JButton("Randomize");
	private JButton jbTrain = new JButton("Train");
	private JButton jbPlay = new JButton(ResourceManager.getImageIcon("Play.gif"));
	private JButton jbStep = new JButton(ResourceManager.getImageIcon("Step.gif"));
	private JLabel rmsError = new JLabel();
	double[][] inputs_train;
	double[][] outputs_train;
	private boolean updateCompleted = false;
	
	private NetworkPanel parentPanel;
	private Backprop theNet;
	private BPTDialogThread theThread = null;
	
	/**
	  * This method is the default constructor.
	  */
	 public BackpropTrainingDialog(NetworkPanel parent, Backprop bp) 
	 {
	 	parentPanel = parent;
	 	theNet = bp;
		init();
	 }

	 /**
	  * This method initialises the components on the panel.
	  */
	 private void init()
	 {
	 	//Initialize Dialog
		setTitle("Train Backprop Network");
		fillFieldValues();
		this.setLocation(600, 0); //Sets location of network dialog
		this.setSize(new Dimension(300, 200));
		
		//Set up grapics panel
		mainPanel.addItem("Input file", jbInputsFile);
		mainPanel.addItem("Output file", jbOutputsFile);
		mainPanel.addItem("Epochs", tfEpochs);
		mainPanel.addItem("Learning rate", tfEta);
		mainPanel.addItem("Momentum", tfMu);
		mainPanel.addItem("Error Interval", tfErrorInterval);
		mainPanel.addItem("Randomize network", jbRandomize);
		mainPanel.addItem("Train network", jbTrain);
		mainPanel.addItem("Play/Stop",jbPlay);
		mainPanel.addItem("Step",jbStep);
		mainPanel.addItem("RMSError",rmsError);
		
		
		jbInputsFile.addActionListener(this);
		jbOutputsFile.addActionListener(this);
		jbRandomize.addActionListener(this);
		jbTrain.addActionListener(this);
		jbPlay.addActionListener(this);
		jbStep.addActionListener(this);
		
		setContentPane(mainPanel);
		this.addWindowListener(this);
	 }
	 
	   public void actionPerformed(ActionEvent e) {
	   	
	   		Object o = e.getSource(); 
	   		if(o == jbInputsFile){
	   			SFileChooser chooser = new SFileChooser(parentPanel.getBackropDirectory(), "csv");
	   			File theFile = chooser.showOpenDialog();
	   			if (theFile == null) {
	   				return;
	   			}
	   			parentPanel.setBackropDirectory(chooser.getCurrentLocation());
	   			inputs_train = Utils.getDoubleMatrix(theFile);
	   			jbInputsFile.setText(theFile.getName());
	   			theNet.setTraining_inputs(inputs_train);
	   		} else if(o == jbOutputsFile){
	   			SFileChooser chooser = new SFileChooser(parentPanel.getBackropDirectory(), "csv");
	   			File theFile = chooser.showOpenDialog();
	   			if (theFile == null) {
	   				return;
	   			}
	   			parentPanel.setBackropDirectory(chooser.getCurrentLocation());
	   			outputs_train = Utils.getDoubleMatrix(theFile);
	   			jbOutputsFile.setText(theFile.getName());
	   			theNet.setTraining_outputs(outputs_train);
	   		} else if(o == jbRandomize){
	   			theNet.randomize();
	   			parentPanel.renderObjects();
	   			parentPanel.repaint();
	   		} else if(o == jbTrain){
	   			setValues();
	   			theNet.train();
	   			parentPanel.renderObjects();
	   			parentPanel.repaint();
	   		} else if(o == jbPlay){
	   			setValues();
				if (theThread == null) {
					theThread = new BPTDialogThread(this);
				}
				if (theThread.isRunning() == false) {
					jbPlay.setIcon(ResourceManager.getImageIcon("Stop.gif"));
					theThread.setRunning(true);
					theThread.start();
				} else {
					jbPlay.setIcon(ResourceManager.getImageIcon("Play.gif"));
					if (theThread == null) return;
					theThread.setRunning(false);
					theThread = null;
				}
    		} else if(o == jbStep){
	   			setValues();
	   			iterate();
    		}
	   }
	   
	   
	   public void iterate(){
  			theNet.iterate();
   			parentPanel.renderObjects();
   			parentPanel.repaint();
   			rmsError.setText(Double.toString(theNet.getOut().getRMSError()));
   			updateCompleted = true;
	   }
	 
	 /**
	 * Populate fields with current data
	 */
	 public void fillFieldValues() {
	 	tfEpochs.setText("" + theNet.getEpochs());
	 	tfEta.setText("" + theNet.getEta());
	 	tfMu.setText("" + theNet.getMu());
	 	tfErrorInterval.setText("" + theNet.getError_interval());	 	
	}
	 
	/**
	* Set projector values based on fields 
	*/
   public void setValues() {
		theNet.setEpochs(Integer.parseInt(tfEpochs.getText()));
   		theNet.setEta(Double.parseDouble(tfEta.getText()));
   		theNet.setMu(Double.parseDouble(tfMu.getText()));
   		theNet.setError_interval(Integer.parseInt(tfErrorInterval.getText()));	   	
	}

	public boolean isUpdateCompleted() {
		return updateCompleted;
	}

	public void setUpdateCompleted(boolean updateCompleted) {
		this.updateCompleted = updateCompleted;
	}

	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowClosed(WindowEvent arg0) {
		if(theThread != null){
			theThread.setRunning(false);
			theThread = null;
		}
	}

	public void windowClosing(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	
	public class BPTDialogThread extends Thread {

		private BackpropTrainingDialog dialog = null;
		private volatile boolean isRunning = false;
		
		Runnable iterate = new Runnable() {
			public void run(){
				dialog.iterate();
			}
		};
		
		public BPTDialogThread(BackpropTrainingDialog dialog){
			this.dialog = dialog;
		}

		
		public void run(){
			try {
				while(isRunning == true){
					dialog.setUpdateCompleted(false);
					SwingUtilities.invokeLater(iterate);
					while(!dialog.isUpdateCompleted()){
						sleep(10);
					}
				}	
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
		public boolean isRunning() {
			return isRunning;
		}

		public void setRunning(boolean isRunning) {
			this.isRunning = isRunning;
		}

	}

	
	
}
