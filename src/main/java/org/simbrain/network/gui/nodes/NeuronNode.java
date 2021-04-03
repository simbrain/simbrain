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
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.nodes.PText;
import org.piccolo2d.util.PBounds;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.events.NeuronEvents;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.NetworkPanelMenusKt;
import org.simbrain.network.neuron_update_rules.interfaces.ActivityGenerator;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;
import org.simbrain.util.math.SimbrainMath;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;

import static org.simbrain.network.gui.NetworkDialogsKt.getNeuronDialog;

/**
 * <b>NeuronNode</b> is a Piccolo PNode corresponding to a Neuron in the neural
 * network model.
 */
@SuppressWarnings("serial")
public class NeuronNode extends ScreenElement implements PropertyChangeListener {

    /**
     * The logical neuron this screen element represents.
     */
    protected Neuron neuron;

    /**
     * Default text visibility threshold.
     */
    private static final double TEXT_VISIBILITY_THRESHOLD = 0.5;

    /**
     * Diameter of neuron.
     */
    public static final int DIAMETER = 24;

    /**
     * Font for input and output labels.
     */
    public static final Font IN_OUT_FONT = new Font("Arial", Font.PLAIN, 9);

    /**
     * Main shape.
     */
    private PPath mainShape;

    /**
     * Circle shape for representing neurons.
     */
    private PPath circle = PPath.createEllipse(0 - DIAMETER / 2, 0 - DIAMETER / 2, DIAMETER, DIAMETER);

    /**
     * Square shape for representing activity generators.
     */
    private PPath square = PPath.createRectangle(0 - DIAMETER / 2, 0 - DIAMETER / 2, DIAMETER, DIAMETER);

    /**
     * A list of SynapseNodes connected to this NeuronNode; used for updating.
     */
    private HashSet<SynapseNode> connectedSynapses = new HashSet<SynapseNode>();

    /**
     * Number text inside neuron.
     */
    private PText activationText = new PText();

    /**
     * Text corresponding to neuron's (optional) label.
     */
    private PText labelText = new PText();

    /**
     * Text corresponding to neuron's update priority.
     */
    private PText priorityText = new PText();

    /**
     * Background for label text, so that background objects don't show up.
     */
    private PNode labelBackground = new PNode();

    /**
     * Heavy stroke for clamped nodes.
     */
    private static final BasicStroke CLAMPED_STROKE = new BasicStroke(2f);

    /**
     * Neuron Font.
     */
    public static final Font NEURON_FONT = new Font("Arial", Font.PLAIN, 11);

    /**
     * Priority Font.
     */
    public static final Font PRIORITY_FONT = new Font("Courier", Font.PLAIN, 9);

    // TODO: These should be replaced with actual scaling of the text object.

    /**
     * Neuron font bold.
     */
    public static final Font NEURON_FONT_BOLD = new Font("Arial", Font.BOLD, 11);

    /**
     * Neuron font small.
     */
    public static final Font NEURON_FONT_SMALL = new Font("Arial", Font.PLAIN, 9);

    /**
     * Neuron font very small.
     */
    public static final Font NEURON_FONT_VERYSMALL = new Font("Arial", Font.PLAIN, 7);

    /**
     * Color of "active" neurons, with positive values.
     */
    private static float hotColor = Color.RGBtoHSB(255, 0, 0, null)[0];

    /**
     * Color of "inhibited" neurons, with negative values.
     */
    private static float coolColor = Color.RGBtoHSB(0, 0, 255, null)[0];

    /**
     * Color of "spiking" synapse.
     */
    private static Color spikingColor = Color.yellow;

    /**
     * Whether text should be visible (when zoomed out, it should be
     * invisible).
     */
    private boolean currentTextVisibility;

    /**
     * If true then a custom color is being used for stroke.
     */
    private boolean customStrokeColor = false;

