/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.world.visionworld;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JToolBar;

import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * Vision world frame.
 */
public final class VisionWorldDesktopComponent extends GuiComponent<VisionWorldComponent> {

    /** Default serial version UID. */
    private static final long serialVersionUID = 1L;
    


    /**
     * Create a new vision world frame with the specified workspace.
     *
     * @param component component, must not be null
     */
    public VisionWorldDesktopComponent(GenericFrame frame, final VisionWorldComponent component) {
        super(frame, component);
        component.addListener(new BasicComponentListener());
        this.setPreferredSize(new Dimension(400,400));

        // Sets a reference within vision world for use by actions.
        component.getVisionWorld().setVisionWorldDesktopComponent(this);

        JMenuBar menuBar = new JMenuBar();
        JToolBar toolBar = new JToolBar();

        JMenu file = new JMenu("File");
        for (Action action : component.getVisionWorld().getFileMenuActions()) {
            file.add(action);
            toolBar.add(action);
        }

        toolBar.addSeparator();

        JMenu edit = new JMenu("Edit");
        for (Action action : component.getVisionWorld().getEditMenuActions()) {
            edit.add(action);
            toolBar.add(action);
        }

        toolBar.addSeparator();

        JMenu view = new JMenu("View");
        for (Action action : component.getVisionWorld().getViewMenuActions()) {
            view.add(action);
            toolBar.add(action);
        }

        menuBar.add(file);
        menuBar.add(edit);
        menuBar.add(view);
        getParentFrame().setJMenuBar(menuBar);

        setLayout(new BorderLayout());
        add("North", toolBar);
        add("Center", component.getVisionWorld());
    }

    public void postAddInit() { 
        setSize(450, 400); 
        setVisible(true); 
     } 

    @Override
    public void closing() {
        // empty
    }
}
