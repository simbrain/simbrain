/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
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

package org.simbrain.world;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

import javax.swing.*;

import com.Ostermiller.util.*;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.simulation.*;

	
/**
 * <b>DialogScript</b> is used to manage world scripts, which control creature behavior in a pre-programmed way
 */
public class DialogScript extends StandardDialog implements ActionListener {

	private static final String FS = Simulation.getFileSeparator();
	private World theWorld;
	private LabelledItemPanel myContentPane = new LabelledItemPanel();
	private JButton runButton = new JButton("Run");
	private JButton stopButton = new JButton("Stop");
	private JButton loadButton = new JButton("Load");
	private JLabel fileLabel = new JLabel("   No script loaded");
	private String[][] values = null;
	private ScriptThread theThread = null;
		
	public DialogScript(World wp)
	{
		theWorld = wp;
		init();
	}

	/**
	 * This method initialises the components on the panel.
	 */
	private void init()
	{
	   setTitle("Script");
	   
	   this.setModal(false);
	   this.setLocation(500,500);
		
	   runButton.addActionListener(this);
	   loadButton.addActionListener(this);
	   stopButton.addActionListener(this);
	 
	   myContentPane.addItem("", fileLabel);		 
	   myContentPane.addItem("", runButton);
	   myContentPane.addItem("", stopButton);			 
	   myContentPane.addItem("", loadButton);	

	   setContentPane(myContentPane);

	}
	 
	 
  public void actionPerformed(ActionEvent e) {
  		Object o = e.getSource();
  		if (o == loadButton) {
  			loadScript();
  		}
  		else  if (o == stopButton){
  			stopScript();
  		} else if (o == runButton) {
  			runScript();
  		}
  }
  
 	
  private void loadScript() {
	JFileChooser chooser = new JFileChooser();
	chooser.setCurrentDirectory(
		new File("." + FS + "simulations" + FS + "worlds"));
	int result = chooser.showDialog(this, "Open");
	if (result == JFileChooser.APPROVE_OPTION) {
		readScript(chooser.getSelectedFile());
	}
  }
  
  public void readScript(File theFile) {
	fileLabel.setText("  " + theFile.getName());
	repaint();
	FileInputStream f = null;
	String line = null;
	CSVParser theParser = null;

	try {
		theParser =
			new CSVParser(f = new FileInputStream(theFile), "", "", "#"); // # is a comment delimeter in net files
		values = theParser.getAllValues();
	} catch (Exception e) {
		JOptionPane.showMessageDialog(null, "Could not find script file \n" + theFile, "Warning", JOptionPane.ERROR_MESSAGE);
		return;
	}
  }
  
  public void runScript() {
 
  	if (values != null) {
		theWorld.getNetworkPanel().clearAll();		
  		theThread = new ScriptThread(theWorld, values);
  		theThread.setRunning(true);
  		theThread.start();
  	}

  }

  private void stopScript() {
  	if (theThread != null) {
		theThread.setRunning(false);
		theThread = null;
  	}
  }
    
}
