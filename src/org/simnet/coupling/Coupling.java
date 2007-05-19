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
package org.simnet.coupling;

import org.simnet.interfaces.Neuron;
import org.simbrain.world.Agent;
import org.simbrain.world.World;

/**
 * <b>Coupling</b> represents a relation between an agent and an
 * input or output node.
 */
public class Coupling {

    // Used by Castor
    /** Agent name for this coupling. */
    private String agentName;

    /** World name for this coupling. */
    private String worldName;

    /** World type for this coupling. */
    private String worldType;

    /** Neuron name for this coupling. */
    private String neuronName;

    /** Network name for this coupling. */
    private String networkName;

    // References to coupled objects
    /** Agent for this coupling. */
    private Agent agent;

    /** Neuron for this coupling. */
    private Neuron neuron;


    /**
     * Create a new coupling.
     */
    public Coupling() {
        initCastor();
    }

    /**
     * Create a new coupling with the specified agent and neuron.
     *
     * @param a agent for this coupling
     * @param n neuron for this coupling
     */
    public Coupling(final Agent a, final Neuron n) {
        setAgent(a);
        setNeuron(n);
        initCastor();
    }

    /**
     * Create a new coupling with the specified neuron.
     *
     * @param n neuron for this coupling
     */
    public Coupling(final Neuron n) {
        setNeuron(n);
        initCastor();
    }

    /**
     * Create a new coupling with the specified agent.
     *
     * @param a agent for this coupling
     */
    public Coupling(final Agent a) {
        setAgent(a);
        initCastor();
    }


    /**
     * Return the agent for this coupling.
     *
     * @return the agent for this coupling
     */
    public Agent getAgent() {
        return agent;
    }

    /**
     * Return the world for this coupling, that is the parent world
     * of this coupling's agent, or <code>null</code> if this coupling's
     * agent is <code>null</code>.
     *
     * @return the world for this coupling, or <code>null</code>
     *    if this coupling's agent is <code>null</code>
     */
    public World getWorld() {
        if (agent == null) {
            return null;
        }

        return agent.getParentWorld();
    }

    /**
     * Set the agent for this coupling to <code>agent</code>.
     *
     * @param agent the agent for this coupling
     */
    public void setAgent(final Agent agent) {
        this.agent = agent;
    }

    /**
     * Initialize Castor support for this coupling.
     */
    public void initCastor() {

        // TODO: net_refactor check later

        //if (neuron.getParentPanel() == null) {
         //   return;
        // }

        if (agent == null) {
            return;
        }

        setAgentName(agent.getName());

        if (agent.getParentWorld() == null) {
            return;
        }

        setWorldName(agent.getParentWorld().getWorldName());
        setWorldType(agent.getParentWorld().getType());
    }

    /**
     * Return the agent name for this coupling.
     *
     * @return the agent name for this coupling
     */
    public String getAgentName() {
        return agentName;
    }

    /**
     * Set the agent name for this coupling to <code>agentName</code>.
     *
     * @param agentName agent name for this coupling
     */
    public void setAgentName(final String agentName) {
        this.agentName = agentName;
    }

    /**
     * Return the world name for this coupling.
     *
     * @return the world name for this coupling
     */
    public String getWorldName() {
        return worldName;
    }

    /**
     * Set the world name for this coupling to <code>worldName</code>.
     *
     * @param worldName world name for this coupling
     */
    public void setWorldName(final String worldName) {
        this.worldName = worldName;
    }

    /**
     * Return the world type for this coupling.
     *
     * @return the world type for this coupling
     */
    public String getWorldType() {
        return worldType;
    }

    /**
     * Set the world type for this coupling to <code>worldType</code>.
     *
     * @param worldType world type for this coupling
     */
    public void setWorldType(final String worldType) {
        this.worldType = worldType;
    }

    /**
     * Return the neuron for this coupling.
     *
     * @return the neuron for this coupling
     */
    public Neuron getNeuron() {
        return neuron;
    }

    /**
     * Set the neuron for this coupling to <code>neuron</code>.
     *
     * @param neuron neuron for this coupling
     */
    public void setNeuron(final Neuron neuron) {
        this.neuron = neuron;
    }

    /**
     * Return the neuron name for this coupling.
     *
     * @return the neuron name for this coupling
     */
    public String getNeuronName() {
        return neuronName;
    }

    /**
     * Set the neuron name for this coupling to <code>neuronName</code>.
     *
     * @param neuronName neuron name for this coupling
     */
    public void setNeuronName(final String neuronName) {
        this.neuronName = neuronName;
    }

    /**
     * Print debug information to <code>System.out</code>.
     * @return String
     */
    public String toString() {

        String ret = new String();

        if (getNeuron() == null) {
           ret += "\t PNode Neuron: null";
        } else {
            // TODO: net_refactor check later
            //System.out.println("\t PNode Neuron: " + getNeuron().getId());
            //System.out.println("\t Network: " + getNeuron().getParentPanel().getName());
        }

        if (getAgent() == null) {
            ret += "\t Agent: null";
            ret += "\t World: " + worldName;
            ret += "\t Type: " + worldType;
        } else {
            ret += "\t Agent: " + getAgent().getName();
            ret += "\t World: " + getAgent().getParentWorld().getName();
            ret +=  "\t Type: " + getAgent().getParentWorld().getType();
        }
        return ret;
    }

    /**
     * Return the network name for this coupling.
     *
     * @return the network name for this coupling
     */
    public String getNetworkName() {
        return networkName;
    }

    /**
     * Set the network name for this coupling to <code>networkName</code>.
     *
     * @param networkName network name for this coupling
     */
    public void setNetworkName(final String networkName) {
        this.networkName = networkName;
    }

    /**
     * Return <code>true</code> if this coupling has an associated agent,
     * that is if <code>getAgent() != null</code>.
     *
     * @return <code>true</code> if this coupling has an associated agent
     */
    public boolean isAttached() {
        return (getAgent() != null);
    }
}
