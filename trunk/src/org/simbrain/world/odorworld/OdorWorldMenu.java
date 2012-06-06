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

import javax.swing.JMenuItem;

/**
 * <b>OdorWorldMenu</b>.
 */
public class OdorWorldMenu {

    /** Parent world. */
    private OdorWorldPanel parentWorld;

    /** Delete menu item. */
    private JMenuItem deleteItem = new JMenuItem("Delete object");
    /** Add agent menu item. */
    private JMenuItem addAgentItem = new JMenuItem("Add new agent");
    /** Objects properties menu item. */
    private JMenuItem objectPropsItem = new JMenuItem("Set object Properties");
    /** World properties menu item. */
    private JMenuItem propsItem = new JMenuItem("Set world properties");
    /** Wall menu item. */
    private JMenuItem wallItem = new JMenuItem("Draw a wall");
    /** Wall properties menu item. */
    private JMenuItem wallPropsItem = new JMenuItem("Set Wall Properties");
    /** Copy object menu item. */
    private JMenuItem copyItem = new JMenuItem("Copy");
    /** Cut object menu item. */
    private JMenuItem cutItem = new JMenuItem("Cut");
    /** Paste object menu item. */
    private JMenuItem pasteItem = new JMenuItem("Paste");

    /**
     * Creates an instance of odor world menu.
     *
     * @param world OdorWorld to create OdorWorldMenu within
     */
    public OdorWorldMenu(final OdorWorldPanel world) {
        parentWorld = world;
    }

    /**
     * Build the popup menu displayed when users right-click in world.
     */
    public void initMenu() {
        // getDeleteItem().addActionListener(parentWorld.deleteListener);
        // getObjectPropsItem().addActionListener(parentWorld.objectPropsListener);
        // getPropsItem().addActionListener(parentWorld.propsListener);
        // getWallItem().addActionListener(parentWorld.wallListener);
        // getWallPropsItem().addActionListener(parentWorld.wallPropsListener);
        // getCutItem().addActionListener(parentWorld.cutListener);
        // getCopyItem().addActionListener(parentWorld.copyListener);
        // getPasteItem().addActionListener(parentWorld.pasteListener);
    }

    /**
     * @param addAgentItem The addAgentItem to set.
     */
    public void setAddAgentItem(final JMenuItem addAgentItem) {
        this.addAgentItem = addAgentItem;
    }

    /**
     * @return Returns the addAgentItem.
     */
    public JMenuItem getAddAgentItem() {
        return addAgentItem;
    }

    /**
     * @param copyItem The copyItem to set.
     */
    public void setCopyItem(final JMenuItem copyItem) {
        this.copyItem = copyItem;
    }

    /**
     * @return Returns the copyItem.
     */
    public JMenuItem getCopyItem() {
        return copyItem;
    }

    /**
     * @param cutItem The cutItem to set.
     */
    public void setCutItem(final JMenuItem cutItem) {
        this.cutItem = cutItem;
    }

    /**
     * @return Returns the cutItem.
     */
    public JMenuItem getCutItem() {
        return cutItem;
    }

    /**
     * @param deleteItem The deleteItem to set.
     */
    public void setDeleteItem(final JMenuItem deleteItem) {
        this.deleteItem = deleteItem;
    }

    /**
     * @return Returns the deleteItem.
     */
    public JMenuItem getDeleteItem() {
        return deleteItem;
    }

    /**
     * @param objectPropsItem The objectPropsItem to set.
     */
    public void setObjectPropsItem(final JMenuItem objectPropsItem) {
        this.objectPropsItem = objectPropsItem;
    }

    /**
     * @return Returns the objectPropsItem.
     */
    public JMenuItem getObjectPropsItem() {
        return objectPropsItem;
    }

    /**
     * @param pasteItem The pasteItem to set.
     */
    public void setPasteItem(final JMenuItem pasteItem) {
        this.pasteItem = pasteItem;
    }

    /**
     * @return Returns the pasteItem.
     */
    public JMenuItem getPasteItem() {
        return pasteItem;
    }

    /**
     * @param propsItem The propsItem to set.
     */
    public void setPropsItem(final JMenuItem propsItem) {
        this.propsItem = propsItem;
    }

    /**
     * @return Returns the propsItem.
     */
    public JMenuItem getPropsItem() {
        return propsItem;
    }

    /**
     * @param wallItem The wallItem to set.
     */
    public void setWallItem(final JMenuItem wallItem) {
        this.wallItem = wallItem;
    }

    /**
     * @return Returns the wallItem.
     */
    public JMenuItem getWallItem() {
        return wallItem;
    }

    /**
     * @param wallPropsItem The wallPropsItem to set.
     */
    public void setWallPropsItem(final JMenuItem wallPropsItem) {
        this.wallPropsItem = wallPropsItem;
    }

    /**
     * @return Returns the wallPropsItem.
     */
    public JMenuItem getWallPropsItem() {
        return wallPropsItem;
    }
}
