package org.simbrain.network.nodes.subnetworks;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.ActorCriticPropertiesDialog;
import org.simbrain.network.nodes.CustomOutline;
import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.network.nodes.SubnetworkNode;
import org.simnet.interfaces.Neuron;
import org.simnet.networks.actorcritic.ActorCritic;

import edu.umd.cs.piccolo.PNode;

public class ActorCriticNetworkNode extends SubnetworkNode {

    /** Randomize network action. */
    private Action randomizeAction;

    /** Train network action. */
    //private Action trainAction;
    
    /** Dash style. */
    private static final float[] DASH = {3.0f};

    /** Dash Stroke. */
    private static final BasicStroke DASHED = new BasicStroke(.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 2.0f, DASH, 0.0f);

    /** Layer outline inset. */
    private static final Color LAYER_COLOR = Color.GRAY;

    /** Outline for state layer. */
    private CustomOutline stateOutline = new CustomOutline();

    /** Outline for actor layer. */
    private CustomOutline actorOutline = new CustomOutline();

    /** Outline for critic layer. */
    private CustomOutline criticOutline = new CustomOutline();
    
    /**
     * Create a new ActorCriticNetworkNode.
     *
     * @param networkPanel reference to network panel
     * @param subnetwork reference to subnetwork
     * @param x initial x position
     * @param y initial y position
     */
    public ActorCriticNetworkNode(final NetworkPanel networkPanel,
                                     final ActorCritic subnetwork,
                                     final double x,
                                     final double y) {

        super(networkPanel, subnetwork, x, y);

        stateOutline.setStroke(DASHED);
        stateOutline.setStrokePaint(LAYER_COLOR);
        actorOutline.setStroke(DASHED);
        actorOutline.setStrokePaint(LAYER_COLOR);
        criticOutline.setStroke(DASHED);
        criticOutline.setStrokePaint(LAYER_COLOR);

        addChild(stateOutline);
        addChild(actorOutline);
        addChild(criticOutline);

        randomizeAction = new AbstractAction("Randomize Actor Critic Network") {
            public void actionPerformed(final ActionEvent event) {
                subnetwork.getRootNetwork().fireNetworkChanged();
            }
        };

        /*trainAction = new AbstractAction("Train Actor Critic Network") {
            public void actionPerformed(final ActionEvent event) {
                JDialog propertyDialog = new ActorCriticTrainingDialog((ActorCritic) subnetwork);
                propertyDialog.pack();
                propertyDialog.setLocationRelativeTo(null);
                propertyDialog.setVisible(true);
                subnetwork.getRootNetwork().fireNetworkChanged();
            }
        };*/
    }
    
    /**
     * Set references to layers.
     */
    public void init() {

        ArrayList<PNode> stateNodes = new ArrayList<PNode>();
        ArrayList<PNode> actorNodes = new ArrayList<PNode>();
        ArrayList<PNode> criticNodes = new ArrayList<PNode>();

        ActorCritic subnetwork = (ActorCritic) this.getSubnetwork();
        for (Iterator i = this.getChildrenIterator(); i.hasNext(); ) {
            PNode node = (PNode) i.next();
            if (node instanceof NeuronNode) {
                Neuron neuron = ((NeuronNode) node).getNeuron();
                if (subnetwork.getNetworkList().get(0).getNeuronList().contains(neuron)) {
                    stateNodes.add((NeuronNode) node);
                } else if (subnetwork.getNetworkList().get(1).getNeuronList().contains(neuron)) {
                    actorNodes.add((NeuronNode) node);
                } else if (subnetwork.getNetworkList().get(2).getNeuronList().contains(neuron)) {
                    criticNodes.add((NeuronNode) node);
                }
            }
        }
        stateOutline.setOutlinedObjects(stateNodes);
        actorOutline.setOutlinedObjects(actorNodes);
        criticOutline.setOutlinedObjects(criticNodes);
    }
    
    /** @see SubnetworkNode. */
    protected void updateOutlineBoundsAndPath() {
        super.updateOutlineBoundsAndPath();

        stateOutline.updateBounds();
        actorOutline.updateBounds();
        criticOutline.updateBounds();
    }
    
    @Override
    protected JDialog getPropertyDialog() {
	return new ActorCriticPropertiesDialog(getActorCriticSubnetwork());
    }
    
    /** @see org.simbrain.network.nodes.ScreenElement */
    public ActorCritic getActorCriticSubnetwork() {
        return ((ActorCritic) getSubnetwork());
    }

    @Override
    protected String getToolTipText() {
	return "Actor Critic Network";
    }

    @Override
    protected boolean hasContextMenu() {
	return true;
    }
    
    /** @see org.simbrain.network.nodes.ScreenElement */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = super.getContextMenu();
        contextMenu.add(randomizeAction);
        //contextMenu.addSeparator();
        //contextMenu.add(trainAction);
        contextMenu.addSeparator();
        contextMenu.add(super.getSetPropertiesAction());
        return contextMenu;

    }    

    @Override
    protected boolean hasPropertyDialog() {
	return true;
    }

    @Override
    protected boolean hasToolTipText() {
	return true;
    }
    
    /** @see PNode */
    public PNode removeChild(final PNode child) {
        PNode ret = super.removeChild(child);

        if (stateOutline.getOutlinedObjects().contains(child)) {
            stateOutline.removeOutlinedObject(child);
        } else if (actorOutline.getOutlinedObjects().contains(child)) {
            actorOutline.removeOutlinedObject(child);
        }
        
        updateOutlineBoundsAndPath();
        return ret;
    }    

}
