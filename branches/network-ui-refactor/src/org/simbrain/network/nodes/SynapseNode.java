
package org.simbrain.network.nodes;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Arc2D;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;
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

    /** Current radius of the circle; represents strength of logical synapse. */
    private double radius = 5;

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
     * Create a new synapse node connecting a source and target neuron.
     *
     * @param net Reference to NetworkPanel
     * @param source source neuronnode
     * @param target target neuronmode
     */
    public SynapseNode(final NetworkPanel net, final NeuronNode source, final NeuronNode target) {

        super(net);
        this.source = source;
        this.target = target;

        updatePosition();

        //calColor(weight.getStrength(), isSelected());
        this.addChild(circle);
        circle.setPaint(Color.CYAN);


        //        
        //        if (source.getNeuron() == target.getNeuron()) {
        //            self_connection = new Arc2D.Double();
        //            weightLine = new PNodeLine(self_connection);
        //        } else {
        //            line = new Line2D.Double();
        //            weightLine = new PNodeLine(line);
        //        }
        line.setStrokePaint(Color.BLACK);
        line.moveToBack();

        setPickable(true);
        setChildrenPickable(false);

        // The main circle is what users select
        setBounds(circle.getBounds());
    }

    /**
     * Update position of synapse.
     */
    public void updatePosition() {
        //Set location of synapse 
        double sourceCX = localToGlobal(source.getOffset()).getX() + NeuronNode.getDIAMETER() / 2;
        double sourceCY = localToGlobal(source.getOffset()).getY() + NeuronNode.getDIAMETER() / 2;
        double targetCX = localToGlobal(target.getOffset()).getX() + NeuronNode.getDIAMETER() / 2;
        double targetCY = localToGlobal(target.getOffset()).getY() + NeuronNode.getDIAMETER() / 2;
        Point newPoint = calcWt(sourceCX, sourceCY, targetCX, targetCY);

        if (circle == null) {
            circle = PPath.createEllipse(  (float) (newPoint.getX() - radius), (float) (newPoint.getY() - radius),
                    (float) radius * 2, (float) radius * 2);            
        } else {
            circle.setX(newPoint.getX() - radius);
            circle.setY(newPoint.getY() - radius);
        }


        if (line != null) {
            this.removeChild(line);
        } 

        line = PPath.createLine((float) sourceCX,(float) sourceCY,(float) targetCX,(float) targetCY);
        this.addChild(line);


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
    public Point calcWt(final double sourceX, final double sourceY, final double targetX, final double targetY) {

        double x = Math.abs(sourceX - targetX);
        double y = Math.abs(sourceY - targetY);
        double alpha = Math.atan(y / x);

        int weightX = 0;
        int weightY = 0;

        double OFFSET = NeuronNode.getDIAMETER() / 2;
 
        if (sourceX < targetX) {
            weightX = (int) Math.round(targetX - (OFFSET * Math.cos(alpha)));
        } else {
            weightX = (int) Math.round(targetX + (OFFSET * Math.cos(alpha)));
        }

        if (sourceY < targetY) {
            weightY = (int) Math.round(targetY - (OFFSET * Math.sin(alpha)));
        } else {
            weightY = (int) Math.round(targetY + (OFFSET * Math.sin(alpha)));
        }

        return new Point(weightX, weightY);
    }


    /** @see ScreenElement */
    protected boolean hasToolTipText() {
        return true;
    }

    /** @see ScreenElement */
    protected String getToolTipText() {
        //return "" + neuron.getActivation();
        return "synapse";
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
        return false;
    }

    /** @see ScreenElement */
    protected JDialog getPropertyDialog() {
        return null;
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



}