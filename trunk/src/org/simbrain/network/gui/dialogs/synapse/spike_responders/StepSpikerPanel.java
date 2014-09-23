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
import org.simbrain.network.synapse_update_rules.spikeresponders.SpikeResponder;
import org.simbrain.network.synapse_update_rules.spikeresponders.Step;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;

/**
 * <b>StepSpikerPanel</b>.
 */
public class StepSpikerPanel extends AbstractSpikeResponsePanel {

    /** Response height field. */
    private JTextField tfResponseHeight = new JTextField();

    /** Response time field. */
    private JTextField tfResponseDuration = new JTextField();

    /**
     * The prototypical rise and decay spike responder. Used for default values
     * and when references are required by other gui classes to aspects of the
     * rise and decay model class.
     */
    public static final Step PROTOTYPE_RESPONDER = new Step();

    /**
     * This method is the default constructor.
     *
     */
    public StepSpikerPanel() {
        tfResponseHeight.setColumns(6);
        this.addItem("Response height", tfResponseHeight);
        this.addItem("Response time", tfResponseDuration);
    }

    /**
     * {@inheritDoc}
     */
    public StepSpikerPanel deepCopy() {
    	StepSpikerPanel cpy = new StepSpikerPanel();
    	cpy.tfResponseDuration.setText(this.tfResponseDuration.getText());
    	cpy.tfResponseHeight.setText(this.tfResponseHeight.getText());
    	return cpy;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void fillDefaultValues() {
        tfResponseHeight.setText(Double.toString(PROTOTYPE_RESPONDER
                .getResponseHeight()));
        tfResponseDuration.setText(Double.toString(PROTOTYPE_RESPONDER
                .getResponseDuration()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillFieldValues(List<SpikeResponder> spikeResponderList) {

        Step spikeResponder = (Step) spikeResponderList.get(0);

        // Handle consistency of multiply selections

        // Handle Response Height
        if (!NetworkUtils.isConsistent(spikeResponderList, Step.class,
                "getResponseHeight")) {
            tfResponseHeight.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfResponseHeight.setText(Double.toString(spikeResponder
                    .getResponseHeight()));
        }

        // Handle Response Duration
        if (!NetworkUtils.isConsistent(spikeResponderList, Step.class,
                "getResponseDuration")) {
            tfResponseDuration.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfResponseDuration.setText(Double.toString(spikeResponder
                    .getResponseDuration()));
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(Synapse synapse) {

        if (!(synapse.getSpikeResponder() instanceof Step)) {
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

    @Override
    protected void writeValuesToRules(Collection<Synapse> synapses) {

        // Response Height
        double responseHeight = Utils.doubleParsable(tfResponseHeight);
        if (!Double.isNaN(responseHeight)) {
            for (Synapse s : synapses) {
                ((Step) s.getSpikeResponder())
                        .setResponseHeight(responseHeight);
            }
        }

        // Response Duration
        double responseDuration = Utils.doubleParsable(tfResponseDuration);
        if (!Double.isNaN(responseDuration)) {
            for (Synapse s : synapses) {
                ((Step) s.getSpikeResponder())
                        .setResponseDuration(responseDuration);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Step getPrototypeResponder() {
        return PROTOTYPE_RESPONDER;
    }

}