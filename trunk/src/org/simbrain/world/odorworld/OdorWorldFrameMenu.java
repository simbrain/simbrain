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

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.simbrain.workspace.component_actions.CloseAction;
import org.simbrain.workspace.component_actions.OpenAction;
import org.simbrain.workspace.component_actions.SaveAction;
import org.simbrain.workspace.component_actions.SaveAsAction;
import org.simbrain.workspace.gui.CouplingMenuComponent;
import org.simbrain.world.odorworld.actions.ShowWorldPrefsAction;


/**
 * <b>OdorWorldFrameMenu</b>.
 */
public class OdorWorldFrameMenu extends JMenuBar {

    private static final long serialVersionUID = 1L;

    //TODO: Replace all this with actions.

    /** Parent frame. */
    private OdorWorldDesktopComponent parent;

    /** File menu. */
    private JMenu fileMenu = new JMenu("File  ");

    /** Preferences menu item. */
    private JMenuItem prefsItem = new JMenuItem("World preferences");

    /** Edit menu. */
    private JMenu editMenu = new JMenu("Edit  ");

    /** Copy menu item. */
    private JMenuItem copyItem = new JMenuItem("Copy");

    /** Cut menu item. */
    private JMenuItem cutItem = new JMenuItem("Cut");

    /** Paste menu item. */
    private JMenuItem pasteItem = new JMenuItem("Paste");

    /** Clear all menu item. */
    private JMenuItem clearAllItem = new JMenuItem("Clear all entities");

    /** Help menu. */
    private JMenu helpMenu = new JMenu("Help");

    /** Help menu item. */
    private JMenuItem helpItem = new JMenuItem("World Help");

    /**
     * Odor world frame menu constructor.
     * @param frame Frame to create menu
     */
    public OdorWorldFrameMenu(final OdorWorldDesktopComponent frame) {
        parent = frame;
    }

    /**
     * Sets up menus.
     */
    public void setUpMenus() {

        setUpFileMenu();
        setUpEditMenu();
        add(helpMenu);
        helpMenu.add(helpItem);
        add(new CouplingMenuComponent("Couple", parent.getWorkspaceComponent()
                .getWorkspace(), parent.getWorkspaceComponent()));
    }

    /**
     * Sets up file menu items.
     */
    public void setUpFileMenu() {
        add(fileMenu);
        fileMenu.add(new OpenAction(parent));
        fileMenu.add(new SaveAction(parent));
        fileMenu.add(new SaveAsAction(parent));
        fileMenu.addSeparator();
        fileMenu.add(new ShowWorldPrefsAction(parent.getWorldPanel()));
        fileMenu.add(new CloseAction(parent.getWorkspaceComponent()));
    }

    /**
     * Sets up edit menu items.
     */
    public void setUpEditMenu() {
        add(editMenu);

        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        editMenu.addSeparator();
        editMenu.addSeparator();
        editMenu.add(clearAllItem);

    }
}
