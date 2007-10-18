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
import java.awt.Dimension;
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
import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.world.dataworld.DataWorldPreferences;
import org.simnet.interfaces.RootNetwork;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;


/**
 * <b>WorldPanel</b> is the container for the world component.   Handles toolbar buttons, and serializing of world
 * data.  The main environment codes is in {@link OdorWorldPanel}.
 */
public class OdorWorldDesktopComponent extends DesktopComponent implements ActionListener {

    /** The height of the scrollbar (used for resizing). */
    private static final int SCROLLBAR_HEIGHT = 75;

    /** The width of the scrollbar (used for resizing). */
    private static final int SCROLLBAR_WIDTH = 29;

   /** Allows the world to be scrolled if it is bigger than the display window. */
    private JScrollPane worldScroller = new JScrollPane();

    /** Odor world to be in frame. */
    private OdorWorldPanel worldPanel;

    /** Odor world frame menu. */
    private OdorWorldFrameMenu menu;

    /**
     * Default constructor.
     */
    public OdorWorldDesktopComponent(OdorWorldComponent component) {
        super(component);
        init();
    }

    /**
     * Initializes frame.
     */
    public void init() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add("Center", worldScroller);
        worldPanel = new OdorWorldPanel(this);
        worldPanel.setPreferredSize(new Dimension(worldPanel.getWorld().getWorldWidth(),worldPanel.getWorld().getWorldHeight()));
        worldScroller.setViewportView(worldPanel);
        worldScroller.setEnabled(false);
        menu = new OdorWorldFrameMenu(this);
        menu.setUpMenus();
        setMaxSize();
    }


    /**
     * Sets maximum size for the parent window.
     */
    public void setMaxSize() {
        this.setMaximumSize(new Dimension(worldPanel.getWorld().getWorldWidth() + SCROLLBAR_WIDTH, worldPanel.getWorld().getWorldHeight() + SCROLLBAR_HEIGHT));
        this.setBounds(getX(), getY(), worldPanel.getWorld().getWorldWidth() + SCROLLBAR_WIDTH, worldPanel.getWorld().getWorldHeight() + SCROLLBAR_HEIGHT);
        worldPanel.setPreferredSize(new Dimension(worldPanel.getWorld().getWorldWidth(), worldPanel.getWorld().getWorldHeight()));
    }

    /**
     * Return the odor world.
     *
     * @return Odor world
     */
    public OdorWorldPanel getWorldPanel() {
        return worldPanel;
    }

    /**
     * Read a world.
     *
     * @param theFile the wld file containing world information
     */
    public void open(final File theFile) {
        this.setCurrentFile(theFile);
        worldPanel.setParentFrame(this);
        FileReader reader;
        try {
            reader = new FileReader(theFile);
            worldPanel.setWorld((OdorWorld) getXStream().fromXML(reader));
            worldPanel.getWorld().postUnmarshallInit();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        setStringReference(theFile);
        worldPanel.repaint();
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
        xstream.omitField(OdorWorld.class, "couplings");
        return xstream;
    }

    /**
     * Save a specified file  Called by "save".
     *
     * @param theFile the file to save to
     */
    public void save(final File theFile) {
        setCurrentFile(theFile);
        String xml = getXStream().toXML(worldPanel.getWorld());
        try {
            FileWriter writer  = new FileWriter(theFile);
            writer.write(xml);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setStringReference(theFile);
        setChangedSinceLastSave(false);
    }

    /**
     * Responds to actions performed.
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
        Object e1 = e.getSource();

        if (e1 == menu.getOpenItem()) {
            showOpenFileDialog();
        } else if (e1 == menu.getSaveItem()) {
            save();
        } else if (e1 == menu.getSaveAsItem()) {
            showSaveFileDialog();
        } else if (e1 == menu.getPrefsItem()) {
            worldPanel.showGeneralDialog();
            this.setChangedSinceLastSave(true);
        } else if (e1 == menu.getScriptItem()) {
            worldPanel.showScriptDialog();
        } else if (e1 == menu.getHelpItem()) {
            Utils.showQuickRef("World.html");
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
    public String getFileExtension() {
        return "wld";
    }

    @Override
    public void setCurrentDirectory(final String currentDirectory) {        
        super.setCurrentDirectory(currentDirectory);
        OdorWorldPreferences.setCurrentDirectory(currentDirectory);
    }

    @Override
    public String getCurrentDirectory() {
        return OdorWorldPreferences.getCurrentDirectory();
    }

    /**
     * {@inheritDoc}
     */
    public CouplingContainer getCouplingContainer() {
        return worldPanel.getWorld();
    }
}
