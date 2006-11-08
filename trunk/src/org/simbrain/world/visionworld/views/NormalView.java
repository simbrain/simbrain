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
package org.simbrain.world.visionworld.views;

import java.awt.Color;
import java.awt.BorderLayout;

import javax.swing.JPanel;

import edu.umd.cs.piccolo.PCanvas;

import edu.umd.cs.piccolo.util.PPaintContext;

import org.simbrain.world.visionworld.EditablePixelMatrix;
import org.simbrain.world.visionworld.SensorMatrix;
import org.simbrain.world.visionworld.VisionWorld;
import org.simbrain.world.visionworld.VisionWorldModel;

import org.simbrain.world.visionworld.nodes.EditablePixelMatrixImageNode;
import org.simbrain.world.visionworld.nodes.PixelMatrixImageNode;
import org.simbrain.world.visionworld.nodes.SensorMatrixNode;

/**
 * Normal view.
 */
public final class NormalView
    extends JPanel {

    /** Canvas. */
    private final PCanvas canvas;

    /** Vision world. */
    private final VisionWorld visionWorld;


    /**
     * Create a new normal view.
     *
     * @param visionWorld vision world, must not be null
     */
    public NormalView(final VisionWorld visionWorld) {
        super();
        if (visionWorld == null) {
            throw new IllegalArgumentException("visionWorld must not be null");
        }
        canvas = new PCanvas();
        canvas.setOpaque(true);
        canvas.setBackground(Color.WHITE);
        canvas.setDefaultRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        canvas.setAnimatingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        canvas.setInteractingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        this.visionWorld = visionWorld;

        setLayout(new BorderLayout());
        add("Center", canvas);

        createNodes();
    }

    /**
     * Create nodes.
     */
    private void createNodes() {
        VisionWorldModel model = visionWorld.getModel();
        if (model.getPixelMatrix() instanceof EditablePixelMatrix) {
            EditablePixelMatrix editablePixelMatrix = (EditablePixelMatrix) model.getPixelMatrix();
            EditablePixelMatrixImageNode editablePixelMatrixNode = new EditablePixelMatrixImageNode(editablePixelMatrix);
            canvas.getLayer().addChild(editablePixelMatrixNode);
        } else {
            PixelMatrixImageNode pixelMatrixNode = new PixelMatrixImageNode(model.getPixelMatrix());
            canvas.getLayer().addChild(pixelMatrixNode);
        }
        for (SensorMatrix sensorMatrix : model.getSensorMatrices()) {
            SensorMatrixNode sensorMatrixNode = new SensorMatrixNode(sensorMatrix);
            canvas.getLayer().addChild(sensorMatrixNode);
        }
    }
}
