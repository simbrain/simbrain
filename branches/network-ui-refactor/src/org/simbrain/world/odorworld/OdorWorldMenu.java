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
 * <b>OdorWorldMenu</b>.
 */
public class OdorWorldMenu {
    private OdorWorld parentWorld;
    private JMenuItem deleteItem = new JMenuItem("Delete object");
    private JMenuItem addItem = new JMenuItem("Add new object");
    private JMenuItem addAgentItem = new JMenuItem("Add new agent");
    private JMenuItem objectPropsItem = new JMenuItem("Set object Properties");
    private JMenuItem propsItem = new JMenuItem("Set world properties");
    private JMenuItem wallItem = new JMenuItem("Draw a wall");
    private JMenuItem wallPropsItem = new JMenuItem("Set Wall Properties");
    private JMenuItem copyItem = new JMenuItem("Copy");
    private JMenuItem cutItem = new JMenuItem("Cut");
    private JMenuItem pasteItem = new JMenuItem("Paste");

    public OdorWorldMenu(final OdorWorld world) {
        parentWorld = world;
    }

    /**
     * Build the popup menu displayed when users right-click in world.
     */
    public void initMenu() {
        getDeleteItem().addActionListener(parentWorld);
        getObjectPropsItem().addActionListener(parentWorld);
        getAddItem().addActionListener(parentWorld);
        getAddAgentItem().addActionListener(parentWorld);
        getPropsItem().addActionListener(parentWorld);
        getWallItem().addActionListener(parentWorld);
        getWallPropsItem().addActionListener(parentWorld);
        getCutItem().addActionListener(parentWorld);
        getCopyItem().addActionListener(parentWorld);
        getPasteItem().addActionListener(parentWorld);
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
     * @param addItem The addItem to set.
     */
    public void setAddItem(final JMenuItem addItem) {
        this.addItem = addItem;
    }

    /**
     * @return Returns the addItem.
     */
    public JMenuItem getAddItem() {
        return addItem;
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
