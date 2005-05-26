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

package org.simbrain.network.old;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.util.LocalConfiguration;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.simbrain.network.NetworkFrame;
import org.simbrain.network.NetworkPanel;
import org.simbrain.network.pnodes.PNodeNeuron;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;
import org.simnet.neurons.StandardNeuron;
import org.simnet.synapses.StandardSynapse;

import com.Ostermiller.util.CSVParser;

import edu.umd.cs.piccolo.PNode;

/**
 * <b>NetworkSerializer </b> contains the code for reading and writing network
 * files
 */
public class NetworkSerializer {

	private boolean isUsingTabs = true;

	public static final String FS = "/"; // System.getProperty("file.separator");Separator();

	private NetworkPanel parent_panel;
	
	private String currentDirectory = "./simulations/networks";

	private static File current_file = null;

	// Number of neuron and weight parameters
	public static final int NEURON_PARAMS = Neuron.NUM_PARAMETERS;

	public static final int WEIGHT_PARAMS = Synapse.NUM_PARAMETERS;

	/**
	 * Construct the serializer object
	 * 
	 * @param parent
	 *            reference to the panel containing the network to be saved
	 */
	public NetworkSerializer(NetworkPanel parent) {
		parent_panel = parent;
	}

	/**
	 * Show the dialog for choosing a network to open
	 */
	public void showOpenFileDialog() {

		SFileChooser chooser = new SFileChooser(currentDirectory, "xml");
		File theFile = chooser.showOpenDialog();
		if (theFile == null) {
			return;
		}
		readNetwork(theFile);
		currentDirectory = chooser.getCurrentLocation();
	}
	
	public void readNetwork(File f) {
		current_file = f;
		try {
			Reader reader = new FileReader(f);
			Mapping map = new Mapping();
			map.loadMapping("." + FS + "lib" + FS + "network_mapping.xml");
			Unmarshaller unmarshaller = new Unmarshaller(parent_panel);
			unmarshaller.setMapping(map);
			unmarshaller.setDebug(false);

			parent_panel.getNodeList().clear();
			parent_panel.getLayer().removeAllChildren();
			parent_panel.resetNetwork();
			parent_panel = (NetworkPanel) unmarshaller.unmarshal(reader);
			parent_panel.initCastor();
			parent_panel.renderObjects();
			parent_panel.repaint();
			
			//Set Path; used in workspace persistence
			String localDir = new String(System.getProperty("user.dir"));
			((NetworkFrame)parent_panel.getParentFrame()).setPath(Utils.getRelativePath(localDir, parent_panel.getCurrentFile().getAbsolutePath()));

			
		} catch (java.io.FileNotFoundException e) {
		    JOptionPane.showMessageDialog(null, "Could not read network file \n"
			        + f, "Warning", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();       
		    return;
		} catch (NullPointerException e){
		    JOptionPane.showMessageDialog(null, "Could not find network file \n"
			        + f, "Warning", JOptionPane.ERROR_MESSAGE);
		    return;
		}
		catch (Exception e){
		    e.printStackTrace();
		    return;
		}
		parent_panel.getParentFrame().setTitle(f.getName());

	}
	
	/**
	 * Place an existing network into the current network
	 * 
	 * @param f the file containing the network to be placed
	 */
	public void placeNetwork(File f) {
		try {
			Reader reader = new FileReader(f);
			Mapping map = new Mapping();
			map.loadMapping("." + FS + "lib" + FS + "network_mapping.xml");
			NetworkPanel np = new NetworkPanel();
			Unmarshaller unmarshaller = new Unmarshaller(np);
			unmarshaller.setMapping(map);
			np = (NetworkPanel) unmarshaller.unmarshal(reader);
			np.initCastor();
			parent_panel.placeNetwork(np);
		} catch (Exception e) {
			e.printStackTrace();
		}	
		
	}
	


	/**
	 * Show the dialog for choosing a network to place
	 */
	public void showPlaceFileDialog() {

		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File("." + FS + "simulations" + FS
				+ "networks"));
		int result = chooser.showOpenDialog(parent_panel);
		if (result != JFileChooser.APPROVE_OPTION) {
			return;
		}
		placeNetwork(chooser.getSelectedFile());
		parent_panel.repaint();
	}
	

