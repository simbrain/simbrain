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

import org.piccolo2d.nodes.PPath;
import org.piccolo2d.util.PBounds;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.synapse.SynapseDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * <b>SynapseNode</b> is a Piccolo PNode corresponding to a Neuron in the neural
 * network model.
 */
public final class SynapseNode extends ScreenElement {

    /**
     * The logical synapse this screen element represents.
     */
    private Synapse synapse;

    /**
     * Location of circle relative to target node.
     */
    private final double offset = 7;

    /**
     * Main circle of synapse.
     */
    private PPath circle;

    /**
     * Line connecting nodes. More of a loop for self cocnections.
     */
    private PPath.Float line;

    /**
     * The line for bound checking.
     */
    private Line2D.Float lineBound = new Line2D.Float();

    /**
     * The arc for bound checking when the synapse is a self connection.
     */
    private Arc2D.Float arcBound = new Arc2D.Float();

    /**
     * Reference to source neuron.
     */
    private NeuronNode source;

    /**
     * Reference to target neuron.
     */
    private NeuronNode target;

    /**
     * Used to approximate zero to prevent divide-by-zero errors.
     */
    private static final double ZERO_PROXY = .001;

    /**
     * Color of "excitatory" synapses, with positive values.
     */
    private static Color excitatoryColor = Color.red;

    /**
     * Color of "inhibitory" synapses, with negative values.
     */
    private static Color inhibitoryColor = Color.blue;

    /**
     * Color of "zero" weights.
     */
    private static Color zeroWeightColor = Color.gray;

    /**
     * Maximum diameter of the circle representing the synapse.
     */
    private static int maxDiameter = 20;

    /**
     * Minimum diameter of the circle representing the synapse.
     */
    private static int minDiameter = 7;

    /**
     * Color of lines in synapse representation.
     */
    private static Color lineColor = Color.black;

    /**
     * Create a new synapse node connecting a source and target neuron.
     *
     * @param net     Reference to NetworkPanel
     * @param source  source neuronnode
     * @param target  target neuronmode
     * @param synapse the model synapse this PNode represents
     */
    public SynapseNode(final NetworkPanel net, final NeuronNode source, final NeuronNode target, final Synapse synapse) {

        super(net);
        this.source = source;
        this.target = target;
        target.getConnectedSynapses().add(this);
        source.getConnectedSynapses().add(this);

        this.synapse = synapse;
        init();
    }

    /**
     * Initialize the SynapseNode.
     */
    private void init() {
        updatePosition();
        this.addChild(circle);
        this.addChild(line);
        line.setStrokePaint(Color.BLACK);
        line.lowerToBottom();

        updateColor();
        updateDiameter();

        setPickable(true);
        circle.setPickable(true);
        line.setPickable(false);
    }

    /**
     * Update position of synapse.
     */
    public void updatePosition() {

        Point2D synapseCenter;

        // Position the synapse
        if (isSelfConnection()) {
            synapseCenter = globalToLocal(new Point2D.Double(target.getCenter().getX() + offset + 3, target.getCenter().getY() + offset + 3));
        } else {
            synapseCenter = globalToLocal(calcCenter(source.getCenter(), target.getCenter()));
        }
        this.offset(synapseCenter.getX() - offset, synapseCenter.getY() - offset);

        // Create the circle
        if (circle == null) {
            circle = PPath.createEllipse(0, 0, (float) offset * 2, (float) offset * 2);
            ((PPath) circle).setStrokePaint(null);
        }
        double ea = 1;
        double ea2 = ea * 2;
        PBounds tempBounds = circle.getFullBounds();
        setBounds(tempBounds.x - ea, tempBounds.y - ea, tempBounds.width + ea2, tempBounds.height + ea2);

        // Create the line
        if (line == null) {
            line = createLine(globalToLocal(synapseCenter));
        }

        // Update the line (unless it's a self connection)
        if (!isSelfConnection()) {
            line.reset();
            line.append(new Line2D.Double(globalToLocal(source.getCenter()), synapseCenter), false);
            lineBound.setLine(source.getCenter(), localToGlobal(synapseCenter));
        } else {
            arcBound =
                    new Arc2D.Float(
                            (float) getGlobalBounds().getX(),
                            (float) getGlobalBounds().getY() - 7,
                            22,
                            15,
                            1,
                            355,
                            Arc2D.OPEN
                    );
        }
    }

    /**
     * of a synapse based on whether it is clamped or not.
     */
    public void updateClampStatus() {
        if (synapse.isFrozen()) {
            circle.setStrokePaint(Color.black);
        } else {
            circle.setStrokePaint(null);
        }
    }

