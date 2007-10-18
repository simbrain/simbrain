/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005-2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network.nodes;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.actions.CopyAction;
import org.simbrain.network.actions.CutAction;
import org.simbrain.network.actions.DeleteAction;
import org.simbrain.network.actions.GroupAction;
import org.simbrain.network.actions.PasteAction;
import org.simbrain.network.actions.SetNeuronPropertiesAction;
import org.simbrain.network.actions.SetSourceNeuronsAction;
import org.simbrain.network.actions.connection.ConnectNeuronsAction;
import org.simbrain.network.actions.connection.ConnectNeuronsSimpleAction;
import org.simbrain.network.actions.connection.ShowConnectDialogAction;
import org.simbrain.network.actions.modelgroups.NewGeneRecGroupAction;
import org.simbrain.network.dialog.neuron.NeuronDialog;
import org.simbrain.util.Utils;
import org.simbrain.workspace.*;
import org.simbrain.workspace.gui.CouplingMenuItem;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.SpikingNeuron;

import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;

/**
 * <b>NeuronNode</b> is a Piccolo PNode corresponding to a Neuron in the neural network model.
 */
public class NeuronNode extends ScreenElement implements ActionListener, PropertyChangeListener {

    /** The logical neuron this screen element represents. */
    private Neuron neuron;

    /** Diameter of neuron. */
    private static final int DIAMETER = 24;

    /** Length of arrow. */
    private static final int ARROW_LINE = 20;

    /** Arrow associated with output node. */
    private PPath outArrow;

    /** Arrow associated with input node. */
    private PPath inArrow;

    /** Text showing sensory coupling information. */
    private PText inLabel = new PText();

    /** Text showing motor coupling information. */
    private PText outLabel = new PText();

    /** Font for input and output labels. */
    public static final Font IN_OUT_FONT = new Font("Arial", Font.PLAIN, 9);

    /** Main circle of node. */
    private PPath circle;

    /** A list of SynapseNodes connected to this NeuronNode; used for updating. */
    private HashSet connectedSynapses = new HashSet();

    /** Id reference to model neuron; used in persistence. */
    private String id;

    /** Number text inside neuron. */
    private PText text;

    /** Whether the node is currently moving or not. */
    private boolean isMoving = false;

    /** Neuron Font. */
    public static final Font NEURON_FONT = new Font("Arial", Font.PLAIN, 11);

    //TODO: These should be replaced with actual scaling of the text object.

    /** Neuron font bold. */
    public static final Font NEURON_FONT_BOLD = new Font("Arial", Font.BOLD, 11);

    /** Neuron font small. */
    public static final Font NEURON_FONT_SMALL = new Font("Arial", Font.PLAIN, 9);

    /** Neuron font very small. */
    public static final Font NEURON_FONT_VERYSMALL = new Font("Arial", Font.PLAIN, 7);


    /**
     * Default constructor; used by Castor.
     */
    public NeuronNode() {
        circle = PPath.createEllipse(0, 0, DIAMETER, DIAMETER);
    }

    //TODO: Delete this when everything has been converted
    /**
     * Initialize a NeuronNode to a location. Used by Castor.
     *
     * @param x x coordinate of NeuronNode
     * @param y y coordinate of NeuronNode
     */
    public NeuronNode(final double x, final double y) {
        offset(x, y);
    }

    /**
     * Create a new neuron node.
     *
     * @param net Reference to NetworkPanel
     * @param neuron reference to model neuron
     */
    public NeuronNode(final NetworkPanel net, final Neuron neuron) {
        super(net);
        this.neuron = neuron;
        offset(neuron.getX(), neuron.getY());
        init();
    }

