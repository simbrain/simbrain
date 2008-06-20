/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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

package org.simbrain.network.networks.actorcritic;

import java.util.HashSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.NetworkListener;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.networks.StandardNetwork;
import org.simbrain.network.neurons.LinearNeuron;
import org.simbrain.network.synapses.SimpleSynapse;
import org.simbrain.network.util.SimpleId;

/**
 * <b>ActorCritic</b>. Implements Temporal Difference learning. This network
 * consists of two components - an 'Adaptive Critic' that learns to predict the
 * goodness of various states of the world and an 'Actor' that learns to take
 * actions that lead the network towards rewarding states.
 */
public class ActorCritic extends Network {

    /** Number of state neuron. */
    private int stateUnits = 2;

    /** Number of possible actionsNetwork. */
    private int actorUnits = 2;

    /** Flag indicating whether the network should be trained or not. */
    private boolean train = true;

    /** Flag to indicate if the absorbing reward condition is true or not. */
    private boolean absorbReward = true;

    /** Simbrain representation of stateNetwork. */
    private StandardNetwork stateNetwork = null;

    /** Simbrain representation of actionsNetwork. */
    private StandardNetwork actionsNetwork = null;

    //TODO: Give critic[0] and critic[1] meaningful names and use those names
    /** Simbrain representation of critic (two nodes: anticipated and current reward). */
    private StandardNetwork critic = null;

    /** Buffers to hold the last activation state of the state network. */
    private double[] lastState = null;

    /** Buffers to hold the last activation state of the action network. */
    private double[] lastActions = null;

    /** Buffers to hold the last activation state of the critic network. */
    private double[] lastCritic = null;

    /** Actor learning rate. */
    private double actorLearningRate = 1;

    /** Critic learning rate. */
    private double criticLearningRate = 1;

    /** Reward discount factor. */
    private double gamma = 1;

    /** Exploration policy. */
    private ExplorationPolicy explorationPolicy = new RandomExplorationPolicy();

    /**
     * Default constructor.
     */
    public ActorCritic() {
        super();
    }

    /**
     * Creates a new actor-critic network.
     *
     * @param stateUnits
     *            Number of stateNetwork neurons
     * @param actorUnits
     *            Number of actor neurons
     * @param layout
     *            the way to layout the network
     * @param root
     *            the root
     */
    public ActorCritic(final RootNetwork root, final int stateUnits,
            final int actorUnits, final Layout layout) {
        super();
        this.stateUnits = stateUnits;
        this.actorUnits = actorUnits;
        setRootNetwork(root);
        setParentNetwork(root);
        initVariables();
        createNeurons();
        layout.layoutNeurons(this);
        createConnections();
    }

    /**
     * Perform intialization required after opening saved networks.
     */
    private void initVariables() {
        this.lastState = new double[stateUnits];
        this.lastActions = new double[actorUnits];
        this.lastCritic = new double[2];
        for (int i = 0; i < stateUnits; i++) {
            this.lastState[i] = 0;
        }
        for (int i = 0; i < actorUnits; i++) {
            this.lastActions[i] = 0;
        }
        this.lastCritic[0] = 0;
        this.lastCritic[1] = 0;
    }

    /**
     * Standard method call made to objects after they are deserialized.
     * See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     * 
     * @return Initialized object.
     */
    private Object readResolve() {
        initVariables();
        return this;
    }

    /**
     *  Create neurons.
     */

    private void createNeurons() {
        stateNetwork = new StandardNetwork(this.getRootNetwork());
        actionsNetwork = new StandardNetwork(this.getRootNetwork());
        critic = new StandardNetwork(this.getRootNetwork());
        stateNetwork.setParentNetwork(this);
        actionsNetwork.setParentNetwork(this);
        critic.setParentNetwork(this);

        for (int i = 0; i < this.stateUnits; i++) {
            this.stateNetwork.addNeuron(new LinearNeuron());
        }
        for (int i = 0; i < this.actorUnits; i++) {
            this.actionsNetwork.addNeuron(new LinearNeuron());
        }
        for (int i = 0; i < 2; i++) {
            this.critic.addNeuron(new LinearNeuron());
        }
        addNetwork(stateNetwork);
        addNetwork(actionsNetwork);
        addNetwork(critic);
    }