    /**
     * Whether this synapse connects a neuron to itself or not.
     *
     * @return true if this synapse connects a neuron to itself.
     */
    public boolean isSelfConnection() {
        return (source.getNeuron() == target.getNeuron());
    }

    /**
     * Create the line depending on whether this is self connected or not.
     *
     * @param center the center of the synapse
     * @return the line
     */
    private PPath.Float createLine(final Point2D center) {
        if (isSelfConnection()) {
            return new PPath.Float(new Arc2D.Float((float) getX(), (float) getY() - 7, 22, 15, 1, 355, Arc2D.OPEN));
        } else {
            return new PPath.Float(new Line2D.Float(globalToLocal(source.getCenter()), center));
        }
    }

    /**
     * Calculates the color for a weight, based on its current strength.
     * Positive values are (for example) red, negative values blue.
     */
    public void updateColor() {
        if (synapse.getStrength() < 0) {
            circle.setPaint(inhibitoryColor);
        } else if (synapse.getStrength() == 0) {
            circle.setPaint(zeroWeightColor);
        } else {
            circle.setPaint(excitatoryColor);
        }
        if (source.getNeuron().isSpike()) {
            line.setStrokePaint(NeuronNode.getSpikingColor());
        } else {
            line.setStrokePaint(lineColor);
        }
    }

    /**
     * Update the diameter of the drawn weight based on the logical weight's
     * strength.
     */
    public void updateDiameter() {
        double diameter;

        double upperBound = synapse.getUpperBound();
        double lowerBound = synapse.getLowerBound();
        double strength = synapse.getStrength();

        // If upper or lower bound are set to zero use a proxy to prevent
        // division errors
        if (upperBound == 0) {
            upperBound = ZERO_PROXY;
        }
        if (lowerBound == 0) {
            lowerBound = ZERO_PROXY;
        }

        // If strength is out of bounds (which is allowed in the model), set it
        // to those bounds for the
        // sake of the GUI representation
        if (strength < lowerBound) {
            strength = lowerBound;
        }
        if (strength > upperBound) {
            strength = upperBound;
        }

        if (synapse.getStrength() == 0) {
            diameter = minDiameter;
        } else if (synapse.getStrength() > 0) {
            diameter = ((maxDiameter - minDiameter) * (strength / upperBound) + minDiameter);
        } else {
            diameter = (((maxDiameter - minDiameter) * (Math.abs(strength / lowerBound))) + minDiameter);
        }

        double delta = (circle.getBounds().getWidth() - diameter) / 2;

        circle.setWidth(diameter);
        circle.setHeight(diameter);
        // offset properly moves circle, but this is not reflected in bounds
        circle.offset(delta, delta);
        setBounds(circle.getFullBounds());
    }

    /**
     * Calculates the position of the synapse circle based on the positions of
     * the source and target NeuronNodes.
     *
     * @param src Source NeuronNode
     * @param tar Target NeuronNode
     * @return the appropriate position for the synapse circle
     */
    public Point2D calcCenter(final Point2D src, final Point2D tar) {

        double sourceX = src.getX();
        double sourceY = src.getY();
        double targetX = tar.getX();
        double targetY = tar.getY();

        double x = Math.abs(sourceX - targetX);
        double y = Math.abs(sourceY - targetY);
        double alpha = Math.atan(y / x);

        double weightX = 0;
        double weightY = 0;

        int neuronOffset = NeuronNode.getDIAMETER() / 2;

        if (sourceX < targetX) {
            weightX = targetX - (neuronOffset * Math.cos(alpha));
        } else {
            weightX = targetX + (neuronOffset * Math.cos(alpha));
        }

        if (sourceY < targetY) {
            weightY = targetY - (neuronOffset * Math.sin(alpha));
        } else {
            weightY = targetY + (neuronOffset * Math.sin(alpha));
        }

        return new Point2D.Double(weightX, weightY);
    }

    /**
     * @return
     * @see ScreenElement
     */
    public boolean isSelectable() {
        return true;
    }

    /**
     * @return
     * @see ScreenElement
     */
    public boolean showSelectionHandle() {
        return true;
    }

    /**
     * @return
     * @see ScreenElement
     */
    public boolean isDraggable() {
        return false;
    }

    /**
     * @see ScreenElement
     */
    protected boolean hasToolTipText() {
        return true;
    }

    /**
     * @see ScreenElement
     */
    protected String getToolTipText() {
        return String.valueOf(synapse.getToolTipText());
    }

    /**
     * @return
     * @see ScreenElement
     */
    public boolean hasContextMenu() {
        return true;
    }

