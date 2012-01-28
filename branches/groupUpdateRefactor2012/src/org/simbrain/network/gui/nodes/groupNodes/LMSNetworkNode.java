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
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.simbrain.network.groups.subnetworks.LMSNetwork;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.InteractionBox;
import org.simbrain.network.gui.trainer.TrainerGuiActions;
import org.simbrain.network.gui.trainer.TrainerPanel;
import org.simbrain.network.gui.trainer.TrainerPanel.TrainerDataType;
import org.simbrain.network.trainers.LMSIterative;
import org.simbrain.network.trainers.LMSOffline;
import org.simbrain.network.trainers.Trainer;
import org.simbrain.util.ClassDescriptionPair;
import org.simbrain.util.genericframe.GenericFrame;

/**
 * PNode representation of a group of a LMS network
 * 
 * @author jyoshimi
 */
public class LMSNetworkNode extends SubnetGroupNode {

    /**
     * Create a layered network
     *
     * @param networkPanel parent panel
     * @param group the layered network
     */
    public LMSNetworkNode(NetworkPanel networkPanel, LMSNetwork group) {
        super(networkPanel, group);
        setInteractionBox(new LMSInteractionBox(networkPanel));
        setContextMenu();
    }
    
    private Trainer getTrainer() {
        return ((LMSNetwork) getGroup()).getTrainer();
    }
    
    /**
     * Custom interaction box for LMS group node.
     */
    private class LMSInteractionBox extends InteractionBox {
        public LMSInteractionBox(NetworkPanel net) {
            super(net, LMSNetworkNode.this);
        }

//        @Override
//        protected JDialog getPropertyDialog() {
//            TrainerPanel panel = new TrainerPanel(getNetworkPanel(),
//                    getTrainer());
//            JDialog dialog = new JDialog();
//            dialog.setContentPane(panel);
//            return dialog;
//        }
//        
//      @Override
//      protected boolean hasPropertyDialog() {
//          return true;
//      }

        @Override
        protected String getToolTipText() {
            return "LMS...";
        }

        @Override
        protected boolean hasToolTipText() {
            return true;
        }

    };

    /**
     * Sets custom menu.
     */
    private void setContextMenu() {
        JPopupMenu menu = super.getDefaultContextMenu();
        menu.addSeparator();
        Action trainNet = new AbstractAction("Show Training Controls...") {
            public void actionPerformed(final ActionEvent event) {
                ClassDescriptionPair[] rules = {
                        new ClassDescriptionPair(LMSIterative.class, "LMS-Iterative"),
                        new ClassDescriptionPair(LMSOffline.class, "LMS-Offline") };
                TrainerPanel trainerPanel = new TrainerPanel(getNetworkPanel(),
                        getTrainer(), rules);
                GenericFrame frame = getNetworkPanel().displayPanel(
                        trainerPanel, "Trainer");
                trainerPanel.setFrame(frame);
            }
        };
        menu.add(new JMenuItem(trainNet));
        menu.add(TrainerGuiActions.getEditDataAction(getNetworkPanel(), getTrainer(),
                TrainerDataType.Input));
        menu.add(TrainerGuiActions.getEditDataAction(getNetworkPanel(), getTrainer(), 
                TrainerDataType.Trainer));
        menu.add(TrainerGuiActions.getShowPlotAction(getNetworkPanel(), getTrainer()));
        setConextMenu(menu);
    }
    

}
