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
package org.simbrain.network.gui.dialogs.synapse.spike_responders;

import java.util.Collections;
import java.util.List;

import javax.swing.JTextField;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.synapse.AbstractSpikeResponsePanel;
import org.simbrain.network.synapse_update_rules.spikeresponders.ProbabilisticResponder;
import org.simbrain.network.synapse_update_rules.spikeresponders.SpikeResponder;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;

/**
 * <b>ProbabilisticSpikeResponderPanel</b>.
 */
public class ProbabilisticSpikeResponderPanel extends
        AbstractSpikeResponsePanel {

    /** Activation Probability. */
    private JTextField tfActivationProbability = new JTextField();

    /** Response value. */
    private JTextField tfResponseValue = new JTextField();

    /** The prototypical probabilistic responder rule. */
    public static final ProbabilisticResponder PROTOTYPE_RESPONDER = new ProbabilisticResponder();

    /**
     * This method is the default constructor.
     *
     */
    public ProbabilisticSpikeResponderPanel() {
        tfActivationProbability.setColumns(6);
        this.addItem("Activation Probability", tfActivationProbability);
        this.addItem("Response value", tfResponseValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillFieldValues(List<SpikeResponder> spikeResponderList) {

        ProbabilisticResponder spikeResponder = (ProbabilisticResponder) spikeResponderList
                .get(0);

        // Handle consistency of multiply selections

        // Handle Activation Probability
        if (!NetworkUtils.isConsistent(spikeResponderList,
                ProbabilisticResponder.class, "getActivationProbability")) {
            tfActivationProbability.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfActivationProbability.setText(Double.toString(spikeResponder
                    .getActivationProbability()));
        }

        // Handle Response Value
        if (!NetworkUtils.isConsistent(spikeResponderList,
                ProbabilisticResponder.class, "getResponseValue")) {
            tfResponseValue.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfResponseValue.setText(Double.toString(spikeResponder
                    .getResponseValue()));
        }

    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
        tfActivationProbability.setText(Double.toString(PROTOTYPE_RESPONDER
                .getActivationProbability()));
        tfResponseValue.setText(Double.toString(PROTOTYPE_RESPONDER
                .getActivationProbability()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(Synapse synapse) {

        if (!(synapse.getSpikeResponder() instanceof ProbabilisticResponder)) {
            synapse.setSpikeResponder(PROTOTYPE_RESPONDER.deepCopy());
        }

        writeValuesToRules(Collections.singletonList(synapse));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(List<Synapse> synapses) {

        if (isReplace()) {
            for (Synapse s : synapses) {
                s.setSpikeResponder(PROTOTYPE_RESPONDER.deepCopy());
            }
        }

        writeValuesToRules(synapses);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeValuesToRules(List<Synapse> synapses) {

        // Activation Probability
        double actProb = Utils.doubleParsable(tfActivationProbability);
        if (!Double.isNaN(actProb)) {
            for (Synapse s : synapses) {
                ((ProbabilisticResponder) s.getSpikeResponder())
                        .setActivationProbability(actProb);
            }
        }

        // Response Value
        double responseValue = Utils.doubleParsable(tfResponseValue);
        if (!Double.isNaN(responseValue)) {
            for (Synapse s : synapses) {
                ((ProbabilisticResponder) s.getSpikeResponder())
                        .setResponseValue(responseValue);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProbabilisticResponder getPrototypeResponder() {
        return PROTOTYPE_RESPONDER;
    }

}