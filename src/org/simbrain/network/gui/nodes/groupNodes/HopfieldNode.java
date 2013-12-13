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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.HopfieldPropertiesPanel;
import org.simbrain.network.subnetworks.Hopfield;
import org.simbrain.util.ShowHelpAction;
import org.simbrain.util.StandardDialog;

/**
 * PNode representation of Hopfield Network.
 *
 * TODO: - Location of top interaction box - Dialog
 *
 * @author jyoshimi
 */
public class HopfieldNode extends SubnetworkNode {

    /**
     * Create a Hopfield Network PNode.
     *
     * @param networkPanel parent panel
     * @param group the Hopfield network
     */
    public HopfieldNode(NetworkPanel networkPanel, Hopfield group) {
        super(networkPanel, group);
        // setStrokePaint(Color.green);
        setContextMenu();
        // setOutlinePadding(15f);
    }

    /**
     * Sets custom menu for Hopfield node.
     */
    private void setContextMenu() {
        JPopupMenu menu = super.getDefaultContextMenu();
        menu.addSeparator();
        Action editNet = new AbstractAction(
                "Set Hopfield Network Properties...") {
            public void actionPerformed(final ActionEvent event) {
                final HopfieldPropertiesPanel panel = new HopfieldPropertiesPanel(
                        getNetworkPanel(), (Hopfield) getGroup());
                StandardDialog dialog = new StandardDialog() {
                    @Override
                    protected void closeDialogOk() {
                        super.closeDialogOk();
                        panel.commitChanges();
                    }
                };
                dialog.setContentPane(panel);
                Action helpAction = new ShowHelpAction(panel.getHelpPath());
                dialog.addButton(new JButton(helpAction));
                dialog.setLocationRelativeTo(null);
                dialog.pack();
                dialog.setVisible(true);
            }
        };
        menu.add(new JMenuItem(editNet));
        Action trainNet = new AbstractAction("Train Hopfield Network...") {
            public void actionPerformed(final ActionEvent event) {
                ((Hopfield) getGroup()).train();
            }
        };
        menu.add(new JMenuItem(trainNet));
        Action randWeights = new AbstractAction("Randomize Weights...") {
            public void actionPerformed(final ActionEvent event) {
                ((Hopfield) getGroup()).randomizeWeights();
            }
        };
        menu.add(new JMenuItem(randWeights));
        setContextMenu(menu);
    }

}
