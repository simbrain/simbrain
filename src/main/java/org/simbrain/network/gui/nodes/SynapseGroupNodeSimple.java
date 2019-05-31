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

import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.NeuronGroupNode.Port;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.widgets.DirectedCubicArrow;
import org.simbrain.util.widgets.DirectedCubicArrow.BezierTemplate;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PNode representation of a group of synapses, where the synapses themselves
 * are not visible.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
@SuppressWarnings("serial")
public class SynapseGroupNodeSimple extends SynapseGroupNode implements SynapseGroupArrow {

    private static final float DEFAULT_ARROW_THICKNESS = 30;

    protected static final Color DEFAULT_COLOR = Color.GREEN;

    private DirectedCubicArrow arrow;

    private Point2D startPt;

    private Point2D endPt;

    private Port startPort;

    private Port endPort;

    private final NeuronGroupNode sourceNode;

    private final NeuronGroupNode targetNode;

    private final NeuronGroup source;

    private final NeuronGroup target;

    // private final PPath.Float dbLine;

    private final AtomicBoolean halt = new AtomicBoolean();

    private final SynapseGroup group;

    private final double[] srcZoneBoundaries = new double[4];

    /**
     * Create a Synapse Group PNode.
     *
     * @param networkPanel parent panel
     * @param group        the synapse group
     */
    public SynapseGroupNodeSimple(final NetworkPanel networkPanel, final SynapseGroup group) {
        super(networkPanel, group);
        this.group = group;
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
        Point2D[] corners = source.getFourCorners();
        srcZoneBoundaries[0] = Math.atan2(corners[0].getY(), corners[0].getX());
        srcZoneBoundaries[1] = Math.atan2(corners[1].getY(), corners[1].getX());
        srcZoneBoundaries[2] = Math.atan2(corners[2].getY(), corners[2].getX());
        srcZoneBoundaries[3] = Math.atan2(corners[3].getY(), corners[3].getX());

        // this.addChild(dbLine);
    }

    public SynapseGroupNodeSimple(final NetworkPanel networkPanel, final SynapseGroup group, final float thickness) {
        super(networkPanel, group);
        this.group = group;
        source = group.getSourceNeuronGroup();
        target = group.getTargetNeuronGroup();
        sourceNode = (NeuronGroupNode) getNetworkPanel().getObjectNodeMap().get(group.getSourceNeuronGroup());
        targetNode = (NeuronGroupNode) getNetworkPanel().getObjectNodeMap().get(group.getTargetNeuronGroup());
        arrow = new DirectedCubicArrow(BezierTemplate.DIRECTED, DEFAULT_COLOR, 0.5f, thickness);
        this.addChild(arrow);
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
        // if((sourceNode == null) || (targetNode == null)) {
        //     return;
        // }

        determineProperEndPoints();
        Point2D src = sourceNode.getDockingPoint(startPort, this);
        Point2D tar = targetNode.getDockingPoint(endPort, this);
        if (src == null || tar == null) {
            return;
        }
        layout(src, tar);

    }

    /**
     * @param pt1
     * @param pt2
     */
    public synchronized void layoutChildrenQuiet(Point2D pt1, Point2D pt2) {
        if (getNetworkPanel().isRunning()) {
            return;
        }
        halt.getAndSet(true);
        if (pt1 == null) {
            if (this.startPt == null) {
                this.startPt = getOpposingDefaultPosition(target);
            }
        } else {
            this.startPt = pt1;
        }

        if (pt2 == null) {
            if (this.endPt == null) {
                this.endPt = getOpposingDefaultPosition(source);
            }
        } else {
            this.endPt = pt2;
        }
        layout(this.startPt, this.endPt);
        halt.getAndSet(false);
    }

    /**
     * @param src
     * @param tar
     */
    public void layout(Point2D src, Point2D tar) {
        if (networkPanel.isRunning()) {
            return;
        }
        arrow.layoutChildren(src, startPort, tar, endPort);

        Point2D.Float bez = arrow.getTemplate().getBez1(src, tar, startPort);
        Point2D.Float bez2 = arrow.getTemplate().getBez2(src, tar, endPort);
        Point2D middle = SimbrainMath.cubicBezierMidpoint(src, bez, bez2, tar);

        // Line2D dbL = new Line2D.Float(src, bez2);
        // dbLine.reset();
        // dbLine.append(dbL, false);

        interactionBox.centerFullBoundsOnPoint(middle.getX(), middle.getY());
        interactionBox.raiseToTop();

    }

    /**
     * @param src
     * @param tar
     * @return
     */
    public Point2D midpoint(Point2D src, Point2D tar) {
        Point2D.Float bez = arrow.getTemplate().getBez1(src, tar, startPort);
        Point2D.Float bez2 = arrow.getTemplate().getBez2(src, tar, endPort);
        return SimbrainMath.cubicBezierMidpoint(src, bez, bez2, tar);
    }

    // /**
    // *
    // * @return
    // */
    // private Point2D closestPoint() {
    // Point2D [] tarPts = target.getFourCorners();
    // double min = Double.MAX_VALUE;
    // Point2D closest = null;
    // Point2D.Float srcPt = new Point2D.Float((float)source.getCenterX(),
    // (float)source.getCenterY());
    // for (Point2D pt : tarPts) {
    // double dist = srcPt.distance(pt);
    // if (min > dist) {
    // min = dist;
    // closest = pt;
    // }
    // }
    // return closest;
    // }
    //

