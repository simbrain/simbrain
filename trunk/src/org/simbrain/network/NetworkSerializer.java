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
package org.simbrain.network;

import java.io.File;

import org.simbrain.gauge.GaugeFrame;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simnet.interfaces.RootNetwork;

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
     *
     * @return true if file exists
     */
    public boolean showOpenFileDialog() {
        SFileChooser chooser = new SFileChooser(currentDirectory, "net");
        File theFile = chooser.showOpenDialog();

        if (theFile == null) {
            return false;
        }

        readNetwork(theFile);
        currentDirectory = chooser.getCurrentLocation();
        NetworkPreferences.setCurrentDirectory(currentDirectory.toString());
        return true;
    }

    /**
     * Read a network in from a <code>File</code> object.
     *
     * @param f file to read.
     */
    public void readNetwork(final File f) {
        currentFile = f;

        // Get reference to gauge by old title.
        GaugeFrame gauge = networkPanel.getWorkspace()
                .getGaugeAssociatedWithNetwork(
                        networkPanel.getNetworkFrame().getTitle());

        // Unmarshall the model network
        networkPanel.getLayer().removeAllChildren();
        networkPanel.getNetworkFrame().setTitle(f.getName());
        networkPanel.setRootNetwork(RootNetwork.readNetwork(f));
        networkPanel.getRootNetwork().addNetworkListener(networkPanel);
        networkPanel.getRootNetwork().postUnmarshallingInit();
        networkPanel.getRootNetwork().initCouplings(networkPanel.getWorkspace());
        networkPanel.repaint();

        // Reset connected gauge, if any
        if (gauge != null) {
            gauge.setVariables(networkPanel.getRootNetwork().getNeuronList(), networkPanel.getNetworkFrame().getTitle());
            networkPanel.getRootNetwork().addNetworkListener(gauge);
        }
    }

    /**
     * Show the dialog for saving a network.
     */
    public void showSaveFileDialog() {
        SFileChooser chooser = new SFileChooser(currentDirectory, "net");
        File theFile = chooser.showSaveDialog();
        if (theFile != null) {
            writeNet(theFile);
            currentDirectory = chooser.getCurrentLocation();
            NetworkPreferences.setCurrentDirectory(currentDirectory.toString());
        }
    }

    /**
     * Saves network information to the specified file.
     *
     * @param theFile the file to save the network to.
     */
    public void writeNet(final File theFile) {
        currentFile = theFile;
        String localDir = new String(System.getProperty("user.dir"));
        ((NetworkFrame) networkPanel.getNetworkFrame()).setPath(Utils
                .getRelativePath(localDir, theFile.getAbsolutePath()));
        RootNetwork.writeNetwork(theFile, networkPanel.getRootNetwork());
        networkPanel.getNetworkFrame().setTitle(theFile.getName());
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
