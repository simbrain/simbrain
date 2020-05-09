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

import org.simbrain.network.events.SynapseGroupEvents;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.SynapseGroupNodeSimple;
import org.simbrain.network.gui.dialogs.connect.SynapsePolarityAndRandomizerPanel;
import org.simbrain.util.StandardDialog;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * PNode representation of a group of synapses connecting one {@link org.simbrain.network.groups.NeuronGroup}
 * to another. Has several modes depending on whether loose synapses should be displayed or not,
 * and whether the connection is recurrent or not.
 *
 * @author Jeff Yoshimi
 * @author Yulin Li
 */
public class SynapseGroupNode extends ScreenElement implements PropertyChangeListener {

    /**
     * Parent network panel.
     */
    protected final NetworkPanel networkPanel;

    /**
     * Reference to represented synapse group
     */
    protected final SynapseGroup synapseGroup;

    /**
     * Reference to the currently used PNode type.
     */
    private Arrow currentNode;

    /**
     * PNode that represents an aggregate of visible "loose" {@link SynapseNode}s.
     * when {@link SynapseGroup#isDisplaySynapses()} is true.
     */
    private SynapseGroupNodeVisible visibleNode;

    /**
     * PNode that represents a single one-directional green arrow from
     * one neuron group to another.
     */
    private SynapseGroupNodeSimple simpleNode = null;

    /**
     * PNode that represents a recurrent arrow from a neuron group to itself.
     */
    private SynapseGroupNodeRecurrent recurrentNode = null;

    /**
     * The interaction box for this neuron group.
     */
    protected SynapseGroupInteractionBox interactionBox;

    /**
     * Create a Synapse Group PNode.
     *
     * @param networkPanel parent panel
     * @param group        the synapse group
     */
    public SynapseGroupNode(NetworkPanel networkPanel, SynapseGroup group) {
        super(networkPanel);
        this.networkPanel = networkPanel;
        this.synapseGroup = group;
        // Note the children pnodes to outlined objects are created in
        // networkpanel and added externally to outlined objects
        interactionBox = new SynapseGroupInteractionBox(networkPanel, group, this);
        interactionBox.setText(synapseGroup.getLabel());
        addChild(interactionBox);

        toggleSynapseVisibility();

        group.getSourceNeuronGroup().getEvents().onLocationChange(this::layoutChildren);
        group.getTargetNeuronGroup().getEvents().onLocationChange(this::layoutChildren);

        // Handle events
        SynapseGroupEvents events = synapseGroup.getEvents();
        events.onDelete(s -> removeFromParent());
        events.onLabelChange((o,n) -> updateText());
        events.onVisibilityChange(this::toggleSynapseVisibility);
        events.onSynapseAdded(s -> {
            SynapseGroupNode.this.getNetworkPanel().add(s);
            refreshVisible();
        });
        events.onSynapseRemoved(s -> {
            SynapseGroupNode.this.getNetworkPanel().add(s);
            refreshVisible();
        });

    }

    private void removeArrows() {
        removeChild(simpleNode);
        removeChild(visibleNode);
        removeChild(recurrentNode);
    }

    private void refreshVisible() {
        removeChild(visibleNode);
        visibleNode = null;
        toggleSynapseVisibility();
    }

    private void initializeArrow() {
        if (synapseGroup.isDisplaySynapses()) {
            if (visibleNode == null) {
                visibleNode = new SynapseGroupNodeVisible(networkPanel, this);
            }
            currentNode = visibleNode;
            return;
        }

        if (getSynapseGroup().isRecurrent()) {
            if (recurrentNode == null) {
                recurrentNode = new SynapseGroupNodeRecurrent(this);
            }
            currentNode = recurrentNode;
        } else {
            if (simpleNode == null) {
                simpleNode = new SynapseGroupNodeSimple(this);
            }
            currentNode = simpleNode;
        }
    }

    public void toggleSynapseVisibility() {
        if (synapseGroup.isDisplaySynapses()) {
            removeArrows();
            if (visibleNode == null) {
                visibleNode = new SynapseGroupNodeVisible(networkPanel, this);
            }
            addChild(visibleNode);
            currentNode = visibleNode;
        } else {
            removeArrows();
            if (synapseGroup.isRecurrent()) {
                if (recurrentNode == null) {
                    recurrentNode = new SynapseGroupNodeRecurrent(this);
                }
                addChild(recurrentNode);
                currentNode = recurrentNode;
            } else {
                if (simpleNode == null) {
                    simpleNode = new SynapseGroupNodeSimple(this);
                }
                addChild(simpleNode);
                currentNode = simpleNode;
            }
        }
        lowerToBottom();
        interactionBox.raiseToTop();
    }

    @Override
    protected void layoutChildren() {
        currentNode.layoutChildren();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // This is needed for synapse groups within subnetworks
        // to be updated properly when neuron groups are moved.
        layoutChildren();
    }

    public SynapseGroupInteractionBox getInteractionBox() {
        return interactionBox;
    }

    public SynapseGroup getSynapseGroup() {
        return synapseGroup;
    }

    /**
     * Update the text in the interaction box.
     */
    public void updateText() {
        interactionBox.setText(synapseGroup.getLabel());
    }

    /**
     * Show randomization dialog
     */
    public void showRandomizationDialog() {
        StandardDialog dialog = new StandardDialog();
        dialog.setContentPane(
            SynapsePolarityAndRandomizerPanel.createPolarityRatioPanel(dialog, this.getSynapseGroup()));
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    @Override
    public boolean isSelectable() {
        return false;
    }

    @Override
    public boolean isDraggable() {
        return false;
    }

    @Override
    public SynapseGroup getModel() {
        return synapseGroup;
    }

    /**
     * Interface for all PNodes used in as the main representation for a synapse group.
     */
    public interface Arrow {
        void layoutChildren();
    }


}
