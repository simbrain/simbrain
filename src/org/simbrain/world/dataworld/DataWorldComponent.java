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
package org.simbrain.world.dataworld;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;
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
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.CouplingContainer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;


/**
 * <b>DataWorldComponent</b> is a "spreadsheet world" used to send rows of raw data to input nodes.
 */
public class DataWorldComponent extends WorkspaceComponent implements ActionListener, MenuListener {

    /** World scroll pane. */
    private JScrollPane worldScroller = new JScrollPane();

    /** Data world. */
    private DataWorld world;

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

    /**
     * This method is the default constructor.
     */
    public DataWorldComponent() {
        super();
        init();
    }

    /**
     * Initilaizes items needed to create frame.
     */
    public void init() {
        this.checkIterationMode();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add("Center", worldScroller);
        world = new DataWorld(this);
        addMenuBar(world);
        getContentPane().add(world.getTable().getTableHeader(), BorderLayout.PAGE_START);
        worldScroller.setViewportView(world);
        worldScroller.setEnabled(false);
        this.pack();
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
     * Read a world from a world-xml file.
     *
     * @param theFile the xml file containing world information
     */
    public void open(final File theFile) {
        setCurrentFile(theFile);
        String[][] data = Utils.getStringMatrix(theFile);

        /* String[][] dataTemp = SimnetUtils.getStringMatrix(theFile);

        String[] names = new String[dataTemp.length];

        String[][] data = new String[dataTemp.length][dataTemp[0].length - 1];

        for (int i = 0; i < dataTemp.length; i++) {
            names[i] = dataTemp[i][0];

            for (int j = 1; j < dataTemp[0].length; j++) {
                data[i][j - 1] = dataTemp[i][j];
            }
        } */

        world.resetModel(data);

        getWorld().setName(theFile.getName());

        setStringReference(theFile);
    }

    /**
     * Save a specified file  Called by "save".
     *
     * @param worldFile File to save world
     */
    public void save(final File worldFile) {
        setCurrentFile(worldFile);
        String[][] data = new String[world.getTable().getRowCount()][world.getTable().getColumnCount() - 1];

        for (int i = 0; i < world.getTable().getRowCount(); i++) {
            for (int j = 0; j < world.getTable().getColumnCount() - 1; j++) {
                data[i][j] = new String("" + world.getTable().getValueAt(i, j + 1));
            }
        }

        Utils.writeMatrix(data, getCurrentFile());

        setStringReference(worldFile);
        getWorld().setName(worldFile.getName());

        setChangedSinceLastSave(false);
    }

    /**
     * Responds to actions performed.
     *
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
        if (e.getActionCommand().equals("open")) {
            showOpenFileDialog();
        } else if (e.getActionCommand().equals("save")) {
            save();
        } else if (e.getActionCommand().equals("saveAs")) {
            showSaveFileDialog();
        } else if (e.getActionCommand().equals("addRow")) {
            this.getWorld().getModel().addRow(this.getWorld().getModel().newRow());
            this.setChangedSinceLastSave(true);
            pack();
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
            this.setChangedSinceLastSave(true);
            pack();
        } else if (e.getActionCommand().equals("addCol")) {
            this.getWorld().getModel().addColumn(Integer.toString(this.getWorld().getModel().getColumnCount()+1));
            this.getWorld().getModel().zeroFillNew();
            this.setChangedSinceLastSave(true);
            pack();
        } else if (e.getActionCommand().equals("addColHere")) {
            this.getWorld().getModel().addColumn(Integer.toString(this.getWorld().getModel().getColumnCount()+1));
            this.getWorld().getModel().zeroFillNew();
            this.setChangedSinceLastSave(true);
            pack();
        } else if (e.getActionCommand().equals("remRow")) {
            this.getWorld().getModel().removeRow(this.getWorld().getTable().getRowCount() - 1);
            this.setChangedSinceLastSave(true);
            pack();
        } else if (e.getActionCommand().equals("remRowHere")) {
            this.getWorld().getModel().removeRow(
                    this.getWorld().getTable().rowAtPoint(
                    this.getWorld().getSelectedPoint()));
            this.setChangedSinceLastSave(true);
            pack();
        } else if (e.getActionCommand().equals("remCol")) {
            this.getWorld().getModel().removeColumn(this.getWorld().getModel().getColumnCount() - 1);
            this.setChangedSinceLastSave(true);
            pack();
        } else if (e.getActionCommand().equals("remColHere")) {
            int col = this.getWorld().getTable().columnAtPoint(this.getWorld().getSelectedPoint());
            this.getWorld().getModel().removeColumn(col);
            this.setChangedSinceLastSave(true);
            pack();
        } else if (e.getActionCommand().equals("zeroFill")) {
            this.getWorld().getModel().zeroFill();
            this.setChangedSinceLastSave(true);
        } else if (e.getActionCommand().equals("randomize")) {
            world.randomize();
            this.setChangedSinceLastSave(true);
        } else if (e.getActionCommand().equals("randomProps")) {
            world.displayRandomizeDialog();
            this.setChangedSinceLastSave(true);
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

    /**
     * @return the world
     */
    public DataWorld getWorld() {
        return world;
    }

    /**
     * @param world the world to set
     */
    public void setWorld(DataWorld world) {
        this.world = world;
    }

    @Override
    public String getFileExtension() {
        // TODO Auto-generated method stub
        return null;
    }

   /**
    * Returns reference to table model which contains couplings.
    */
   public CouplingContainer getCouplingContainer() {
       return this.getWorld().getModel();
   }

    @Override
    public void updateComponent() {
        this.getWorld().getModel().fireTableDataChanged();
        this.getWorld().completedInputRound();
        repaint();
    }

    @Override
    public void close() {
    }
}

