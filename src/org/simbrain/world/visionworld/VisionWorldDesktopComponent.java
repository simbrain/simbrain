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

import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.workspace.gui.CouplingMenuComponent;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.world.visionworld.filter.PixelAccumulator;
import org.simbrain.world.visionworld.pixelmatrix.BufferedImagePixelMatrix;
import org.simbrain.world.visionworld.sensormatrix.DenseSensorMatrix;

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
            //toolBar.add(action);
        }

        //toolBar.addSeparator();

        JMenu view = new JMenu("View");
        for (Action action : component.getVisionWorld().getViewMenuActions()) {
            view.add(action);
            toolBar.add(action);
        }

        menuBar.add(file);
        menuBar.add(edit);
        menuBar.add(view);

        menuBar.add(new CouplingMenuComponent("Couple", this.getWorkspaceComponent()
                .getWorkspace(), this.getWorkspaceComponent()));

        getParentFrame().setJMenuBar(menuBar);

        setLayout(new BorderLayout());
        add("North", toolBar);
        add("Center", component.getVisionWorld());
    }

    /**
     * {@inheritDoc}
     */
    public void postAddInit() {

        // Open with a default pixel matrix
        PixelMatrix pixelMatrix = new BufferedImagePixelMatrix(100, 100);
        getWorkspaceComponent().getVisionWorld().getModel().setPixelMatrix(
                pixelMatrix);
        Filter defaultFilter = new PixelAccumulator();
        SensorMatrix sensorMatrix = new DenseSensorMatrix(10, 10, 10, 10,
                defaultFilter);
        getWorkspaceComponent().getVisionWorld().getModel().setSensorMatrix(
                sensorMatrix);
        setVisible(true);
    }

    @Override
    public void closing() {
        // empty
    }
}
