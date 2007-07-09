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
package org.simbrain.world.odorworld;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.event.InternalFrameEvent;

import org.simbrain.network.NetworkComponent;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.CouplingContainer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simnet.interfaces.RootNetwork;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;


/**
 * <b>WorldPanel</b> is the container for the world component.   Handles toolbar buttons, and serializing of world
 * data.  The main environment codes is in {@link OdorWorldPanel}.
 */
public class OdorWorldComponent extends WorkspaceComponent implements ActionListener {

    /** Current file. */
    private File currentFile = null;

    /** Allows the world to be scrolled if it is bigger than the display window. */
    private JScrollPane worldScroller = new JScrollPane();

    /** Odor world to be in frame. */
    private OdorWorldPanel worldPanel;

    /** Odor world frame menu. */
    private OdorWorldFrameMenu menu;

    /**
     * Default constructor.
     */
    public OdorWorldComponent() {
        super();
        init();
    }

    /**
     * Initializes frame.
     */
    public void init() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add("Center", worldScroller);
        worldPanel = new OdorWorldPanel(this);
        worldPanel.resize();
        worldScroller.setViewportView(worldPanel);
        worldScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        worldScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        worldScroller.setEnabled(false);
        menu = new OdorWorldFrameMenu(this);
        menu.setUpMenus();
    }

    /**
     * Return the current file.
     *
     * @return Current file
     */
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * Return the odor world.
     *
     * @return Odor world
     */
    public OdorWorldPanel getWorldPanel() {
        return worldPanel;
    }

    public boolean openWorld() {
       SFileChooser chooser = new SFileChooser(this.getCurrentDirectory(), this.getFileExtension());
       File theFile = chooser.showOpenDialog();

       if (theFile != null) {
           open(theFile);
           setCurrentDirectory(chooser.getCurrentLocation());
           return true;
       }
       return false;
    }

    /**
     * Read a world.
     *
     * @param theFile the wld file containing world information
     */
    public void open(final File theFile) {
        currentFile = theFile;
        setName(theFile.getName());
        worldPanel.setParentFrame(this);

        FileReader reader;
        try {
            reader = new FileReader(theFile);
            worldPanel.setWorld((OdorWorld) getXStream().fromXML(reader));
            worldPanel.getWorld().postUnmarshallInit();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        worldPanel.repaint();
        setName(theFile.getName());
        //OdorWorldPreferences.setCurrentDirectory(getCurrentDirectory());

        //Set Path; used in workspace persistence
        String localDir = new String(System.getProperty("user.dir"));
        setPath(Utils.getRelativePath(localDir, theFile.getAbsolutePath()));
        worldPanel.repaint();
    }

    /**
     * Opens a file-save dialog and saves world information to the specified file  Called by "Save As".
     */
    public void saveWorld() {
        SFileChooser chooser = new SFileChooser(".", getFileExtension());
        File worldFile = chooser.showSaveDialog();

        if (worldFile != null) {
            save(worldFile);
            currentFile = worldFile;
            setCurrentDirectory(chooser.getCurrentLocation());
        }
    }

    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
    private static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.setMode(XStream.ID_REFERENCES);
        xstream.omitField(OdorWorldEntity.class, "theImage");
        xstream.omitField(OdorWorldAgent.class, "effectorList");
        xstream.omitField(OdorWorldAgent.class, "sensorList");
        return xstream;
    }

    /**
     * Save a specified file  Called by "save".
     *
     * @param theFile the file to save to
     */
    public void save(final File theFile) {
        currentFile = theFile;
        String xml = getXStream().toXML(worldPanel.getWorld());
        try {
            FileWriter writer  = new FileWriter(theFile);
            writer.write(xml);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String localDir = new String(System.getProperty("user.dir"));
        setPath(Utils.getRelativePath(localDir, theFile.getAbsolutePath()));

        setName("" + theFile.getName());
        setChangedSinceLastSave(false);
    }

    /**
     * Responds to actions performed.
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
        Object e1 = e.getSource();

        if (e1 == menu.getOpenItem()) {
            openWorld();
            this.setChangedSinceLastSave(false);
        } else if (e1 == menu.getSaveItem()) {
            if (currentFile == null) {
                saveWorld();
            } else {
                save(currentFile);
            }
        } else if (e1 == menu.getSaveAsItem()) {
            saveWorld();
        } else if (e1 == menu.getPrefsItem()) {
            worldPanel.showGeneralDialog();
            this.setChangedSinceLastSave(true);
        } else if (e1 == menu.getScriptItem()) {
            worldPanel.showScriptDialog();
        } else if (e1 == menu.getClose()) {
            if (isChangedSinceLastSave()) {
                hasChanged();
            } else {
                dispose();
            }
        } else if (e1 == menu.getHelpItem()) {
            Utils.showQuickRef("World.html");
        }
    }

    /**
     * Tasks to peform when frame is opened.
     * @param e Internal frame event
     */
    public void internalFrameOpened(final InternalFrameEvent e) {
    }

    /**
     * Tasks to perform when frame is closing.
     * @param e Internal frame event
     */
    public void internalFrameClosing(final InternalFrameEvent e) {
        if (isChangedSinceLastSave()) {
            hasChanged();
        } else {
            dispose();
        }
    }

    /**
     * Return the arraylist of agents.
     *
     * @return List of agents
     */
    public ArrayList getAgentList() {
        return worldPanel.getWorld().getAgentList();
    }

    /**
     * @return Odor world frame menu.
     */
    public OdorWorldFrameMenu getMenu() {
        return menu;
    }

    /**
     * Sets odor world frame menu.
     * @param menu Menu
     */
    public void setMenu(final OdorWorldFrameMenu menu) {
        this.menu = menu;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

    @Override
    public int getDefaultWidth() {
        return 300;
    }

    @Override
    public int getDefaultHeight() {
        return 300;
    }

    @Override
    public String getFileExtension() {
        return "wld";
    }

    @Override
    public int getWindowIndex() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public CouplingContainer getCouplingContainer() {
        return worldPanel.getWorld();
    }
}
