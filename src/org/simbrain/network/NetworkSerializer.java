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

package org.simbrain.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import java.io.*;
import org.exolab.castor.xml.*;
import org.exolab.castor.util.*;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.simbrain.simulation.Simulation;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;
import org.simnet.networks.StandardNetwork;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.CSVPrinter;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import edu.umd.cs.piccolo.PNode;

/**
 * <b>NetworkSerializer</b> contains the code for reading and writing network files
 */
public class NetworkSerializer {

	public static final String FS = Simulation.getFileSeparator();
	private NetworkPanel parent_panel;
	private static File current_file = null;

	// Number of neuron and weight parameters
	public static final int NEURON_PARAMS = Neuron.NUM_PARAMETERS;
	public static final int WEIGHT_PARAMS = Synapse.NUM_PARAMETERS;

	/**
	 * Construct the serializer object
	 * 
	 * @param parent reference to the panel containing the network to be saved
	 */
	public NetworkSerializer(NetworkPanel parent) {
		parent_panel = parent;
	}

	
	/**
	 * Show the dialog for choosing a network to open
	 */
	public void showOpenFileDialog() {

		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(
			new File("." + FS + "simulations" + FS + "networks"));
		int result = chooser.showOpenDialog(parent_panel);
		if (result != JFileChooser.APPROVE_OPTION) {
			return;
		}
		StandardNetwork net = new StandardNetwork();
		try {
 			Reader reader = new FileReader(chooser.getSelectedFile());
			Mapping map = new Mapping();
			map.loadMapping("." + FS + "lib" + FS + "mapping.xml");
			Unmarshaller unmarshaller = new Unmarshaller(StandardNetwork.class);
			unmarshaller.setDebug(true);
			unmarshaller.setMapping(map);
			net = (StandardNetwork)unmarshaller.unmarshal(reader);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//readNet(chooser.getSelectedFile());
		net.debug();
		parent_panel.repaint();
	}

	/**
	 * Creates a neural network based on .net text file
	 * 
	 * @param theFile the network file to be read
	 */
	public void readNet(File theFile) {
		current_file = theFile;
		FileInputStream f = null;
		String line = null;
		String[][] values = null;
		CSVParser theParser = null;

		try {
			theParser =
				new CSVParser(f = new FileInputStream(theFile), "", "", "#"); // # is a comment delimeter in net files
			values = theParser.getAllValues();
		} catch (Exception e) {
			//System.out.println("Could not open file stream: " + e.toString());
			JOptionPane.showMessageDialog(null, "Could not find network file \n" + theFile, "Warning", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		parent_panel.deleteAll();
		parent_panel.getCamera().setVisible(false); // prevent user from seeing the network be drawn

		// Read in neurons
		for (int i = 0; i < values.length; i++) {
			// If the second item is not "n.." this is a neuron.. TODO: Better way to determine this is a neuron line?			
			if (!values[i][1].startsWith("n")) {
				if (values[i].length < NEURON_PARAMS + 2) {
					values[i] = fillArray(values[i], NEURON_PARAMS + 2);
				}
				String[] neuron_values = stripArray(values[i]);
				parent_panel.addNeuron(
					Integer.parseInt(values[i][1]),	// x coordinate
					Integer.parseInt(values[i][2]), // y coordinate
					new Neuron(neuron_values));			// logical Neuron
			}
		}
		
		parent_panel.getNetwork().updateInOut();

		// Read in weights
		for (int i = 0; i < values.length; i++) { // This is a weight
			if (values[i][1].startsWith("n")) {
				if (values[i].length < WEIGHT_PARAMS) {
					values[i] = fillArray(values[i], WEIGHT_PARAMS);
				}
				Synapse w = new Synapse(values[i]);
				PNodeNeuron src = findNeuronByName(values[i][0]);
				PNodeNeuron tar = findNeuronByName(values[i][1]);

				parent_panel.addWeight(src, tar, w);
			}
		}

		parent_panel.owner.setTitle(theFile.getName());
		parent_panel.resetNetwork();
		parent_panel.getCamera().setVisible(true);
		parent_panel.centerCamera();

	}
	
	/**
	 * Helper method for readNet.  Associates names of neurons with references to the PNodeNeurons named.
	 * Used when reading in weights from a network file
	 *
	 * @param name name of a neuron in the network file
	 * @return reference to neuron of that name
	 */
	public PNodeNeuron findNeuronByName(String name) {
		PNodeNeuron ret = null;
		ArrayList node_list = parent_panel.getNodeList();
		for (int i = 0; i < node_list.size(); i++) {
			PNode node = (PNode) node_list.get(i);
			if (node instanceof PNodeNeuron) {
				if (((PNodeNeuron) node).getNeuron().getName().equals(name)) {
					ret = (PNodeNeuron) node;
					break;
				}
			}
		}
		return ret;
	}
	
	/**
	 * Helper method.  Removes the second and third components of the neuron parameters
	 *  (x and y coordinates) so that what are sent to the Neuron constructor are only neuron 
	 * paramteres, without GUI stuff. 
	 * 
	 * @param array array to be stripped
	 * @return stripped array
	 */
	public static String[] stripArray(String[] array) {
		String[] ret = new String[NEURON_PARAMS];
		int j = 0;
		for (int i = 0; i < array.length; i++, j++) {
			if (i == 1 || i == 2) {
				  j--;
				 continue; 
			} 
			ret[j] = array[i];
		}
		return ret;
	}


	/**
	 * Get a reference to the current network file
	 * 
	 * @return a reference to the current network file
	 */
	public File getCurrentFile() {
		return current_file;
	}

	/**
	 * Helper method to expand an array of strings to length len, setting extra 
	 * elements of the array to null.  Used when old .net files are read in, which 
	 * don't have newer fields (those extra fields get set to null)
	 * 
	 * @param toFill the string to be expanded
	 * @param len the desired length of the string array
	 * @return the expanded string array
	 */
	public String[] fillArray(String[] toFill, int len) {
		String[] ret = new String[len];
		int i;
		for (i = 0; i < toFill.length; i++) {
			ret[i] = toFill[i];
		}
		for (int j = i; j < len; j++) {
			ret[j] = null;
		}
		return ret;
	}


	/**
	 * Show the dialog for saving a network
	 */
	public void showSaveFileDialog() {

		String line = null;
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(
			new File("." + FS + "simulations" + FS + "networks"));

		int result = chooser.showDialog(parent_panel, "Save");
		if (result != JFileChooser.APPROVE_OPTION) {
			return;
		}

		try {

		    		LocalConfiguration.getInstance().getProperties().setProperty("org.exolab.castor.indent", "true");
            		FileWriter writer = new FileWriter(chooser.getSelectedFile());
            		Mapping map = new Mapping();
            		map.loadMapping("." + FS + "lib" + FS + "mapping.xml");
            		Marshaller marshaller = new Marshaller(writer);
            		marshaller.setMapping(map);
            		marshaller.marshal(parent_panel.getNetwork());
            
				//FileOutputStream f = new FileOutputStream((File)chooser.getSelectedFile());
				//writeNet((File)chooser.getSelectedFile());
				//NetworkFileWriter.write(f, parent_panel);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	/**
	 * Opens a file-save dialog and saves network information to the specified file
	 */
	public void writeNet(File theFile) {
		
		
		current_file = theFile;

		FileOutputStream f = null;
		try {
			f = new FileOutputStream(theFile);
		} catch (Exception e) {
			System.out.println("Could not open file stream: " + e.toString());
		}

		if (f == null) {
			return;
		}
		
		//Create network file
		PrintStream ps = new PrintStream(f);
		CSVPrinter thePrinter = new CSVPrinter(f);

		thePrinter.printlnComment("");
		thePrinter.printlnComment("File: " + theFile.getName());
		thePrinter.printlnComment("");
		thePrinter.println();

		// Save neuron information
		thePrinter.printlnComment("Neurons");
		String[] node = new String[NEURON_PARAMS+2];
		ArrayList node_list = parent_panel.getNodeList();
		for (int i = 0; i < node_list.size(); i++) {
			PNode pn = (PNode) node_list.get(i);
			if (pn instanceof PNodeNeuron) {

				Neuron n = ((PNodeNeuron) pn).getNeuron();

				node[0] = new String("n" + i);
				n.setName(node[0]);
				//Update the name of this node for serialization
				node[1] = Integer.toString((int) NetworkPanel.getGlobalX(pn));
				node[2] = Integer.toString((int) NetworkPanel.getGlobalY(pn));
				node[3] = n.getInputLabel();
				node[4] = n.getOutputLabel();
				node[5] = n.getActivationFunction().getName();
				node[6] = null;
				node[7] = Double.toString(n.getActivation());
				node[8] = Double.toString(n.getLowerBound());
				node[9] = Double.toString(n.getUpperBound());
				node[10] = Double.toString(n.getOutputSignal());
				node[11] = Double.toString(n.getOutputThreshold());
				node[12] = Double.toString(n.getActivationThreshold());
				node[13] = Double.toString(n.getIncrement());
				node[14] = Double.toString(n.getDecay());
				node[15] = Double.toString(n.getBias());
						
				thePrinter.println(node);
			}
			parent_panel.owner.setTitle(theFile.getName());
		}

		// Save weight information
		thePrinter.println();
		thePrinter.printlnComment("Weights");
		String[] weight = new String[WEIGHT_PARAMS];
		for (int i = 0; i < node_list.size(); i++) {
			PNode pn = (PNode) node_list.get(i);
			if (pn instanceof PNodeWeight) {
				Synapse w = ((PNodeWeight) pn).getWeight();
				weight[0] = ((Neuron) w.getSource()).getName();
				weight[1] = ((Neuron) w.getTarget()).getName();
				weight[2] = w.getLearningRule().getName();
				weight[3] = Double.toString(w.getStrength());
				weight[4] = Double.toString(w.getLowerBound());
				weight[5] = Double.toString(w.getUpperBound());
				weight[6] = Double.toString(w.getIncrement());
				weight[7] = Double.toString(w.getMomentum());
				thePrinter.println(weight);
			}
		}
		
		thePrinter.println();

		parent_panel.theWorld.setNetworkPanel(parent_panel);

	}


}
