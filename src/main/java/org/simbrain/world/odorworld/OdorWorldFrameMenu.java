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

import org.simbrain.util.piccolo.TMXUtils;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.actions.LoadTileMapAction;
import org.simbrain.world.odorworld.actions.ShowWorldPrefsAction;
import org.simbrain.world.odorworld.gui.OdorWorldActions;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * <b>OdorWorldFrameMenu</b>.
 */
public class OdorWorldFrameMenu extends JMenuBar {

    private static final long serialVersionUID = 1L;

    // TODO: Replace all this with new actions.

    /**
     * Parent frame.
     */
    private OdorWorldDesktopComponent parent;

    private OdorWorldActions odorWorldActions;

    /**
     * File menu.
     */
    private JMenu fileMenu = new JMenu("File  ");

    /**
     * Edit menu.
     */
    private JMenu editMenu = new JMenu("Edit  ");

    /**
     * Copy menu item.
     */
    private JMenuItem copyItem = new JMenuItem("Copy");

    /**
     * Cut menu item.
     */
    private JMenuItem cutItem = new JMenuItem("Cut");

    /**
     * Paste menu item.
     */
    private JMenuItem pasteItem = new JMenuItem("Paste");

    /**
     * Clear all menu item.
     */
    private JMenuItem clearAllItems = new JMenuItem("Clear all entities");

    /**
     * Help menu.
     */
    private JMenu helpMenu = new JMenu("Help");

    /**
     * Help menu item.
     */
    private JMenuItem helpItem = new JMenuItem("World Help");

    /**
     * Reference to odor world.
     */
    private final OdorWorld world;

    /**
     * Odor world frame menu constructor.
     *
     * @param frame Frame to create menu
     * @param world
     */
    public OdorWorldFrameMenu(final OdorWorldDesktopComponent frame, OdorWorld world) {
        parent = frame;
        this.world = world;
        odorWorldActions = parent.getWorldPanel().odorWorldActions;
    }

    /**
     * Sets up menus.
     */
    public void setUpMenus() {

        setUpFileMenu();
        setUpEditMenu();

        // Help Menu
        add(helpMenu);
        ShowHelpAction helpAction = new ShowHelpAction("Pages/Worlds/OdorWorld/OdorWorld.html");
        helpItem.setAction(helpAction);
        helpMenu.add(helpItem);
    }

    /**
     * Sets up file menu items.
     */
    public void setUpFileMenu() {
        add(fileMenu);
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createImportAction(parent));
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createExportAction(parent));
        fileMenu.addSeparator();

        JMenu loadTileMapMenu = new JMenu("Load Tile Map...");
        JMenuItem loadTileMapItem = new JMenuItem(new LoadTileMapAction(parent.getWorldPanel()));
        loadTileMapMenu.add(loadTileMapItem);
        loadTileMapMenu.addSeparator();

        loadTileMapMenu.add(new JMenuItem(new AbstractAction("Load Empty World") {
            @Override
            public void actionPerformed(ActionEvent e) {
                parent.getWorldPanel().world.setTileMap(TMXUtils.loadTileMap("empty.tmx"));
            }
        }));


        loadTileMapMenu.add(new JMenuItem(new AbstractAction("Load Ari's World") {
            @Override
            public void actionPerformed(ActionEvent e) {
                parent.getWorldPanel().world.setTileMap(TMXUtils.loadTileMap("aris_world.tmx"));
            }
        }));

        loadTileMapMenu.add(new JMenuItem(new AbstractAction("Load Yulin's World") {
            @Override
            public void actionPerformed(ActionEvent e) {
                parent.getWorldPanel().world.setTileMap(TMXUtils.loadTileMap("yulins_world.tmx"));
            }
        }));

        fileMenu.add(loadTileMapMenu);
        fileMenu.addSeparator();
        fileMenu.add(new ShowWorldPrefsAction(parent.getWorldPanel()));
        fileMenu.add(odorWorldActions.getShowInfoAction());
        fileMenu.addSeparator();
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createRenameAction(parent));
        fileMenu.addSeparator();
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createCloseAction(parent));
    }

    /**
     * Sets up edit menu items.
     */
    public void setUpEditMenu() {
        add(editMenu);

        //editMenu.add(cutItem);
        //editMenu.add(copyItem);
        //editMenu.add(pasteItem);
        //editMenu.addSeparator();
        clearAllItems.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                world.deleteAllEntities();
            }
        });
        editMenu.add(clearAllItems);
        editMenu.addSeparator();

        // TODO: Factor the code for placing new entities out of network, to utils, and reuse here.
        JMenuItem addEntity = new JMenuItem("Add Entity");
        addEntity.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                world.addEntity();
            }
        });
        editMenu.add(addEntity);
        JMenuItem addAgent = new JMenuItem("Add Agent");
        addAgent.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                world.addAgent();
            }
        });
        editMenu.add(addAgent);

        editMenu.addSeparator();

        JMenuItem turnOnAllTrails = new JMenuItem("Turn on all trails");
        turnOnAllTrails.addActionListener(odorWorldActions.getTurnOnTrails());
        editMenu.add(turnOnAllTrails);

        JMenuItem turnOffAllTrails = new JMenuItem("Turn off all trails");
        turnOffAllTrails.addActionListener(odorWorldActions.getTurnOffTrails());
        editMenu.add(turnOffAllTrails);

        JMenuItem clearAllTrails = new JMenuItem("Clear all trails");
        clearAllTrails.addActionListener(odorWorldActions.getClearAllTrails());
        editMenu.add(clearAllTrails);

        // JMenuItem loadVectors = new JMenuItem("Load stimulus vectors...");
        // loadVectors.addActionListener(new ActionListener() {
        //     public void actionPerformed(ActionEvent e) {
        //         SFileChooser chooser = new SFileChooser(".", "Load vectors");
        //         File theFile = chooser.showOpenDialog();
        //         if (theFile != null) {
        //             double[][] vecs = Utils.getDoubleMatrix(theFile);
        //             world.loadStimulusVectors(vecs);
        //         }
        //     }
        // });
        // editMenu.add(loadVectors);

    }

}