    /**
     * Initialize the NeuronNode.
     */
    private void init() {
        circle = PPath.createEllipse(0, 0, DIAMETER, DIAMETER);

        addChild(circle);

        // Handle input and output arrows
        outArrow = createOutArrow();
        inArrow = createInArrow();
        inLabel = createInputLabel();
        outLabel = createOutputLabel();
        addChild(outArrow);
        addChild(inArrow);
        addChild(inLabel);
        addChild(outLabel);
        updateOutArrow();
        updateInArrow();
        updateInLabel();
        updateOutLabel();

        text = new PText(String.valueOf((int) Math.round(neuron.getActivation())));
        text.setFont(NEURON_FONT);
        setTextPosition();

        this.addChild(text);

        update();

        setPickable(true);
        setChildrenPickable(false);

        addPropertyChangeListener(PROPERTY_FULL_BOUNDS, this);

        // The main circle is what users select
        setBounds(circle.getBounds());
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
      ret += getCouplingText();
      return ret;
    }

    /**
     * Returns information about couplings.
     *
     * @return coupling information.
     */
    private String getCouplingText() {
        String ret = new String("");
//        if (neuron.isInput()) {
//            ret += " \n Sensory Coupling  ";
//            if (neuron.getSensoryCoupling().getAgent() == null) {
//                ret += " ** unattached ** ";
//            }
//            ret += "\n   World: " + neuron.getSensoryCoupling().getWorldName() + " ";
//            ret += "\n   Agent: " + neuron.getSensoryCoupling().getAgentName() + " ";
//            ret += "\n   Sensor: " + neuron.getSensoryCoupling().getShortLabel() + " ";
//        }
//        if (neuron.isOutput()) {
//            ret += " \n Motor Coupling ";
//            if (neuron.getMotorCoupling().getAgent() == null) {
//                ret += " ** unattaached ** ";
//            }
//            ret += "\n   World: " + neuron.getMotorCoupling().getWorldName() + " ";
//            ret += "\n   Agent: " + neuron.getMotorCoupling().getAgentName() + " ";
//            ret += "\n   Command: " + neuron.getMotorCoupling().getShortLabel()  + " ";
//        }
        return ret;
    }

    /** @see ScreenElement */
    protected boolean hasContextMenu() {
        return true;
    }

    /**
     * Return the center of this node (the circle) in global coordinates.
     * @return the center point of this node.
     */
    public Point2D getCenter() {
        return circle.getGlobalBounds().getCenter2D();
    }

    /** @see ScreenElement */
    protected JPopupMenu getContextMenu() {

        JPopupMenu contextMenu = new JPopupMenu();

        // Cut, copy, paste
        contextMenu.add(new CutAction(getNetworkPanel()));
        contextMenu.add(new CopyAction(getNetworkPanel()));
        contextMenu.add(new PasteAction(getNetworkPanel()));
        contextMenu.addSeparator();

        // Group action
        contextMenu.add(getNetworkPanel().getActionManager().getGroupAction());
        contextMenu.addSeparator();

        // Delete action
        contextMenu.add(new DeleteAction(getNetworkPanel()));
        contextMenu.addSeparator();

        //Model Group Action; TODO Make this a submenu with group types
        contextMenu.add(new NewGeneRecGroupAction(getNetworkPanel()));
        contextMenu.addSeparator();

        // Add Connect Actions
        if (getNetworkPanel().getSelectedNeurons() != null) {
            contextMenu.add(new ConnectNeuronsSimpleAction(getNetworkPanel(), getNetworkPanel().getSelectedNeurons()
                    , this));
        }
        contextMenu.add(getConnectMenu());
        contextMenu.addSeparator();

        // Add align and space menus if objects are selected
        if (getNetworkPanel().getSelectedNeurons().size() > 1) {
            contextMenu.add(getNetworkPanel().createAlignMenu());
            contextMenu.add(getNetworkPanel().createSpacingMenu());
            contextMenu.addSeparator();
        }

        // Add coupling menus
        if (getNetworkPanel().getSelectedNeurons().size() == 1) {
            JMenu producerMenu = org.simbrain.workspace.gui.SimbrainDesktop.getInstance().getProducerMenu(this);
            producerMenu.setText("Set input source");
            contextMenu.add(producerMenu);
            JMenu consumerMenu = org.simbrain.workspace.gui.SimbrainDesktop.getInstance().getConsumerMenu(this);
            consumerMenu.setText("Set output target");
            contextMenu.add(consumerMenu);
            contextMenu.addSeparator();
        } else if (getNetworkPanel().getSelectedNeurons().size() > 1) {
            JMenu producerMenu = org.simbrain.workspace.gui.SimbrainDesktop.getInstance().getProducerListMenu(this);
            producerMenu.setText("Set input sources");
            contextMenu.add(producerMenu);
            JMenu consumerMenu = org.simbrain.workspace.gui.SimbrainDesktop.getInstance().getConsumerListMenu(this);
            consumerMenu.setText("Set output targets");
            contextMenu.add(consumerMenu);
            contextMenu.addSeparator();
        }

        contextMenu.add(new SetNeuronPropertiesAction(getNetworkPanel()));

       return contextMenu;
    }

