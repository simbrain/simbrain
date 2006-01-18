
package org.simbrain.network.nodes;

import java.awt.Color;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.actions.DeleteAction;
import org.simbrain.network.actions.CopyAction;
import org.simbrain.network.actions.CutAction;
import org.simbrain.network.actions.PasteAction;
import org.simbrain.network.actions.SetSynapsePropertiesAction;
import org.simbrain.network.dialog.synapse.SynapseDialog;
import org.simbrain.workspace.Workspace;
import org.simnet.interfaces.SpikingNeuron;
import org.simnet.interfaces.Synapse;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;

/**
 * <b>NeuronNode</b> is a Piccolo PNode corresponding to a Neuron in the neural network model.
 */
public final class SynapseNode
    extends ScreenElement {

    /** The logical synapse this screen element represents. */
    private Synapse synapse;

    /** Location of circle relative to target node. */
    private static double OFFSET = 7;
    
    /** Main circle of synapse. */
    private PNode circle;

    /** Line connecting nodes. */
    private PPath line;

    /** Line used when the synapse connects a neuron to itself. */
    private Arc2D self_connection;

    /** Reference to source neuron. */
    private NeuronNode source;

    /** Reference to target neuron. */
    private NeuronNode target;

    /**
     * Default constructor; used by Castor.
     */
    public SynapseNode() {
    }

    /**
     * Create a new synapse node connecting a source and target neuron.
     *
     * @param net Reference to NetworkPanel
     * @param source source neuronnode
     * @param target target neuronmode
     * @param synapse the model synapse this PNode represents
     */
    public SynapseNode(final NetworkPanel net, final NeuronNode source,
                       final NeuronNode target, final Synapse synapse) {

        super(net);
        this.source = source;
        this.target = target;
        target.getConnectedSynapses().add(this);
        source.getConnectedSynapses().add(this);

        this.synapse = synapse;
        init();
    }

    /** @see ScreenElement */
    public void initCastor(final NetworkPanel net) {
        super.initCastor(net);
        target.getConnectedSynapses().add(this);
        source.getConnectedSynapses().add(this);
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
        line.moveToBack();

        updateColor();
        updateDiameter();

        setPickable(true);
        setChildrenPickable(false);
    }

    /**
     * Update position of synapse.
     */
    public void updatePosition() {

        Point2D synapseCenter;

        // Position the synapse
        if (isSelfConnection()) {
            synapseCenter = globalToLocal(new Point2D.Double(target.getCenter().getX() + OFFSET,
                    target.getCenter().getY() + OFFSET));
        } else {
            synapseCenter = globalToLocal(calcCenter(source.getCenter(), target.getCenter()));
        }
        this.offset(synapseCenter.getX() - OFFSET, synapseCenter.getY() - OFFSET);

        // Create the circle
        if (circle == null) {
            circle = PPath.createEllipse((float) 0, (float) 0, (float) OFFSET * 2, (float) OFFSET * 2);
            ((PPath) circle).setStrokePaint(null);
            setBounds(circle.getFullBounds());
        }

        // Create the line
        if (line == null) {
            line = getLine(globalToLocal(synapseCenter));
        }

        // Update the line (unless it's a self connection)
        if (!isSelfConnection()) {
            line.reset();
            line.append(new Line2D.Double(globalToLocal(source.getCenter()), synapseCenter), false);
        }
    }

    /**
     * Whether this synapse connets a neuron to itself or not.
     *
     * @return true if this synapse connects a neuron to itself.
     */
    private boolean isSelfConnection() {
        return (source.getNeuron() == target.getNeuron());
    }

    /**
     * Create the line depending on whether this is self connected or not.
     *
     * @param center the center of the synapse
     * @return the line
     */
    private PPath getLine(final Point2D center) {
        if (isSelfConnection()) {
            return new PPath(new Arc2D.Double(getX(), getY() - 7, 22, 15, 1, 355, Arc2D.OPEN));    
        } else {
            return new PPath(new Line2D.Double(globalToLocal(source.getCenter()), center));
        }
    }

    /**
     * Calculates the color for a weight, based on its current strength.  Positive values are (for example) red,
     * negative values blue.
     */
    public void updateColor() {
        if (synapse.getStrength() < 0) {
            circle.setPaint(getNetworkPanel().getInhibitoryColor());
        } else if (synapse.getStrength() == 0) {
            circle.setPaint(getNetworkPanel().getInhibitoryColor());
        } else {
            circle.setPaint(getNetworkPanel().getExcitatoryColor());
        }

        if (source.getNeuron() instanceof SpikingNeuron) {
            if (((SpikingNeuron) source.getNeuron()).hasSpiked()) {
                line.setStrokePaint(getNetworkPanel().getSpikingColor());
            } else {
                line.setStrokePaint(getNetworkPanel().getLineColor());
            }
        }
    }

    /**
     * Update the diameter of the drawn weight based on the logical weight's strength.
     */
    public void updateDiameter() {
        double diameter;

        if (synapse.getStrength() > 0) {
            diameter = (((getNetworkPanel().getMaxDiameter() - getNetworkPanel()
                    .getMinDiameter()) * (synapse.getStrength() / synapse
                    .getUpperBound())) + getNetworkPanel().getMinDiameter());
        } else {
            diameter = (((getNetworkPanel().getMaxDiameter() - getNetworkPanel()
                    .getMinDiameter()) * (Math.abs(synapse.getStrength()
                    / synapse.getLowerBound()))) + getNetworkPanel()
                    .getMinDiameter());
        }

        double delta = (circle.getBounds().getWidth() - diameter) / 2;

        circle.setWidth(diameter);
        circle.setHeight(diameter);
        // offset properly moves circle, but this is not reflected in bounds
        circle.offset(delta, delta);
        setBounds(circle.getBounds());
    }

    /**
     * Calculates the position of the synapse circle based on the positions of the source and target
     * NeuronNodes.
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

        int NEURON_OFFSET = NeuronNode.getDIAMETER() / 2;

        if (sourceX < targetX) {
            weightX = targetX - (NEURON_OFFSET * Math.cos(alpha));
        } else {
            weightX = targetX + (NEURON_OFFSET * Math.cos(alpha));
        }

        if (sourceY < targetY) {
            weightY = targetY - (NEURON_OFFSET * Math.sin(alpha));
        } else {
            weightY = targetY + (NEURON_OFFSET * Math.sin(alpha));
        }

        return new Point2D.Double(weightX, weightY);
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
        return false;
    }

    /** @see ScreenElement */
    protected boolean hasToolTipText() {
        return true;
    }

    /** @see ScreenElement */
    protected String getToolTipText() {
        return String.valueOf(synapse.getStrength());
    }

    /** @see ScreenElement */
    public boolean hasContextMenu() {
        return true;
    }

    /** @see ScreenElement */
    protected JPopupMenu getContextMenu() {

        JPopupMenu contextMenu = new JPopupMenu();

        contextMenu.add(new CutAction(getNetworkPanel()));
        contextMenu.add(new CopyAction(getNetworkPanel()));
        contextMenu.add(new PasteAction(getNetworkPanel()));
        contextMenu.addSeparator();

        contextMenu.add(new DeleteAction(getNetworkPanel()));
        contextMenu.addSeparator();

        Workspace workspace = getNetworkPanel().getWorkspace();
        if (workspace.getGaugeList().size() > 0) {
            contextMenu.add(workspace.getGaugeMenu(getNetworkPanel()));
            contextMenu.addSeparator();
        }

        contextMenu.add(new SetSynapsePropertiesAction(getNetworkPanel()));

        return contextMenu;
    }


    /** @see ScreenElement */
    protected boolean hasPropertyDialog() {
        return true;
    }

    /** @see ScreenElement */
    protected JDialog getPropertyDialog() {
        SynapseDialog dialog = new SynapseDialog(this.getNetworkPanel().getSelectedSynapses());
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

    /** @see ScreenElement. */
    public void resetColors() {
        line.setStrokePaint(getNetworkPanel().getLineColor());
        updateColor();
    }

}