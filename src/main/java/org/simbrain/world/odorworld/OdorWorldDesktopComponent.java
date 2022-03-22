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

import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.workspace.gui.DesktopComponent;

import java.awt.*;

/**
 * <b>WorldPanel</b> is the container for the world component. Handles toolbar
 * buttons, and serializing of world data. The main environment codes is in
 * {@link OdorWorldPanel}.
 */
public class OdorWorldDesktopComponent extends DesktopComponent<OdorWorldComponent> {

    private static final long serialVersionUID = 1L;

    /**
     * Odor world to be in frame.
     */
    private OdorWorldPanel worldPanel;

    /**
     * Odor world frame menu.
     */
    private OdorWorldFrameMenu menu;

    /**
     * Default constructor.
     *
     * @param frame
     * @param component
     */
    public OdorWorldDesktopComponent(GenericFrame frame, OdorWorldComponent component) {
        super(frame, component);
        setLayout(new BorderLayout());
        worldPanel = new OdorWorldPanel(component, component.getWorld());
        add("Center", worldPanel);
        menu = new OdorWorldFrameMenu(this, component.getWorld());
        menu.setUpMenus();
        getParentFrame().setJMenuBar(menu); // TODO: Move menu creation to this

        worldPanel.getWorld().getEvents().onTileMapChanged(this::setGuiMaxSizeToWorldSize);

        // component.setCurrentDirectory(OdorWorldPreferences.getCurrentDirectory());

        menu = new OdorWorldFrameMenu(this, worldPanel.getWorld());
        menu.setUpMenus();
        getParentFrame().setJMenuBar(menu);
        worldPanel.syncToModel();
        setGuiSizeToWorldSize();
    }

    /**
     * Sets the size of the window based on the size of the underlying world / tilemap.
     * Ignore the default panel preferences.
     */
    public void setGuiSizeToWorldSize() {
        int widthOffset = getParentFrame().getSize().width - worldPanel.getWidth();
        int heightOffset = getParentFrame().getSize().height - worldPanel.getHeight();
        getParentFrame().setPreferredSize(new Dimension(worldPanel.getWorld().getWidth() + widthOffset,
                worldPanel.getWorld().getHeight() + heightOffset));
        getParentFrame().setMaximumSize(
                new Dimension(worldPanel.getWorld().getWidth() + widthOffset,
                worldPanel.getWorld().getHeight() + heightOffset));
        getParentFrame().pack();
    }

    /**
     * Sets the max size of the window based on the size of the underlying world / tilemap;
     * set the default size base don the panel preferences.
     */
    private void setGuiMaxSizeToWorldSize() {
        worldPanel.setPreferredSize(worldPanel.getPreferredSize());
        int widthOffset = getParentFrame().getSize().width - worldPanel.getWidth();
        int heightOffset = getParentFrame().getSize().height - worldPanel.getHeight();
        getParentFrame().setMaximumSize(
                new Dimension(worldPanel.getWorld().getWidth() + widthOffset,
                        worldPanel.getWorld().getHeight() + heightOffset));
        getParentFrame().pack();
    }

    /**
     * Return the odor world.
     *
     * @return Odor world
     */
    public OdorWorldPanel getWorldPanel() {
        return worldPanel;
    }

    /**
     * @return Odor world frame menu.
     */
    public OdorWorldFrameMenu getMenu() {
        return menu;
    }

    /**
     * Sets odor world frame menu.
     *
     * @param menu Menu
     */
    public void setMenu(final OdorWorldFrameMenu menu) {
        this.menu = menu;
    }

}