    /**
     * Create a new neuron node.
     *
     * @param net    Reference to NetworkPanel
     * @param neuron reference to model neuron
     */
    public NeuronNode(final NetworkPanel net, final Neuron neuron) {
        super(net);
        this.neuron = neuron;

        // Set up label text
        //priorityText.setFont(PRIORITY_FONT);
        labelBackground.setPaint(this.getNetworkPanel().getBackground());
        labelBackground.setBounds(labelText.getBounds());
        labelBackground.addChild(labelText);
        addChild(labelBackground);

        // Set graphics of node based on neuron propertiess
        updateShape();
        updateColor();
        updateText();
        updateTextLabel();
        updateClampStatus();

        this.centerFullBoundsOnPoint(neuron.getX(), neuron.getY());

        setPickable(true);

        addPropertyChangeListener(PROPERTY_FULL_BOUNDS, this);

        // Handle events
        NeuronEvents events = neuron.getEvents();
        events.onDeleted(n -> removeFromParent());
        events.onActivationChange((o, n) -> {
            updateColor();
            updateText();
        });
        events.onSpiked((o, n) -> {
            updateSpikeColor();
        });
        events.onColorChange(this::updateColor);
        events.onLabelChange((o,n) -> updateTextLabel());
        events.onClampedChange((o, n) -> updateClampStatus());
        events.onLocationChange(this::pullViewPositionFromModel);
        events.onUpdateRuleChange((o, n) -> updateShape());

    }

    /**
     * Update the shape (square or circle) of the neuron based on whether it's an activity generator or not.
     */
    public void updateShape() {
        if (neuron.getUpdateRule() instanceof ActivityGenerator) {
            removeChild(circle);
            addChild(square);
            mainShape = square;
        } else {
            removeChild(square);
            addChild(circle);
            mainShape = circle;
        }
        mainShape.lowerToBottom();
    }

    /**
     * Update the stroke of a node based on whether it is clamped or not.
     */
    public void updateClampStatus() {
        if (customStrokeColor) {
            return;
        }
        if (neuron.isClamped()) {
            circle.setStroke(CLAMPED_STROKE);
        } else {
            circle.setStroke(DEFAULT_STROKE);
        }
    }

