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
package org.simbrain.world.dataworld;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simbrain.workspace.Workspace;


/**
 * <b>DataWorldFrame</b> is a "spreadsheet world" used to send rows of raw data to input nodes.
 */
public class DataWorldFrame extends JInternalFrame implements ActionListener, InternalFrameListener, MenuListener {

    /** Current file. */
    private File currentFile = null;

    /** Current directory. */
    private String currentDirectory = DataWorldPreferences.getCurrentDirectory();

    /** World scroll pane. */
    private JScrollPane worldScroller = new JScrollPane();

    /** Workspace. */
    private Workspace workspace;

    /** Data world. */
    private DataWorld world;

    /** Path string. */
    private String path;

    /** X position. */
    private int xpos;

    /** Y position. */
    private int ypos;

    /** The width. */
    private int theWidth;

    /** The height. */
    private int theHeight;

    /** Menu bar. */
    private JMenuBar mb = new JMenuBar();

    /** File menu. */
    private JMenu file = new JMenu("File  ");

    /** Open menu item. */
    private JMenuItem open = new JMenuItem("Open");

    /** Save menu item. */
    private JMenuItem save = new JMenuItem("Save");

    /** Save as menu item. */
    private JMenuItem saveAs = new JMenuItem("Save as");

    /** Close menu item. */
    private JMenuItem close = new JMenuItem("Close");

    /** Edit menu item. */
    private JMenu edit = new JMenu("Edit");

    /** Add row menu item. */
    private JMenuItem addRow = new JMenuItem("Add a row");

    /** Add column menu item. */
    private JMenuItem addCol = new JMenuItem("Add a column");

    /** Zero fill menu item. */
    private JMenuItem zeroFill = new JMenuItem("ZeroFill the Table");

    /** Remove row menu item. */
    private JMenuItem remRow = new JMenuItem("Remove a row");

    /** Remove column menu item. */
    private JMenuItem remCol = new JMenuItem("Remove a column");

    /** Randomize menu item. */
    private JMenuItem randomize = new JMenuItem("Randomize");

    /** Random properties menu item. */
    private JMenuItem randomProps = new JMenuItem("Adjust Randomization Bounds");

    /** Determines whether table is in iteration mode. */
    private JCheckBoxMenuItem iterationMode = new JCheckBoxMenuItem("Iteration mode");

    /** Determines whether iteration mode uses last column. */
    private JCheckBoxMenuItem columnIteration = new JCheckBoxMenuItem("Use last column");


    /** Changed since last save boolean. */
    private boolean changedSinceLastSave = false;

    /**
     * This method is the default constructor.
     */
    public DataWorldFrame() {
    }

    /**
     * Construct a new world panel.  Set up the toolbars.  Create an  instance of a world object.
     *
     * @param ws Workspace
     */
    public DataWorldFrame(final Workspace ws) {
        workspace = ws;
        init();
    }

    /**
     * Initilaizes items needed to create frame.
     */
    public void init() {
        this.checkIterationMode();
        this.setResizable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setClosable(true);
        this.addInternalFrameListener(this);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add("Center", worldScroller);
        world = new DataWorld(this);
        addMenuBar(world);
        getContentPane().add(world.getTable().getTableHeader(), BorderLayout.PAGE_START);
        worldScroller.setViewportView(world);
        worldScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        worldScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        worldScroller.setEnabled(false);

        this.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);

        this.pack();

