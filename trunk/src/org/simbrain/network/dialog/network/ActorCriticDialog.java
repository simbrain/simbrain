/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/Documentation/docs/SimbrainDocs.html#Credits
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

package org.simbrain.network.dialog.network;

import javax.swing.JTextField;

import org.simbrain.network.NetworkPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.layouts.MultipathLayout;
import org.simnet.layouts.LayersLayout;
import org.simnet.networks.actorcritic.ActorCritic;;

/**
 * <b>ActorCriticDialog</b> is a dialog box for creating actor-critic networks.
 */
public class ActorCriticDialog extends StandardDialog {

    /** Main panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Number of state units. */
    private JTextField numberOfStateUnits = new JTextField();

    /** Number of actor units. */
    private JTextField numberOfActorUnits = new JTextField();

    /** Reference to network panel. */
    private NetworkPanel networkPanel;

    /**
     * Default constructor.
     *
     * @param np Network panel.
     */
    public ActorCriticDialog(final NetworkPanel np) {
        init();
        networkPanel = np;
    }

    /**
     * This method initialises the components on the panel.
     */
    private void init() {
        //Initialize Dialog
        setTitle("New Actor-Critic Network");

        fillFieldValues();

        numberOfStateUnits.setColumns(3);

        //Set up grapics panel
        mainPanel.addItem("Number of State Units", numberOfStateUnits);
        mainPanel.addItem("Number of Actor Units", numberOfActorUnits);

        setContentPane(mainPanel);
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
	MultipathLayout layout = new MultipathLayout(40, 80, 2, MultipathLayout.HORIZONTAL);
	layout.setInitialLocation(networkPanel.getLastClickedPosition());
	int state = Integer.parseInt(numberOfStateUnits.getText());
	int actions = Integer.parseInt(numberOfActorUnits.getText());
	ActorCritic ac = new ActorCritic(networkPanel.getRootNetwork(), state, actions, layout);
	networkPanel.getRootNetwork().addNetwork(ac);
	networkPanel.repaint();
	super.closeDialogOk();
    }

    /**
     * Populate fields with current data.
     */
    private void fillFieldValues() {
        ActorCritic ac = new ActorCritic();
        numberOfStateUnits.setText(Integer.toString(ac.getStateUnits()));
        numberOfActorUnits.setText(Integer.toString(ac.getActorUnits()));
    }

}