    /**
     * Determine what font to use for this neuron based in its activation level.
     * TODO: Redo by scaling the text object.
     */
    private void updateText() {
        if (!currentTextVisibility) {
            return;
        }
        // Todo: a bit of a performance drain.

        double act = neuron.getActivation();
        activationText.setScale(1);
        setActivationTextPosition();

        priorityText.setScale(1);
        setPriorityTextPosition();
        priorityText.setText("" + neuron.getUpdatePriority()); // todo: respond
        // to listener

        if (java.lang.Double.isNaN(neuron.getActivation())) {
            activationText.setText("NaN");
            activationText.scale(.7);
            activationText.translate(-4, 3);
        } else if ((act > 0) && (neuron.getActivation() < 1)) { // Between 0 and
            // 1
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
            activationText.setText(Utils.round(act, 1).replaceAll("^-0*", "-").replaceAll(".0$", ""));
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
     * Update the visibility of all text nodes depending on view scale. When
     * "zoomed in" show all text; when zoomed out, don't.
     */
    public void updateTextVisibility() {
        double scale = this.getNetworkPanel().getCanvas().getCamera().getViewScale();
        if (scale > TEXT_VISIBILITY_THRESHOLD) {
            if (!currentTextVisibility) {
                setDisplayText(true);
                currentTextVisibility = true;
            }
            updateText();
            updateTextLabel();
        } else {
            if (currentTextVisibility) {
                setDisplayText(false);
                currentTextVisibility = false;
            }
        }
    }

    /**
     * Support text visibility toggling by adding and removing text pnodes.
     *
     * @param displayText whether text should be displayed or not.
     */
    private void setDisplayText(boolean displayText) {
        if (displayText) {
            addChild(activationText);
            addChild(labelBackground);
            setPriorityView(getNetworkPanel().getPrioritiesVisible());
            resetToDefault();
            updateText();
            updateTextLabel();
        } else {
            removeChild(activationText);
            removeChild(labelText);
            removeChild(priorityText);
            removeChild(labelBackground);
        }
    }

    /**
     * Sets the color of this neuron based on its activation level.
     */
    private void updateColor() {
        double activation = neuron.getUpdateRule().getGraphicalValue(neuron);
        // Force to blank if 0 (or close to it)
        double gLow = neuron.getUpdateRule().getGraphicalLowerBound();
        double gUp = neuron.getUpdateRule().getGraphicalUpperBound();

        // A "graphical zero point" that shows as white
        double gZeroPoint = 0;
        if (NeuronUpdateRule.usesCustomZeroPoint(neuron.getUpdateRule())) {
            // Current custom choice is between upper and lower bounds.
            // For example useful to capture whether a biological neuron is
            // depolarized or hyperpolarized
            gZeroPoint = ((gUp - gLow) / 2) + gLow;
        }

        if (Math.abs(activation - gZeroPoint) < 0.001) {
            mainShape.setPaint(Color.white);
        } else if (activation > gZeroPoint) {
            double saturation = SimbrainMath.rescale(activation, 0.0, gUp,0.0,1.0);
            mainShape.setPaint(Color.getHSBColor(hotColor, (float) saturation, 1));
        } else if (activation < gZeroPoint) {
            double saturation = SimbrainMath.rescale(activation, 0,gLow,0,1);
            mainShape.setPaint(Color.getHSBColor(coolColor, (float) saturation, 1));
        }

        if (!customStrokeColor) {

            // Color stroke paint based on Polarity
            if(neuron.getPolarity() == SimbrainConstants.Polarity.EXCITATORY) {
                circle.setStrokePaint(Color.red);
            } else if (neuron.getPolarity() == SimbrainConstants.Polarity.INHIBITORY) {
                circle.setStrokePaint(Color.blue);
            } else {
                circle.setStrokePaint(DEFAULT_STROKE_PAINT);
            }
        }

    }

    /**
     * When spiking change the color of the line around the node.
     */
    private void updateSpikeColor() {
        if (!customStrokeColor) {
            if (neuron.isSpike()) {
                mainShape.setStrokePaint(spikingColor);
                mainShape.setPaint(spikingColor);
            } else {
                // "Erase" the spike color
                // TODO: Interaction with polarity based coloring not tested.
                mainShape.setStrokePaint(SynapseNode.getLineColor());
            }
        }


    }

    /**
     * Update the text label.
     */
    public void updateTextLabel() {
        if (!currentTextVisibility) {
            return;
        }

        if (neuron.getLabel() == null) {
            return;
        }

        // Set label text
        if ((!neuron.getLabel().equalsIgnoreCase("")) || (!neuron.getLabel().equalsIgnoreCase(SimbrainConstants.NULL_STRING))) {
            labelText.setFont(NEURON_FONT);
            labelText.setText("" + neuron.getLabel());
            labelText.setOffset(mainShape.getX() - labelText.getWidth() / 2 + DIAMETER / 2, mainShape.getY() - DIAMETER / 2 - 1);
            labelBackground.setBounds(labelText.getFullBounds());

            // update bounds to include text
            PBounds bounds = mainShape.getBounds();
            bounds.add(labelText.localToParent(labelText.getBounds()));
            setBounds(bounds);
        }
    }

    /**
     * Set basic position of text in the PNode, which is then adjusted depending
     * on the size of the text.
     */
    private void setActivationTextPosition() {
        if (activationText != null) {
            activationText.setOffset(mainShape.getX() + DIAMETER / 4 + 2, mainShape.getY() + DIAMETER / 4 + 1);
        }
    }

    /**
     * Toggles the visibility of the priority view text label.
     *
     * @param makePriorityTextVisible whether the priority text label should be
     *                                visible or not
     */
    public void setPriorityView(boolean makePriorityTextVisible) {
        if (makePriorityTextVisible) {
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
        if (priorityText == null || !currentTextVisibility) {
            return;
        }
        priorityText.setOffset(mainShape.getBounds().getCenterX(), mainShape.getBounds().getCenterY() + DIAMETER - 10);
    }

    /**
     * @return screen element selectable
     * @see ScreenElement
     */
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    public String getToolTipText() {
        String ret = new String();
        ret += neuron.getToolTipText();
        return ret;
    }

    /**
     * Return the center of this node (the circle) in global coordinates.
     *
     * @return the center point of this node.
     */
    public Point2D getCenter() {
        return mainShape.getGlobalBounds().getCenter2D();
    }

    @Override
    public JPopupMenu getContextMenu() {
        return NetworkPanelMenusKt.getNeuronContextMenu(getNetworkPanel());
    }

    @Override
    public JDialog getPropertyDialog() {
        return getNeuronDialog(getNetworkPanel());
    }

    /**
     * Returns String representation of this NeuronNode.
     *
     * @return String representation of this node.
     */
    public String toString() {
        String ret = new String();
        ret += "NeuronNode: (" + this.getGlobalFullBounds().x + ")(" + getGlobalFullBounds().y + ")\n";
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
     * @param neuron neuron for this neuron node
     */
    public void setNeuron(final Neuron neuron) {
        Neuron oldNeuron = this.neuron;
        this.neuron = neuron;
        firePropertyChange(-1, "neuron", oldNeuron, neuron);
    }

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
        getNeuron().setLocation(p);
    }

    /**
     * Updates the position of the view neuron based on the position of the
     * model neuron.
     */
    public void pullViewPositionFromModel() {
        // This is not necessarily a performance drain.  These updates do not automatically cause the
        // canvas to repaint.  See PRoot#processInputs
        Point2D p = new Point2D.Double(getNeuron().getX(), getNeuron().getY());
        this.setGlobalTranslation(p);
    }

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
     * @param xpos The xpos to set.
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
     * @param ypos The ypos to set.
     */
    public void setYpos(final double ypos) {
        Point2D p = new Point2D.Double(getXpos(), ypos);
        globalToLocal(p);
        this.setBounds(p.getX(), p.getY(), this.getWidth(), this.getHeight());
    }

    @Override
    public void resetToDefault() {
        if (!customStrokeColor) {
            mainShape.setStrokePaint(SynapseNode.getLineColor());
        }
        // TODO: Check if change only?
        labelBackground.setPaint(getNetworkPanel().getBackgroundColor());
        updateColor();
    }

    //@Override
    //public void setGrouped(final boolean isGrouped) {
    //    super.setGrouped(isGrouped);
    //    for (SynapseNode synapseNode : connectedSynapses) {
    //        synapseNode.setGrouped(isGrouped);
    //    }
    //}

    @Override
    public Neuron getModel() {
        return getNeuron();
    }

    public static float getHotColor() {
        return hotColor;
    }

    public static void setHotColor(float hotColor) {
        NeuronNode.hotColor = hotColor;
    }

    public static float getCoolColor() {
        return coolColor;
    }

    public static void setCoolColor(float coolColor) {
        NeuronNode.coolColor = coolColor;
    }

    public static Color getSpikingColor() {
        return spikingColor;
    }

    public static void setSpikingColor(Color spikingColor) {
        NeuronNode.spikingColor = spikingColor;
    }

    /**
     * Set a custom color for the circle stroke (not the fill).
     *
     * @param color Color to use
     */
    public void setCustomStrokeColor(Color color) {
        // TODO:Perhaps at some point make it possible to define
        // custom extension of neuron node with custom color schemes
        // This feature hasn't been used much so if it is to stay
        // in the main code it might need some refinement.
        customStrokeColor = true;
        circle.setStrokePaint(color);
        // Custom colors more visible with the clamped stroke
        circle.setStroke(CLAMPED_STROKE);
    }

    public void setUsingCustomStrokeColor(boolean customColor) {
        this.customStrokeColor = customColor;
    }

}
