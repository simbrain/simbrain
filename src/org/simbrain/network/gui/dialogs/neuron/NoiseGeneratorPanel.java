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

import java.awt.Window;
import java.util.List;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.util.randomizer.gui.RandomizerPanel;

/**
 * A panel specifically for the randomizers used by neurons... a higher level
 * version of this should be created if its ideas are to be extended to other
 * objects which require higher level noise generators. Used to give each neuron
 * being edited the <b>same</b> Randomizer object as an optimization.
 *
 * @author Zach Tosi
 *
 */
public class NoiseGeneratorPanel extends RandomizerPanel {

    /**
     * Commits the randomizer for the list of neurons being edited. Every neuron
     * is assigned the same randomizer, that way any time some number of neurons
     * is simultaneously edited, they receive the same randomizer. This prevents
     * each neuron from having to have its own randomizer with exactly the same
     * parameters. However, it also allows different neurons to have different
     * randomizers, since any time any neurons are singled out, they will
     * receive a brand new randomizer.
     *
     * @param noisyNeurons
     *            the neurons
     * @exception ClassCastException
     */
    public void commitRandom(List<Neuron> noiseyNeurons)
        throws ClassCastException {
         if (!getCbDistribution().getSelectedItem().toString()
            .equals(SimbrainConstants.NULL_STRING)) {
            Randomizer rand = new Randomizer();
            super.commitRandom(rand);
            for (Neuron n : noiseyNeurons) {
                ((NoisyUpdateRule) n.getUpdateRule()).setNoiseGenerator(rand);
            }
            //TODO: The above does not work for an inconsistent set of rules.  Any 
            // "..." is ignored.
        }
    }

}