    /**
     * @see ScreenElement
     */
    protected JPopupMenu getContextMenu() {

        // JPopupMenu contextMenu = new JPopupMenu();
        //
        // contextMenu.add(new CutAction(getNetworkPanel()));
        // contextMenu.add(new CopyAction(getNetworkPanel()));
        // contextMenu.add(new PasteAction(getNetworkPanel()));
        // contextMenu.addSeparator();
        //
        // contextMenu.add(new DeleteAction(getNetworkPanel()));
        // contextMenu.addSeparator();
        //
        // contextMenu.add(getNetworkPanel().getActionManager().getGroupMenu());
        // contextMenu.addSeparator();
        //
        // // Workspace workspace = getNetworkPanel().getWorkspace();
        // // if (workspace.getGaugeList().size() > 0) {
        // // contextMenu.add(workspace.getGaugeMenu(getNetworkPanel()));
        // // contextMenu.addSeparator();
        // // }
        //
        // contextMenu.add(new SetSynapsePropertiesAction(getNetworkPanel()));
        //
        // return contextMenu;
        return this.getNetworkPanel().getSynapseContextMenu(synapse);
    }

    /**
     * @see ScreenElement
     */
    protected boolean hasPropertyDialog() {
        return true;
    }

    /**
     * @see ScreenElement
     */
    protected JDialog getPropertyDialog() {
        SynapseDialog dialog = (SynapseDialog) getNetworkPanel().getSynapseDialog(getNetworkPanel().getSelectedSynapses());
        return dialog;
    }

    /**
     * Returns String representation of this NeuronNode.
     *
     * @return String representation of this node.
     */
    public String toString() {
        String ret = new String();
        ret += "SynapseNode: (" + this.getGlobalFullBounds().x + ")(" + getGlobalFullBounds().y + ")\n";
        return ret;
    }

    /**
     * @return Returns the synapse.
     */
    public Synapse getSynapse() {
        return synapse;
    }

    /**
     * @param synapse The synapse to set.
     */
    public void setSynapse(final Synapse synapse) {
        this.synapse = synapse;
    }

    /**
     * @return Returns the source.
     */
    public NeuronNode getSource() {
        return source;
    }

    /**
     * @return Returns the target.
     */
    public NeuronNode getTarget() {
        return target;
    }

    /**
     * @param source The source to set.
     */
    public void setSource(final NeuronNode source) {
        this.source = source;
    }

    /**
     * @param target The target to set.
     */
    public void setTarget(final NeuronNode target) {
        this.target = target;
    }

    /**
     * @see ScreenElement
     */
    public void resetColors() {
        line.setStrokePaint(lineColor);
        updateColor();
        updateDiameter();
    }

    /**
     * @return the excitatoryColor
     */
    public static Color getExcitatoryColor() {
        return excitatoryColor;
    }

    /**
     * @param excitatoryColor the excitatoryColor to set
     */
    public static void setExcitatoryColor(Color excitatoryColor) {
        SynapseNode.excitatoryColor = excitatoryColor;
    }

    /**
     * @return the inhibitoryColor
     */
    public static Color getInhibitoryColor() {
        return inhibitoryColor;
    }

    /**
     * @param inhibitoryColor the inhibitoryColor to set
     */
    public static void setInhibitoryColor(Color inhibitoryColor) {
        SynapseNode.inhibitoryColor = inhibitoryColor;
    }

    /**
     * @return the zeroWeightColor
     */
    public static Color getZeroWeightColor() {
        return zeroWeightColor;
    }

    /**
     * @param zeroWeightColor the zeroWeightColor to set
     */
    public static void setZeroWeightColor(Color zeroWeightColor) {
        SynapseNode.zeroWeightColor = zeroWeightColor;
    }

    /**
     * @return the maxDiameter
     */
    public static int getMaxDiameter() {
        return maxDiameter;
    }

    /**
     * @param maxDiameter the maxDiameter to set
     */
    public static void setMaxDiameter(int maxDiameter) {
        SynapseNode.maxDiameter = maxDiameter;
    }

    /**
     * @return the minDiameter
     */
    public static int getMinDiameter() {
        return minDiameter;
    }

    /**
     * @param minDiameter the minDiameter to set
     */
    public static void setMinDiameter(int minDiameter) {
        SynapseNode.minDiameter = minDiameter;
    }

    /**
     * @return the lineColor
     */
    public static Color getLineColor() {
        return lineColor;
    }

    /**
     * @param lineColor the lineColor to set
     */
    public static void setLineColor(Color lineColor) {
        SynapseNode.lineColor = lineColor;
    }

    /**
     * @return the line
     */
    public PPath.Float getLine() {
        return line;
    }

    public Arc2D.Float getArcBound() {
        return arcBound;
    }


    public Line2D.Float getLineBound() {
        return lineBound;
    }
}