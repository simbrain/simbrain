/*
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
package org.simbrain.network.groups;

import org.jetbrains.annotations.NotNull;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.layouts.Layout;
import org.simbrain.util.propertyeditor.CopyableObject;

import java.util.Collection;
import java.util.List;

/**
 * A collection of free neurons (neurons in a {@link NeuronGroup} can be added to a collection). Allows them to be
 * labelled, moved around as a unit, coupled to, etc. However no special processing occurs in neuron collections. They
 * are a convenience. NeuronCollections can overlap each other in the sense of having neurons in common.
 */
public class NeuronCollection extends AbstractNeuronCollection {

    /**
     * Construct a new neuron group from a list of free neurons.
     *
     * Assumes the neurons have already been added to the root network.
     */
    public NeuronCollection(final Network net, final List<Neuron> neurons) {
        super(net);
        addNeurons(neurons);

        neurons.forEach(n -> {
            n.getEvents().getLocationChanged().on(() -> events.getLocationChanged());
            n.getEvents().getActivationChanged().on((aold, anew) -> {
                invalidateCachedActivations();
            });
            n.getEvents().getDeleted().on(null, true, toDelete -> {
                removeNeuron((Neuron) toDelete);
                events.getLocationChanged().fireAndForget();
                if (isEmpty()) {
                    delete();
                }
            });
        });
    }

    /**
     * Translate all neurons (the only objects with position information).
     *
     * @param offsetX x offset for translation.
     * @param offsetY y offset for translation.
     */
    public void offset(final double offsetX, final double offsetY) {
        for (Neuron neuron : getNeuronList()) {
            neuron.offset(offsetX, offsetY, false);
        }
        events.getLocationChanged().fireAndForget();
    }

    /**
     * Call after deleting neuron collection from parent network.
     */
    public void delete() {
        events.getDeleted().fireAndForget(this);
    }

    /**
     * Set the update rule for the neurons in this group.
     *
     * @param base the neuron update rule to set.
     */
    public void setNeuronType(NeuronUpdateRule base) {
        getNeuronList().forEach(n -> n.setUpdateRule(base.deepCopy()));
    }

    /**
     * Set the string update rule for the neurons in this group.
     *
     * @param rule the neuron update rule to set.
     */
    public void setNeuronType(String rule) {
        try {
            NeuronUpdateRule newRule =
                    (NeuronUpdateRule) Class.forName("org.simbrain.network.neuron_update_rules." + rule).newInstance();
            setNeuronType(newRule);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the summed hash codes of contained neurons.  Used to prevent creation of neuron collections from
     * identical sets of neurons.
     *
     * @return summed hash
     */
    public int getSummedNeuronHash() {
        return getNeuronList().stream().mapToInt(Object::hashCode).sum();
    }

    @Override
    public boolean shouldAdd() {
        int hashCode = getSummedNeuronHash();
        for (NeuronCollection other : getNetwork().getModels(NeuronCollection.class)) {
            if (hashCode == other.getSummedNeuronHash()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clear() {
        for (Neuron n : neuronList) {
            n.clear();
        }
    }

    @Override
    public void addNeuron(Neuron neuron) {
        // These neurons already have ids and listeners
        neuronList.add(neuron);
        addListener(neuron);
    }

    @Override
    public void addNeurons(Collection<Neuron> neurons) {
        super.addNeurons(neurons);
    }

    @Override
    public void postOpenInit() {
        super.postOpenInit();
        getNeuronList().forEach(this::addListener);
    }

    /**
     * Convenience method for applying a {@link Layout} to a neuron collection.
     */
    public void layout(Layout layout) {
        layout.layoutNeurons(neuronList);
    }

    @NotNull
    @Override
    public CopyableObject copy() {
        return new NeuronCollection(getNetwork(), getNeuronList());
    }
}
