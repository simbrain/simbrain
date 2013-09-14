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
package org.simbrain.network.gui.nodes.groupNodes;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.WeightMatrixViewer;
import org.simbrain.network.gui.dialogs.SynapseAdjustmentPanel;
import org.simbrain.network.gui.nodes.GroupNode;
import org.simbrain.network.gui.nodes.InteractionBox;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.gui.nodes.SynapseNode;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.propertyeditor.ReflectivePropertyEditor;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * PNode representation of a group of synapses.
 *
 * @author jyoshimi
 */
public class SynapseGroupNode extends GroupNode {

    /** Reference to represented group node. */
    private final SynapseGroup group;

    /**
     * Create a Synapse Group PNode.
     *
     * @param networkPanel parent panel
     * @param group the synapse group
     */
    public SynapseGroupNode(NetworkPanel networkPanel, SynapseGroup group) {
        super(networkPanel, group);
        this.group = group;
        setStroke(null); // Comment this out to see outline
        // getInteractionBox().setPaint(Color.white);
        // setOutlinePadding(-30);
        setPickable(false);
        setContextMenu();
    }

    @Override
    public void updateBounds() {
        PBounds bounds = new PBounds();
        if (getOutlinedObjects().size() > 0) {
            for (PNode node : getOutlinedObjects()) {
                PBounds childBounds = node.getGlobalBounds();
                bounds.add(childBounds);
                if (node instanceof SynapseNode) {
                    // Recurrent synapses screw things up when they have area 0
                    Rectangle synapseBounds = ((SynapseNode) node).getLine()
                            .getBounds();
                    double area = synapseBounds.getHeight()
                            * synapseBounds.getWidth();
                    if (area > 0) {
                        bounds.add(((SynapseNode) node).getLine().getBounds());
                    }
                }
            }

            double inset = getOutlinePadding();
            bounds.setRect(bounds.getX() - inset, bounds.getY() - inset,
                    bounds.getWidth() + (2 * inset), bounds.getHeight()
                            + (2 * inset));

            // Can also use setPathToEllipse
            setPathToRectangle((float) bounds.getX(), (float) bounds.getY(),
                    (float) bounds.getWidth(), (float) bounds.getHeight());

        } else {
            // TODO Need to get reference to parent nodes.
            System.err.println("Bounds are null");
            bounds = null;
        }

        updateInteractionBox();
    }

    /**
     * Sets custom menu for SynapseGroup nodes. To add new menu items to context
     * menu add them here.
     */
    protected void setContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        // Edit
        final ReflectivePropertyEditor editor = new ReflectivePropertyEditor();
        editor.setUseSuperclass(false);
        editor.setObject(getGroup());
        // Only add edit properties action if there are properties to edit
        if (editor.getFieldCount() > 0) {
            Action editGroup = new AbstractAction("Edit...") {
                public void actionPerformed(final ActionEvent event) {
                    JDialog dialog = editor.getDialog();
                    dialog.setLocationRelativeTo(null);
                    dialog.pack();
                    dialog.setVisible(true);
                }
            };
            menu.add(editGroup);
        }
        menu.add(removeGroupAction);

        // Weight adjustment stuff
        menu.addSeparator();
        Action adjustSynapses = new AbstractAction("Adjust Synapses...") {
            public void actionPerformed(final ActionEvent event) {
                selectSynapses();
                final SynapseAdjustmentPanel synapsePanel = new SynapseAdjustmentPanel(
                        getNetworkPanel(), group.getSynapseList());
                getNetworkPanel().displayPanel(synapsePanel,
                        "Adjust selected synapses");
            }
        };
        menu.add(adjustSynapses);
        ((SynapseGroup) this.getGroup()).getSynapseList();
        menu.add(new JMenuItem(showWeightMatrixAction));

        // Selection stuff
        menu.addSeparator();
        Action selectSynapses = new AbstractAction("Select Synapses") {
            public void actionPerformed(final ActionEvent event) {
                selectSynapses();
            }
        };
        menu.add(selectSynapses);
        Action selectIncomingNodes = new AbstractAction(
                "Select incoming neurons") {
            public void actionPerformed(final ActionEvent event) {
                List<NeuronNode> incomingNodes = new ArrayList<NeuronNode>();
                for (Neuron neuron : group.getSourceNeurons()) {
                    incomingNodes.add((NeuronNode) getNetworkPanel()
                            .getObjectNodeMap().get(neuron));

                }
                getNetworkPanel().clearSelection();
                getNetworkPanel().setSelection(incomingNodes);
            }
        };
        menu.add(selectIncomingNodes);
        Action selectOutgoingNodes = new AbstractAction(
                "Select outgoing neurons") {
            public void actionPerformed(final ActionEvent event) {
                List<NeuronNode> outgoingNodes = new ArrayList<NeuronNode>();
                for (Neuron neuron : group.getTargetNeurons()) {
                    outgoingNodes.add((NeuronNode) getNetworkPanel()
                            .getObjectNodeMap().get(neuron));

                }
                getNetworkPanel().clearSelection();
                getNetworkPanel().setSelection(outgoingNodes);
            }
        };
        menu.add(selectOutgoingNodes);

        // TODO: Add coupling stuff at higher desktop level...

        // Add the menu...
        setContextMenu(menu);
    }

    /**
     * Select the synapses in this group.
     */
    private void selectSynapses() {
        List<SynapseNode> nodes = new ArrayList<SynapseNode>();
        for (Synapse synapse : group.getSynapseList()) {
            nodes.add((SynapseNode) getNetworkPanel().getObjectNodeMap().get(
                    synapse));

        }
        getNetworkPanel().clearSelection();
        getNetworkPanel().setSelection(nodes);
    }

    /**
     * Action for showing the weight matrix for this neuron group.
     */
    Action showWeightMatrixAction = new AbstractAction() {

        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("grid.png"));
            putValue(NAME, "Show weight matrix");
            putValue(SHORT_DESCRIPTION, "Show weight matrix");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            List<Neuron> sourceNeurons = ((SynapseGroup) SynapseGroupNode.this
                    .getGroup()).getSourceNeurons();
            List<Neuron> targetNeurons = ((SynapseGroup) SynapseGroupNode.this
                    .getGroup()).getTargetNeurons();
            JPanel panel = WeightMatrixViewer
                    .getWeightMatrixPanel(new WeightMatrixViewer(sourceNeurons,
                            targetNeurons, SynapseGroupNode.this
                                    .getNetworkPanel()));
            getNetworkPanel().displayPanel(panel, "Edit weights");
        }
    };

    @Override
    protected void updateInteractionBox() {
        InteractionBox interactionBox = getInteractionBox();
        interactionBox.setOffset(
                this.getBounds().getCenterX() - interactionBox.getWidth() / 2,
                this.getBounds().getCenterY() - interactionBox.getHeight() / 2);
    }

    /**
     * Returns the SynapseGroup to this SynapseGroupNode.
     *
     * @return the synapse group
     */
    public SynapseGroup getSynapseGroup() {
        return (SynapseGroup) getGroup();
    }

}