    /**
     * Includes start and end of arrow.
     */
    public void determineProperEndPoints() {
        if (networkPanel.isRunning()) {
            return;
        }
        float centerXSrc = (float) source.getCenterX();
        float centerYSrc = (float) source.getCenterY();
        float centerXTar = (float) target.getCenterX();
        float centerYTar = (float) target.getCenterY();

        float distance = (float) Point2D.distance(centerXSrc, centerYSrc, centerXTar, centerYTar);

        float theta = (float) Math.atan2(centerYSrc - centerYTar, centerXTar - centerXSrc);

        float zoneModifier = distance * distance / 5000;

        if (sourceNode == null || targetNode == null) {
            return;
        }
        boolean left = target.getMaxX() < (source.getMinX() - zoneModifier);
        boolean right = target.getMinX() > (source.getMaxX() + zoneModifier);
        boolean above = target.getMinY() > source.getMaxY() + zoneModifier;
        boolean below = target.getMaxY() < source.getMinY() - zoneModifier;

        if (theta <= srcZoneBoundaries[0] && theta >= srcZoneBoundaries[3]) {
            startPort = Port.EAST;
            if (above || below) {
                if (above) {
                    endPort = Port.SOUTH;
                } else {
                    endPort = Port.NORTH;
                }
            } else {
                if (centerXTar < centerXSrc) {
                    endPort = Port.EAST;
                } else {
                    endPort = Port.WEST;
                }
            }
        } else if (theta > srcZoneBoundaries[0] && theta <= srcZoneBoundaries[1]) {
            startPort = Port.SOUTH;
            if (left || right) {
                if (left) { // Offset left (I/IIIa)
                    endPort = Port.EAST;
                } else { // Offset right (I/IIIb)
                    endPort = Port.WEST;
                }
            } else { // Not offset I/III
                if (centerYTar < centerYSrc) {
                    endPort = Port.NORTH;
                } else {
                    endPort = Port.SOUTH;
                    if (left || right) {
                        if (left) { // Offset left (I/IIIa)
                            endPort = Port.EAST;
                        } else { // Offset right (I/IIIb)
                            endPort = Port.WEST;
                        }
                    } else { // Not offset I/III
                        if (centerYTar < centerYSrc) {
                            endPort = Port.NORTH;
                        } else {
                            endPort = Port.SOUTH;
                        }
                    }
                }
            }
        } else if (theta > srcZoneBoundaries[1] || theta <= srcZoneBoundaries[2]) {
            startPort = Port.WEST;
            if (above || below) {
                if (above) {
                    endPort = Port.SOUTH;
                } else {
                    endPort = Port.NORTH;
                }
            } else {
                if (centerXTar < centerXSrc) {
                    endPort = Port.EAST;
                } else {
                    endPort = Port.WEST;
                }
            }
        } else {
            startPort = Port.NORTH;
            if (left || right) {
                if (left) { // Offset left (I/IIIa)
                    endPort = Port.EAST;
                } else { // Offset right (I/IIIb)
                    endPort = Port.WEST;
                }
            } else { // Not offset I/III
                if (centerYTar < centerYSrc) {
                    endPort = Port.NORTH;
                } else {
                    endPort = Port.SOUTH;
                    if (left || right) {
                        if (left) { // Offset left (I/IIIa)
                            endPort = Port.EAST;
                        } else { // Offset right (I/IIIb)
                            endPort = Port.WEST;
                        }
                    } else { // Not offset I/III
                        if (centerYTar < centerYSrc) {
                            endPort = Port.NORTH;
                        } else {
                            endPort = Port.SOUTH;
                        }
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc} TODO: Not sure why below is needed. Without the explicit
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

    public Point2D getStartPt() {
        return startPt;
    }

    public void setStartPt(Point2D.Float startPt) {
        this.startPt = startPt;
    }

    public Point2D getEndPt() {
        return endPt;
    }

    public void setEndPt(Point2D.Float endPt) {
        this.endPt = endPt;
    }

    public Port getStartPort() {
        return startPort;
    }

    public void setStartPort(Port startPort) {
        this.startPort = startPort;
    }

    /**
     * Returns a default position...
     *
     * @param ng
     * @return
     */
    public Point2D getOpposingDefaultPosition(NeuronGroup ng) {
        if (group.getSourceNeuronGroup() != ng && group.getTargetNeuronGroup() != ng) {
            throw new IllegalArgumentException("Synapse group does not begin" + " or end in this group.");
        }
        NeuronGroup opposite;
        Port opPort;
        float x = 0;
        float y = 0;
        if (group.getSourceNeuronGroup() == ng) {
            opposite = group.getTargetNeuronGroup();
            opPort = endPort;
        } else {
            opposite = group.getSourceNeuronGroup();
            opPort = startPort;
        }
        if (opPort == Port.NORTH || opPort == Port.SOUTH) {
            x = (float) opposite.getCenterX();
            if (opPort == Port.NORTH) {
                y = (float) opposite.getMaxY();
            } else {
                y = (float) opposite.getMinY();
            }
        } else {
            y = (float) opposite.getCenterY();
            if (opPort == Port.WEST) {
                x = (float) opposite.getMinX();
            } else {
                x = (float) opposite.getMaxX();
            }
        }
        return new Point2D.Float(x, y);
    }

    public SynapseGroup getGroup() {
        return group;
    }

    /**
     * @return the sourceNode
     */
    public NeuronGroupNode getSourceNode() {
        return sourceNode;
    }

    /**
     * @return the targetNode
     */
    public NeuronGroupNode getTargetNode() {
        return targetNode;
    }

    /**
     * @return the source
     */
    public NeuronGroup getSource() {
        return source;
    }

    /**
     * @return the target
     */
    public NeuronGroup getTarget() {
        return target;
    }

    @Override
    public float getRequiredSpacing() {
        return arrow.getStrokeWidth() * 2;
    }

}
