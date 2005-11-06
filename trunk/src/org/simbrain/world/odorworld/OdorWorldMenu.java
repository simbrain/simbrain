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

import javax.swing.JMenuItem;


/**
 * <b>OdorWorldMenu</b>
 */
public class OdorWorldMenu {
    private OdorWorld parentWorld;
    public JMenuItem deleteItem = new JMenuItem("Delete object");
    public JMenuItem addItem = new JMenuItem("Add new object");
    public JMenuItem addAgentItem = new JMenuItem("Add new agent"); //TODO: menu with submenus
    public JMenuItem objectPropsItem = new JMenuItem("Set object Properties");
    public JMenuItem propsItem = new JMenuItem("Set world properties");
    public JMenuItem wallItem = new JMenuItem("Draw a wall");
    public JMenuItem wallPropsItem = new JMenuItem("Set Wall Properties");
    public JMenuItem copyItem = new JMenuItem("Copy");
    public JMenuItem cutItem = new JMenuItem("Cut");
    public JMenuItem pasteItem = new JMenuItem("Paste");

    public OdorWorldMenu(final OdorWorld world) {
        parentWorld = world;
    }

    /**
     * Build the popup menu displayed when users right-click in world
     */
    public void initMenu() {
        deleteItem.addActionListener(parentWorld);
        objectPropsItem.addActionListener(parentWorld);
        addItem.addActionListener(parentWorld);
        addAgentItem.addActionListener(parentWorld);
        propsItem.addActionListener(parentWorld);
        wallItem.addActionListener(parentWorld);
        wallPropsItem.addActionListener(parentWorld);
        cutItem.addActionListener(parentWorld);
        copyItem.addActionListener(parentWorld);
        pasteItem.addActionListener(parentWorld);
    }
}
