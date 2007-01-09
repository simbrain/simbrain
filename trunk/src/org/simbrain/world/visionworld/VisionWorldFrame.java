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

import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.simbrain.world.visionworld.pixelmatrix.EditableBufferedImagePixelMatrix;

import org.simbrain.world.visionworld.sensormatrix.SparseSensorMatrix;

import org.simbrain.world.visionworld.views.StackedView;

import org.simbrain.workspace.Workspace;

/**
 * Vision world frame.
 */
public final class VisionWorldFrame
    extends JInternalFrame {

    /** Default title. */
    private static final String DEFAULT_TITLE = "Vision world";

    /** Resizeable flag. */
    private static final boolean RESIZEABLE = true;

    /** Closeable flag. */
    private static final boolean CLOSEABLE = true;

    /** Maximizeable flag. */
    private static final boolean MAXIMIZEABLE = true;

    /** Iconifiable flag. */
    private static final boolean ICONIFIABLE = true;

    /** Workspace. */
    private final Workspace workspace;

    /** Reference to stacked view for (temporary?) solution to repaint problem. */    
    StackedView stackedView;

    /**
     * Create a new vision world frame with the specified workspace.
     *
     * @param workspace workspace, must not be null
     */
    public VisionWorldFrame(final Workspace workspace) {
        super(DEFAULT_TITLE, RESIZEABLE, CLOSEABLE, MAXIMIZEABLE, ICONIFIABLE);
        if (workspace == null) {
            throw new IllegalArgumentException("workspace must not be null");
        }
        this.workspace = workspace;

        // todo:  just for demonstration at the moment
        // creates circular package dependencies!
        PixelMatrix pixelMatrix = new EditableBufferedImagePixelMatrix(50, 50);
        SensorMatrix sensorMatrix = new SparseSensorMatrix(10, 10, 5, 5);
        VisionWorldModel visionWorldModel = new ImmutableVisionWorldModel(pixelMatrix, sensorMatrix);
        VisionWorld visionWorld = new VisionWorld(visionWorldModel);
        stackedView = new StackedView(visionWorld);
        setContentPane(stackedView);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JMenuBar menuBar = new JMenuBar();
        JMenu file = new JMenu("File");
        menuBar.add(file);
        setJMenuBar(menuBar);
    }

    /**
     * Temporary solution to camera centering problem.
     */
    public void repaintIt() {
        stackedView.repaintIt();
    }
}
