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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simnet.interfaces.RootNetwork;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

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
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    private XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.setMode(XStream.ID_REFERENCES);
        xstream.omitField(RootNetwork.class, "listenerList");
        return xstream;
    }

    /**
     * Read a network in from a <code>File</code> object.
     *
     * @param theFile file to read.
     */
    public void readNetwork(final File theFile) {
        currentFile = theFile;
        networkPanel.getLayer().removeAllChildren();
        networkPanel.getNetworkFrame().setTitle(theFile.getName());

        FileReader reader;
        try {
            reader = new FileReader(theFile);
            RootNetwork net = (RootNetwork) getXStream().fromXML(reader);
            networkPanel.setRootNetwork(net);
            net.postUnmarshallingInit(networkPanel);

            String localDir = new String(System.getProperty("user.dir"));
            ((NetworkComponent) networkPanel.getNetworkFrame()).setPath(Utils
                    .getRelativePath(localDir, theFile.getAbsolutePath()));

            networkPanel.repaint();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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

        String xml = getXStream().toXML(networkPanel.getRootNetwork());
        try {
            FileWriter writer  = new FileWriter(theFile);
            writer.write(xml);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String localDir = new String(System.getProperty("user.dir"));
        ((NetworkComponent) networkPanel.getNetworkFrame()).setPath(Utils
                .getRelativePath(localDir, theFile.getAbsolutePath()));
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
