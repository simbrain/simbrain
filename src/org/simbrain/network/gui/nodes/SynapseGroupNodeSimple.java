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
package org.simbrain.network.gui.nodes;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;

import org.piccolo2d.nodes.PPath;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;

/**
 * PNode representation of a group of synapses, where the synapses themselves
 * are not visible.
 *
 * @author jyoshimi
 */
public class SynapseGroupNodeSimple extends SynapseGroupNode {

    /** Line connecting nodes. */
    private PPath.Float curve;

    /**
     * Create a Synapse Group PNode.
     *
     * @param networkPanel parent panel
     * @param group the synapse group
     */
    public SynapseGroupNodeSimple(final NetworkPanel networkPanel,
            final SynapseGroup group) {
        super(networkPanel, group);
        curve = new PPath.Float();
        this.addChild(curve);
        curve.setStrokePaint(Color.BLACK);
        curve.lowerToBottom();
    }

    /**
     * Override PNode layoutChildren method in order to properly set the
     * positions of children nodes.
     */
    @Override
    public void layoutChildren() {
        float srcX = (float) synapseGroup.getSourceNeuronGroup().getCenterX();
        float srcY = (float) synapseGroup.getSourceNeuronGroup().getCenterY();
        float tarX = (float) synapseGroup.getTargetNeuronGroup().getCenterX();
        float tarY = (float) synapseGroup.getTargetNeuronGroup().getCenterY();
        float x = (srcX + tarX) / 2;
        float y = (srcY + tarY) / 2;
        float slope = (tarY - srcY) / (tarX - srcX);
        float distanceToMidpoint = (float) Point2D.distance(srcX, srcY, x, y);
        float bez_x = (float) Math.sqrt(Math.pow(distanceToMidpoint, 2)
                / (1 + Math.pow(1 / slope, 2)));
        float bez_y = bez_x * 1 / slope;

        QuadCurve2D.Float theCurve = new QuadCurve2D.Float(srcX, srcY, x
                + bez_x, y + bez_y, tarX, tarY);
        curve.append(theCurve, false);

        interactionBox.setOffset(x - interactionBox.getWidth() / 2, y
                - interactionBox.getHeight() / 2);
    }

}
