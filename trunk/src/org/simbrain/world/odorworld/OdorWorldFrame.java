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
package org.simbrain.world.odorworld;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.ArrayList;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.util.LocalConfiguration;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simbrain.workspace.Workspace;


/**
 * <b>WorldPanel</b> is the container for the world component.   Handles toolbar buttons, and serializing of world
 * data.  The main environment codes is in {@link OdorWorld}.
 */
public class OdorWorldFrame extends JInternalFrame implements ActionListener, InternalFrameListener {
    /** File system seperator. */
    private static final String FS = System.getProperty("file.separator");
    /** Current file. */
    private File currentFile = null;
    /** Current directore. */
    private String currentDirectory = OdorWorldPreferences.getCurrentDirectory();
    /** Allows the world to be scrolled if it is bigger than the display window. */
    private JScrollPane worldScroller = new JScrollPane();
    /** Workspace containing frame. */
    private Workspace workspace;
    /** Odor world to be in frame. */
    private OdorWorld world;
    /** Odor world frame menu. */
    private OdorWorldFrameMenu menu;

    // For workspace persistence
    private String path;
    /** X position of frame. */
    private int xpos;
    /** Y position of frame. */
    private int ypos;
    /** Width of frame . */
    private int theWidth;
    /** Height of frame. */
    private int theHeight;
    /** Has frame been changed since last save. */
    private boolean changedSinceLastSave = false;

    /**
     * Default constructor.
     */
    public OdorWorldFrame() {
    }

    /**
     * Construct a new world panel.  Set up the toolbars.  Create an  instance of a world object.
     * @param ws the workspace associated with this frame
     */
    public OdorWorldFrame(final Workspace ws) {
        workspace = ws;
        init();
    }

    /**
     * Initializes frame.
     */
    public void init() {
        this.setResizable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setClosable(true);
        this.addInternalFrameListener(this);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add("Center", worldScroller);
        world = new OdorWorld(this);
        world.resize();
        worldScroller.setViewportView(world);
        worldScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        worldScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        worldScroller.setEnabled(false);

        setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);

        menu = new OdorWorldFrameMenu(this);

        menu.setUpMenus();

