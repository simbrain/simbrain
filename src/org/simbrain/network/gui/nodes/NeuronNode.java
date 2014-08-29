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
import java.awt.Font;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.piccolo2d.PNode;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.nodes.PText;
import org.piccolo2d.util.PBounds;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.neuron.NeuronDialog;
import org.simbrain.util.Utils;

/**
 * <b>NeuronNode</b> is a Piccolo PNode corresponding to a Neuron in the neural
 * network model.
 */
@SuppressWarnings("serial")
public class NeuronNode extends ScreenElement implements PropertyChangeListener {

    /** The logical neuron this screen element represents. */
    protected Neuron neuron;

    /** Diameter of neuron. */
    private static final int DIAMETER = 24;

    /** Font for input and output labels. */
    public static final Font IN_OUT_FONT = new Font("Arial", Font.PLAIN, 9);

    /** Main circle of node. */
    private PPath circle;

    /** A list of SynapseNodes connected to this NeuronNode; used for updating. */
    private HashSet<SynapseNode> connectedSynapses = new HashSet<SynapseNode>();

    /** Number text inside neuron. */
    private PText activationText;

    /** Text corresponding to neuron's (optional) label. */
    private PText labelText = new PText("...");

    /** Text corresponding to neuron's update priority. */
    private PText priorityText = new PText("...");

    /** Background for label text, so that background objects don't show up. */
    private PNode labelBackground;

    /** Whether the node is currently moving or not. */
    private boolean isMoving = false;

    /** Neuron Font. */
    public static final Font NEURON_FONT = new Font("Arial", Font.PLAIN, 11);

    /** Priority Font. */
    public static final Font PRIORITY_FONT = new Font("Courier", Font.PLAIN, 9);

    // TODO: These should be replaced with actual scaling of the text object.

    /** Neuron font bold. */
    public static final Font NEURON_FONT_BOLD = new Font("Arial", Font.BOLD, 11);

    /** Neuron font small. */
    public static final Font NEURON_FONT_SMALL = new Font("Arial", Font.PLAIN,
            9);

    /** Neuron font very small. */
    public static final Font NEURON_FONT_VERYSMALL = new Font("Arial",
            Font.PLAIN, 7);

    /** Color of "active" neurons, with positive values. */
    private static float hotColor = Color.RGBtoHSB(255, 0, 0, null)[0];

    /** Color of "inhibited" neurons, with negative values. */
    private static float coolColor = Color.RGBtoHSB(0, 0, 255, null)[0];

    /** Color of "spiking" synapse. */
    private static Color spikingColor = Color.yellow;

    /**
     * Create a new neuron node.
     *
     * @param net
     *            Reference to NetworkPanel
     * @param neuron
     *            reference to model neuron
     */
    public NeuronNode(final NetworkPanel net, final Neuron neuron) {
        super(net);
        this.neuron = neuron;
        this.centerFullBoundsOnPoint(neuron.getX(), neuron.getY());
        init();
    }

    /**
     * Initialize the NeuronNode.
     */
    private void init() {
        circle = PPath.createEllipse(0 - DIAMETER / 2, 0 - DIAMETER / 2,
                DIAMETER, DIAMETER);

        addChild(circle);

        setPriorityView(getNetworkPanel().getPrioritiesVisible());

        activationText = new PText(String.valueOf((int) Math.round(neuron
                .getActivation())));
        activationText.setFont(NEURON_FONT);
        setActivationTextPosition();
        addChild(activationText);

        // TODO: create a white background for the activation text
        labelBackground = new PNode();
        labelBackground.setPaint(this.getNetworkPanel().getBackground());
        labelBackground.setBounds(labelText.getBounds());
        labelBackground.addChild(labelText);
        addChild(labelBackground);

        resetColors();
        update();

        setPickable(true);

        addPropertyChangeListener(PROPERTY_FULL_BOUNDS, this);

        // The main circle is what users select
        PBounds bounds = circle.getBounds();
        setBounds(bounds);

        updateTextLabel();

    }

    /**
     * Toggles the visibility of the priority view text label.
     *
     * @param visibility
     *            whether the priority text label should be visible
     */
    public void setPriorityView(boolean visibility) {
        if (visibility == true) {
            priorityText = new PText("" + neuron.getUpdatePriority());
            priorityText.setFont(PRIORITY_FONT);
            setPriorityTextPosition();
            addChild(priorityText);
        } else {
            if (priorityText != null) {
                removeChild(priorityText);
            }
        }
    }

    /**
     * Set position of priority label.
     */
    private void setPriorityTextPosition() {
        if (priorityText == null) {
            return;
        }
        priorityText.setOffset(getX() + DIAMETER + 2, getY() + DIAMETER - 4);
    }

    /** @see ScreenElement */
    public boolean isSelectable() {
        return true;
    }

    /** @see ScreenElement */
    public boolean showSelectionHandle() {
        return true;
    }

    /** @see ScreenElement */
    public boolean isDraggable() {
        return true;
    }

