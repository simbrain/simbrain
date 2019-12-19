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

import org.piccolo2d.PNode;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.NeuronGroupNode.Port;
import org.simbrain.util.Pair;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.widgets.DirectedCubicArrow;
import org.simbrain.util.widgets.DirectedCubicArrow.BezierTemplate;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PNode representation of a "green arrow" (representing a group of synapses) from one
 * NeuronGroup to another.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
@SuppressWarnings("serial")
public class SynapseGroupNodeSimple extends PNode implements SynapseGroupNode.Arrow {

    private static final float DEFAULT_ARROW_THICKNESS = 30;

    protected static final Color DEFAULT_COLOR = Color.GREEN;

    private DirectedCubicArrow arrow;

    private Port startPort;

    private Port endPort;

    private Point2D startPoint;

    private Point2D endPoint;

    private final NeuronGroupNode sourceNode;

    private final NeuronGroupNode targetNode;

    private final NeuronGroup source;

    private final NeuronGroup target;

    private final NetworkPanel networkPanel;

    private final AtomicBoolean halt = new AtomicBoolean();

    private final SynapseGroup group;

    private final SynapseGroupNode synapseGroupNode;

    /**
     * Create a Synapse Group PNode.
     */
    public SynapseGroupNodeSimple(final NetworkPanel networkPanel, final SynapseGroupNode node) {
        this.networkPanel = networkPanel;
        this.group = node.getSynapseGroup();
        this.synapseGroupNode = node;

        // this.dbLine = new PPath.Float(new BasicStroke(5,
        // BasicStroke.CAP_ROUND,
        // BasicStroke.JOIN_MITER));
        // dbLine.setStrokePaint(Color.BLACK);
        source = group.getSourceNeuronGroup();
        target = group.getTargetNeuronGroup();
        sourceNode = (NeuronGroupNode) getNetworkPanel().getObjectNodeMap().get(group.getSourceNeuronGroup());
        targetNode = (NeuronGroupNode) getNetworkPanel().getObjectNodeMap().get(group.getTargetNeuronGroup());
        arrow = new DirectedCubicArrow(BezierTemplate.DIRECTED, DEFAULT_COLOR, 0.5f, DEFAULT_ARROW_THICKNESS);
        this.addChild(arrow);

        // this.addChild(dbLine);
    }

    /**
     * Override PNode layoutChildren method in order to properly set the
     * positions of children nodes.
     */
    @Override
    public void layoutChildren() {
        if (getNetworkPanel().isRunning()) {
            return;
        }
        if (halt.get()) {
            return;
        }
        if (group.isMarkedForDeletion()) {
            return;
        }
         if((sourceNode == null) || (targetNode == null)) {
             return;
         }

        determinePortsAndPoints();

        layout(startPoint, endPoint);

    }

    /**
     * Position the arrow given starting and ending ports (N,S,E,W) and points.
     */
    public void layout(Point2D src, Point2D tar) {
        if (networkPanel.isRunning()) {
            return;
        }
        arrow.layoutChildren(src, startPort, tar, endPort);

        Point2D.Float bez = arrow.getTemplate().getBez1(src, tar, startPort);
        Point2D.Float bez2 = arrow.getTemplate().getBez2(src, tar, endPort);
        Point2D middle = SimbrainMath.cubicBezierMidpoint(src, bez, bez2, tar);

        synapseGroupNode.interactionBox.centerFullBoundsOnPoint(middle.getX(), middle.getY());
        synapseGroupNode.interactionBox.raiseToTop();

    }

    public Point2D midpoint(Point2D src, Point2D tar) {
        Point2D.Float bez = arrow.getTemplate().getBez1(src, tar, startPort);
        Point2D.Float bez2 = arrow.getTemplate().getBez2(src, tar, endPort);
        return SimbrainMath.cubicBezierMidpoint(src, bez, bez2, tar);
    }

    /**
     * Sets ports (N,S,W,E) and their start and end points, which are used in layout.
     */
    public void determinePortsAndPoints() {

        // Create a list of 16 pairs, one for each combination of (N,S,W,E) ports. Associate each of these
        // with a vector from the point associated with the source port to the point associated with the target port
        List<Pair<
                Pair<Map.Entry<Port, Point2D>, Map.Entry<Port, Point2D>>, // source, target pair
                Point2D>> vectors = new ArrayList<>();                    // associated vector
        for (Map.Entry<Port, Point2D> sp : source.getMidPointOfFourEdges().entrySet()) {
            for (Map.Entry<Port, Point2D> tp : target.getMidPointOfFourEdges().entrySet()) {
                vectors.add(new Pair<>(new Pair<>(sp, tp), SimbrainMath.subtract(tp.getValue(), sp.getValue())));
            }
        }

        // Filter out small distances
        // Get the pair with the minimum distance
        Optional<Pair<Pair<Map.Entry<Port, Point2D>, Map.Entry<Port, Point2D>>, Point2D>> bestPair = vectors.stream()
                .filter(v -> SimbrainMath.magnitudeSq(v.getSecond()) > DEFAULT_ARROW_THICKNESS * DEFAULT_ARROW_THICKNESS)
                .min((p1, p2) -> SimbrainMath.distanceComparator.compare(p1.getSecond(), p2.getSecond()));

        if (bestPair.isPresent()) {
            startPoint = bestPair.get().getFirst().getFirst().getValue();
            endPoint = bestPair.get().getFirst().getSecond().getValue();
            startPort = bestPair.get().getFirst().getFirst().getKey();
            endPort = bestPair.get().getFirst().getSecond().getKey();
        } else {
            startPort = Port.NORTH;
            endPort = Port.SOUTH;
            startPoint = source.getMidPointOfFourEdges().get(Port.NORTH);
            endPoint = target.getMidPointOfFourEdges().get(Port.SOUTH);
        }

    }

    /**
     * TODO: Not sure why below is needed. Without the explicit
     * null sets a fatal error occurs in the JRE.
     */
    @Override
    public synchronized void removeFromParent() {
        halt.getAndSet(true);
        if (startPort != null) {
            sourceNode.removeSynapseDock(startPort, this);
        }
        if (endPort != null) {
            targetNode.removeSynapseDock(endPort, this);
        }
        arrow = null;
        super.removeFromParent();
    }

    public Port getStartPort() {
        return startPort;
    }

    public void setStartPort(Port startPort) {
        this.startPort = startPort;
    }

    public SynapseGroup getSynapseGroup() {
        return group;
    }

    public SynapseGroup getGroup() {
        return group;
    }

    public NeuronGroupNode getSourceNode() {
        return sourceNode;
    }

    public NeuronGroupNode getTargetNode() {
        return targetNode;
    }

    public NeuronGroup getSource() {
        return source;
    }

    public NeuronGroup getTarget() {
        return target;
    }

    public NetworkPanel getNetworkPanel() {
        return networkPanel;
    }
}
