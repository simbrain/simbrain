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
package org.simbrain.network.synapse_update_rules;

import org.simbrain.network.core.Connector;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.gui.dialogs.NetworkPreferences;
import org.simbrain.network.matrix.NeuronArray;
import org.simbrain.network.matrix.WeightMatrix;
import org.simbrain.network.util.EmptyMatrixData;
import org.simbrain.network.util.EmptyScalarData;
import org.simbrain.util.UserParameter;
import smile.math.matrix.Matrix;

/**
 * <b>Hebbian</b> implements a standard Hebbian learning rule.
 */
public class HebbianRule extends SynapseUpdateRule<EmptyScalarData, EmptyMatrixData> {

    @UserParameter(label = "Learning rate", description = "Learning rate for Hebb rule",
        preferenceKey = "hebbLearningRate",  increment = .1,  order = 1)
    private double learningRate = NetworkPreferences.INSTANCE.getDefaultLearningRate();

    @Override
    public void init(Synapse synapse) {
    }

    @Override
    public String getName() {
        return "Hebbian";
    }

    @Override
    public SynapseUpdateRule deepCopy() {
        HebbianRule h = new HebbianRule();
        h.setLearningRate(getLearningRate());
        return h;
    }

    @Override
    public void apply(Connector connector, EmptyMatrixData data) {
        if (connector instanceof  WeightMatrix) {
            Matrix wm = ((WeightMatrix)connector).getWeightMatrix();
            Matrix input = ((NeuronArray)connector.getSource()).getActivations();
            Matrix output = ((NeuronArray)connector.getTarget()).getActivations();
            // delta = rate * (input * output^T)
            wm.add(output.mt(input).mul(learningRate));
        }
    }

    @Override
    public void apply(Synapse synapse, EmptyScalarData data) {
        double input = synapse.getSource().getActivation();
        double output = synapse.getTarget().getActivation();
        double strength = synapse.clip(synapse.getStrength() + (learningRate * input * output));
        synapse.setStrength(strength);
    }

    public double getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(final double rate) {
        this.learningRate = rate;
    }

}
