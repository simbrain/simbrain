package org.simbrain.network.gui.nodes;

import org.piccolo2d.PNode;
import org.simbrain.network.NetworkModel;
import org.simbrain.network.groups.AbstractNeuronCollection;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

public abstract class AbstractNeuronCollectionNode extends PNode implements GroupNode {

    /**
     * Parent network panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * The outlined objects (neurons) for this neuron group.
     */
    private final NeuronGroupingOutline outlinedObjects;

    /**
     * The interaction box for this neuron colection
     */
    private InteractionBox interactionBox;

    private Set<NeuronNode> neuronNodes = new HashSet<>();

    public AbstractNeuronCollectionNode(NetworkPanel networkPanel, Group group) {
        this.networkPanel = networkPanel;

        outlinedObjects = new NeuronGroupingOutline();
        addChild(outlinedObjects);

        group.addPropertyChangeListener(evt -> {
            //System.out.println(evt.getPropertyName());
            if ("delete".equals(evt.getPropertyName())) {
                updateOutline();
            } else if ("moved".equals(evt.getPropertyName())) {
                updateOutline();
            }
        });
    }

    /**
     * Override PNode layoutChildren method in order to properly set the
     * positions of children nodes.
     */
    @Override
    public void layoutChildren() {
        if (this.getVisible() && !networkPanel.isRunning()) {
            interactionBox.setOffset(outlinedObjects.getFullBounds().getX() + NeuronGroupingOutline.ARC_SIZE / 2,
                    outlinedObjects.getFullBounds().getY() - interactionBox.getFullBounds().getHeight() + 1);
        }
    }

    /**
     * Sync all neuron nodes in the group to the model.
     */
    public void syncToModel() {
        for (NeuronNode neuronNode : neuronNodes) {
            neuronNode.pullViewPositionFromModel();
        }
    }

    @Override
    public void offset(double dx, double dy) {
        if (networkPanel.isRunning()) {
            return;
        }
        for (NeuronNode neuronNode : neuronNodes) {
            neuronNode.offset(dx, dy);
        }
        updateOutline();
    }

    /**
     * Add a neuron node to the group node.
     *
     * @param node to add
     */
    public void addNeuronNode(NeuronNode node) {
        neuronNodes.add(node);
        updateOutline();
    }

    public void addNeuronNodes(Collection<NeuronNode> neuronNodes) {
        this.neuronNodes.addAll(neuronNodes);
        updateOutline();
    }

    public void removeNeuronNode(NeuronNode neuronNode) {
        neuronNodes.remove(neuronNode);
    }

    public void updateOutline() {
        outlinedObjects.updateBound(
                getModel().getMinX(),
                getModel().getMinY(),
                getModel().getWidth(),
                getModel().getHeight()
        );
    }

    protected abstract AbstractNeuronCollection getModel();

    public InteractionBox getInteractionBox() {
        return interactionBox;
    }


    /**
     * Set a custom interaction box.  Subclasses can call this to customize its behavior.
     *
     * @param newBox the newBox to set.
     */
    protected void setInteractionBox(InteractionBox newBox) {
        this.removeChild(interactionBox);
        this.interactionBox = newBox;
        this.addChild(interactionBox);
    }

    public NeuronGroupingOutline getOutlinedObjects() {
        return outlinedObjects;
    }

    public NetworkPanel getNetworkPanel() {
        return networkPanel;
    }

    @Override
    public List<InteractionBox> getInteractionBoxes() {
        return Collections.singletonList(interactionBox);
    }

    @Override
    public void updateConstituentNodes() {
        for (NeuronNode neuronNode : neuronNodes) {
            neuronNode.update();
        }
        if (getNetworkPanel().isRunning()) {
            return;
        }
        updateText();
    }

    public abstract void updateText();

    /**
     * Action for editing the group name.
     */
    protected Action renameAction = new AbstractAction("Rename Neuron Collection...") {
        @Override
        public void actionPerformed(final ActionEvent event) {
            String newName = JOptionPane.showInputDialog("Name:", getModel().getLabel());
            getModel().setLabel(newName);
        }
    };

    public Set<NeuronNode> getNeuronNodes() {
        return neuronNodes;
    }
}
