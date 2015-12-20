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
package org.simbrain.network.gui.dialogs.neuron.rule_panels;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronRulePanel;
import org.simbrain.network.gui.dialogs.neuron.NeuronNoiseGenPanel;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.ProductRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.Utils;
import org.simbrain.util.widgets.TristateDropDown;

import javax.swing.JTabbedPane;
import java.util.Collections;
import java.util.List;

/**
 * <b>ProductNeuronPanel</b>.
 */
public class ProductRulePanel extends AbstractNeuronRulePanel {

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** Random tab. */
    private NeuronNoiseGenPanel randTab = new NeuronNoiseGenPanel();

    /** Add noise combo box. */
    private TristateDropDown isUseWeights = new TristateDropDown();

    /** Add noise combo box. */
    private TristateDropDown isAddNoise = new TristateDropDown();

    /** A reference to the neuron update rule being edited. */
    private static final ProductRule prototypeRule = new ProductRule();

    /**
     * Creates an instance of this panel.
     *
     */
    public ProductRulePanel() {
        this.add(tabbedPane);
        mainTab.addItem("Use weights", isUseWeights);
        mainTab.addItem("Add noise", isAddNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }

    /**
     * Populate fields with current data.
     * @param ruleList
     */
    public void fillFieldValues(List<NeuronUpdateRule> ruleList) {

        ProductRule neuronRef = (ProductRule) ruleList.get(0);

        // (Below) Handle consistency of multiple selections

        // Handle weights
        if (!NetworkUtils.isConsistent(ruleList, ProductRule.class,
                "getUseWeights")) {
            isUseWeights.setNull();            
        } else {
            isUseWeights.setSelected(neuronRef.getUseWeights());
        }
        
        // Handle Noise
        if (!NetworkUtils.isConsistent(ruleList, ProductRule.class,
                "getAddNoise")) {
            isAddNoise.setNull();            
        } else {
            isAddNoise.setSelected(neuronRef.getAddNoise());
        }

        randTab.fillFieldValues(getRandomizers(ruleList));

    }

    /**
     * Fill field values to default values for linear neuron.
     */
    public void fillDefaultValues() {
        isUseWeights.setSelected(prototypeRule.getUseWeights());
        isAddNoise.setSelected(prototypeRule.getAddNoise());
        randTab.fillDefaultValues();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(Neuron neuron) {

        if (!(neuron.getUpdateRule() instanceof ProductRule)) {
            neuron.setUpdateRule(prototypeRule.deepCopy());
        }

        writeValuesToRules(Collections.singletonList(neuron));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitChanges(List<Neuron> neurons) {

        if (isReplace()) {
            ProductRule neuronRef = prototypeRule.deepCopy();
            for (Neuron n : neurons) {
                n.setUpdateRule(neuronRef.deepCopy());
            }
        }

        writeValuesToRules(neurons);

    }

    @Override
    protected void writeValuesToRules(List<Neuron> neurons) {
        int numNeurons = neurons.size();

        // Use weights
        if (!isUseWeights.isNull()) {
            boolean useWeights = isUseWeights.getSelectedIndex() == TristateDropDown
                    .getTRUE();
            for (int i = 0; i < numNeurons; i++) {
                ((ProductRule) neurons.get(i).getUpdateRule())
                        .setUseWeights(useWeights);
            }

        }
        
        // Add Noise?
        if (!isAddNoise.isNull()) {
            boolean addNoise = isAddNoise.getSelectedIndex() == TristateDropDown
                    .getTRUE();
            for (int i = 0; i < numNeurons; i++) {
                ((ProductRule) neurons.get(i).getUpdateRule())
                        .setAddNoise(addNoise);
            }
            if (addNoise) {
                randTab.commitRandom(neurons);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NeuronUpdateRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }

}
