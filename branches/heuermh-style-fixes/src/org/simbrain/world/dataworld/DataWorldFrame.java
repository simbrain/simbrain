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

import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;

import org.simbrain.workspace.Workspace;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.io.File;

import java.util.Vector;

import javax.swing.JButton;
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


/**
 * <b>DataWorldFrame</b> is a "spreadsheet world" used to send rows of raw data to input nodes.
 */
public class DataWorldFrame extends JInternalFrame implements ActionListener, InternalFrameListener, MenuListener {
    private File current_file = null;
    private String currentDirectory = DataWorldPreferences.getCurrentDirectory();
    private JScrollPane worldScroller = new JScrollPane();
    private Workspace workspace;
    private DataWorld world;

    // For workspace persistence 
    private String path;
    private int xpos;
    private int ypos;
    private int the_width;
    private int the_height;
    JMenuBar mb = new JMenuBar();
    JMenu file = new JMenu("File  ");
    JMenuItem open = new JMenuItem("Open");
    JMenuItem save = new JMenuItem("Save");
    JMenuItem saveAs = new JMenuItem("Save as");
    JMenuItem close = new JMenuItem("Close");
    JMenu edit = new JMenu("Edit");
    JMenuItem addRow = new JMenuItem("Add a row");
    JMenuItem addCol = new JMenuItem("Add a column");
    JMenuItem zeroFill = new JMenuItem("ZeroFill the Table");
    JMenuItem remRow = new JMenuItem("Remove a row");
    JMenuItem remCol = new JMenuItem("Remove a column");
    JMenuItem randomize = new JMenuItem("Randomize");
    JMenuItem randomProps = new JMenuItem("Adjust Randomization Bounds");
    private boolean changedSinceLastSave = false;

    public DataWorldFrame() {
    }

    /**
     * Construct a new world panel.  Set up the toolbars.  Create an  instance of a world object.
     */
    public DataWorldFrame(Workspace ws) {
        workspace = ws;
        init();
    }

    public void init() {
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

        this.resize();

        setVisible(true);
    }

