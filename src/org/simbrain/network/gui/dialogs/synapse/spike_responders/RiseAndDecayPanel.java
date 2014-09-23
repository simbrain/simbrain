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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JTextField;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.synapse.AbstractSpikeResponsePanel;
import org.simbrain.network.synapse_update_rules.spikeresponders.RiseAndDecay;
import org.simbrain.network.synapse_update_rules.spikeresponders.SpikeResponder;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;

/**
 * <b>RiseAndDecayPanel</b>.
 */
public class RiseAndDecayPanel extends AbstractSpikeResponsePanel {

    /** Maximum response field. */
    private JTextField tfMaximumResponse = new JTextField();

    /** Decay rate field. */
    private JTextField tfTimeConstant = new JTextField();

    /**
     * The prototypical rise and decay spike responder. Used for default values
     * and when references are required by other gui classes to aspects of the
     * rise and decay model class.
     */
    public static final RiseAndDecay PROTOTYPE_RESPONDER = new RiseAndDecay();

    /**
     * This method is the default constructor.
     */
    public RiseAndDecayPanel() {
        tfMaximumResponse.setColumns(6);
        this.addItem("Maximum response", tfMaximumResponse);
        this.addItem("Time Constant", tfTimeConstant);
    }
    
    /**
     * {@inheritDoc}
     */
    public RiseAndDecayPanel deepCopy() {
    	RiseAndDecayPanel cpy = new RiseAndDecayPanel();
    	cpy.tfMaximumResponse.setText(this.tfMaximumResponse.getText());
    	cpy.tfTimeConstant.setText(this.tfTimeConstant.getText());
    	return cpy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillDefaultValues() {
        tfMaximumResponse.setText(Double.toString(PROTOTYPE_RESPONDER
                .getMaximumResponse()));
        tfTimeConstant.setText(Double.toString(PROTOTYPE_RESPONDER
                .getTimeConstant()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillFieldValues(List<SpikeResponder> spikeResponderList) {

        RiseAndDecay spikeResponder = (RiseAndDecay) spikeResponderList.get(0);

        // Handle consistency of multiply selections

        // Handle Maximum Response
        if (!NetworkUtils.isConsistent(spikeResponderList, RiseAndDecay.class,
                "getMaximumResponse")) {
            tfMaximumResponse.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfMaximumResponse.setText(Double.toString(spikeResponder
                    .getMaximumResponse()));
        }

        // Handle Decay Rate
        if (!NetworkUtils.isConsistent(spikeResponderList, RiseAndDecay.class,
                "getTimeConstant")) {
            tfTimeConstant.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfTimeConstant.setText(Double.toString(spikeResponder
                    .getTimeConstant()));

        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(Synapse synapse) {

        if (!(synapse.getSpikeResponder() instanceof RiseAndDecay)) {
            synapse.setSpikeResponder(PROTOTYPE_RESPONDER.deepCopy());
        }

        writeValuesToRules(Collections.singletonList(synapse));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(Collection<Synapse> synapses) {

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
    protected void writeValuesToRules(Collection<Synapse> synapses) {

        // Max Response
        double maxResponse = Utils.doubleParsable(tfMaximumResponse);
        if (!Double.isNaN(maxResponse)) {
            for (Synapse s : synapses) {
                ((RiseAndDecay) s.getSpikeResponder())
                        .setMaximumResponse(maxResponse);
            }
        }

        // Time Constant
        double timeConstant = Utils.doubleParsable(tfTimeConstant);
        if (!Double.isNaN(timeConstant)) {
            for (Synapse s : synapses) {
                ((RiseAndDecay) s.getSpikeResponder())
                        .setTimeConstant(timeConstant);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RiseAndDecay getPrototypeResponder() {
        return PROTOTYPE_RESPONDER;
    }

}