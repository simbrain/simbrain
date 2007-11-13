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
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Random;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.simbrain.workspace.gui.DesktopComponent;

/**
 * <b>DataWorldComponent</b> is a "spreadsheet world" used to send rows of raw data to input nodes.
 */
public class DataWorldDesktopComponent extends DesktopComponent<DataWorldComponent> { 

    private static final long serialVersionUID = 1L;

    /** World scroll pane. */
    private JScrollPane worldScroller = new JScrollPane();

    /** Data world. */
    private final DataWorld world;

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
    public DataWorldDesktopComponent(DataWorldComponent component) {
        super(component);
        
        component.addListener(new BasicComponentListener());
        world = new DataWorld(this, component.getDataModel());
        checkIterationMode();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add("Center", worldScroller);
        addMenuBar(world);
        getContentPane().add(world.getTable().getTableHeader(), BorderLayout.PAGE_START);
        worldScroller.setViewportView(world);
        worldScroller.setEnabled(false);
        
        pack();
    }

    /**
     * Creates the Menu Bar and adds it to the frame.
     *
     * @param table Table to be used for world
     */
    public void addMenuBar(final DataWorld table) {
        open.addActionListener(openListener);
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit
                .getDefaultToolkit().getMenuShortcutKeyMask()));
        save.addActionListener(saveListener);
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit
                .getDefaultToolkit().getMenuShortcutKeyMask()));
        saveAs.addActionListener(saveListener);
        close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit
                .getDefaultToolkit().getMenuShortcutKeyMask()));
        mb.add(file);
        file.add(open);
        file.add(save);
        file.add(saveAs);
        file.add(close);
        file.addMenuListener(menuListener);

        addRow.addActionListener(addRowListener);
        addCol.addActionListener(addColListener);
        remRow.addActionListener(remRowListener);
        remCol.addActionListener(remColListener);
        zeroFill.addActionListener(zeroFillListener);
        randomize.addActionListener(randomizeListener);
        randomProps.addActionListener(randomPropsListener);
        iterationMode.addActionListener(iterationModeListener);
        columnIteration.addActionListener(columnIterationListener);

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
//        setCurrentFile(theFile);
//        FileReader reader;
//        try {
//            reader = new FileReader(theFile);
//            TableModel model = (TableModel) getXStream().fromXML(reader);
//            model.postOpenInit();
//            world.setTableModel(model);
//            pack();
//            setStringReference(theFile);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * Returns a properly initialized xstream object.
     * @return the XStream object
     */
//    private XStream getXStream() {
//        XStream xstream = new XStream(new DomDriver());
//        xstream.omitField(TableModel.class, "consumers");
//        xstream.omitField(TableModel.class, "producers");
//        xstream.omitField(TableModel.class, "couplingList");
//        xstream.omitField(TableModel.class, "model");
//        return xstream;
//    }

    /**
     * Save a specified file.
     *
     * @param worldFile File to save world
     */
    public void save(final File worldFile) {
//        setCurrentFile(worldFile);
//        world.getTableModel().preSaveInit();
//        String xml = getXStream().toXML(world.getTableModel());
//        try {
//            FileWriter writer  = new FileWriter(worldFile);
//            writer.write(xml);
//            writer.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        setStringReference(worldFile);
//        setChangedSinceLastSave(false);
    }
    
    private ActionListener openListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            showOpenFileDialog();
        }
    };
    
    private ActionListener saveListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            save();
        }
    };
    
    private ActionListener addRowListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            world.getDataModel().addNewRow();
            setChangedSinceLastSave(true);
            pack();
        }
    };
        
    private ActionListener addColListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            world.getDataModel().addNewColumn();
            world.getDataModel().fillNew(new Double(0));
            setChangedSinceLastSave(true);
            pack();
        }
    };
   
    private ActionListener remRowListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            world.getDataModel().removeLastRow();
            setChangedSinceLastSave(true);
            pack();
        }
    };
   
    private ActionListener remColListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            world.getDataModel().removeLastColumn();
            setChangedSinceLastSave(true);
            pack();
        }
    };

    private ActionListener zeroFillListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            world.getDataModel().fill(new Double(0));
            setChangedSinceLastSave(true);
        }
    };
    
    private ActionListener randomizeListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            Random rand = new Random();
            
            DataModel<Double> model = world.getDataModel();
            int height = model.getRowCount();
            int width = model.getColumnCount();
            int lower = model.getLowerBound();
            int range = model.getUpperBound() - lower;
            
            for (int i = 0; i < height * width; i++) {
                double value = (rand.nextDouble() * range) + lower;
                model.set(i / width, i % height, value);
            }

            setChangedSinceLastSave(true);
        }
    };
    
    private ActionListener randomPropsListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            world.displayRandomizeDialog();
            setChangedSinceLastSave(true);
        }
    };
    
    private ActionListener iterationModeListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            world.getDataModel().setIterationMode(iterationMode.getState());
            checkIterationMode();
        }
    };
    
    private ActionListener columnIterationListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            world.getDataModel().setLastColumnBasedIteration(columnIteration.getState());
        }
    };
    
    private final MenuListener menuListener = new MenuListener() {
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
    
        public void menuDeselected(final MenuEvent e) {
        }
    
        public void menuCanceled(final MenuEvent e) {
        }
    };

    @Override
    public String getFileExtension() {
       return "xml";
    }

    @Override
    public void setCurrentDirectory(final String currentDirectory) {        
        super.setCurrentDirectory(currentDirectory);
        DataWorldPreferences.setCurrentDirectory(currentDirectory);
    }

    @Override
    public String getCurrentDirectory() {
        return DataWorldPreferences.getCurrentDirectory();
    }

    @Override
    protected void update() {
        // TODO refactor
        world.getDataModel().update();
//        this.getWorld().getTableModel().fireTableDataChanged();
        world.completedInputRound();
        super.update();
    }

    @Override
    public void close() {
    }

    /** @see javax.swing.JFrame */
    public void pack() {
        super.pack();
        setMaximumSize(new Dimension((int) getMaximumSize().getWidth(), (int) getSize().getHeight()));
        repaint();
    }
}
