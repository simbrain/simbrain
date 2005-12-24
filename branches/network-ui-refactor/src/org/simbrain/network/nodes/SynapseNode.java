
package org.simbrain.network.nodes;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.neuron.NeuronDialog;
import org.simbrain.network.dialog.synapse.SynapseDialog;
import org.simnet.interfaces.Network;
import org.simnet.interfaces.Synapse;
import org.simnet.synapses.ClampedSynapse;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;

/**
 * <b>NeuronNode</b> is a Piccolo PNode corresponding to a Neuron in the neural network model.
 */
public final class SynapseNode
    extends ScreenElement {

    /** The logical synapse this screen element represents. */
    private Synapse synapse;

    /** Current radius of the circle; represents strength of logical synapse. */
    private double radius = 7;

    /** Main circle of synapse. */
    private PNode circle;

    /** Line connecting nodes. */
    private PPath line;

    /** Line used when the synapse connects a neuron to itself. */
    private Arc2D self_connection;

    /** Maximum radius of the circle representing the synapse. */
    private static int maxRadius = 16;

    /** Maximum radius of the circle representing the synapse. */
    private static int minRadius = 7;

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
    public SynapseNode(final NetworkPanel net, final NeuronNode source, final NeuronNode target, Synapse synapse) {

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
        circle.setPaint(Color.CYAN);
        line.setStrokePaint(Color.BLACK);
        line.moveToBack();

        //calColor(weight.getStrength(), isSelected());


        //        if (source.getNeuron() == target.getNeuron()) {
        //            self_connection = new Arc2D.Double();
        //            weightLine = new PNodeLine(self_connection);
        //        } else {
        //            line = new Line2D.Double();
        //            weightLine = new PNodeLine(line);
        //        }

        setPickable(true);
        setChildrenPickable(false);
    }

    /**
     * Update position of synapse.
     */
    public void updatePosition() {

        Point2D synapseCenter = globalToLocal(calcCenter(source.getCenter(), target.getCenter()));

        this.offset(synapseCenter.getX() - radius, synapseCenter.getY() - radius);

        if (circle == null) {
            circle = PPath.createEllipse((float) 0, (float) 0, (float) radius * 2, (float) radius * 2);
            setBounds(circle.getBounds());
        }

        if (line == null) {
            line = new PPath(new Line2D.Double(globalToLocal((source.getCenter())), globalToLocal(synapseCenter)));
        } else {
            line.reset();
            line.append(new Line2D.Double(globalToLocal((source.getCenter())), synapseCenter), false);
        }
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

        double OFFSET = NeuronNode.getDIAMETER() / 2;

        if (sourceX < targetX) {
            weightX = targetX - (OFFSET * Math.cos(alpha));
        } else {
            weightX = targetX + (OFFSET * Math.cos(alpha));
        }

        if (sourceY < targetY) {
            weightY = targetY - (OFFSET * Math.sin(alpha));
        } else {
            weightY = targetY + (OFFSET * Math.sin(alpha));
        }

        return new Point2D.Double(weightX, weightY);
    }

    /**
     * Change the type of weight this pnode is associated with It is assumed that the basic properties of the new
     * weight have been set.
     *
     * @param newSynapse the synapse to change to
     */
    public void changeSynapse(final Synapse newSynapse) {
        Network.changeSynapse(synapse, newSynapse);
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
//        contextMenu.add(getNetworkPanel().getWorkspace().getMotorCommandMenu(this, this));
//        contextMenu.add(getNetworkPanel().getWorkspace().getSensorIdMenu(this, this));

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



}