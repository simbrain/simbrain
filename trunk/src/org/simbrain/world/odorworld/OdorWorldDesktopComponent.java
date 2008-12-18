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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JScrollPane;

import org.simbrain.workspace.WorkspaceComponentListener;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.workspace.gui.GenericFrame;

import com.thoughtworks.xstream.XStream;


/**
 * <b>WorldPanel</b> is the container for the world component.   Handles toolbar buttons, and serializing of world
 * data.  The main environment codes is in {@link OdorWorldPanel}.
 */
public class OdorWorldDesktopComponent extends GuiComponent<OdorWorldComponent> implements WorkspaceComponentListener {

    private static final long serialVersionUID = 1L;

    /** The height of the scrollbar (used for resizing). */
    private static final int SCROLLBAR_HEIGHT = 75;

    /** The width of the scrollbar (used for resizing). */
    private static final int SCROLLBAR_WIDTH = 29;

   /** Allows the world to be scrolled if it is bigger than the display window. */
    private JScrollPane worldScroller = new JScrollPane();

    /** Odor world to be in frame. */
    private OdorWorldPanel worldPanel;

    /** Odor world frame menu. */
    private OdorWorldFrameMenu menu;

    /**
     * Default constructor.
     */
    public OdorWorldDesktopComponent(GenericFrame frame, OdorWorldComponent component) {
        super(frame, component);
        component.addListener(this);
        init();
    }

    /**
     * Initializes frame.
     */
    public void init() {
        setLayout(new BorderLayout());
        add("Center", worldScroller);
        worldPanel = new OdorWorldPanel(this);
        setPreferredSize(new Dimension(worldPanel.getWorld().getWorldWidth(),worldPanel.getWorld().getWorldHeight()));
        worldScroller.setViewportView(worldPanel);
        worldScroller.setEnabled(false);
        menu = new OdorWorldFrameMenu(this);
        menu.setUpMenus();
        setMaxSize();
    }


    /**
     * Sets maximum size for the parent window.
     */
    public void setMaxSize() {
        this.setMaximumSize(new Dimension(worldPanel.getWorld().getWorldWidth() + SCROLLBAR_WIDTH, worldPanel.getWorld().getWorldHeight() + SCROLLBAR_HEIGHT));
        this.setBounds(getX(), getY(), worldPanel.getWorld().getWorldWidth() + SCROLLBAR_WIDTH, worldPanel.getWorld().getWorldHeight() + SCROLLBAR_HEIGHT);
        worldPanel.setPreferredSize(new Dimension(worldPanel.getWorld().getWorldWidth(), worldPanel.getWorld().getWorldHeight()));
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

    public void componentUpdated() {
        worldPanel.repaint();        
    }
}
