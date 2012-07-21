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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.workspace.component_actions.CloseAction;
import org.simbrain.workspace.component_actions.OpenAction;
import org.simbrain.workspace.component_actions.SaveAction;
import org.simbrain.workspace.component_actions.SaveAsAction;
import org.simbrain.workspace.gui.CouplingMenuComponent;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * <b>DataWorldComponent</b> is a "spreadsheet world" used to send rows of raw
 * data to input nodes.
 */
public class DataWorldDesktopComponent extends GuiComponent<DataWorldComponent> {

    private static final long serialVersionUID = 1L;

    /** World scroll pane. */
    private JScrollPane scroller;

    /** Data world. */
    private DesktopJTable table;

    /** Menu bar. */
    private JMenuBar mb;

    /** File menu. */
    private JMenu fileItem = new JMenu("File  ");

    /** Save menu item. */
    private JMenuItem saveItem = new JMenuItem("Save");

    /** Determines whether table is in iteration mode. */
    private JCheckBoxMenuItem iterationMode = new JCheckBoxMenuItem(
            "Iteration mode");

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
        setLayout(new BorderLayout());

        // Even the data has not been initialized, initialize it to zero values
        if (component.getDataModel() == null) {
            component.getDataModel().fill(new Double(0));
        }

        // Set up table
        table = new DesktopJTable(component.getDataModel(), component);
        scroller = new JScrollPane();
        scroller.setViewportView(table);
        add(scroller, BorderLayout.CENTER);

        // Add toolbars
        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        toolbarPanel.add(table.getToolbarEditTable());
        toolbarPanel.add(table.getToolbarRandomize());
        add(toolbarPanel, BorderLayout.NORTH);

        addMenuBar(table);

    }

    /**
     * Creates the Menu Bar and adds it to the frame.
     *
     * @param table Table to be used for world
     */
    public void addMenuBar(final SimbrainJTable table) {
        mb = new JMenuBar();
        JMenu fileMenu = new JMenu("File  ");

        // Add file menu
        mb.add(fileMenu);
        fileMenu.add(new OpenAction(this));
        fileMenu.add(new SaveAction(this));
        fileMenu.add(new SaveAsAction(this));
        fileMenu.addSeparator();
        fileMenu.add(table.getMenuCSV());
        fileMenu.addSeparator();
        fileMenu.add(new CloseAction(this.getWorkspaceComponent()));

        // Edit menu
        JMenu editMenu = table.getMenuEdit();
        iterationMode.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                component.getDataModel().setIterationMode(
                        iterationMode.getState());
            }
        });
        editMenu.add(table.getMenuFill());
        editMenu.add(table.getMenuRandomize());
        editMenu.add(table.getMenuNormalize());
        editMenu.addSeparator();
        editMenu.add(iterationMode);

        mb.add(editMenu);

        // Add coupling menu
        mb.add(new CouplingMenuComponent("Couple", this.getWorkspaceComponent()
                .getWorkspace(), this.getWorkspaceComponent()));

        getParentFrame().setJMenuBar(mb);
    }

    /**
     * Listen for menu events.
     */
    private final MenuListener menuListener = new MenuListener() {
        /**
         * Menu selected event.
         *
         * @param e Menu event
         */
        public void menuSelected(final MenuEvent e) {
            if (e.getSource().equals(fileItem)) {
                if (getWorkspaceComponent().hasChangedSinceLastSave()) {
                    saveItem.setEnabled(true);
                } else if (!getWorkspaceComponent().hasChangedSinceLastSave()) {
                    saveItem.setEnabled(false);
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
        if (this.getParentFrame().getJMenuBar() == null) {
            addMenuBar(table);
        }
        pack();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.workspace.gui.GuiComponent#update()
     */
    @Override
    protected void update() {
        super.update();
        table.updateRowSelection();
    }

}
