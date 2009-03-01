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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Random;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.simbrain.workspace.Attribute;
import org.simbrain.workspace.AttributeHolder;
import org.simbrain.workspace.WorkspaceComponentListener;
import org.simbrain.workspace.gui.ComponentMenu;
import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * <b>DataWorldComponent</b> is a "spreadsheet world" used to send rows of raw data to input nodes.
 */
public class DataWorldDesktopComponent extends GuiComponent<DataWorldComponent> implements WorkspaceComponentListener { 

    private static final long serialVersionUID = 1L;

    /** World scroll pane. */
    private JScrollPane scroller;

    /** Data world. */
    private DataWorldPanel worldPanel;

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

    /** Component. */
    private final DataWorldComponent component;
    
    /**
     * Default constructor.
     *
     * @param component reference to model component
     */
    public DataWorldDesktopComponent(final GenericFrame frame, 
            final DataWorldComponent component) {

        super(frame, component);
        
        this.component = component;
        component.addListener(this);
        component.getDataModel().initValues(new Double(0));

        addMenuBar(worldPanel);

        worldPanel = new DataWorldPanel(component);
        scroller = new JScrollPane();
        scroller.setViewportView(worldPanel);
        setLayout(new BorderLayout());
        add(scroller, BorderLayout.CENTER);
    }

    /**
     * Creates the Menu Bar and adds it to the frame.
     *
     * @param table Table to be used for world
     */
    public void addMenuBar(final DataWorldPanel table) {
        open.addActionListener(openListener);
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit
                .getDefaultToolkit().getMenuShortcutKeyMask()));
        save.addActionListener(saveListener);
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit
                .getDefaultToolkit().getMenuShortcutKeyMask()));
        saveAs.addActionListener(saveAsListener);
        close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit
                .getDefaultToolkit().getMenuShortcutKeyMask()));
        randomize.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit
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
        mb.add(edit);
        
        JMenu producerMenu = new ComponentMenu("Couple",  getWorkspaceComponent().getWorkspace(), getWorkspaceComponent());
        mb.add(producerMenu);

        getParentFrame().setJMenuBar(mb);
    }


    private ActionListener openListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            showOpenFileDialog();
        }
    };

    private ActionListener saveAsListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            showSaveFileDialog();
        }
    };
    
    private ActionListener saveListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            save();
        }
    };

    private ActionListener addRowListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            component.getDataModel().addNewRow(new Double(0));
            getWorkspaceComponent().setChangedSinceLastSave(true);
            pack();
        }
    };

    private ActionListener addColListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            component.getDataModel().addNewColumn(new Double(0));
            getWorkspaceComponent().setChangedSinceLastSave(true);
            pack();
        }
    };
   
    private ActionListener remRowListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            component.getDataModel().removeLastRow();
            getWorkspaceComponent().setChangedSinceLastSave(true);
            pack();
        }
    };
   
    private ActionListener remColListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            component.getDataModel().removeLastColumn();
            getWorkspaceComponent().setChangedSinceLastSave(true);
            pack();
        }
    };

    private ActionListener zeroFillListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            component.getDataModel().fill(new Double(0));
            getWorkspaceComponent().setChangedSinceLastSave(true);
            repaint();
        }
    };
    
    private ActionListener randomizeListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            Random rand = new Random();
            
            DataModel<Double> model = component.getDataModel();
            int height = model.getRowCount();
            int width = model.getColumnCount();
            int lower = model.getLowerBound();
            int range = model.getUpperBound() - lower;

            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++ ) {
                    double value = (rand.nextDouble() * range) + lower;
                    model.set(i , j, value);
                }
            }
            getWorkspaceComponent().setChangedSinceLastSave(true);
        }
    };
    
    private ActionListener randomPropsListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            worldPanel.displayRandomizeDialog();
            getWorkspaceComponent().setChangedSinceLastSave(true);
        }
    };
    
    private ActionListener iterationModeListener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            component.getDataModel().setIterationMode(iterationMode.getState());
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
                if (getWorkspaceComponent().hasChangedSinceLastSave()) {
                    save.setEnabled(true);
                } else if (!getWorkspaceComponent().hasChangedSinceLastSave()) {
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
    public void closing() {
    }

    /** @see javax.swing.JFrame */
    public void pack() {
        getParentFrame().pack();
        // Set max size somehow?
        repaint();
    }
    
    @Override
    public void postAddInit() {
        pack();
    }

    /**
     * {@inheritDoc}
     */
    public void attributeRemoved(AttributeHolder parent, Attribute attribute) {
        // TODO Auto-generated method stub        
    }

    /**
     * {@inheritDoc}
     */
    public void componentUpdated() {
        worldPanel.updateRowSelection();
    }

}
