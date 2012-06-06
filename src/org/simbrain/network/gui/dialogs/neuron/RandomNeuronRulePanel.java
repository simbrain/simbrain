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
package org.simbrain.network.gui.dialogs.neuron;

import java.util.ArrayList;

import org.simbrain.network.core.Network;
import org.simbrain.network.gui.dialogs.RandomPanel;
import org.simbrain.network.neuron_update_rules.RandomNeuronRule;
import org.simbrain.network.util.RandomSource;

/**
 * <b>RandomNeuronPanel</b>.
 */
public class RandomNeuronRulePanel extends AbstractNeuronPanel {

    /** Random pane. */
    private RandomPanel rp = new RandomPanel(false);

    /**
     * Creates an instance of this panel.
     *
     */
    public RandomNeuronRulePanel(Network network) {
        super(network);
        this.add(rp);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        ArrayList<RandomSource> randomPanels = new ArrayList<RandomSource>();

        for (int i = 0; i < ruleList.size(); i++) {
            randomPanels.add(((RandomNeuronRule) ruleList.get(i))
                    .getRandomizer());
        }

        rp.fillFieldValues(randomPanels);
    }

    /**
     * Fill field values to default values for random neuron.
     */
    public void fillDefaultValues() {
        rp.fillDefaultValues();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < ruleList.size(); i++) {
            RandomNeuronRule neuronRef = (RandomNeuronRule) ruleList.get(i);
            rp.commitRandom(neuronRef.getRandomizer());
        }
    }
}
