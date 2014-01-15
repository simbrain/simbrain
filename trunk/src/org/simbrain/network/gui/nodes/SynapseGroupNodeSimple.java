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
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;

import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.group.SynapseGroupDialog;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;

/**
 * PNode representation of a group of synapses, where the synapses themselves
 * are not visible.
 *
 * @author jyoshimi
 */
public class SynapseGroupNodeSimple extends SynapseGroupNode {

    /** Line connecting nodes. */
    private PPath line;

    /**
     * Create a Synapse Group PNode.
     *
     * @param networkPanel parent panel
     * @param group the synapse group
     */
    public SynapseGroupNodeSimple(final NetworkPanel networkPanel,
            final SynapseGroup group) {
        super(networkPanel, group);
        line = new PPath();
        this.addChild(line);
        line.setStrokePaint(Color.BLACK);
        line.moveToBack();
    }

    /**
     * Override PNode layoutChildren method in order to properly set the
     * positions of children nodes.
     */
    @Override
    public void layoutChildren() {
        double srcX = synapseGroup.getSourceNeuronGroup().getCenterX();
        double srcY = synapseGroup.getSourceNeuronGroup().getCenterY();
        double tarX = synapseGroup.getTargetNeuronGroup().getCenterX();
        double tarY = synapseGroup.getTargetNeuronGroup().getCenterY();
        double x = (srcX + tarX) / 2;
        double y = (srcY + tarY) / 2;
        interactionBox.setOffset(x - interactionBox.getWidth() / 2, y
                - interactionBox.getHeight() / 2);
        Point2D.Double srcPoint = new Point2D.Double(srcX, srcY);
        Point2D.Double tarPoint = new Point2D.Double(tarX, tarY);
        line.setPathToPolyline(new Point2D.Double[] { srcPoint, tarPoint });
    }

}
