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
import org.simbrain.network.synapse_update_rules.spikeresponders.JumpAndDecay;
import org.simbrain.network.synapse_update_rules.spikeresponders.SpikeResponder;
import org.simbrain.util.Utils;

/**
 * <b>JumpAndDecayPanel</b>.
 */
public class JumpAndDecayPanel extends AbstractSpikeResponsePanel {

    /** Jump height field. */
    private JTextField tfJumpHeight = new JTextField();

    /** Base line field. */
    private JTextField tfBaseLine = new JTextField();

    /** Decay rate field. */
    private JTextField tfTimeConstant = new JTextField();

    /** The prototypical jump and decay responder. */
    public static final JumpAndDecay PROTOTYPE_RESPONDER = new JumpAndDecay();

    /**
     * This method is the default constructor.
     */
    public JumpAndDecayPanel() {
        tfJumpHeight.setColumns(6);
        this.addItem("Jump height", tfJumpHeight);
        this.addItem("Base-line", tfBaseLine);
        this.addItem("Time Constant", tfTimeConstant);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillFieldValues(List<SpikeResponder> spikeResponderList) {

        JumpAndDecay spikeResponder = (JumpAndDecay) spikeResponderList.get(0);

        // Handle consistency of multiply selections

        // Handle Jump Height
        if (!NetworkUtils.isConsistent(spikeResponderList, JumpAndDecay.class,
                "getJumpHeight"))
        {
            tfJumpHeight.setText(NULL_STRING);
        } else {
            tfJumpHeight.setText(Double.toString(spikeResponder.
                    getJumpHeight()));
        }

        // Handle Baseline
        if (!NetworkUtils.isConsistent(spikeResponderList, JumpAndDecay.class,
                "getBaseLine"))
        {
            tfBaseLine.setText(NULL_STRING);
        } else {
            tfBaseLine.setText(Double.toString(spikeResponder.getBaseLine()));
        }

        // Handle Decay Rate
        if (!NetworkUtils.isConsistent(spikeResponderList, JumpAndDecay.class,
                "getTimeConstant"))
        {
            tfTimeConstant.setText(NULL_STRING);
        } else {
            tfTimeConstant.setText(Double.toString(spikeResponder.
                    getTimeConstant()));
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillDefaultValues() {
        tfJumpHeight.setText(Double.toString(PROTOTYPE_RESPONDER.
                getJumpHeight()));
        tfBaseLine.setText(Double.toString(PROTOTYPE_RESPONDER.getBaseLine()));
        tfTimeConstant.setText(Double.toString(PROTOTYPE_RESPONDER.
                getTimeConstant()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(Synapse synapse) {

        if (!(synapse.getSpikeResponder() instanceof JumpAndDecay)) {
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

        // Jump Height
        double jumpHeight = Utils.doubleParsable(tfJumpHeight);
        if (!Double.isNaN(jumpHeight)) {
            for (Synapse s : synapses) {
                ((JumpAndDecay) s.getSpikeResponder()).setJumpHeight(
                        jumpHeight);
            }
        }

        // Base Line
        double baseLine = Utils.doubleParsable(tfBaseLine);
        if (!Double.isNaN(baseLine)) {
            for (Synapse s : synapses) {
                ((JumpAndDecay) s.getSpikeResponder()).setBaseLine(baseLine);
            }
        }

        // Decay Rate
        double timeConstant = Utils.doubleParsable(tfTimeConstant);
        if (!Double.isNaN(timeConstant)) {
            for (Synapse s : synapses) {
                ((JumpAndDecay) s.getSpikeResponder()).setTimeConstant(
                        timeConstant);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JumpAndDecay getPrototypeResponder() {
        return PROTOTYPE_RESPONDER;
    }

}