    /**
     * Create the connetions.
     */
    private void createConnections() {

        // create the connections between states and critic
        for (Neuron s : stateNetwork.getFlatNeuronList()) {
            SimpleSynapse w = new SimpleSynapse(s, critic.getNeuron(0));
            w.setLowerBound(10);
            w.setUpperBound(-10);
            w.setStrength(0);
            this.addSynapse(w);
        }

        // create the connections between states and actionsNetwork
        AllToAll connector = new AllToAll(this, stateNetwork.getFlatNeuronList(),
        actionsNetwork.getFlatNeuronList());
        connector.connectNeurons();

        for (int i = 0; i < getFlatSynapseList().size(); i++) {
            ((Synapse) getFlatSynapseList().get(i)).setUpperBound(10);
            ((Synapse) getFlatSynapseList().get(i)).setLowerBound(-10);
            ((Synapse) getFlatSynapseList().get(i)).randomize();
        }

        for (int i = 0; i < getFlatNeuronList().size(); i++) {
            ((Neuron) getFlatNeuronList().get(i)).setUpperBound(1);
            ((Neuron) getFlatNeuronList().get(i)).setLowerBound(0);
            ((Neuron) getFlatNeuronList().get(i)).setIncrement(1);
        }

    }

    /**
     * Randomize the network.
     */
    public void randomize() {
        if (this.getNetworkList().size() == 0) {
            return;
        }

        for (int i = 0; i < getFlatSynapseList().size(); i++) {
            ((Synapse) getFlatSynapseList().get(i)).randomize();
        }
    }

    /**
     * Update network.
     */
    public void update() {

        // First update all the stateNetwork neurons
        for (int i = 0; i < stateNetwork.getNeuronCount(); i++) {
            this.lastState[i] = stateNetwork.getNeuron(i).getActivation();
            stateNetwork.getNeuron(i).update();
            stateNetwork.getNeuron(i).setActivation(stateNetwork.getNeuron(i).getBuffer());
        }
        // now find the action
        double[] a = new double[actionsNetwork.getNeuronCount()];
        for (int i = 0; i < actionsNetwork.getNeuronCount(); i++) {
            this.lastActions[i] = actionsNetwork.getNeuron(i).getActivation();
            actionsNetwork.getNeuron(i).update();
            a[i] = actionsNetwork.getNeuron(i).getBuffer();
        }
//        this.explorationPolicy.selectAction(a);
        for (int i = 0; i < actionsNetwork.getNeuronCount(); i++) {
            actionsNetwork.getNeuron(i).setActivation(a[i]);
        }
        // now the critic
        for (int i = 0; i < critic.getNeuronCount(); i++) {
            this.lastCritic[i] = critic.getNeuron(i).getActivation();
            critic.getNeuron(i).update();
            critic.getNeuron(i).setActivation(critic.getNeuron(i).getBuffer());
        }
        if (this.train) {
            updateWeights();
        }

    }

    /**
     * Update the network weights.
     */
    private void updateWeights() {
        double delta = this.gamma * this.critic.getNeuron(0).getActivation()
                + this.lastCritic[1] - this.lastCritic[0];

        if (delta < 0) {
            System.out.print("negative delta");
        }
        int i;
        // update critic weights
        for (i = 0; i < this.stateUnits; i++) {
            this.getSynapse(this.stateNetwork.getNeuron(i), this.critic.getNeuron(0)).setStrength(
                    this.getSynapse(this.stateNetwork.getNeuron(i), this.critic.getNeuron(0)).getStrength()
                    + this.criticLearningRate * this.lastState[i] * delta);
            this.getSynapse(i).checkBounds();
        }
        // update actor weights
        for (int k = 0; k < this.stateUnits; k++, i++) {
            for (int j = 0; j < this.actorUnits; j++) {
                this.getSynapse(this.stateNetwork.getNeuron(k), this.actionsNetwork.getNeuron(j)).setStrength(
                        this.getSynapse(this.stateNetwork.getNeuron(k), this.actionsNetwork.getNeuron(j)).getStrength()
                                + this.actorLearningRate * this.lastState[k]
                                * delta * this.lastActions[j]);
            }
            this.getSynapse(i).checkBounds();
        }
    }

    /**
     * Reset the network: resets all activations, absorbs any pending rewards.
     */
    public void reset() {
        for (int i = 0; i < stateNetwork.getNeuronCount(); i++) {
            stateNetwork.getNeuron(i).setInputValue(0);
        }
        /*double[] a = new double[actionsNetwork.getNeuronCount()];
        for (int i = 0; i < actionsNetwork.getNeuronCount(); i++) {
            this.lastActions[i] = actionsNetwork.getNeuron(i).getActivation();
            actionsNetwork.getNeuron(i).setActivation(0);
        }
        for (int i = 0; i < critic.getNeuronCount(); i++) {
            this.lastCritic[i] = critic.getNeuron(i).getActivation();
            critic.getNeuron(i).setActivation(0);
        }
        if (this.train) {
            updateWeights();
        }*/
        critic.getNeuron(1).setInputValue(0);
        update();
    }