    /** @see ScreenElement */
    protected boolean hasToolTipText() {
        return true;
    }

    /** @see ScreenElement */
    protected String getToolTipText() {
        String ret = new String();
        ret += neuron.getToolTipText();
        return ret;
    }

    /** @see ScreenElement */
    protected boolean hasContextMenu() {
        return true;
    }

    /**
     * Return the center of this node (the circle) in global coordinates.
     *
     * @return the center point of this node.
     */
    public Point2D getCenter() {
        return circle.getGlobalBounds().getCenter2D();
    }

    /** @see ScreenElement */
    protected JPopupMenu getContextMenu() {
        return getNetworkPanel().getNeuronContextMenu(neuron);
    }

    /** @see ScreenElement */
    protected boolean hasPropertyDialog() {
        return true;
    }

    /** @see ScreenElement */
    protected JDialog getPropertyDialog() {
        NeuronDialog dialog = NeuronDialog.createNeuronDialog(this
                .getNetworkPanel().getSelectedNeurons());
        return dialog;
    }

    /**
     * Update the neuron view based on the model neuron.
     */
    public void update() {
        updateColor();
        updateText();
    }

    /**
     * Update the text label.
     */
    public void updateTextLabel() {
        // Set label text
        if ((!neuron.getLabel().equalsIgnoreCase(""))
                || (!neuron.getLabel().equalsIgnoreCase(
                        NeuronDialog.NULL_STRING))) {
            labelText.setFont(NEURON_FONT);
            labelText.setText("" + neuron.getLabel());
            labelText.setOffset(circle.getX() - labelText.getWidth() / 2
                    + DIAMETER / 2, circle.getY() - DIAMETER / 2 - 1);
            labelBackground.setBounds(labelText.getFullBounds());

            // update bounds to include text
            PBounds bounds = circle.getBounds();
            bounds.add(labelText.localToParent(labelText.getBounds()));
            setBounds(bounds);

            // TODO: Make background bigger than label text
        }
    }

    /**
     * Sets the color of this neuron based on its activation level.
     */
    private void updateColor() {
        double activation = neuron.getActivation();

        // Force to blank if 0 (or close to it)
        if ((activation > -.1) && (activation < .1)) {
            circle.setPaint(Color.white);
        } else if (activation > 0) {
            float saturation = checkValid((float) Math.abs(activation
                    / neuron.getUpdateRule().getGraphicalUpperBound()));
            circle.setPaint(Color.getHSBColor(hotColor, saturation, 1));
        } else if (activation < 0) {
            float saturation = checkValid((float) Math.abs(activation
                    / neuron.getUpdateRule().getGraphicalLowerBound()));
            circle.setPaint(Color.getHSBColor(coolColor, saturation, 1));
        }

        if (neuron.getUpdateRule() instanceof SpikingNeuronUpdateRule) {
            if (((SpikingNeuronUpdateRule) neuron.getUpdateRule()).hasSpiked()) {
                circle.setStrokePaint(spikingColor);
                circle.setPaint(spikingColor);
            } else {
                circle.setStrokePaint(SynapseNode.getLineColor());
            }
        } else {
            circle.setStrokePaint(SynapseNode.getLineColor());
        }
    }

    /**
     * Check whether the specified saturation is valid or not.
     *
     * @param val
     *            the saturation value to check.
     * @return whether it is valid or not.
     */
    private float checkValid(final float val) {
        float tempval = val;

        if (val > 1) {
            tempval = 1;
        }

        if (val < 0) {
            tempval = 0;
        }

        return tempval;
    }

    /**
     * Determine what color and and font to use for this neuron based in its
     * activation level.
     */
    private void updateText() {
        double act = neuron.getActivation();
        activationText.setScale(1);
        setActivationTextPosition();

        priorityText.setScale(1);
        setPriorityTextPosition();
        priorityText.setText("" + neuron.getUpdatePriority()); // todo: respond
        // to listener

        if ((act > 0) && (neuron.getActivation() < 1)) { // Between 0 and 1
            activationText.setFont(NEURON_FONT_BOLD);
            String text = Utils.round(act, 1);
            if (text.startsWith("0.")) {
                text = text.replaceAll("0.", ".");
                if (text.equals(".0")) {
                    text = "0";
                }
            } else {
                text = text.replaceAll(".0$", "");
            }
            activationText.setText(text);
        } else if ((act > -1) && (act < 0)) { // Between -1 and 0
            activationText.setFont(NEURON_FONT_BOLD);
            activationText.setText(Utils.round(act, 1).replaceAll("^-0*", "-")
                    .replaceAll(".0$", ""));
        } else {
            // greater than 1 or less than -1
            activationText.setFont(NEURON_FONT_BOLD);
            if (Math.abs(act) < 10) {
                activationText.scale(.9);
            } else if (Math.abs(act) < 100) {
                activationText.scale(.8);
                activationText.translate(1, 1);
            } else {
                activationText.scale(.7);
                activationText.translate(-1, 2);
            }
            activationText.setText(String.valueOf((int) Math.round(act)));
        }
    }

