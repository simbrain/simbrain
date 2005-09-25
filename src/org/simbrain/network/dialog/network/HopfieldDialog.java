/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

import com.Ostermiller.util.CSVParser;

/**
 * <b>HopfieldDialog</b> is a dialog box for creating hopfield networks.
 */
public class HopfieldDialog extends StandardDialog implements ActionListener {

	private static final String FS = System.getProperty("file.separator");
	public static final int DISCRETE = 0;
	public static final int CONTINUOUS = 1;

	private JTabbedPane tabbedPane = new JTabbedPane();
	private JPanel tabLogic = new JPanel();
	private JPanel tabLayout = new JPanel();
	private LabelledItemPanel logicPanel = new LabelledItemPanel();
	private LayoutPanel layoutPanel = new LayoutPanel();

	private JTextField numberOfUnits = new JTextField();
	private JComboBox cbType = new JComboBox(new String[] {"Discrete", "Continuous"});
	private JButton trainingFile = new JButton("Set");
	

	private String[][] values = null;
	
	/**
	  * This method is the default constructor.
	  */
	 public HopfieldDialog() 
	 {
		init();
	 }

	 /**
	  * This method initialises the components on the panel.
	  */
	 private void init()
	 {
	 	//Initialize Dialog
		setTitle("New Hopfield Network");
		fillFieldValues();
		this.setLocation(500, 0); //Sets location of network dialog
		layoutPanel.setCurrentLayout(LayoutPanel.GRID);
		layoutPanel.initPanel();

		trainingFile.addActionListener(this);
		
		//Set up grapics panel
		logicPanel.addItem("Type", cbType);
		logicPanel.addItem("Number of Units", numberOfUnits);
		logicPanel.addItem("Set training file", trainingFile);
		
		//Set up tab panel
		tabLogic.add(logicPanel);
		tabLayout.add(layoutPanel);
		tabbedPane.addTab("Logic", logicPanel);
		tabbedPane.addTab("Layout", layoutPanel);
		setContentPane(tabbedPane);

	 }
		
	 
	 /**
	 * Populate fields with current data
	 */
	 public void fillFieldValues() {
		
	}
	 
	/**
	* Set values based on fields 
	*/
   public void getValues() {  	
	}
   
   public String getCurrentLayout(){
   	return layoutPanel.getCurrentLayout();
   }

   public int getNumUnits() {
   	return Integer.parseInt(numberOfUnits.getText());
   }
   
   public int getType() {
   		if(cbType.getSelectedIndex() == 0) {
   			return DISCRETE;
   		} else return CONTINUOUS;
   }
    
	 
public void actionPerformed(ActionEvent e) {
	loadFile();
}

	
private void loadFile() {
	JFileChooser chooser = new JFileChooser();
	chooser.setCurrentDirectory(
		new File("." + FS + "simulations" + FS + "networks"));
	int result = chooser.showDialog(this, "Open");
	if (result == JFileChooser.APPROVE_OPTION) {
		readFile(chooser.getSelectedFile());
	}
}

public void readFile(File theFile) {

	CSVParser theParser = null;

	try {
		theParser =
			new CSVParser(new FileInputStream(theFile), "", "", "#"); // # is a comment delimeter in net files
		values = theParser.getAllValues();
	} catch (java.io.FileNotFoundException e) {
		JOptionPane.showMessageDialog(null, "Could not find the file \n" + theFile,
		        "Warning", JOptionPane.ERROR_MESSAGE);
		return;
	} catch (Exception e){
	    JOptionPane.showMessageDialog(null, "There was a problem opening the file \n" + theFile,
		        "Warning", JOptionPane.ERROR_MESSAGE);
	    e.printStackTrace();
		return;
	}
}


}
