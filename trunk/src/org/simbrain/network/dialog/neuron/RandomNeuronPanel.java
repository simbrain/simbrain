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
package org.simbrain.network.dialog.neuron;

import java.util.ArrayList;

import org.simbrain.network.dialog.RandomPanel;
import org.simnet.neurons.RandomNeuron;


/**
 * <b>RandomNeuronPanel</b>
 */
public class RandomNeuronPanel extends AbstractNeuronPanel {
    RandomPanel rp = new RandomPanel(false);

    public RandomNeuronPanel() {
        this.add(rp);
    }

    /**
     * Populate fields with current data
     */
    public void fillFieldValues() {
        ArrayList randomPanels = new ArrayList();

        for (int i = 0; i < neuron_list.size(); i++) {
            randomPanels.add(((RandomNeuron) neuron_list.get(i)).getRandomizer());
        }

        rp.fillFieldValues(randomPanels);
    }

    /**
     * Fill field values to default values for random neuron
     */
    public void fillDefaultValues() {
        rp.fillDefaultValues();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made
     */
    public void commitChanges() {
        for (int i = 0; i < neuron_list.size(); i++) {
            RandomNeuron neuron_ref = (RandomNeuron) neuron_list.get(i);
            rp.commitRandom(neuron_ref.getRandomizer());
        }
    }
}
