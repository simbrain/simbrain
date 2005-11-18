/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network.pnodes;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

import org.simbrain.gauge.GaugeSource;
import org.simbrain.network.NetworkPanel;
import org.simbrain.network.ScreenElement;
import org.simnet.interfaces.Network;
import org.simnet.interfaces.SpikingNeuron;
import org.simnet.interfaces.Synapse;
import org.simnet.synapses.ClampedSynapse;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PPaintContext;


/**
 * <b>PNodeWeight</b> is a Piccolo representation of a Piccolo PNode corresponding to a Weight in the neural network
 * model.  It is a GUI representation of a weight.  It has two PPaths as children, a Ball and a Line; the Ball is
 * given a bounding box when weights are selected, but can't be moved.
 *
 * @author Mai Ngoc Thang
 */
public class PNodeWeight extends PPath implements GaugeSource, ScreenElement {
    // The neural-network weight this PNode represents
    private Synapse weight;
    private double radius = 0;
    private static int maxRadius = 16;
    private static int minRadius = 7;
    private boolean selected;

    // References to the PNodes connected from and to
    private PNodeNeuron source;
    private PNodeNeuron target;

    // Ball and line children of this PNode
    private PPath weightBall;
    private Ellipse2D ball;
    private Line2D line;
    private Arc2D self_connection;
    public PNodeLine weightLine;

    public PNodeWeight() {
    }

    public PNodeWeight(final PNodeNeuron source, final PNodeNeuron target, final Synapse the_weight) {
        this.source = source;
        this.target = target;
        weight = the_weight;
        weight.setSource(source.getNeuron());
        weight.setTarget(target.getNeuron());
        updateRadius();
        init();
    }

    public PNodeWeight(final PNodeNeuron source, final PNodeNeuron target) {
        this.source = source;
        this.target = target;
        weight = new ClampedSynapse(source.getNeuron(), target.getNeuron());
        updateRadius();
        init();
    }

    public PNodeWeight(final PNodeNeuron source, final PNodeNeuron target, final double val, final String id) {
        this.source = source;
        this.target = target;
        weight = new ClampedSynapse(source.getNeuron(), target.getNeuron(), val, id);
        updateRadius();
        init();
    }

    /**
     * Change the type of weight this pnode is associated with It is assumed that the basic properties of the new
     * weight have been set
     *
     * @param new_synapse the synapse to change to
     */
    public void changeWeight(final Synapse new_synapse) {
        Network.changeSynapse(weight, new_synapse);
        weight = new_synapse;
    }

    public void init() {
        //Set source and target for the line
        double SourceCX = NetworkPanel.getGlobalX(getSource()) + PNodeNeuron.NEURON_HALF;
        double SourceCY = NetworkPanel.getGlobalY(getSource()) + PNodeNeuron.NEURON_HALF;
        double TargetCX = NetworkPanel.getGlobalX(getTarget()) + PNodeNeuron.NEURON_HALF;
        double TargetCY = NetworkPanel.getGlobalY(getTarget()) + PNodeNeuron.NEURON_HALF;

        //Set bounds of weight "ball"
        Point newPoint = calcWt(SourceCX, SourceCY, TargetCX, TargetCY);
        float offset = (float) (radius / 2);
        ball = new Ellipse2D.Float(
                                   (float) (newPoint.getX() - offset), (float) (newPoint.getY() - offset),
                                   (float) getRadius() * 2, (float) getRadius() * 2);

        // Create children line and ball
        weightBall = new PPath(ball);

        calColor(weight.getStrength(), isSelected());
        this.addChild(weightBall);
        weightBall.setStrokePaint(null);

        if (source.getNeuron() == target.getNeuron()) {
            self_connection = new Arc2D.Double();
            weightLine = new PNodeLine(self_connection);
        } else {
            line = new Line2D.Double();
            weightLine = new PNodeLine(line);
        }

        this.addChild(weightLine);
        this.weightLine.setStrokePaint(source.getParentPanel().getLineColor());

        // add tool tip text updater
        addInputEventListener(new ToolTipTextUpdater()
            {
                /** @see ToolTipTextUpdater */
                protected String getToolTipText() {
                    StringBuffer sb = new StringBuffer();
                    sb.append("id=");
                    sb.append(getId());
                    sb.append(" strength=");
                    sb.append(weight.getStrength());
                    return sb.toString();
                }
            });
    }

