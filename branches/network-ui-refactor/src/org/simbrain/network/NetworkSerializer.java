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
package org.simbrain.network;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.Iterator;

import javax.swing.JOptionPane;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.util.LocalConfiguration;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.network.nodes.SynapseNode;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;

import edu.umd.cs.piccolo.PNode;

/**
 * <b>NetworkSerializer</b> contains the code for reading and writing network files.
 */
class NetworkSerializer {

    /** Whether the xml files should use tabs or not. */
    private boolean isUsingTabs = true;

    /** File Separator constant. */
    public static final String FS = System.getProperty("file.separator");

    /** Reference to <code>NetworkPanel</code> this is serializing. */
    private NetworkPanel parentPanel;

    /** Current directory for browsing networks. */
    private String currentDirectory = NetworkPreferences.getCurrentDirectory();

    /** Current network file. */
    private File currentFile = null;

    /**
     * Construct the serializer object.
     *
     * @param parent reference to the panel containing the network to be saved
     */
    public NetworkSerializer(final NetworkPanel parent) {
        parentPanel = parent;
    }

    /**
     * Show the dialog for choosing a network to open.
     */
    public void showOpenFileDialog() {
        SFileChooser chooser = new SFileChooser(currentDirectory, "xml");
        File theFile = chooser.showOpenDialog();

        if (theFile == null) {
            return;
        }

        readNetwork(theFile);
        currentDirectory = chooser.getCurrentLocation();
        NetworkPreferences.setCurrentDirectory(currentDirectory.toString());
    }

    /**
     * Read a network in from a <code>File</code> object.
     *
     * @param f file to read.
     */
    public void readNetwork(final File f) {
        currentFile = f;

        try {
            Reader reader = new FileReader(f);
            Mapping map = new Mapping();
            map.loadMapping("." + FS + "lib" + FS + "network_mapping.xml");

            Unmarshaller unmarshaller = new Unmarshaller(parentPanel);
            unmarshaller.setMapping(map);
            //unmarshaller.setDebug(true);
            parentPanel.resetNetwork();
            parentPanel = (NetworkPanel) unmarshaller.unmarshal(reader);
            initializeNetworkPanel();

            //Set Path; used in workspace persistence
            String localDir = new String(System.getProperty("user.dir"));
            ((NetworkFrame) parentPanel.getNetworkFrame()).setPath(
                    Utils.getRelativePath(localDir,f.getAbsolutePath()));
        } catch (java.io.FileNotFoundException e) {
            JOptionPane.showMessageDialog(null, "Could not find the file \n" + f, "Warning", JOptionPane.ERROR_MESSAGE);

            return;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                                          null, "There was a problem opening the file \n" + f, "Warning",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();

            return;
        }

        parentPanel.repaint();
        parentPanel.getNetworkFrame().setTitle(f.getName());
    }

    /**
     * Initializes relevant NetworkPanel data after it has been unmarshalled via Castor.
     */
    private void initializeNetworkPanel() {

        parentPanel.getNetwork().init();
        parentPanel.getNetwork().addNetworkListener(parentPanel);
        parentPanel.getNetwork().setWorkspace(parentPanel.getWorkspace());

        // First add all screen elements
        Iterator nodes = parentPanel.getNodeList().iterator();
        while (nodes.hasNext()) {
            parentPanel.getLayer().addChild((PNode) nodes.next());
        }

        // Second initialize neurons, because synapses depend on them
        Iterator neurons = parentPanel.getNeuronNodes().iterator();
        while (neurons.hasNext()) {
            NeuronNode node = (NeuronNode) neurons.next();
            node.initCastor(parentPanel);
        }

        // Third init all synapses and move them to the back
        Iterator synapses = parentPanel.getSynapseNodes().iterator();
        while (synapses.hasNext()) {
            SynapseNode node = (SynapseNode) synapses.next();
            node.initCastor(parentPanel);
            node.moveToBack();
        }

        //resetGauges();
    }

    /**
     * Show the dialog for saving a network.
     */
    public void showSaveFileDialog() {
        SFileChooser chooser = new SFileChooser(currentDirectory, "xml");

        File theFile = chooser.showSaveDialog();

        if (theFile == null) {
            return;
        }

        writeNet(theFile);
        currentDirectory = chooser.getCurrentLocation();
        NetworkPreferences.setCurrentDirectory(currentDirectory.toString());
    }

    /**
     * Saves network information to the specified file.
     *
     * @param theFile the file to save the network to.
     */
    public void writeNet(final File theFile) {
        currentFile = theFile;

        try {
            if (isUsingTabs) {
                LocalConfiguration.getInstance().getProperties().setProperty("org.exolab.castor.indent", "true");
            } else {
                LocalConfiguration.getInstance().getProperties().setProperty("org.exolab.castor.indent", "false");
            }

            FileWriter writer = new FileWriter(theFile);
            Mapping map = new Mapping();
            map.loadMapping("." + FS + "lib" + FS + "network_mapping.xml");
            Marshaller marshaller = new Marshaller(writer);
            marshaller.setMapping(map);
            prepareToSave();
            //marshaller.setDebug(true);
            marshaller.marshal(parentPanel);

        } catch (Exception e) {
            e.printStackTrace();
        }

        String localDir = new String(System.getProperty("user.dir"));
        //        ((NetworkFrame) parentPanel.getParentFrame()).setPath(Utils.getRelativePath(
        //               localDir,parentPanel.getCurrentFile().getAbsolutePath()));

        parentPanel.getNetworkFrame().setTitle(theFile.getName());
    }

    /**
     * Perform operations necessary before writing <code>NetworkPanel</code> to xml.
     */
    private void prepareToSave() {
        // Fill nodeList in NetworkPanel
        parentPanel.getNodeList().clear();
        parentPanel.getNodeList().addAll(parentPanel.getPersistentNodes());

        //Update Ids
        parentPanel.getNetwork().updateIds();

    }

    /**
     * Get a reference to the current network file.
     *
     * @return a reference to the current network file
     */
    public File getCurrentFile() {
        return currentFile;
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
    public void setUsingTabs(final boolean isUsingTabs) {
        this.isUsingTabs = isUsingTabs;
    }

    /**
     * @return Returns the currentDirectory.
     */
    public String getCurrentDirectory() {
        return currentDirectory;
    }
}
