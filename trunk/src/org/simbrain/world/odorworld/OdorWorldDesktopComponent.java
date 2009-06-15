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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JScrollPane;

import org.simbrain.workspace.Attribute;
import org.simbrain.workspace.AttributeHolder;
import org.simbrain.workspace.WorkspaceComponentListener;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.workspace.gui.GenericFrame;

import com.thoughtworks.xstream.XStream;


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
    public OdorWorldDesktopComponent(GenericFrame frame, OdorWorldComponent component) {
        super(frame, component);
        init();
    }

    /**
     * Initializes frame.
     */
    public void init() {
        setLayout(new BorderLayout());
        worldPanel = new OdorWorldPanel(this);
        setPreferredSize(new Dimension(worldPanel.getWorld().getWorldWidth(),worldPanel.getWorld().getWorldHeight()));
        add("Center", worldPanel);
        menu = new OdorWorldFrameMenu(this);
        menu.setUpMenus();
        this.addComponentListener(new ComponentListener() {  

				public void componentHidden(ComponentEvent arg0) {
				}

				public void componentMoved(ComponentEvent arg0) {
				}

				public void componentResized(ComponentEvent arg0) {
					worldPanel.getWorld().setWorldWidth(worldPanel.getWidth());
					worldPanel.getWorld().setWorldHeight(worldPanel.getHeight());					
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
        worldPanel.setWorld(this.getWorkspaceComponent().getWorld());
        worldPanel.repaint();
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
    @Override
    public void update() {
        worldPanel.repaint();
    }


}