    /*
     * Update the size of the weight (learning), and its location (relative to nodes).
     */
    public void updatePosition() {
        updateRadius();

        double SourceCX = NetworkPanel.getGlobalX(getSource()) + PNodeNeuron.NEURON_HALF;
        double SourceCY = NetworkPanel.getGlobalY(getSource()) + PNodeNeuron.NEURON_HALF;
        double TargetCX = NetworkPanel.getGlobalX(getTarget()) + PNodeNeuron.NEURON_HALF;
        double TargetCY = NetworkPanel.getGlobalY(getTarget()) + PNodeNeuron.NEURON_HALF;

        Point newPoint = calcWt(SourceCX, SourceCY, TargetCX, TargetCY);
        float offset = (float) (radius / 2);

        if (source.getNeuron() == target.getNeuron()) {
            ball.setFrame(SourceCX + 8, SourceCY + 4, offset * 1.4, offset * 1.4);
            self_connection.setArc(SourceCX, SourceCY - 7, 22, 15, 1, 355, Arc2D.OPEN);
            weightLine.reset();
            weightLine.append(self_connection, false);
        } else {
            ball.setFrame(newPoint.getX() - offset, newPoint.getY() - offset, offset * 2, offset * 2);
            line.setLine(SourceCX, SourceCY, TargetCX, TargetCY);
            weightLine.reset();
            weightLine.append(line, false);
        }

        this.weightBall.reset();
        this.weightBall.append(this.ball, false);
    }

    public Synapse getWeight() {
        return weight;
    }

    public PNode getWeightBall() {
        return weightBall;
    }

    /**
     * Get a reference to the neuron this weight is connected to
     */
    public PNodeNeuron getTarget() {
        return target;
    }

    public void setTarget(final PNodeNeuron n) {
        this.target = n;
    }

    public PNodeNeuron getSource() {
        return source;
    }

    public void setSource(final PNodeNeuron n) {
        this.source = n;
    }

    /**
     * Calculates the intersection point between the line that connects a source and target PNodeNeuron. This point
     * will be the position for a PNodeWeight.weightBall.
     *
     * @param sourceX X coordinate of the source PNodeNeuron
     * @param sourceY Y coordinate of the source PNodeNeuron
     * @param targetX X coordinate of the target PNodeNeuron
     * @param targetY Y coordinate of the target PNodeNeuron
     *
     * @return The intersection point between the line connecting two PNodeNeuron and the target PNodeNeuron
     */
    public static Point calcWt(final double sourceX, final double sourceY, final double targetX, final double targetY) {
        double radius = PNodeNeuron.NEURON_HALF;
        double x = Math.abs(sourceX - targetX);
        double y = Math.abs(sourceY - targetY);
        double alpha = Math.atan(y / x);

        int weightX = 0;
        int weightY = 0;

        if (sourceX < targetX) {
            weightX = (int) Math.round(targetX - (radius * Math.cos(alpha)));
        } else {
            weightX = (int) Math.round(targetX + (radius * Math.cos(alpha)));
        }

        if (sourceY < targetY) {
            weightY = (int) Math.round(targetY - (radius * Math.sin(alpha)));
        } else {
            weightY = (int) Math.round(targetY + (radius * Math.sin(alpha)));
        }

        return new Point(weightX, weightY);
    }

    /**
     * Calculates the color for a weight, based on its current strength.  Positive values are (for example) red,
     * negative values blue.
     *
     * @param weightValue strength of the weight
     * @param isSelected whether the weight is selected or not
     */
    public void calColor(final double weightValue, final boolean isSelected) {
        if (isSelected) {
            weightBall.setPaint(source.getParentPanel().getLassoColor());
        } else if (weightValue < 0) {
            weightBall.setPaint(source.getParentPanel().getInhibitoryColor());
        } else if (weightValue == 0) {
            weightBall.setPaint(source.getParentPanel().getInhibitoryColor());
        } else {
            weightBall.setPaint(source.getParentPanel().getExcitatoryColor());
        }
    }

