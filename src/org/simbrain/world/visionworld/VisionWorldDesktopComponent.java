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
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.simbrain.workspace.Producer;
import org.simbrain.workspace.gui.DesktopComponent;

/**
 * Vision world frame.
 */
public final class VisionWorldDesktopComponent extends DesktopComponent<VisionWorldComponent> {

    /** Default serial version UID. */
    private static final long serialVersionUID = 1L;
    


    /**
     * Create a new vision world frame with the specified workspace.
     *
     * @param component component, must not be null
     */
    public VisionWorldDesktopComponent(final VisionWorldComponent component) {
        super(component);
        component.addListener(new BasicComponentListener());
        this.setPreferredSize(new Dimension(400,400));


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
        setJMenuBar(menuBar);

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add("North", toolBar);
        contentPane.add("Center", component.getVisionWorld());
    }

    @Override
    public void close() {
        // empty
    }

    @Override
    public String getFileExtension() {
        return null;
    }
    
    @Override
    public void save(final File saveFile) {
        // empty
    }

    @Override
    public void open(File openFile) {
        // TODO Auto-generated method stub
    }
}
