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
package org.simbrain.network.gui.dialogs.synapse.plasticity_panels;

import java.util.Collections;
import java.util.List;

import javax.swing.JTextField;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.synapse.AbstractSynapsePanel;
import org.simbrain.network.synapse_update_rules.HebbianCPCARule;
import org.simbrain.util.Utils;

/**
 * <b>HebbianCPCAPanel</b>.
 */
public class HebbianCPCARulePanel extends AbstractSynapsePanel {

    /** TODO: Auto-Generated Stub */
    private static final long serialVersionUID = 1L;

    /** Learning rate field. */
    private final JTextField tfLearningRate = new JTextField();

    /** Maximum weight value (see equation 4.19 in O'Reilly and Munakata). */
    private final JTextField tfM = new JTextField();

    /** Weight offset. */
    private final JTextField tfTheta = new JTextField();

    /** Sigmoidal function. */
    private final JTextField tfLambda = new JTextField();

    /** Synapse reference. */
    private static final HebbianCPCARule prototypeRule =
            new HebbianCPCARule();

    /**
     * This method is the default constructor.
     */
    public HebbianCPCARulePanel() {
        this.addItem("Learning rate", tfLearningRate);
        this.addItem("Maximum Weight Value", tfM);
        this.addItem("Weight Offset Value", tfTheta);
        this.addItem("Sigmoidal Function", tfLambda);
    }

    /**
     * Populate fields with current data.
     * 
     * @param ruleList
     *            the list of rules being edited
     */
    public void fillFieldValues(List<SynapseUpdateRule> ruleList) {

        HebbianCPCARule synapseRef = (HebbianCPCARule) ruleList.get(0);

        // Handle consistency of multiply selections

        // Handle Learning Rate
        if (!NetworkUtils.isConsistent(ruleList, HebbianCPCARule.class,
                "getLearningRate")) {
            tfLearningRate.setText(NULL_STRING);
        } else {
            tfLearningRate.setText(Double.toString(synapseRef
                    .getLearningRate()));
        }

        // Handle M
        if (!NetworkUtils.isConsistent(ruleList, HebbianCPCARule.class,
                "getM")) {
            tfM.setText(NULL_STRING);
        } else {
            tfM.setText(Double.toString(synapseRef.getM()));
        }

        // Handle Theta
        if (!NetworkUtils.isConsistent(ruleList, HebbianCPCARule.class,
                "getTheta")) {
            tfTheta.setText(NULL_STRING);
        } else {
            tfTheta.setText(Double.toString(synapseRef.getTheta()));
        }

        // Handle Lambda
        if (!NetworkUtils.isConsistent(ruleList, HebbianCPCARule.class,
                "getLambda")) {
            tfLambda.setText(NULL_STRING);
        } else {
            tfLambda.setText(Double.toString(synapseRef.getLambda()));
        }

    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
        // HebbianCPCA synapseRef = new HebbianCPCA();
        tfLearningRate.setText(Double
                .toString(HebbianCPCARule.DEFAULT_LEARNING_RATE));
        tfM.setText(Double.toString(HebbianCPCARule.DEFAULT_M));
        tfTheta.setText(Double.toString(HebbianCPCARule.DEFAULT_THETA));
        tfLambda.setText(Double.toString(HebbianCPCARule.DEFAULT_LAMBDA));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(final Synapse synapse) {

        if (!(synapse.getLearningRule() instanceof HebbianCPCARule)) {
            synapse.setLearningRule(prototypeRule.deepCopy());
        }

        writeValuesToRules(Collections.singletonList(synapse));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(final List<Synapse> synapses) {

        if (isReplace()) {
            for (Synapse s : synapses) {
                s.setLearningRule(prototypeRule.deepCopy());
            }
        }

        writeValuesToRules(synapses);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeValuesToRules(final List<Synapse> synapses) {

        // Learning Rate
        double learningRate = Utils.doubleParsable(tfLearningRate);
        if (!Double.isNaN(learningRate)) {
            for (Synapse s : synapses) {
                ((HebbianCPCARule) s.getLearningRule())
                        .setLearningRate(learningRate);
            }
        }

        // M
        double m = Utils.doubleParsable(tfM);
        if (!Double.isNaN(m)) {
            for (Synapse s : synapses) {
                ((HebbianCPCARule) s.getLearningRule()).setM(m);
            }
        }

        // Theta
        double theta = Utils.doubleParsable(tfTheta);
        if (!Double.isNaN(theta)) {
            for (Synapse s : synapses) {
                ((HebbianCPCARule) s.getLearningRule()).setTheta(theta);
            }
        }

        // Lambda
        double lambda = Utils.doubleParsable(tfLambda);
        if (!Double.isNaN(lambda)) {
            for (Synapse s : synapses) {
                ((HebbianCPCARule) s.getLearningRule()).setLambda(lambda);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HebbianCPCARule getPrototypeRule() {
        return prototypeRule;
    }

}
