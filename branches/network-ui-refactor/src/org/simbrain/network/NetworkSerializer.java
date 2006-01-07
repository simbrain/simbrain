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
import org.simbrain.gauge.GaugeFrame;
import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.network.nodes.SynapseNode;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;

import edu.umd.cs.piccolo.PNode;

/**
 * <b>NetworkSerializer</b> contains the code for reading and writing network files.
 */
class NetworkSerializer {

    /** File Separator constant. */
    public static final String FS = System.getProperty("file.separator");

    /** Reference to <code>NetworkPanel</code> this is serializing. */
    private NetworkPanel networkPanel;

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
        networkPanel = parent;
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

        // Get reference to gauge by old title.
        GaugeFrame gauge = networkPanel.getWorkspace().getGaugeAssociatedWithNetwork(networkPanel.getNetworkFrame().getTitle());

        try {
            Reader reader = new FileReader(f);
            Mapping map = new Mapping();
            networkPanel.getLayer().removeAllChildren();
            networkPanel.getNodeList().clear();
            map.loadMapping("." + FS + "lib" + FS + "network_mapping.xml");

            Unmarshaller unmarshaller = new Unmarshaller(networkPanel);
            unmarshaller.setIgnoreExtraElements(true);unmarshaller.setMapping(map);
            //unmarshaller.setDebug(true);
            networkPanel = (NetworkPanel) unmarshaller.unmarshal(reader);
            initializeNetworkPanel();

            //Set Path; used in workspace persistence
            String localDir = new String(System.getProperty("user.dir"));
            ((NetworkFrame) networkPanel.getNetworkFrame()).setPath(
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

        networkPanel.repaint();
        networkPanel.getNetworkFrame().setTitle(f.getName());

        // Reset connected gauge, if any
        if (gauge != null) {
            gauge.setVariables(networkPanel.getNetwork().getNeuronList(), networkPanel.getNetworkFrame().getTitle());
            networkPanel.getNetwork().addNetworkListener(gauge);
        }
    }

    /**
     * Initializes relevant NetworkPanel data after it has been unmarshalled via Castor.
     */
    private void initializeNetworkPanel() {

        networkPanel.getNetwork().init();
        networkPanel.getNetwork().addNetworkListener(networkPanel);
        networkPanel.getNetwork().setWorkspace(networkPanel.getWorkspace());

        // First add all screen elements
        Iterator nodes = networkPanel.getNodeList().iterator();
        while (nodes.hasNext()) {
            networkPanel.getLayer().addChild((PNode) nodes.next());
        }

        // Second initialize neurons, because synapses depend on them
        Iterator neurons = networkPanel.getNeuronNodes().iterator();
        while (neurons.hasNext()) {
            NeuronNode node = (NeuronNode) neurons.next();
            node.initCastor(networkPanel);
        }

        // Third init all synapses and move them to the back
        Iterator synapses = networkPanel.getSynapseNodes().iterator();
        while (synapses.hasNext()) {
            SynapseNode node = (SynapseNode) synapses.next();
            node.initCastor(networkPanel);
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
            if (networkPanel.getUsingTabs()) {
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
            marshaller.marshal(networkPanel);

        } catch (Exception e) {
            e.printStackTrace();
        }

        String localDir = new String(System.getProperty("user.dir"));
        //        ((NetworkFrame) parentPanel.getParentFrame()).setPath(Utils.getRelativePath(
        //               localDir,parentPanel.getCurrentFile().getAbsolutePath()));

        networkPanel.getNetworkFrame().setTitle(theFile.getName());
    }

    /**
     * Perform operations necessary before writing <code>NetworkPanel</code> to xml.
     */
    private void prepareToSave() {
        // Fill nodeList in NetworkPanel
        networkPanel.getNodeList().clear();
        networkPanel.getNodeList().addAll(networkPanel.getPersistentNodes());

        //Update Ids
        networkPanel.getNetwork().updateIds();

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
     * @return Returns the currentDirectory.
     */
    public String getCurrentDirectory() {
        return currentDirectory;
    }
}