        setVisible(true);
    }

    /**
     * Creates the Menu Bar and adds it to the frame.
     *
     * @param table Table to be used for world
     */
    public void addMenuBar(final DataWorld table) {
        open.addActionListener(this);
        open.setActionCommand("open");
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit
                .getDefaultToolkit().getMenuShortcutKeyMask()));
        save.addActionListener(this);
        save.setActionCommand("save");
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit
                .getDefaultToolkit().getMenuShortcutKeyMask()));
        saveAs.addActionListener(this);
        saveAs.setActionCommand("saveAs");
        close.addActionListener(this);
        close.setActionCommand("close");
        close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit
                .getDefaultToolkit().getMenuShortcutKeyMask()));
        mb.add(file);
        file.add(open);
        file.add(save);
        file.add(saveAs);
        file.add(close);
        file.addMenuListener(this);

        addRow.addActionListener(this);
        addRow.setActionCommand("addRow");
        addCol.addActionListener(this);
        addCol.setActionCommand("addCol");
        remRow.addActionListener(this);
        remRow.setActionCommand("remRow");
        remCol.addActionListener(this);
        remCol.setActionCommand("remCol");
        zeroFill.addActionListener(this);
        zeroFill.setActionCommand("zeroFill");
        randomize.addActionListener(this);
        randomize.setActionCommand("randomize");
        randomProps.addActionListener(this);
        randomProps.setActionCommand("randomProps");
        iterationMode.addActionListener(this);
        iterationMode.setActionCommand("iterationMode");
        columnIteration.addActionListener(this);
        columnIteration.setActionCommand("columnIteration");
        edit.add(addRow);
        edit.add(addCol);
        edit.add(zeroFill);
        edit.addSeparator();
        edit.add(remRow);
        edit.add(remCol);
        edit.addSeparator();
        edit.add(randomize);
        edit.add(randomProps);
        edit.addSeparator();
        edit.add(iterationMode);
        edit.add(columnIteration);
        mb.add(edit);

        setJMenuBar(mb);
    }

    /**
     * Checks iteration mode to enable or disable column iteration.
     */
    private void checkIterationMode() {
        if (iterationMode.getState()) {
            columnIteration.setEnabled(true);
        } else {
            columnIteration.setEnabled(false);
        }
    }

    /**
     * @return The current file.
     */
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * @return The data world.
     */
    public DataWorld getWorld() {
        return world;
    }

    /**
     * Show the dialog for choosing a world to open.
     *
     * @return true if file exists
     */
    public boolean openWorld() {
        SFileChooser chooser = new SFileChooser(currentDirectory, "csv");
        File theFile = chooser.showOpenDialog();

        if (theFile != null) {
            readWorld(theFile);
            currentDirectory = chooser.getCurrentLocation();
            return true;
        }
        return false;
    }

    /**
     * Read a world from a world-xml file.
     *
     * @param theFile the xml file containing world information
     */
    public void readWorld(final File theFile) {
        currentFile = theFile;
        String[][] data = Utils.getStringMatrix(theFile);

        /* String[][] dataTemp = Utils.getStringMatrix(theFile);

        String[] names = new String[dataTemp.length];

        String[][] data = new String[dataTemp.length][dataTemp[0].length - 1];

        for (int i = 0; i < dataTemp.length; i++) {
            names[i] = dataTemp[i][0];

            for (int j = 1; j < dataTemp[0].length; j++) {
                data[i][j - 1] = dataTemp[i][j];
            }
        } */

        world.resetModel(data);

        //world.setButtonNames(names);

        getWorkspace().attachAgentsToCouplings();
        setWorldName(theFile.getName());

        //Set Path; used in workspace persistence
        String localDir = new String(System.getProperty("user.dir"));
        setPath(Utils.getRelativePath(localDir, theFile.getAbsolutePath()));
    }

    /**
     * Opens a file-save dialog and saves world information to the specified file.
     * Called by "Save As."
     */
    public void saveWorld() {
        SFileChooser chooser = new SFileChooser(currentDirectory, "csv");
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
     * @param worldFile File to save world
     */
    public void saveWorld(final File worldFile) {
        currentFile = worldFile;

        String[][] data = new String[world.getTable().getRowCount()][world.getTable().getColumnCount() - 1];

        for (int i = 0; i < world.getTable().getRowCount(); i++) {
            for (int j = 0; j < world.getTable().getColumnCount() - 1; j++) {
                data[i][j] = new String("" + world.getTable().getValueAt(i, j + 1));
            }
        }

        Utils.writeMatrix(data, currentFile);

        String localDir = new String(System.getProperty("user.dir"));
        String path = Utils.getRelativePath(localDir, worldFile.getAbsolutePath());
        if (path != null) {
            setPath(path);
        } else {
            setPath(worldFile.getName());
        }

        setWorldName(worldFile.getName());

        setChangedSinceLastSave(false);
    }

    /**
     * Responds to internal frame opened event.
     *
     * @param e Internal frame event
     */
    public void internalFrameOpened(final InternalFrameEvent e) {
    }

    /**
     * Responds to internal frame closing event.
     *
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
     * Responds to internal frame closed event.
     *
     * @param e Internal frame event
     */
    public void internalFrameClosed(final InternalFrameEvent e) {
        this.getWorkspace().removeAgentsFromCouplings(this.getWorld());
        this.getWorkspace().getDataWorldList().remove(this);

        DataWorldFrame dat = workspace.getLastDataWorld();

        if (dat != null) {
            dat.grabFocus();
            workspace.repaint();
        }

        DataWorldPreferences.setCurrentDirectory(currentDirectory);
    }

    /**
     * Responds to internal frame iconified event.
     *
     * @param e Internal frame event
     */
    public void internalFrameIconified(final InternalFrameEvent e) {
    }

    /**
     * Responds to internal frame deiconified event.
     *
     * @param e Internal frame event
     */
    public void internalFrameDeiconified(final InternalFrameEvent e) {
    }

    /**
     * Responds to internal frame activated event.
     *
     * @param e Internal frame event
     */
    public void internalFrameActivated(final InternalFrameEvent e) {
    }

    /**
     * Responds to internal frame deactivated event.
     *
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
     * @return Returns the theHeight.
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
     * Sets the name of the world.
     *
     * @param name The value to be set
     */
    public void setWorldName(final String name) {
        setTitle(name);
        world.setWorldName(name);
    }

    /**
     * Responds to actions performed.
     *
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
        if (e.getActionCommand().equals("open")) {
            openWorld();
            changedSinceLastSave = false;
        } else if (e.getActionCommand().equals("save")) {
            if (currentFile == null) {
                saveWorld();
            } else {
                saveWorld(currentFile);
            }
        } else if (e.getActionCommand().equals("saveAs")) {
            saveWorld();
        } else if (e.getActionCommand().equals("addRow")) {
            this.getWorld().getModel().addRow(this.getWorld().getModel().newRow());
            changedSinceLastSave = true;
        } else if (e.getActionCommand().equals("addRowHere")) {
            if (
                this.getWorld().getSelectedPoint().x < (this.getWorld()
                    .getTable().getRowHeight() * this.getWorld().getTable()
                    .getRowCount())) {
                this.getWorld().getModel().insertRow(
                        this.getWorld().getTable().rowAtPoint(
                                this.getWorld().getSelectedPoint()),
                        this.getWorld().getModel().newRow());
            } else {
                this.getWorld().getModel().addRow(this.getWorld().getModel().newRow());
            }

            changedSinceLastSave = true;
        } else if (e.getActionCommand().equals("addCol")) {
            this.getWorld().getModel().addColumn(Integer.toString(this.getWorld().getModel().getColumnCount()));
            this.getWorld().getModel().zeroFillNew();
            changedSinceLastSave = true;
        } else if (e.getActionCommand().equals("addColHere")) {
            insertColumnAtPoint(this.getWorld().getSelectedPoint());
            this.getWorld().getModel().zeroFillNew();
            changedSinceLastSave = true;
        } else if (e.getActionCommand().equals("remRow")) {
            this.getWorld().getModel().removeRow(this.getWorld().getTable().getRowCount() - 1);
            changedSinceLastSave = true;
        } else if (e.getActionCommand().equals("remRowHere")) {
            this.getWorld().getModel().removeRow(
                    this.getWorld().getTable().rowAtPoint(
                    this.getWorld().getSelectedPoint()));
            changedSinceLastSave = true;
        } else if (e.getActionCommand().equals("remCol")) {
            this.getWorld().getModel().removeColumn(this.getWorld().getModel().getColumnCount() - 1);
            changedSinceLastSave = true;
        } else if (e.getActionCommand().equals("remColHere")) {
            int col = this.getWorld().getTable().columnAtPoint(this.getWorld().getSelectedPoint());
            this.getWorld().getModel().removeColumn(col);
            changedSinceLastSave = true;
        } else if (e.getActionCommand().equals("zeroFill")) {
            this.getWorld().getModel().zeroFill();
            changedSinceLastSave = true;
        } else if (e.getActionCommand().equals("close")) {
            if (isChangedSinceLastSave()) {
                hasChanged();
            } else {
                dispose();
            }
        } else if (e.getActionCommand().equals("randomize")) {
            world.randomize();
            changedSinceLastSave = true;
        } else if (e.getActionCommand().equals("randomProps")) {
            world.displayRandomizeDialog();
            changedSinceLastSave = true;
        } else if (e.getActionCommand().equals("changeButtonName")) {
            DataWorld.editButtons = true;
        } else if (e.getActionCommand().equals("iterationMode")) {
            world.setIterationMode(iterationMode.getState());
            checkIterationMode();
        } else if (e.getActionCommand().equals("columnIteration")) {
            world.setColumnIteration(columnIteration.getState());
        }
    }

    /**
     * Inserts a new column a the point indicated.
     *
     * @param p Point to insert column
     */
    private void insertColumnAtPoint(final Point p) {
        Vector data = this.getWorld().getModel().getDataVector();
        int target = this.getWorld().getTable().columnAtPoint(p);
        int numRows = data.size();
        int numCols = ((Vector) data.get(0)).size();

        for (int j = 0; j < numRows; j++) {
            ((Vector) data.get(j)).insertElementAt(new Double(0), target);
        }

        Vector headers = new Vector(numCols + 1);
        headers.add(0, "");

        for (int j = 1; j < (numCols + 1); j++) {
            headers.add(j, Integer.toString(j));
        }

        this.getWorld().getModel().setDataVector(data, headers);
    }

    /**
     * Checks to see if anything has changed and then offers to save if true.
     */
    private void hasChanged() {
        Object[] options = {"Save", "Don't Save", "Cancel" };
        int s = JOptionPane
                .showInternalOptionDialog(this,
                 "This World has changed since last save,\nWould you like to save these changes?",
                 "World Has Changed", JOptionPane.YES_NO_OPTION,
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
     * @return Returns the changedSinceLastSave.
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
     * Menu selected event.
     *
     * @param e Menu event
     */
    public void menuSelected(final MenuEvent e) {
        if (e.getSource().equals(file)) {
            if (isChangedSinceLastSave()) {
                save.setEnabled(true);
            } else if (!isChangedSinceLastSave()) {
                save.setEnabled(false);
            }
        }
    }

    /**
     * Menu deselected event.
     *
     * @param e Menu event
     */
    public void menuDeselected(final MenuEvent e) {
    }

    /**
     * Menu canceled event.
     *
     * @param e Menu event
     */
    public void menuCanceled(final MenuEvent e) {
    }
}