	/**
	 * Creates a neural network based on .net text file
	 * 
	 * @param theFile
	 *            the network file to be read
	 */
	public void readNet(File theFile) {
		current_file = theFile;
		FileInputStream f = null;
		String line = null;
		String[][] values = null;
		CSVParser theParser = null;

		try {
			theParser = new CSVParser(f = new FileInputStream(theFile), "", "",
					"#"); // # is a comment delimeter in net files
			values = theParser.getAllValues();
		} catch (Exception e) {
			//System.out.println("Could not open file stream: " +
			// e.toString());
			JOptionPane.showMessageDialog(null,
					"Could not find network file \n" + theFile, "Warning",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		parent_panel.deleteAll();
		parent_panel.getCamera().setVisible(false); // prevent user from seeing
													// the network be drawn

		// Read in neurons
		for (int i = 0; i < values.length; i++) {
			// If the second item is not "n.." this is a neuron.. TODO: Better
			// way to determine this is a neuron line?
			if (!values[i][1].startsWith("n")) {
				if (values[i].length < NEURON_PARAMS + 2) {
					values[i] = fillArray(values[i], NEURON_PARAMS + 2);
				}
				String[] neuron_values = stripArray(values[i]);
				parent_panel.addNeuron(Integer.parseInt(values[i][1]), // x
																	   // coordinate
				Integer.parseInt(values[i][2]), // y coordinate
				new StandardNeuron(neuron_values)); // logical Neuron
			}
		}

		parent_panel.getNetwork().updateInOut();

		// Read in weights
		for (int i = 0; i < values.length; i++) { // This is a weight
			if (values[i][1].startsWith("n")) {
				if (values[i].length < WEIGHT_PARAMS) {
					values[i] = fillArray(values[i], WEIGHT_PARAMS);
				}
				Synapse w = new StandardSynapse(values[i], new String("w" + i));
				PNodeNeuron src = findNeuronByName(values[i][0]);
				PNodeNeuron tar = findNeuronByName(values[i][1]);

				parent_panel.addWeight(src, tar, w);
			}
		}

		parent_panel.getParentFrame().setTitle(theFile.getName());
		parent_panel.resetNetwork();
		parent_panel.getCamera().setVisible(true);
		parent_panel.centerCamera();

	}

	/**
	 * Helper method for readNet. Associates names of neurons with references to
	 * the PNodeNeurons named. Used when reading in weights from a network file
	 * 
	 * @param name
	 *            name of a neuron in the network file
	 * @return reference to neuron of that name
	 */
	public PNodeNeuron findNeuronByName(String name) {
		PNodeNeuron ret = null;
		ArrayList node_list = parent_panel.getNodeList();
		for (int i = 0; i < node_list.size(); i++) {
			PNode node = (PNode) node_list.get(i);
			if (node instanceof PNodeNeuron) {
				if (((PNodeNeuron) node).getNeuron().getId().equals(name)) {
					ret = (PNodeNeuron) node;
					break;
				}
			}
		}
		return ret;
	}

	/**
	 * Helper method. Removes the second and third components of the neuron
	 * parameters (x and y coordinates) so that what are sent to the Neuron
	 * constructor are only neuron paramteres, without GUI stuff.
	 * 
	 * @param array
	 *            array to be stripped
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
	 * elements of the array to null. Used when old .net files are read in,
	 * which don't have newer fields (those extra fields get set to null)
	 * 
	 * @param toFill
	 *            the string to be expanded
	 * @param len
	 *            the desired length of the string array
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

		SFileChooser chooser = new SFileChooser(currentDirectory, "xml");

		File theFile = chooser.showSaveDialog();
		if (theFile == null) {
			return;
		}

		writeNet(theFile);
		currentDirectory = chooser.getCurrentLocation();
	}

	/**
	 * Saves network information to the specified file
	 */
	public void writeNet(File theFile) {

		current_file = theFile;

		try {
			if (isUsingTabs == true) {
				LocalConfiguration.getInstance().getProperties().setProperty(
						"org.exolab.castor.indent", "true");
			} else {
				LocalConfiguration.getInstance().getProperties().setProperty(
						"org.exolab.castor.indent", "false");
			}
			FileWriter writer = new FileWriter(theFile);
			Mapping map = new Mapping();
			map.loadMapping("." + FS + "lib" + FS + "network_mapping.xml");
			Marshaller marshaller = new Marshaller(writer);
			marshaller.setMapping(map);
			//marshaller.setDebug(true);
			parent_panel.getNetwork().updateIds();
			parent_panel.updateIds();
			marshaller.marshal(parent_panel);
			

		} catch (Exception e) {
			e.printStackTrace();
		}
		parent_panel.getParentFrame().setTitle(theFile.getName());
		//System.gc();

	}

	/**
	 * @return Returns the isUsingTabs.
	 */
	public boolean isUsingTabs() {
		return isUsingTabs;
	}
	/**
	 * @param isUsingTabs The isUsingTabs to set.
	 */
	public void setUsingTabs(boolean isUsingTabs) {
		this.isUsingTabs = isUsingTabs;
	}
}