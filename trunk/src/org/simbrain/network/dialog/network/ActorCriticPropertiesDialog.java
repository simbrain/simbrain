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

package org.simbrain.network.dialog.network;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.simbrain.network.actions.ShowHelpAction;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.networks.actorcritic.ActorCritic;
import org.simnet.networks.actorcritic.ExplorationPolicy;
import org.simnet.networks.actorcritic.NoExplorationPolicy;
import org.simnet.networks.actorcritic.RandomExplorationPolicy;

/**
 * <b>ActorCriticPropertiesDialog</b> is a dialog box for setting the properties of an actor-critic network.
 *
 */
public class ActorCriticPropertiesDialog extends StandardDialog {

    /** Main Panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Actor learning rate field. */
    private JTextField tfActorLearningRate = new JTextField();

    /** Adaptive critic learning rate field. */
    private JTextField tfCriticLearningRate = new JTextField();
    
    /** Reward discount file */
    private JTextField tfGamma = new JTextField();
    
    /** Whether the network shoud be trained or not */
    private JCheckBox chTrain = new JCheckBox();
    
    /** Whether the absorbing reward condition is set or not */
    private JCheckBox chAbsorbReward = new JCheckBox();
    
    /** List of exploration policies */
    private String[] explorationPolicies = {"None", "Random"};
    
    /** Combo box for selecting the exploration policy */
    private JComboBox coExplorationPolicy = new JComboBox(explorationPolicies);
    
    /** The model subnetwork. */
    private ActorCritic actorcritic;

     /** Help Button. */
     private JButton helpButton = new JButton("Help");

     /** Show Help Action. */
     private ShowHelpAction helpAction = new ShowHelpAction();

    /**
     * Default constructor.
     *
     * @param backprop Backprop network being modified.
     */
    public ActorCriticPropertiesDialog(final ActorCritic actorcritic) {
        this.actorcritic = actorcritic;
        setTitle("Set Actor-Critic Properties");
        fillFieldValues();
        this.setLocation(500, 0); //Sets location of network dialog
        helpAction.setTheURL("Network/network/actorcritic.html");

        helpButton.setAction(helpAction);
        this.addButton(helpButton);
        mainPanel.addItem("Actor Learning Rate", tfActorLearningRate);
        mainPanel.addItem("Critic Learning Rate", tfCriticLearningRate);
        mainPanel.addItem("Reward Discount Factor", tfGamma);
        mainPanel.addItem("Train the network", chTrain);
        mainPanel.addItem("Absorb reward condition", chAbsorbReward);
        mainPanel.addItem("Exploration policy", coExplorationPolicy);
        setContentPane(mainPanel);
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
      actorcritic.setActorLearningRate((Double.parseDouble(tfActorLearningRate.getText())));
      actorcritic.setCriticLearningRate((Double.parseDouble(tfCriticLearningRate.getText())));
      actorcritic.setRewardDiscountFactor(((Double.parseDouble(tfGamma.getText()))));
      actorcritic.setTrain(chTrain.isSelected());
      actorcritic.setAbsorbReward(chAbsorbReward.isSelected());
      actorcritic.setExplorationPolicy(getExplorationPolicyFromIndex(coExplorationPolicy.getSelectedIndex()));
      super.closeDialogOk();
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        tfActorLearningRate = new JTextField("" + actorcritic.getActorLearningRate());
        tfCriticLearningRate = new JTextField("" + actorcritic.getCriticLearningRate());
        tfGamma = new JTextField("" + actorcritic.getRewardDiscountFactor());
        chTrain = new JCheckBox("", actorcritic.isTrain());
        chAbsorbReward = new JCheckBox("", actorcritic.isAbsorbReward());
        coExplorationPolicy.setSelectedIndex(getIndexFromExplorationPolicy(actorcritic.getExplorationPolicy()));
    }
    
    private ExplorationPolicy getExplorationPolicyFromIndex(int policy){
	switch(policy){
	case 0:
	    return new NoExplorationPolicy();
	case 1:
	    return new RandomExplorationPolicy();
	default:
	    return new NoExplorationPolicy();
	}
    }
    
    private int getIndexFromExplorationPolicy(ExplorationPolicy policy){
	if(policy instanceof NoExplorationPolicy) return 0;
	if(policy instanceof RandomExplorationPolicy) return 1;
	return 0;
    }

}
