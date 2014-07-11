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
package org.simbrain.network.gui.dialogs.neuron.generator_panels;

import java.util.List;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronPanel;

/**
 * <b>RandomNeuronPanel</b> Currently unimplemented pending decisions about
 * changing random neurons into "input generators".
 */
public class RandomGeneratorPanel extends AbstractNeuronPanel {

    // /** Random pane. */
    // private RandomizerPanel rp = new RandomizerPanel();

    /**
     * Creates an instance of this panel.
     *
     */
    public RandomGeneratorPanel() {
        super();
        // this.add(rp);
    }

    // /**
    // * Populate fields with current data.
    // */
    // public void fillFieldValues() {
    // ArrayList<Randomizer> randomPanels = new ArrayList<Randomizer>();
    //
    // for (int i = 0; i < ruleList.size(); i++) {
    // randomPanels.add(((RandomNeuronRule) ruleList.get(i))
    // .getRandomizer());
    // }
    //
    // rp.fillFieldValues(randomPanels);
    // }

    /**
     * Fill field values to default values for random neuron.
     */
    public void fillDefaultValues() {
        // rp.fillDefaultValues();
    }

    // /**
    // * Called externally when the dialog is closed, to commit any changes
    // made.
    // */
    // public void commitChanges() {
    // for (int i = 0; i < ruleList.size(); i++) {
    // RandomNeuronRule neuronRef = (RandomNeuronRule) ruleList.get(i);
    // rp.commitRandom(neuronRef.getRandomizer());
    // }
    // }

    @Override
    public void commitChanges(Neuron neuron) {
        // TODO Auto-generated method stub

    }

    @Override
    public void commitChanges(List<Neuron> neuron) {
        // TODO Auto-generated method stub

    }

    @Override
    public void fillFieldValues(List<NeuronUpdateRule> ruleList) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NeuronUpdateRule getPrototypeRule() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void writeValuesToRules(List<Neuron> neurons) {
        // TODO Auto-generated method stub

    }
}
