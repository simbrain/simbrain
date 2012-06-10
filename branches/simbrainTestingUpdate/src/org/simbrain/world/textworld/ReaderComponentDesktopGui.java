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
package org.simbrain.world.textworld;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.workspace.component_actions.CloseAction;
import org.simbrain.workspace.component_actions.OpenAction;
import org.simbrain.workspace.component_actions.SaveAction;
import org.simbrain.workspace.component_actions.SaveAsAction;
import org.simbrain.workspace.gui.CouplingMenuComponent;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * <b>ReaderComponentDesktopGui</b> is the container for the reader text world.
 */
public class ReaderComponentDesktopGui extends GuiComponent<ReaderComponent> {

    /** Default height. */
    private static final int DEFAULT_HEIGHT = 250;

    /** Default width. */
    private static final int DEFAULT_WIDTH = 400;

    /** Menu Bar. */
    private JMenuBar menuBar = new JMenuBar();

    /** File menu for saving and opening world files. */
    private JMenu file = new JMenu("File");

    /** Edit menu Item. */
    private JMenu edit = new JMenu("Edit");

    /** Opens the dialog to define TextWorld Dictionary. */
    private JMenuItem loadDictionary = new JMenuItem("Load dictionary");

    /** Show dictionary. */
    private JMenuItem showDictionary = new JMenuItem("Show dictionary");

    /** Opens user preferences dialog. */
    private JMenuItem preferences = new JMenuItem("Preferences");

    /** Opens the help dialog for TextWorld. */
    private JMenu help = new JMenu("Help");

    /** The pane representing the text world. */
    private ReaderPanel panel;

    /** The text world. */
    private ReaderWorld world;

    /**
     * Creates a new frame of type TextWorld.
     */
    public ReaderComponentDesktopGui(GenericFrame frame,
            ReaderComponent component) {
        super(frame, component);

        world = component.getWorld();
        panel = new ReaderPanel(world);
        this.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        addMenuBar();
        add(panel);
        frame.pack();

        // Force component to fill up parent panel
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Component component = e.getComponent();
                panel.setPreferredSize(new Dimension(component.getWidth(),
                        component.getHeight()));
                panel.revalidate();
            }
        });

    }

    @Override
    public void postAddInit() {
        super.postAddInit();
        this.getParentFrame().pack();
    }

    /**
     * Adds menu bar to the top of TextWorldComponent.
     */
    private void addMenuBar() {

        // File Menu
        menuBar.add(file);
        file.add(new OpenAction(this));
        file.add(new SaveAction(this));
        file.add(new SaveAsAction(this));
        file.addSeparator();
        file.add(new CloseAction(this.getWorkspaceComponent()));

        // Edit menu
        loadDictionary.setAction(TextWorldActions
                .getLoadDictionaryAction(world));
        showDictionary.setAction(TextWorldActions
                .getShowDictionaryAction(world));
        preferences.setAction(TextWorldActions
                .getShowPreferencesDialogAction(world));
        edit.add(loadDictionary);
        edit.add(showDictionary);
        edit.addSeparator();
        edit.add(preferences);
        menuBar.add(edit);

        // Coupling menu
        menuBar.add(new CouplingMenuComponent("Couple", this
                .getWorkspaceComponent().getWorkspace(), this
                .getWorkspaceComponent()));

        // Help Menu
        menuBar.add(help);

        // Add menu
        getParentFrame().setJMenuBar(menuBar);
    }

    @Override
    public void closing() {
        // TODO Auto-generated method stub

    }

}