    /**
     * Used by duplicate().
     */
    public void duplicateLayers() {
        stateNetwork = (StandardNetwork) this.getNetwork(0);
        actionsNetwork = (StandardNetwork) this.getNetwork(1);
        critic = (StandardNetwork) this.getNetwork(2);
    }

    /**
     * Duplicate the network.
     * @return ac dup. network
     */
    public Network duplicate() {
        ActorCritic ac = new ActorCritic();
        ac = (ActorCritic) super.duplicate(ac);
        ac.setAbsorbReward(this.isAbsorbReward());
        ac.setActorLearningRate(this.getActorLearningRate());
        ac.setCriticLearningRate(this.getCriticLearningRate());
        ac.setExplorationPolicy(this.getExplorationPolicy());
        ac.setRewardDiscountFactor(this.getRewardDiscountFactor());
        ac.setActorUnits(this.getActorUnits());
        ac.setStateUnits(this.getStateUnits());
        ac.duplicateLayers();
        return ac;

    }

    /**
     * get current action.
     * @return return
     */
    public int getCurrentAction() {
        for (int i = 0; i < this.actorUnits; i++) {
            if (this.actionsNetwork.getNeuron(i).getActivation() > 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return the actorLearningRate
     */
    public double getActorLearningRate() {
        return actorLearningRate;
    }

    /**
     * @param actorLearningRate
     *            to set
     */
    public void setActorLearningRate(final double actorLearningRate) {
        this.actorLearningRate = actorLearningRate;
    }

    /**
     * @return the criticLearningRate
     */
    public double getCriticLearningRate() {
        return criticLearningRate;
    }

    /**
     * @param criticLearningRate
     *            to set
     */
    public void setCriticLearningRate(final double criticLearningRate) {
        this.criticLearningRate = criticLearningRate;
    }

    /**
     * @return the reward discount factor
     */
    public double getRewardDiscountFactor() {
        return gamma;
    }

    /**
     * @param rewardDiscountFactor
     *            the reward discount factor to set
     */
    public void setRewardDiscountFactor(final double rewardDiscountFactor) {
        this.gamma = rewardDiscountFactor;
    }

    /**
     * @return the number of stateUnits
     */
    public int getStateUnits() {
        return stateUnits;
    }

    /**
     * @param stateUnits
     *            to set
     */
    public void setStateUnits(final int stateUnits) {
        this.stateUnits = stateUnits;
    }

    /**
     * @return the number of actorUnits
     */
    public int getActorUnits() {
        return actorUnits;
    }

    /**
     * @param actorUnits
     *            to set
     */
    public void setActorUnits(final int actorUnits) {
        this.actorUnits = actorUnits;
    }

    /**
     * @return true if network is being trained
     */
    public boolean isTrain() {
        return train;
    }

    /**
     * @param train
     *            to set
     */
    public void setTrain(final boolean train) {
        this.train = train;
    }

    /**
     * @return true if absorbing reward conditiong is set
     */
    public boolean isAbsorbReward() {
        return absorbReward;
    }

    /**
     * @param absorbReward
     *            to set
     */
    public void setAbsorbReward(final boolean absorbReward) {
        this.absorbReward = absorbReward;
    }

    /**
     *
     * @return ret
     */
    public ExplorationPolicy getExplorationPolicy() {
        return explorationPolicy;
    }

    /**
    *
    * @param explorationPolicy the policy
    */
    public void setExplorationPolicy(final ExplorationPolicy explorationPolicy) {
        this.explorationPolicy = explorationPolicy;
    }

    /**
     * @return the actionsNetwork
     */
    public StandardNetwork getActionsNetwork() {
        return actionsNetwork;
    }

    /**
     * @param actionsNetwork the actionsNetwork to set
     */
    public void setActionsNetwork(final StandardNetwork actionsNetwork) {
        this.actionsNetwork = actionsNetwork;
    }

    /**
     * @return the critic
     */
    public StandardNetwork getCritic() {
        return critic;
    }

    /**
     * @param critic the critic to set
     */
    public void setCritic(final StandardNetwork critic) {
        this.critic = critic;
    }

    /**
     * @return the stateNetwork
     */
    public StandardNetwork getStateNetwork() {
        return stateNetwork;
    }

    /**
     * @param state the stateNetwork to set
     */
    public void setStateNetwork(final StandardNetwork state) {
        this.stateNetwork = state;
    }

}
