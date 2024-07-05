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
package org.simbrain.network.gui.nodes.subnetworkNodes;

import org.jetbrains.annotations.Nullable;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.ScreenElementActions;
import org.simbrain.network.gui.nodes.SubnetworkNode;
import org.simbrain.network.subnetworks.Hopfield;
import org.simbrain.util.StandardDialog;
import org.simbrain.workspace.gui.SimbrainDesktop;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

/**
 * PNode representation of Hopfield Network.
 *
 * @author jyoshimi
 */
public class HopfieldNode extends SubnetworkNode {

    /**
     * Reference to hopfield node being edited
     */
    private final Hopfield hopfield;

    /**
     * Create a Hopfield Network PNode.
     *
     * @param networkPanel parent panel
     * @param group        the Hopfield network
     */
    public HopfieldNode(NetworkPanel networkPanel, Hopfield group) {
        super(networkPanel, group);
        hopfield = group;
        // setStrokePaint(Color.green);
        // setOutlinePadding(15f);
    }

    @Override
    public StandardDialog getPropertyDialog() {
        return null;
        // return new HopfieldEditTrainDialog(getNetworkPanel(), (Hopfield) getSubnetwork());
    }

    @Nullable
    @Override
    public JPopupMenu getContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(createEditAction("Edit / Train Hopfield..."));
        applyDefaultActions(menu);
        menu.addSeparator();
        // menu.add(addInputRowAction);
        Action trainNet = new AbstractAction("Train on current pattern") {
            public void actionPerformed(final ActionEvent event) {
                hopfield.trainOnCurrentPattern();
            }
        };
        menu.add(trainNet);
        menu.addSeparator();
        Action randomizeNet = ScreenElementActions.createRandomNetAction(this);
        menu.add(randomizeNet);
        Action clearWeights = new AbstractAction("Set weights to zero") {
            public void actionPerformed(final ActionEvent event) {
                hopfield.getSynapseGroup().clear();
            }
        };
        menu.add(clearWeights);
        menu.addSeparator();

        var sd =  SimbrainDesktop.INSTANCE;
        Action couplingProjection =
                sd.getActionManager().createCoupledProjectionPlotAction(
                        sd.getWorkspace().getCouplingManager().getProducer(
                                hopfield.getNeuronGroup(), "getActivations"
                        ),
                        Objects.requireNonNull(hopfield.getId()),
                        "Projection");
        menu.add(couplingProjection);
        Action imageInput =
                sd.getActionManager().createImageInput(
                        sd.getWorkspace().getCouplingManager().getConsumer(
                                hopfield.getNeuronGroup(), "addInputs"
                        ),
                        hopfield.getNeuronGroup().getNeuronList().size());
        menu.add(imageInput);

        return menu;
    }

}