    /**
     * Connection sub menu.
     *
     * @return Connection sub menu
     */
    private JMenu getConnectMenu() {
        JMenu menu = new JMenu("Connect");

        menu.add(new SetSourceNeuronsAction(getNetworkPanel()));

        // If neurons have been selected, create an acction which will connect selected neurons to this one
        if (getNetworkPanel().getSelectedNeurons() != null) {
            menu.add(new ConnectNeuronsAction(getNetworkPanel(), getNetworkPanel().getSourceModelNeurons(),
                    getNetworkPanel().getSelectedModelNeurons()));
        }
        menu.add(new ShowConnectDialogAction(getNetworkPanel()));
        return menu;
    }

    /** @see ScreenElement */
    protected boolean hasPropertyDialog() {
        return true;
    }

    /** @see ScreenElement */
    protected JDialog getPropertyDialog() {
        NeuronDialog dialog = new NeuronDialog(this.getNetworkPanel().getSelectedNeurons());
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
     * Sets the color of this neuron based on its activation level.
     */
    private void updateColor() {
        double activation = neuron.getActivation();

        //Force to blank if 0
        if ((activation > -.1) && (activation < .1)) {
            circle.setPaint(Color.white);
        } else if (activation > 0) {
            float saturation = checkValid((float) Math.abs(activation / neuron.getUpperBound()));
            circle.setPaint(Color.getHSBColor(getNetworkPanel().getHotColor(), saturation, (float) 1));
        } else if (activation < 0) {
            float saturation = checkValid((float) Math.abs(activation / neuron.getLowerBound()));
            circle.setPaint(Color.getHSBColor(getNetworkPanel().getCoolColor(), saturation, (float) 1));
        }

        if (neuron instanceof SpikingNeuron) {
            if (((SpikingNeuron) neuron).hasSpiked()) {
                circle.setStrokePaint(getNetworkPanel().getSpikingColor());
                outArrow.setStrokePaint(getNetworkPanel().getSpikingColor());
            } else {
                circle.setStrokePaint(getNetworkPanel().getLineColor());
                outArrow.setStrokePaint(getNetworkPanel().getLineColor());
            }
        }
    }

    /**
     * Check whether the specified saturation is valid or not.
     *
     * @param val the saturation value to check.
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
     * Creates an arrow which designates an on-screen neuron as an output node, which sends signals to an external
     * environment (the world object).
     *
     * @return an object representing the input arrow of a PNodeNeuron
     *
     * @see org.simbrain.sim.world
     */
    private PPath createOutArrow() {
        PPath path = new PPath();
        GeneralPath arrow = new GeneralPath();
        Point2D p = this.globalToLocal(this.getOffset());
        float cx = (float) p.getX() + DIAMETER / 2;
        float cy = (float) p.getY() + DIAMETER / 2;

        arrow.moveTo(cx, cy - DIAMETER / 2);
        arrow.lineTo(cx, cy - DIAMETER / 2 - ARROW_LINE);

        arrow.moveTo(cx, cy - DIAMETER / 2 - ARROW_LINE);
        arrow.lineTo(cx - DIAMETER / 4, cy - DIAMETER);

        arrow.moveTo(cx, cy - DIAMETER / 2 - ARROW_LINE);
        arrow.lineTo(cx + DIAMETER / 4, cy - DIAMETER);

        path.append(arrow, true);
        return path;
    }

    /**
     * Create the input label.
     *
     * @return the input label.
     */
    private PText createInputLabel() {
        PText ret = new PText();
        ret.setFont(IN_OUT_FONT);
        //ret.setPaint(getNetworkPanel().getLineColor());
        ret.translate(this.getX(), this.getY() + DIAMETER / 2 + ARROW_LINE + 15);
        return ret;
    }

    /**
     * Create the output label.
     *
     * @return the output label.
     */
    private PText createOutputLabel() {
        PText ret = new PText();
        ret.setFont(IN_OUT_FONT);
        //ret.setPaint(getNetworkPanel().getLineColor());
        ret.translate(this.getX(), this.getY() - DIAMETER / 2 - ARROW_LINE - 5);
        return ret;
    }

    /**
     * Creates an arrow which designates an on-screen neuron as an input node, which receives signals from an external
     * environment (the world object).
     *
     * @return an object representing the input arrow of a PNodeNeuron
     *
     * @see org.simbrain.sim.world
     */
    private PPath createInArrow() {
        PPath path = new PPath();
        GeneralPath arrow = new GeneralPath();
        Point2D p = this.globalToLocal(this.getOffset());
        float cx = (float) p.getX() + DIAMETER / 2;
        float cy = (float) p.getY() + DIAMETER / 2;
        float top = cy + DIAMETER / 2 + 1;

        arrow.moveTo(cx, top);
        arrow.lineTo(cx, top + ARROW_LINE);

        arrow.moveTo(cx, top);
        arrow.lineTo(cx - DIAMETER / 4 , cy + DIAMETER / 2 + DIAMETER / 4);

        arrow.moveTo(cx, top);
        arrow.lineTo(cx + DIAMETER / 4 , top + DIAMETER / 4);

        path.append(arrow, true);

//        path.addInputEventListener(new ToolTipTextUpdater() {
//
//            /** @see ToolTipTextUpdater */
//            protected String getToolTipText() {
//                return getCouplingText();
//            }
//        });
        return path;
    }

    /**
     * Determine what color and and font to use for this neuron based in its activation level.
     */
    private void updateText() {
        double act = neuron.getActivation();
        text.setScale(1);
        setTextPosition();

        // 0 (or close to it) is a special case--a black font
        if ((act == 0)) {
            //text.setPaint(Color.black);
            text.setFont(NEURON_FONT);
            text.setText("0");
            // In all other cases the background color of the neuron is white
            // Between 0 and 1
        } else if ((act > 0) && (neuron.getActivation() < 1)) {
            //text.setPaint(Color.white);
            text.setFont(NEURON_FONT_BOLD);
            text.setText(String.valueOf(Utils.round(act, 4)).substring(1, 3));
        } else if ((act < 0) && (act > -1)) { // Between 0 and -.1
            //text.setPaint(Color.white);
            text.setFont(NEURON_FONT_BOLD);
            text.setText("-" + String.valueOf(Utils.round(act, 4)).substring(2, 4));
        } else { // greater than 1 or less than -1
            //text.setPaint(Color.white);
            text.setFont(NEURON_FONT_BOLD);
            if (Math.abs(act) < 10) {
                text.scale(.9);
            } else if (Math.abs(act) < 100) {
                text.scale(.8);
                text.translate(1, 1);
            } else {
                text.scale(.7);
                text.translate(-1, 2);
            }
            text.setText(String.valueOf((int) Math.round(act)));
        }
    }

    /**
     * Set basic position of text int the PNode, which is then adjusted depending on the size of the text.
     */
    private void setTextPosition() {
        if (text == null) {
            return;
        }
        text.setOffset(getX() + DIAMETER / 4 + 2, getY() + DIAMETER / 4 + 1);
    }

    /**
     * Updates graphics depending on whether this is an input node or not.
     */
    public void updateInArrow() {
        if (neuron.isInput()) {
            inArrow.setVisible(true);
        } else {
            inArrow.setVisible(false);
        }
    }

    /**
     * Updates graphics depending on whether this is an output node or not.
     */
    public void updateOutArrow() {
        if (neuron.isOutput()) {
            outArrow.setVisible(true);
        } else {
            outArrow.setVisible(false);
        }
    }

    /**
     * Update the label showing sensory coupling information.
     */
    public void updateInLabel() {
        if (getNetworkPanel().getInOutMode()) {
            if (getNeuron().isInput()) {
               // inLabel.setText(getNeuron().getSensoryCoupling().getShortLabel());
                inLabel.setVisible(true);
            } else {
                inLabel.setVisible(false);
            }
        } else {
            inLabel.setVisible(false);
        }
    }

    /**
     * Update the label showing sensory coupling information.
     */
    public void updateOutLabel() {
        if (getNetworkPanel().getInOutMode()) {
            if (getNeuron().isOutput()) {
               // outLabel.setText(getNeuron().getMotorCoupling().getShortLabel());
                outLabel.setVisible(true);
            } else {
                outLabel.setVisible(false);
            }
        } else {
            outLabel.setVisible(false);
        }
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
     *
     * <p>This is a bound property.</p>
     *
     * @param neuron neuron for this neuron node
     */
    public void setNeuron(final Neuron neuron) {

        Neuron oldNeuron = this.neuron;
        this.neuron = neuron;
        firePropertyChange("neuron", oldNeuron, neuron);
    }

    /** @see ScreenElement */
    protected JPopupMenu createContextMenu() {
        return getContextMenu();
    }

    /** @see PropertyChangeListener */
    public void propertyChange(final PropertyChangeEvent event) {
        updateSynapseNodePositions();
    }

    /**
     * Update the position of the model neuron based on the global coordinates of this pnode.
     */
    public void pushViewPositionToModel() {
        //System.out.println("model neuron updated");
        Point2D p = this.getGlobalTranslation();
        getNeuron().setX(p.getX());
        getNeuron().setY(p.getY());
    }


    /**
     * Updates the psoition of the view neuron based on the position of the model neuron.
     */
    public void pullViewPositionFromModel() {
        //System.out.println("view neuron updated");
        Point2D p = new Point2D.Double(getNeuron().getX(), getNeuron().getY());
        this.setGlobalTranslation(p);
    }

    /**
     * @see ActionListener
     */
    public void actionPerformed(final ActionEvent e) {
        // Handle pop-up menu events
        Object o = e.getSource();

        // Actions are based on what is set in the coupling menu item...
        if (o instanceof CouplingMenuItem) {
            CouplingMenuItem m = (CouplingMenuItem) o;
            if (m.getEventType() == CouplingMenuItem.EventType.SINGLE_PRODUCER) {
                Coupling coupling = new Coupling( m.getProducingAttribute(), this.getNeuron().getDefaultConsumingAttribute());
                this.getNetworkPanel().getRootNetwork().getCouplings().add(coupling); // THIS IS WRONG!
            } else if (m.getEventType() == CouplingMenuItem.EventType.SINGLE_CONSUMER) {
                Coupling coupling = new Coupling(this.getNeuron().getDefaultProducingAttribute(), m.getConsumingAttribute());
                this.getNetworkPanel().getRootNetwork().getCouplings().add(coupling);
            } else if (m.getEventType() == CouplingMenuItem.EventType.PRODUCER_LIST) {
                     // BUT WHAT IF A COUPLINGCONTAINER has producers and consumers?
                    // Iterate through selected neurons and attach as many producers as possible
                    // TODO: Move this code to networkpanel and make it more general than neurons.
                    Iterator producerIterator = m.getWorkspaceComponent().getProducers().iterator(); // Get the other guy's producers
                    for (Neuron neuron : getNetworkPanel().getSelectedModelNeurons()) { // Iterate through our consumers
                        if (producerIterator.hasNext()) {
                            Coupling coupling = new Coupling(((org.simbrain.workspace.Producer)producerIterator.next()).getDefaultProducingAttribute(), neuron.getDefaultConsumingAttribute());
                            this.getNetworkPanel().getRootNetwork().getCouplings().add(coupling);
                        } else {
                            break;
                        }
                    }
            } else if (m.getEventType() == CouplingMenuItem.EventType.CONSUMER_LIST) {
                    // Send our producers over to their consumers
                // TODO refactor
//                    m.getCouplingContainer().getCouplings().clear(); //TODO: need some form of reset also
                    Iterator producerIterator =  getNetworkPanel().getSelectedModelNeurons().iterator(); // Get the other guy's producers
                    for (Consumer consumer : m.getWorkspaceComponent().getConsumers()) {
                        if (producerIterator.hasNext()) {
                            Coupling coupling = new Coupling(((org.simbrain.workspace.Producer) producerIterator.next()).getDefaultProducingAttribute(), consumer.getDefaultConsumingAttribute());
                            // TODO refactor
//                            m.getCouplingContainer().getCouplings().add(coupling);
                        } else {
                            break;
                        }
                    }
            }
        }
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
    public Set getConnectedSynapses() {
        // TODO:
        // may want to make this set unmodifiable
        return connectedSynapses;
    }

    /**
     * Update connected synapse node positions.
     */
    public void updateSynapseNodePositions() {

        for (Iterator i = connectedSynapses.iterator(); i.hasNext(); ) {
            SynapseNode synapseNode = (SynapseNode) i.next();
            synapseNode.updatePosition();
        }
    }

    /**
     * Synapse node position updater, called in response
     * to changes in this neuron node's fullBounds property.
     */
    private class SynapseNodePositionUpdater
        implements PropertyChangeListener {

        /** @see PropertyChangeListener */
        public void propertyChange(final PropertyChangeEvent event) {
            updateSynapseNodePositions();
        }
    }

    /**
     * @return Returns the id.
     */
    public String getId() {
        return "pnode_" + neuron.getId();
    }

    /**
     * @param id The id to set.
     */
    public void setId(final String id) {
        this.id = id;
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

    /**
     * Change the color of input and output nodes to reflect whether they are 'attached' to an agent in a world.
     */
    public void updateAttachmentStatus() {
//        if (neuron.getSensoryCoupling() != null) {
//            if (neuron.getSensoryCoupling().isAttached()) {
//                inArrow.setStrokePaint(getNetworkPanel().getLineColor());
//            } else {
//                inArrow.setStrokePaint(Color.GRAY);
//            }
//        }
//
//        if (neuron.getMotorCoupling() != null) {
//            if (neuron.getMotorCoupling().isAttached()) {
//                outArrow.setStrokePaint(getNetworkPanel().getLineColor());
//            } else {
//                outArrow.setStrokePaint(Color.GRAY);
//            }
//        }
    }

    /** @see ScreenElement */
    public void resetColors() {
        circle.setStrokePaint(getNetworkPanel().getLineColor());
        inArrow.setStrokePaint(getNetworkPanel().getLineColor());
        outArrow.setStrokePaint(getNetworkPanel().getLineColor());
        updateColor();
    }

    /**
     * @return Returns the isMoving.
     */
    public boolean isMoving() {
        return isMoving;
    }

    /**
     * @param isMoving The isMoving to set.
     */
    public void setMoving(final boolean isMoving) {
        this.isMoving = isMoving;
    }

    /** @see ScreenElement. */
    public void setGrouped(final boolean isGrouped) {
        super.setGrouped(isGrouped);
        for (Iterator i = connectedSynapses.iterator(); i.hasNext(); ) {
            SynapseNode synapseNode = (SynapseNode) i.next();
            synapseNode.setGrouped(isGrouped);
        }
    }

}