    /**
     * @param paintContext
     */
    protected void paint(final PPaintContext paintContext) {
        super.paint(paintContext);
    }

    /*
     * Invoked when user directly increases the value of a weight
     */
    public void upArrow() {
        weight.incrementWeight();
        updateRadius();
        render();
    }

    /*
     * Invoked when user directly decreases the value of a weight
     */
    public void downArrow() {
        weight.decrementWeight();
        updateRadius();
        render();
    }

    /*
     * Randomize the_neuron
     */
    public void randomize() {
        weight.randomize();
        updateRadius();
        render();
    }

    /**
     * get a reference to the child-node, line
     */
    public PNodeLine getWeightLine() {
        return this.weightLine;
    }

    public double getRadius() {
        return radius;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(final boolean sel) {
        selected = sel;
    }

    public void resetLineColors() {
        weightLine.setStrokePaint(source.getParentPanel().getLineColor());
    }

    /**
     * Update the radius of the drawn weight based on the logical weight's strength
     */
    public void updateRadius() {
        double str = weight.getStrength();

        if (str > 0) {
            radius = (((maxRadius - minRadius) * (str / weight.getUpperBound())) + minRadius);
        } else {
            radius = (((maxRadius - minRadius) * (Math.abs(str / weight.getLowerBound()))) + minRadius);
        }
    }

    /**
     * Calculates the euclidean distance between two points.
     *
     * @param x1 x coordinate of point 1
     * @param y1 y coordinate of point 1
     * @param x2 x coordinate of point 2
     * @param y2 y coordinate of point 2
     *
     * @return distnace between points 1 and 2
     */
    public static double distance(final double x1, final double y1, final double x2, final double y2) {
        return Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
    }

    /**
     * Update graphics of the weight object
     */
    public void render() {
        calColor(weight.getStrength(), isSelected());
        this.updatePosition();

        if (source.getNeuron() instanceof SpikingNeuron) {
            if (((SpikingNeuron) source.getNeuron()).hasSpiked()) {
                weightLine.setStrokePaint(Color.YELLOW);
            } else {
                resetLineColors();
            }
        }

        moveToBack();
    }

    /**
     * @return Returns the maxRadius.
     */
    public static int getMaxRadius() {
        return maxRadius;
    }

    /**
     * @param maxRadius The maxRadius to set.
     */
    public static void setMaxRadius(final int maxRadius) {
        PNodeWeight.maxRadius = maxRadius;
    }

    /**
     * @return Returns the minRadius.
     */
    public static int getMinRadius() {
        return minRadius;
    }

    /**
     * @param minRadius The minRadius to set.
     */
    public static void setMinRadius(final int minRadius) {
        PNodeWeight.minRadius = minRadius;
    }

    /**
     * @param weight The weight to set.
     */
    public void setWeight(final Synapse weight) {
        this.weight = weight;
    }

    /**
     * returns the value used by the Gauge
     */
    public double getGaugeValue() {
        return this.getWeight().getStrength();
    }

    /**
     * Return the associated weight's id
     */
    public String getId() {
        return this.getWeight().getId();
    }

    /**
     * Perform initialization needed when this object is added to the network.
     */
    public void addToNetwork(final NetworkPanel np) {
        if (np.getNetwork().getFlatSynapseList().contains(getWeight()) == false) {
            np.getNetwork().addWeight(getWeight());
        }
    }

    public void delete() {
        setSource(null);

        // Must remove source and target's reference to this weight
        setTarget(null);
        weight.getTarget().getParentNetwork().deleteWeight(weight);
    }

    public void drawBoundary() {
        return;
    }

    public boolean isSelectable() {
        return true;
    }

    /**
     * @param np Reference to parent NetworkPanel
     */
    public void initCastor(final NetworkPanel np) {
        init();
    }

    public void increment() {
        upArrow();
    }

    public void decrement() {
        downArrow();
    }

    public void nudge(final int offsetX, final int offsetY, final double nudgeAmount) {
        return;
    }

    public void renderNode() {
        render();
        weightLine.moveToBack();
    }

    public ScreenElement duplicate() {
        return (ScreenElement) this.clone();
    }
}
