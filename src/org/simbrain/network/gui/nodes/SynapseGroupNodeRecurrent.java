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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.geom.Arc2D;
import java.util.concurrent.atomic.AtomicBoolean;

import org.piccolo2d.nodes.PPath;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;

/**
 * Creates a simple synapse group node that represents a recurrent synapse
 * group.
 *
 * @author zach
 *
 */
@SuppressWarnings("serial")
public class SynapseGroupNodeRecurrent extends SynapseGroupNode {

    private PPath arrowHead;

    private PPath arcCurve;

    private float strokeWidth;

    private AtomicBoolean halt = new AtomicBoolean(false);

    /**
     *
     * @param networkPanel
     * @param group
     * @return
     */
    public static SynapseGroupNodeRecurrent createRecurrentSynapseGN(
        final NetworkPanel networkPanel, SynapseGroup group) {
        SynapseGroupNodeRecurrent synGNR = new SynapseGroupNodeRecurrent(
            networkPanel, group);
        synGNR.addChild(synGNR.arcCurve);
        synGNR.addChild(synGNR.arrowHead);
        ((NeuronGroupNode) networkPanel.getObjectNodeMap().get(
            group.getTargetNeuronGroup())).addChild(synGNR);
        synGNR.lowerToBottom();
        // ((NeuronGroupNode)
        // networkPanel.getObjectNodeMap().get(group.getTargetNeuronGroup())).addPropertyChangeListener(synGNR);
        // createArc(getSynapseGroup());
        return synGNR;
    }

    private SynapseGroupNodeRecurrent(NetworkPanel networkPanel,
        SynapseGroup group) {
        super(networkPanel, group);
        if (!group.isRecurrent()) {
            throw new IllegalArgumentException("Using a recurrent synapse node"
                + " for a non-recurrent synapse group.");
        }
        arrowHead = new PPath.Float();
        arcCurve = new PPath.Float();
        arrowHead.setStroke(null);
        // TODO: Below may look a bit better.   But then overlap is visible.   Need to find a way to nicely join the arc and head.
        //arrowHead.setTransparency(0.5f); 
        arrowHead.setPaint(Color.green);
        strokeWidth = (float) (group.getSourceNeuronGroup().getMaxDim() / 6);
        arcCurve.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_SQUARE,
            BasicStroke.JOIN_MITER));
        arcCurve.setStrokePaint(Color.green);
        arcCurve.setTransparency(0.5f);
        arcCurve.setPaint(null);
        arrowHead.lowerBelow(networkPanel.getObjectNodeMap()
        		.get(group.getTargetNeuronGroup().getNeuronList().get(0)));
        arcCurve.lowerToBottom(networkPanel.getObjectNodeMap()
        		.get(group.getTargetNeuronGroup()));
    }

    @Override
    public synchronized void layoutChildren() {
    	if (networkPanel.isRunning()) {
    		return;
    	}
        if (halt.get())
            return;
        NeuronGroup ng = getSynapseGroup().getSourceNeuronGroup();
        float quarterSizeX =
            (float) Math.abs((ng.getMaxX() - ng.getMinX())) / 4;
        float quarterSizeY =
            (float) Math.abs((ng.getMaxY() - ng.getMinY())) / 4;
        float quarterSize = quarterSizeX < quarterSizeY ? quarterSizeX
            : quarterSizeY;
        float qRatio;
        if (quarterSize == 0) { // LineLayout
            quarterSize = (float) ng.getMaxDim() / 15.0f;
        } else if (quarterSize < 30.0f) {
            quarterSize = 30.0f;
        }

        qRatio = quarterSize / (quarterSizeX + quarterSizeY);
        if (Float.isNaN(qRatio)) {
            qRatio = 1;
        }
        Arc2D.Float recArc =
            new Arc2D.Float((float) ng.getCenterX() - 3 * quarterSize / 2,
                (float) ng.getCenterY() - 3 * quarterSize / 2,
                quarterSize * 3,
                quarterSize * 3, 30, 300, Arc2D.OPEN);

        strokeWidth =
            (float) ((qRatio * (ng.getWidth() + ng.getHeight())) / 6);
        arcCurve.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_SQUARE,
            BasicStroke.JOIN_MITER));
        arcCurve.reset();
        arcCurve.append(recArc, false);
        arrowHead.reset();
        double endAng = -(11.0 * Math.PI / 6.0);
        arrowHead.append(
            traceArrowHead(endAng - 3.1 * Math.PI / 6.0, recArc
                .getEndPoint().getX() + 0.9 * strokeWidth, recArc
                .getEndPoint().getY() - 0.9 * 2 * strokeWidth), false);
        interactionBox.centerFullBoundsOnPoint(recArc.getCenterX(),
                recArc.getCenterY());
        interactionBox.raiseToTop();
        arrowHead.lowerToBottom();
        arcCurve.lowerToBottom();
    }

    private Polygon traceArrowHead(double theta, double tarX, double tarY) {
        int numSides = 3;
        int[] triPtx = new int[numSides];
        int[] triPty = new int[numSides];
        double phi = Math.PI / 6;

        triPtx[0] = (int) (tarX - (strokeWidth / 2 * Math.cos(theta)));
        triPty[0] = (int) (tarY - (strokeWidth / 2 * Math.sin(theta)));

        triPtx[1] = (int) (tarX - (2 * strokeWidth * Math.cos(theta + phi)));
        triPty[1] = (int) (tarY - (2 * strokeWidth * Math.sin(theta + phi)));

        triPtx[2] = (int) (tarX - (2 * strokeWidth * Math.cos(theta - phi)));
        triPty[2] = (int) (tarY - (2 * strokeWidth * Math.sin(theta - phi)));

        return new Polygon(triPtx, triPty, numSides);
    }

    @Override
    public synchronized void removeFromParent() {
        halt.getAndSet(true);
        arcCurve = null;
        super.removeFromParent();
    }

}
