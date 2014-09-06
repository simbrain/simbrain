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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.piccolo2d.PNode;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.NeuronGroupNode.Port;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.widgets.DirectedCubicArrow;
import org.simbrain.util.widgets.DirectedCubicArrow.BezierTemplate;

/**
 * A bidirectional simple synapse group node. Uses two directed cubic arrows to
 * indicate bidirectionality. Essentially this is a compound group node as it
 * represents and contains two separate synapse groups which make it
 * "bidirectional".
 *
 * @author Zach Tosi
 *
 */
@SuppressWarnings("serial")
public class SynapseGroupNodeBidirectional extends PNode implements
    PropertyChangeListener, SynapseGroupArrow, GroupNode {

    /** The default thickness. */
    private static final float DEFAULT_ARROW_THICKNESS = 30;

    /**
     * The default buffer, or number of pixels on either side of this node to
     * separate it from its source and target neuron group nodes.
     */
    private static final float DEFAULT_BUFFER = 15;

    /**
     * The standard color of all synapse group nodes.
     */
    private static final Color DEFAULT_COLOR = Color.GREEN;

    /** The network panel this node will be placed in. */
    private final NetworkPanel networkPanel;

    /**
     * The "source" neuron group node, defined as the source group of the first
     * synapse group passed to this object's constructor. That would make it the
     * target group of the second neuron group passed to this object's
     * constructor.
     */
    private final NeuronGroupNode srcGroupNode;

    /**
     * The "target" neuron group node, defined as the target group of the first
     * synapse group passed to this object's constructor.That would make it the
     * source group of the second neuron group passed to this object's
     * constructor.
     */
    private final NeuronGroupNode tarGroupNode;

    /**
     * The "source" neuron group, defined as the source group of the first
     * synapse group passed to this object's constructor. That would make it the
     * target group of the second neuron group passed to this object's
     * constructor.
     */
    private final NeuronGroup srcNGroup;

    /**
     * The "target" neuron group, defined as the target group of the first
     * synapse group passed to this object's constructor.That would make it the
     * source group of the second neuron group passed to this object's
     * constructor.
     */
    private final NeuronGroup tarNGroup;

    /** The first synapse group making up this node. */
    private final SynapseGroup synGroup1;

    /** The second synapse group making up this node. */
    private final SynapseGroup synGroup2;

    /** The interaction box for the first synapse group. */
    private final SynapseGroupInteractionBox synGroup1Box;

    /** The interaction box for the second synapse group. */
    private final SynapseGroupInteractionBox synGroup2Box;

    /**
     * The arrow corresponding to the first synapse group. This should point
     * toward the target neuron group node of the first synapse group.
     */
    private DirectedCubicArrow arrow1;

    /**
     * The arrow corresponding to the second synapse group. This should point
     * toward the source neuron group node of the first synapse group.
     */
    private DirectedCubicArrow arrow2;

    /**
     * The port the source neuron group node of the first synapse group has
     * assigned to this group node.
     */
    private Port srcPort;

    /**
     * The port the target neuron group node of the first synapse group has
     * assigned to this group node.
     */
    private Port tarPort;

    /**
     * The point the source neuron group node of the first synapse group has
     * assigned to this group node on the source port.
     */
    private Point2D srcPt;

    /**
     * The point the target neuron group node of the first synapse group has
     * assigned to this group node on the target port.
     */
    private Point2D tarPt;

    /**
     * Used to maintain synchronization when multiple piccolo threads attempt to
     * lay out this node simultaneously.
     */
    private AtomicBoolean halt = new AtomicBoolean();

    private final double[] srcZoneBoundaries = new double[4];

    /**
     * Create a bidirectional simple synapse group PNode.
     *
     * @param networkPanel
     *            parent panel
     * @param group
     *            the synapse group
     */
    public static SynapseGroupNodeBidirectional createBidirectionalSynapseGN(
        final NetworkPanel networkPanel, final SynapseGroup synGroup1,
        final SynapseGroup synGroup2) {
        SynapseGroupNodeBidirectional synGNBi =
            new SynapseGroupNodeBidirectional(
                networkPanel, synGroup1, synGroup2);
        synGNBi.addChild(synGNBi.arrow1);
        synGNBi.addChild(synGNBi.arrow2);
        synGNBi.addChild(synGNBi.synGroup1Box);
        synGNBi.addChild(synGNBi.synGroup2Box);
        synGNBi.srcGroupNode.addPropertyChangeListener(PNode.PROPERTY_BOUNDS,
            synGNBi);
        synGNBi.tarGroupNode.addPropertyChangeListener(PNode.PROPERTY_BOUNDS,
            synGNBi);
        return synGNBi;
    }

    /**
     * Create a bidirectional simple synapse group PNode.
     *
     * @param networkPanel
     *            parent panel
     * @param group
     *            the synapse group
     */
    private SynapseGroupNodeBidirectional(final NetworkPanel networkPanel,
        final SynapseGroup synGroup1, final SynapseGroup synGroup2) {
        consistencyCheck(synGroup1, synGroup2);
        this.networkPanel = networkPanel;
        this.synGroup1 = synGroup1;
        this.synGroup2 = synGroup2;
        synGroup1Box = new SynapseGroupInteractionBox(networkPanel, synGroup1);
        synGroup2Box = new SynapseGroupInteractionBox(networkPanel, synGroup2);
        srcNGroup = synGroup1.getSourceNeuronGroup();
        tarNGroup = synGroup1.getTargetNeuronGroup();
        srcGroupNode = (NeuronGroupNode) networkPanel.getObjectNodeMap().get(
            srcNGroup);
        tarGroupNode = (NeuronGroupNode) networkPanel.getObjectNodeMap().get(
            tarNGroup);
        arrow1 = new DirectedCubicArrow(BezierTemplate.BIDIRECTIONAL,
            DEFAULT_COLOR, 0.5f, DEFAULT_ARROW_THICKNESS);
        arrow2 = new DirectedCubicArrow(BezierTemplate.BIDIRECTIONAL,
            DEFAULT_COLOR, 0.5f, DEFAULT_ARROW_THICKNESS);

        synGroup1Box.setText(synGroup1.getLabel());
        synGroup2Box.setText(synGroup2.getLabel());

        Point2D[] corners = srcNGroup.getFourCorners();
        srcZoneBoundaries[0] = Math.atan2(corners[0].getY(), corners[0].getX());
        srcZoneBoundaries[1] = Math.atan2(corners[1].getY(), corners[1].getX());
        srcZoneBoundaries[2] = Math.atan2(corners[2].getY(), corners[2].getX());
        srcZoneBoundaries[3] = Math.atan2(corners[3].getY(), corners[3].getX());

        synGroup1Box.updateText();
        synGroup2Box.updateText();
    }

    /**
     * Checks to make sure that this node is being appropriately applied.
     * Namely, that the target group of the first synapse group equals the
     * source group of the second synapse group and vice-versa.
     *
     * @param group1
     * @param group2
     * @throws IllegalStateException
     *             if the synapse groups in question don't actually qualify as
     *             bidirectional when taken together.
     */
    private void consistencyCheck(SynapseGroup group1, SynapseGroup group2)
        throws IllegalStateException {
        boolean state1Error = !(group1.getTargetNeuronGroup().equals(group2
            .getSourceNeuronGroup()));
        boolean state2Error = !(group1.getSourceNeuronGroup().equals(group2
            .getTargetNeuronGroup()));
        if (state1Error || state2Error) {
            String errMsg = "Illegal instance of a bidirectional"
                + " synapse group node: ";
            if (state1Error) {
                errMsg.concat("The target group of " + group1.getLabel()
                    + " does not equal the source group of "
                    + group2.getLabel());
            }
            if (state2Error) {
                if (state1Error) {
                    errMsg.concat(" and ");
                }
                errMsg.concat("The target group of " + group2.getLabel()
                    + " does not equal the source group of "
                    + group1.getLabel());
            }
            throw new IllegalArgumentException(errMsg);
        } else {
            return;
        }
    }

    /**
     * Override PNode layoutChildren method in order to properly set the
     * positions of children nodes.
     */
    @Override
    public void layoutChildren() {
        if (halt.get()) {
            return;
        }

        if (synGroup1.isMarkedForDeletion() || synGroup2.isMarkedForDeletion()) {
            return;
        }

        determineProperEndPoints();
        Point2D src = srcGroupNode.getDockingPoint(srcPort, this);
        Point2D tar = tarGroupNode.getDockingPoint(tarPort, this);
        if (src == null || tar == null) {
            System.out.println("Null");
            return;
        }
        layout(src, tar);

    }

    @Override
    public void layout(Point2D src, Point2D tar) {

        float offset = (2 * DEFAULT_ARROW_THICKNESS + DEFAULT_BUFFER) / 2;

        Point2D stOffset = getOffset(getTheta(srcPort), offset);
        Point2D endOffset = getOffset(getTheta(tarPort), offset);

        Point2D ar1StPt = new Point2D.Float(
            (float) (src.getX() + stOffset.getX()),
            (float) (src.getY() + stOffset.getY()));
        Point2D ar1EndPt = new Point2D.Float(
            (float) (tar.getX() - endOffset.getX()),
            (float) (tar.getY() - endOffset.getY()));

        Point2D ar2StPt = new Point2D.Float(
            (float) (tar.getX() + endOffset.getX()),
            (float) (tar.getY() + endOffset.getY()));
        Point2D ar2EndPt = new Point2D.Float(
            (float) (src.getX() - stOffset.getX()),
            (float) (src.getY() - stOffset.getY()));

        arrow1.layoutChildren(ar1StPt, srcPort, ar1EndPt, tarPort);
        arrow2.layoutChildren(ar2StPt, tarPort, ar2EndPt, srcPort);

        Point2D.Float bezAr1 = arrow1.getTemplate().getBez1(ar1StPt, ar1EndPt,
            srcPort);
        Point2D.Float bez2Ar1 = arrow1.getTemplate().getBez2(ar1StPt, ar1EndPt,
            tarPort);
        Point2D middleAr1 = SimbrainMath.cubicBezierMidpoint(ar1StPt, bezAr1,
            bez2Ar1, ar1EndPt);

        Point2D.Float bezAr2 = arrow2.getTemplate().getBez1(ar2StPt, ar2EndPt,
            tarPort);
        Point2D.Float bez2Ar2 = arrow2.getTemplate().getBez2(ar2StPt, ar2EndPt,
            srcPort);
        Point2D middleAr2 = SimbrainMath.cubicBezierMidpoint(ar2StPt, bezAr2,
            bez2Ar2, ar2EndPt);

        synGroup1Box.setOffset(middleAr1.getX() - synGroup1Box.getWidth() / 2,
            middleAr1.getY() - synGroup1Box.getHeight() / 2);

        synGroup2Box.setOffset(middleAr2.getX() - synGroup2Box.getWidth() / 2,
            middleAr2.getY() - synGroup2Box.getHeight() / 2);

        synGroup1Box.raiseToTop();
        synGroup2Box.raiseToTop();
    }

    private static float getTheta(Port port) {
        double theta = 0;
        switch (port) {
            case NORTH:
                theta = Math.PI / 2;
                break;
            case SOUTH:
                theta = -Math.PI / 2;
                break;
            case EAST:
                theta = Math.PI;
                break;
            case WEST:
                theta = 0;
                break;
        }
        return (float) theta;
    }

    private static Point2D getOffset(float theta, float offset) {
        float x = 0f;
        float y = 0f;
        x = (float) (offset * Math.sin(theta));
        y = (float) (offset * Math.cos(theta));
        return new Point2D.Float(x, y);
    }

    /**
     *
     */
    @Override
    public synchronized void layoutChildrenQuiet(Point2D pt1, Point2D pt2) {
        halt.getAndSet(true);
        if (pt1 == null) {
            if (this.srcPt == null) {
                this.srcPt = getOpposingDefaultPosition(tarNGroup);
            }
        } else {
            this.srcPt = pt1;
        }

        if (pt2 == null) {
            if (this.tarPt == null) {
                this.tarPt = getOpposingDefaultPosition(srcNGroup);
            }
        } else {
            this.tarPt = pt2;
        }
        layout(this.srcPt, this.tarPt);
        halt.getAndSet(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Point2D midpoint(Point2D pt1, Point2D pt2) {
        int srcX = (int) srcPt.getX();
        int srcY = (int) srcPt.getY();
        int tarX = (int) tarPt.getX();
        int tarY = (int) tarPt.getY();
        return new Point2D.Float(((srcX + tarX) >> 1), ((srcY + tarY) >> 1));
    }

    /**
     * {@inheritDoc}
     *
     * For the purposes of determining proper ports, the group being moved by
     * the user is considered to be the end/target group.
     */
    @Override
    public void determineProperEndPoints() {
        Port startPort;
        Port endPort;

        float centerXSrc = (float) srcNGroup.getCenterX();
        float centerYSrc = (float) srcNGroup.getCenterY();
        float centerXTar = (float) tarNGroup.getCenterX();
        float centerYTar = (float) tarNGroup.getCenterY();

        float distance = (float) Point2D.distance(centerXSrc, centerYSrc,
            centerXTar, centerYTar);

        float theta = (float) Math.atan2(centerYSrc - centerYTar, centerXTar
            - centerXSrc);

        float zoneModifier = distance * distance / 5000;

        boolean left =
            tarNGroup.getMaxX() < (srcNGroup.getMinX() - zoneModifier);
        boolean right =
            tarNGroup.getMinX() > (srcNGroup.getMaxX() + zoneModifier);
        boolean above = tarNGroup.getMinY() > srcNGroup.getMaxY()
            + zoneModifier;
        boolean below = tarNGroup.getMaxY() < srcNGroup.getMinY()
            - zoneModifier;

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
        } else if (theta > srcZoneBoundaries[0]
            && theta <= srcZoneBoundaries[1]) {
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
        } else if (theta > srcZoneBoundaries[1]
            || theta <= srcZoneBoundaries[2]) {
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
        srcPort = startPort;
        tarPort = endPort;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Point2D getOpposingDefaultPosition(NeuronGroup ng) {
        if (synGroup1.getSourceNeuronGroup() != ng
            && synGroup1.getTargetNeuronGroup() != ng) {
            throw new IllegalArgumentException("Synapse group does not begin"
                + " or end in this group.");
        }
        NeuronGroup opposite;
        Port opPort;
        float x = 0;
        float y = 0;
        if (synGroup1.getSourceNeuronGroup() == ng) {
            opposite = synGroup1.getTargetNeuronGroup();
            opPort = tarPort;
        } else {
            opposite = synGroup1.getSourceNeuronGroup();
            opPort = srcPort;
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

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        layoutChildren();
    }

    /**
     * {@inheritDoc} TODO: Not sure why below is needed. Without the explicit
     * null sets a fatal error occurs in the JRE.
     */
    @Override
    public synchronized void removeFromParent() {
        halt.getAndSet(true);
        srcGroupNode.removeSynapseDock(srcPort, this);
        tarGroupNode.removeSynapseDock(tarPort, this);
        arrow1 = null;
        arrow2 = null;
        super.removeFromParent();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (srcNGroup == null || tarNGroup == null) {
                    return;
                }
                if (srcNGroup.isMarkedForDeletion()
                    || tarNGroup.isMarkedForDeletion()) {
                    return;
                }
                if (!networkPanel.isSelected(synGroup1Box)
                    && networkPanel.isSelected(synGroup2Box)) {
                    networkPanel.getObjectNodeMap().remove(synGroup1);
                    networkPanel.getNetwork().addGroup(synGroup1);
                }
                if (networkPanel.isSelected(synGroup1Box)
                    && !networkPanel.isSelected(synGroup2Box)) {
                    networkPanel.getObjectNodeMap().remove(synGroup2);
                    networkPanel.getNetwork().addGroup(synGroup2);
                }
            }

        });

    }

    @Override
    public SynapseGroup getGroup() {
        return synGroup1;
    }

    public SynapseGroup getGroup2() {
        return synGroup2;
    }

    public SynapseGroup[] getSynapseGroups() {
        return new SynapseGroup[] { synGroup1, synGroup2 };
    }

    @Override
    public Point2D getStartPt() {
        return srcPt;
    }

    @Override
    public Point2D getEndPt() {
        return tarPt;
    }

    @Override
    public float getRequiredSpacing() {
        return arrow1.getStrokeWidth() * 2 + DEFAULT_BUFFER
            + arrow2.getStrokeWidth() * 2;
    }

    @Override
    public void updateConstituentNodes() {
        // TODO Auto-generated method stub
    }

}