        setVisible(true);
    }

    /**
     * @return Current file.
     */
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * @return Odor world.
     */
    public OdorWorld getWorld() {
        return world;
    }

    /**
     * Show the dialog for choosing a world to open.
     */
    public void openWorld() {
        SFileChooser chooser = new SFileChooser(currentDirectory, "xml");
        File theFile = chooser.showOpenDialog();

        if (theFile != null) {
            readWorld(theFile);
            currentDirectory = chooser.getCurrentLocation();
        }
    }

    /**
     * Read a world from a world-xml file.
     *
     * @param theFile the xml file containing world information
     */
    public void readWorld(final File theFile) {
        currentFile = theFile;

        try {
            Reader reader = new FileReader(theFile);
            Mapping map = new Mapping();
            map.loadMapping("." + FS + "lib" + FS + "world_mapping.xml");

            Unmarshaller unmarshaller = new Unmarshaller(world);
            unmarshaller.setMapping(map);

//            unmarshaller.setDebug(true);
            this.getWorkspace().removeAgentsFromCouplings(world);
            world.clear();
            world = (OdorWorld) unmarshaller.unmarshal(reader);
            world.init();
            world.setParentFrame(this);
        } catch (java.io.FileNotFoundException e) {
            JOptionPane.showMessageDialog(
                                          null, "Could not find network file \n" + theFile, "Warning",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();

            return;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                                          null, "There was a problem opening file \n" + theFile, "Warning",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();

            return;
        }

        getWorkspace().attachAgentsToCouplings();
        setName(theFile.getName());

        //Set Path; used in workspace persistence
        String localDir = new String(System.getProperty("user.dir"));
        setPath(Utils.getRelativePath(localDir, theFile.getAbsolutePath()));
        world.repaint();
    }

    /**
     * Opens a file-save dialog and saves world information to the specified file  Called by "Save As".
     */
    public void saveWorld() {
        SFileChooser chooser = new SFileChooser(currentDirectory, "xml");
        File worldFile = chooser.showSaveDialog();

        if (worldFile != null) {
            saveWorld(worldFile);
            currentFile = worldFile;
            currentDirectory = chooser.getCurrentLocation();
        }
    }

    /**
     * Save a specified file  Called by "save".
     *
     * @param worldFile the file to save to
     */
    public void saveWorld(final File worldFile) {
        currentFile = worldFile;

        LocalConfiguration.getInstance().getProperties().setProperty("org.exolab.castor.indent", "true");

        try {
            FileWriter writer = new FileWriter(worldFile);
            Mapping map = new Mapping();
            map.loadMapping("." + FS + "lib" + FS + "world_mapping.xml");

            Marshaller marshaller = new Marshaller(writer);
            marshaller.setMapping(map);

            //marshaller.setDebug(true);
            marshaller.marshal(world);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String localDir = new String(System.getProperty("user.dir"));
        setPath(Utils.getRelativePath(localDir, worldFile.getAbsolutePath()));

        setName("" + worldFile.getName());
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
            changedSinceLastSave = false;
        } else if (e1 == menu.getSaveItem()) {
            if (currentFile == null) {
                saveWorld();
            } else {
                saveWorld(currentFile);
            }
        } else if (e1 == menu.getSaveAsItem()) {
            saveWorld();
        } else if (e1 == menu.getPrefsItem()) {
            world.showGeneralDialog();
            changedSinceLastSave = true;
        } else if (e1 == menu.getScriptItem()) {
            world.showScriptDialog();
        } else if (e1 == menu.getClose()) {
            if (isChangedSinceLastSave()) {
                hasChanged();
            } else {
                dispose();
            }
        } else if (e1 == menu.getHelpItem()) {
            Utils.showQuickRef(this);
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
     * Tasks to perform when frame is closed.
     * @param e Internal frame event
     */
    public void internalFrameClosed(final InternalFrameEvent e) {
        this.getWorkspace().removeAgentsFromCouplings(this.getWorld());
        this.getWorkspace().getOdorWorldList().remove(this);

        OdorWorldFrame odo = workspace.getLastOdorWorld();

        if (odo != null) {
            odo.grabFocus();
            workspace.repaint();
        }

        OdorWorldPreferences.setCurrentDirectory(currentDirectory);
    }

    /**
     * Tasks to perform when frame is iconified.
     * @param e Internal frame event
     */
    public void internalFrameIconified(final InternalFrameEvent e) {
    }

    /**
     * Tasks to peform when frame is deiconified.
     * @param e Internal frame event
     */
    public void internalFrameDeiconified(final InternalFrameEvent e) {
    }

    /**
     * Tasks to perform when frame is activated.
     * @param e Internal frame event
     */
    public void internalFrameActivated(final InternalFrameEvent e) {
    }

    /**
     * Tasks to perform when frame is deactivated.
     * @param e Internal frame event
     */
    public void internalFrameDeactivated(final InternalFrameEvent e) {
    }

    /**
     * @param path The path to set; used in persistence.
     */
    public void setPath(final String path) {
        String thePath = path;

        if (thePath.charAt(2) == '.') {
            thePath = path.substring(2, path.length());
        }

        thePath = thePath.replace(System.getProperty("file.separator").charAt(0), '/');
        this.path = thePath;
    }

    /**
     * @return path information; used in persistence
     */
    public String getPath() {
        return path;
    }

    /**
     * @return platform-specific path
     */
    public String getGenericPath() {
        String ret = path;

        if (path == null) {
            return null;
        }

        ret.replace('/', System.getProperty("file.separator").charAt(0));

        return ret;
    }

    /**
     * @return Returns the workspace.
     */
    public Workspace getWorkspace() {
        return workspace;
    }

    /**
     * @param workspace The workspace to set.
     */
    public void setWorkspace(final Workspace workspace) {
        this.workspace = workspace;
    }

    /**
     * For Castor.  Turn Component bounds into separate variables.
     */
    public void initBounds() {
        xpos = this.getX();
        ypos = this.getY();
        theWidth = this.getBounds().width;
        theHeight = this.getBounds().height;
    }

    /**
     * @return Returns the xpos.
     */
    public int getXpos() {
        return xpos;
    }

    /**
     * @param xpos The xpos to set.
     */
    public void setXpos(final int xpos) {
        this.xpos = xpos;
    }

    /**
     * @return Returns the ypos.
     */
    public int getYpos() {
        return ypos;
    }

    /**
     * @param ypos The ypos to set.
     */
    public void setYpos(final int ypos) {
        this.ypos = ypos;
    }

    /**
     * @return Returns the theheight.
     */
    public int getTheHeight() {
        return theHeight;
    }

    /**
     * @param theHeight The theHeight to set.
     */
    public void setTheHeight(final int theHeight) {
        this.theHeight = theHeight;
    }

    /**
     * @return Returns the theWidth.
     */
    public int getTheWidth() {
        return theWidth;
    }

    /**
     * @param theWidth The theWidth to set.
     */
    public void setTheWidth(final int theWidth) {
        this.theWidth = theWidth;
    }

    /**
     * @return List of agents.
     */
    public ArrayList getAgentList() {
        return world.getAgentList();
    }

    /**
     * Sets the name of an agent.
     * @param name Name of agent
     */
    public void setName(final String name) {
        setTitle(name);
        world.setName(name);
    }

    /**
     * Checks to see if anything has changed and then offers to save if true.
     */
    private void hasChanged() {
        Object[] options = {"Save", "Don't Save", "Cancel" };
        int s = JOptionPane.showInternalOptionDialog(
                                     this,
                                     "This World has changed since last save,\nWould you like to save these changes?",
                                     "World Has Changed", JOptionPane.YES_NO_CANCEL_OPTION,
                                     JOptionPane.WARNING_MESSAGE, null, options, options[0]);

        if (s == 0) {
            saveWorld();
            dispose();
        } else if (s == 1) {
            dispose();
        } else if (s == 2) {
            return;
        }
    }

    /**
     * @return Returns changedSinceLastSave.
     */
    public boolean isChangedSinceLastSave() {
        return changedSinceLastSave;
    }

    /**
     * @param hasChangedSinceLastSave The changedSinceLastSave to set.
     */
    public void setChangedSinceLastSave(final boolean hasChangedSinceLastSave) {
        this.changedSinceLastSave = hasChangedSinceLastSave;
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
}