    /**
     * Creates the Menu Bar and adds it to the frame.
     *
     * @param frame
     * @param table
     */
    public void addMenuBar(DataWorld table) {
        open.addActionListener(this);
        open.setActionCommand("open");
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        save.addActionListener(this);
        save.setActionCommand("save");
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        saveAs.addActionListener(this);
        saveAs.setActionCommand("saveAs");
        close.addActionListener(this);
        close.setActionCommand("close");
        close.setAccelerator(KeyStroke.getKeyStroke(
                                                    KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
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
        edit.add(addRow);
        edit.add(addCol);
        edit.add(zeroFill);
        edit.addSeparator();
        edit.add(remRow);
        edit.add(remCol);
        edit.addSeparator();
        edit.add(randomize);
        edit.add(randomProps);
        mb.add(edit);

        setJMenuBar(mb);
    }

    /**
     * Resize based on number of rows
     */
    public void resize() {
        int height = 70 + (world.getTable().getRowCount() * world.getTable().getRowHeight());
        int width = 80 + (world.getTable().getColumnCount() * 70);
        this.setBounds(this.getX(), this.getY(), width, height);
    }

    public File getCurrentFile() {
        return current_file;
    }

    public DataWorld getWorld() {
        return world;
    }

    /**
     * Show the dialog for choosing a world to open
     */
    public void openWorld() {
        SFileChooser chooser = new SFileChooser(currentDirectory, "csv");
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
    public void readWorld(File theFile) {
        current_file = theFile;

        String[][] dataTemp = Utils.getStringMatrix(theFile);

        String[] names = new String[dataTemp.length];

        String[][] data = new String[dataTemp.length][dataTemp[0].length - 1];

        for (int i = 0; i < dataTemp.length; i++) {
            names[i] = dataTemp[i][0];

            for (int j = 1; j < dataTemp[0].length; j++) {
                data[i][j - 1] = dataTemp[i][j];
            }
        }

        world.resetModel(data);

        world.setButtonNames(names);

        getWorkspace().attachAgentsToCouplings();
        setName(theFile.getName());

        //Set Path; used in workspace persistence
        String localDir = new String(System.getProperty("user.dir"));
        setPath(Utils.getRelativePath(localDir, theFile.getAbsolutePath()));
    }

    /**
     * Opens a file-save dialog and saves world information to the specified file  Called by "Save As"
     */
    public void saveWorld() {
        SFileChooser chooser = new SFileChooser(currentDirectory, "csv");
        File worldFile = chooser.showSaveDialog();

        if (worldFile != null) {
            saveWorld(worldFile);
            current_file = worldFile;
            currentDirectory = chooser.getCurrentLocation();
        }
    }

    /**
     * Save a specified file  Called by "save"
     *
     * @param worldFile
     */
    public void saveWorld(File worldFile) {
        current_file = worldFile;

        String[][] data = new String[world.getTable().getRowCount()][world.getTable().getColumnCount()];

        for (int i = 0; i < world.getTable().getRowCount(); i++) {
            data[i][0] = world.getButtonNames()[i];

            for (int j = 1; j < world.getTable().getColumnCount(); j++) {
                data[i][j] = new String("" + world.getTable().getValueAt(i, j));
            }
        }

        Utils.writeMatrix(data, current_file);

        String localDir = new String(System.getProperty("user.dir"));
        setPath(Utils.getRelativePath(localDir, worldFile.getAbsolutePath()));

        setName(worldFile.getName());

        setChangedSinceLastSave(false);
    }

    public void internalFrameOpened(InternalFrameEvent e) {
    }

    public void internalFrameClosing(InternalFrameEvent e) {
        if (isChangedSinceLastSave()) {
            hasChanged();
        } else {
            dispose();
        }
    }

    public void internalFrameClosed(InternalFrameEvent e) {
        this.getWorkspace().removeAgentsFromCouplings(this.getWorld());
        this.getWorkspace().getDataWorldList().remove(this);

        DataWorldFrame dat = workspace.getLastDataWorld();

        if (dat != null) {
            dat.grabFocus();
            workspace.repaint();
        }

        DataWorldPreferences.setCurrentDirectory(currentDirectory);
    }

    public void internalFrameIconified(InternalFrameEvent e) {
    }

    public void internalFrameDeiconified(InternalFrameEvent e) {
    }

    public void internalFrameActivated(InternalFrameEvent e) {
    }

    public void internalFrameDeactivated(InternalFrameEvent e) {
    }

    /**
     * @param path The path to set; used in persistence.
     */
    public void setPath(String path) {
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
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    /**
     * For Castor.  Turn Component bounds into separate variables.
     */
    public void initBounds() {
        xpos = this.getX();
        ypos = this.getY();
        the_width = this.getBounds().width;
        the_height = this.getBounds().height;
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
    public void setXpos(int xpos) {
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
    public void setYpos(int ypos) {
        this.ypos = ypos;
    }

    /**
     * @return Returns the the_height.
     */
    public int getThe_height() {
        return the_height;
    }

    /**
     * @param the_height The the_height to set.
     */
    public void setThe_height(int the_height) {
        this.the_height = the_height;
    }

    /**
     * @return Returns the the_width.
     */
    public int getThe_width() {
        return the_width;
    }

    /**
     * @param the_width The the_width to set.
     */
    public void setThe_width(int the_width) {
        this.the_width = the_width;
    }

    public void setName(String name) {
        setTitle(name);
        world.setName(name);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("open")) {
            openWorld();
            changedSinceLastSave = false;
        } else if (e.getActionCommand().equals("save")) {
            if (current_file == null) {
                saveWorld();
            } else {
                saveWorld(current_file);
            }
        } else if (e.getActionCommand().equals("saveAs")) {
            saveWorld();
        } else if (e.getActionCommand().equals("addRow")) {
            this.getWorld().getModel().addRow(this.getWorld().getModel().newRow());
            changedSinceLastSave = true;
            resize();
        } else if (e.getActionCommand().equals("addRowHere")) {
            if (
                this.getWorld().getSelectedPoint().x < (this.getWorld().getTable().getRowHeight() * this.getWorld()
                                                                                                        .getTable()
                                                                                                        .getRowCount())) {
                this.getWorld().getModel().insertRow(
                                                     this.getWorld().getTable().rowAtPoint(this.getWorld()
                                                                                           .getSelectedPoint()),
                                                     this.getWorld().getModel().newRow());
            } else {
                this.getWorld().getModel().addRow(this.getWorld().getModel().newRow());
            }

            changedSinceLastSave = true;
            resize();
        } else if (e.getActionCommand().equals("addCol")) {
            this.getWorld().getModel().addColumn(Integer.toString(this.getWorld().getModel().getColumnCount()));
            this.getWorld().getModel().zeroFillNew();

            //Necessary to keep the buttons properly rendered
            this.getWorld().getTable().getColumnModel().getColumn(0).setCellRenderer(new ButtonRenderer(this.getWorld()
                                                                                                        .getTable()
                                                                                                        .getDefaultRenderer(JButton.class)));
            this.getWorld().columnResize();
            changedSinceLastSave = true;
            resize();
        } else if (e.getActionCommand().equals("addColHere")) {
            insertColumnAtPoint(this.getWorld().getSelectedPoint());
            this.getWorld().columnResize();
            changedSinceLastSave = true;
            resize();
        } else if (e.getActionCommand().equals("remRow")) {
            this.getWorld().getModel().removeRow(this.getWorld().getTable().getRowCount() - 1);
            changedSinceLastSave = true;
            resize();
        } else if (e.getActionCommand().equals("remRowHere")) {
            this.getWorld().getModel().removeRow(this.getWorld().getTable().rowAtPoint(this.getWorld().getSelectedPoint()));
            changedSinceLastSave = true;
            resize();
        } else if (e.getActionCommand().equals("remCol")) {
//			this.getWorld().getTable().getColumnModel().removeColumn(
//					this.getWorld().getTable().getColumnModel().getColumn(
// 			this.getWorld().getTable().getColumnCount()-1));
            Vector cid = this.getWorld().getModel().getColumnIdentifiers();
            cid.remove(this.getWorld().getTable().getColumnCount() - 1);
            this.getWorld().getModel().setDataVector(this.getWorld().getModel().getDataVector(), cid);

            this.getWorld().getTable().getColumnModel().getColumn(0).setCellRenderer(new ButtonRenderer(this.getWorld()
                                                                                                        .getTable()
                                                                                                        .getDefaultRenderer(JButton.class)));
            this.getWorld().columnResize();

            changedSinceLastSave = true;
            resize();
        } else if (e.getActionCommand().equals("remColHere")) {
            int col = this.getWorld().getTable().columnAtPoint(this.getWorld().getSelectedPoint());
            Vector data = this.getWorld().getModel().getDataVector();
            Vector cid = this.getWorld().getModel().getColumnIdentifiers();

            cid.remove(col);

            for (int i = col; i < cid.size(); i++) {
                cid.set(i, Integer.toString(i));
            }

            for (int i = 0; i < this.getWorld().getTable().getRowCount(); i++) {
                ((Vector) data.get(i)).remove(col);
            }

            this.getWorld().getModel().setDataVector(data, cid);

            this.getWorld().getTable().getColumnModel().getColumn(0).setCellRenderer(new ButtonRenderer(this.getWorld()
                                                                                                        .getTable()
                                                                                                        .getDefaultRenderer(JButton.class)));
            this.getWorld().columnResize();

            changedSinceLastSave = true;
            resize();
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
        }
    }

    private void insertColumnAtPoint(Point p) {
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

        this.getWorld().getTable().getColumnModel().getColumn(0).setCellRenderer(new ButtonRenderer(this.getWorld()
                                                                                                    .getTable()
                                                                                                    .getDefaultRenderer(JButton.class)));
    }

    /**
     * Checks to see if anything has changed and then offers to save if true
     */
    private void hasChanged() {
        Object[] options = { "Save", "Don't Save", "Cancel" };
        int s = JOptionPane.showInternalOptionDialog(
                                                     this,
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
     * @param changedSinceLastSave The changedSinceLastSave to set.
     */
    public void setChangedSinceLastSave(boolean hasChangedSinceLastSave) {
        this.changedSinceLastSave = hasChangedSinceLastSave;
    }

    public void menuSelected(MenuEvent e) {
        if (e.getSource().equals(file)) {
            if (isChangedSinceLastSave()) {
                save.setEnabled(true);
            } else if (!isChangedSinceLastSave()) {
                save.setEnabled(false);
            }
        }
    }

    public void menuDeselected(MenuEvent e) {
    }

    public void menuCanceled(MenuEvent e) {
    }
}
