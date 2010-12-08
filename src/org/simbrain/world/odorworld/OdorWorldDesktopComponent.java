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

import org.simbrain.network.desktop.NetworkGuiPreferences;
import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * <b>WorldPanel</b> is the container for the world component.   Handles toolbar buttons, and serializing of world
 * data.  The main environment codes is in {@link OdorWorldPanel}.
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
        worldPanel.setPreferredSize(new Dimension(400, 500));
        add("Center", worldPanel);
        menu = new OdorWorldFrameMenu(this);
        menu.setUpMenus();
        getParentFrame().setJMenuBar(menu); // TODO: Move menu creation to this class?
        component.setCurrentDirectory(OdorWorldPreferences.getCurrentDirectory());

        this.getParentFrame().pack();

        this.addComponentListener(new ComponentListener() {

            public void componentHidden(ComponentEvent arg0) {
            }

            public void componentMoved(ComponentEvent arg0) {
            }

            public void componentResized(ComponentEvent arg0) {
                worldPanel.getWorld().setWidth(worldPanel.getWidth());
                worldPanel.getWorld().setHeight(worldPanel.getHeight());
            }

            public void componentShown(ComponentEvent arg0) {
            }
        });

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
        getParentFrame().setJMenuBar(menu); // TODO: Move menu creation to this class?
    }

    /**
     * @return Odor world frame menu.
     */
    public OdorWorldFrameMenu getMenu() {
        return menu;
    }

    /**
     * Sets odor world frame menu.
     * @param menu Menu
     */
    public void setMenu(final OdorWorldFrameMenu menu) {
        this.menu = menu;
    }

    @Override
    public void closing() {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see org.simbrain.workspace.gui.GuiComponent#update()
     */
//    @Override
    // Commented this out since it has no obvious effect
//    protected void update() {
//        worldPanel.repaint();
//    }

}
