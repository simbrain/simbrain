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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.Sensor;

/**
 * <b>WorldPanel</b> is the container for the world component. Handles toolbar
 * buttons, and serializing of world data. The main environment codes is in
 * {@link OdorWorldPanel}.
 */
public class OdorWorldDesktopComponent extends GuiComponent<OdorWorldComponent> {

    private static final long serialVersionUID = 1L;

    /** Odor world to be in frame. */
    private OdorWorldPanel worldPanel;

    /** Odor world frame menu. */
    private OdorWorldFrameMenu menu;

    /**
     * Default constructor.
     */
    public OdorWorldDesktopComponent(GenericFrame frame,
            OdorWorldComponent component) {
        super(frame, component);
        setLayout(new BorderLayout());
        worldPanel = new OdorWorldPanel(component.getWorld());
        add("Center", worldPanel);
        menu = new OdorWorldFrameMenu(this);
        menu.setUpMenus();
        setGuiSizeToWorldSize();
        getParentFrame().setJMenuBar(menu); // TODO: Move menu creation to this
                                            // class?
        // component.setCurrentDirectory(OdorWorldPreferences.getCurrentDirectory());
        // //TODO: Think then Remove

        this.getParentFrame().pack();

        this.addComponentListener(new ComponentListener() {

            public void componentHidden(ComponentEvent arg0) {
            }

            public void componentMoved(ComponentEvent arg0) {
            }

            public void componentResized(ComponentEvent arg0) {
                worldPanel.getWorld().setWidth(worldPanel.getWidth(), false);
                worldPanel.getWorld().setHeight(worldPanel.getHeight(), false);
            }

            public void componentShown(ComponentEvent arg0) {
            }
        });

        worldPanel.getWorld().addListener(new WorldListener() {

            public void updated() {
            }

            public void entityAdded(OdorWorldEntity entity) {
            }

            public void entityChanged(OdorWorldEntity entity) {
            }

            public void entityRemoved(OdorWorldEntity entity) {
            }

            public void sensorAdded(Sensor sensor) {
            }

            public void sensorRemoved(Sensor sensor) {
            }

            public void effectorRemoved(Effector effector) {
            }

            public void effectorAdded(Effector effector) {
            }

            public void propertyChanged() {
                setGuiSizeToWorldSize();
            }

        });

    }

    /**
     * Sets the size of the gui panel based on the "logical" size of the world
     * object.
     */
    private void setGuiSizeToWorldSize() {
        worldPanel.setPreferredSize(new Dimension(worldPanel.getWorld()
                .getWidth(), worldPanel.getWorld().getHeight()));
        worldPanel.setSize(new Dimension(worldPanel.getWorld().getWidth(),
                worldPanel.getWorld().getHeight()));
        this.getParentFrame().pack();
    }

    /**
     * Return the odor world.
     *
     * @return Odor world
     */
    public OdorWorldPanel getWorldPanel() {
        return worldPanel;
    }

    @Override
    public void postAddInit() {
        menu = new OdorWorldFrameMenu(this);
        menu.setUpMenus();
        getParentFrame().setJMenuBar(menu); // TODO: Move menu creation to this
                                            // class?
        getParentFrame().pack(); // Force a repaint
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

    @Override
    public void closing() {
        // TODO Auto-generated method stub
    }

}