    /**
     * Set basic position of text in the PNode, which is then adjusted depending
     * on the size of the text.
     */
    private void setActivationTextPosition() {
        if (activationText != null) {
            activationText.setOffset(circle.getX() + DIAMETER / 4 + 2,
                    circle.getY() + DIAMETER / 4 + 1);
        }
    }

    /**
     * Returns String representation of this NeuronNode.
     *
     * @return String representation of this node.
     */
    public String toString() {
        String ret = new String();
        ret += "NeuronNode: (" + this.getGlobalFullBounds().x + ")("
                + getGlobalFullBounds().y + ")\n";
        return ret;
    }

    /**
     * Return the neuron for this neuron node.
     *
     * @return the neuron for this neuron node
     */
    public Neuron getNeuron() {
        return neuron;
    }

    /**
     * Set the neuron for this neuron node to <code>neuron</code>.
     * <p>
     * This is a bound property.
     * </p>
     *
     * @param neuron
     *            neuron for this neuron node
     */
    public void setNeuron(final Neuron neuron) {

        Neuron oldNeuron = this.neuron;
        this.neuron = neuron;
        firePropertyChange(-1, "neuron", oldNeuron, neuron);
    }

    /** @see ScreenElement */
    protected JPopupMenu createContextMenu() {
        return getContextMenu();
    }

    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        updateSynapseNodePositions();
    }

    @Override
    public void offset(double dx, double dy) {
        super.offset(dx, dy);
        pushViewPositionToModel();
    }

    /**
     * Update the position of the model neuron based on the global coordinates
     * of this pnode.
     */
    public void pushViewPositionToModel() {
        Point2D p = this.getGlobalTranslation();
        getNeuron().setX(p.getX());
        getNeuron().setY(p.getY());
    }

    /**
     * Updates the position of the view neuron based on the position of the
     * model neuron.
     */
    public void pullViewPositionFromModel() {
        Point2D p = new Point2D.Double(getNeuron().getX(), getNeuron().getY());
        this.setGlobalTranslation(p);
    }

    /**
     * @return Returns the DIAMETER.
     */
    public static int getDIAMETER() {
        return DIAMETER;
    }

    /**
     * @return Connected synapses.
     */
    public Set<SynapseNode> getConnectedSynapses() {
        return connectedSynapses;
    }

    /**
     * Update connected synapse node positions.
     */
    public void updateSynapseNodePositions() {
        for (SynapseNode synapseNode : connectedSynapses) {
            synapseNode.updatePosition();
        }
    }

    /**
     * @return Returns the xpos.
     */
    public double getXpos() {
        return this.getGlobalBounds().getX();
    }

    /**
     * @param xpos
     *            The xpos to set.
     */
    public void setXpos(final double xpos) {
        Point2D p = new Point2D.Double(xpos, getYpos());
        globalToLocal(p);
        this.setBounds(p.getX(), p.getY(), this.getWidth(), this.getHeight());
    }

    /**
     * @return Returns the ypos.
     */
    public double getYpos() {
        return this.getGlobalBounds().getY();
    }

    /**
     * @param ypos
     *            The ypos to set.
     */
    public void setYpos(final double ypos) {
        Point2D p = new Point2D.Double(getXpos(), ypos);
        globalToLocal(p);
        this.setBounds(p.getX(), p.getY(), this.getWidth(), this.getHeight());
    }

    /** @see ScreenElement */
    public void resetColors() {
        circle.setStrokePaint(SynapseNode.getLineColor());
        labelBackground.setPaint(NetworkPanel.getBackgroundColor());
        updateColor();
    }

    /**
     * @return Returns the isMoving.
     */
    public boolean isMoving() {
        return isMoving;
    }

    /**
     * @param isMoving
     *            The isMoving to set.
     */
    public void setMoving(final boolean isMoving) {
        this.isMoving = isMoving;
    }

    /** @see ScreenElement. */
    public void setGrouped(final boolean isGrouped) {
        super.setGrouped(isGrouped);
        for (SynapseNode synapseNode : connectedSynapses) {
            synapseNode.setGrouped(isGrouped);
        }
    }

    /**
     * @return the hotColor
     */
    public static float getHotColor() {
        return hotColor;
    }

    /**
     * @param hotColor
     *            the hotColor to set
     */
    public static void setHotColor(float hotColor) {
        NeuronNode.hotColor = hotColor;
    }

    /**
     * @return the coolColor
     */
    public static float getCoolColor() {
        return coolColor;
    }

    /**
     * @param coolColor
     *            the coolColor to set
     */
    public static void setCoolColor(float coolColor) {
        NeuronNode.coolColor = coolColor;
    }

    /**
     * @return the spikingColor
     */
    public static Color getSpikingColor() {
        return spikingColor;
    }

    /**
     * @param spikingColor
     *            the spikingColor to set
     */
    public static void setSpikingColor(Color spikingColor) {
        NeuronNode.spikingColor = spikingColor;
    }

}
