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

	private File current_file = null;

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
			//unmarshaller.setDebug(true);
			parent_panel.getParentFrame().getWorkspace().getCouplingList().removeCouplings(parent_panel);
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