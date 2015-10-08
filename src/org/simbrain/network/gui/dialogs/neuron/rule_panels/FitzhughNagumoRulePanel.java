package org.simbrain.network.gui.dialogs.neuron.rule_panels;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.ParameterGetter;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronRulePanel;
import org.simbrain.network.neuron_update_rules.FitzhughNagumo;
import org.simbrain.network.neuron_update_rules.MorrisLecarRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;
import org.simbrain.util.widgets.TristateDropDown;


import javax.swing.*;
import java.util.Collections;
import java.util.List;


/**
 * @author Amanda Pandey <amanda.pandey@gmail.com>
 */
public class FitzhughNagumoRulePanel extends AbstractNeuronRulePanel {

    /** W. - recovery variable */
    private JTextField tfw = new JTextField();

    /** V. - membrane potential */
    private JTextField tfv = new JTextField();

    /** Constant background current. KEEP */
    private JTextField tfIbg = new JTextField();

    /** Threshold value to signal a spike. KEEP */
    private JTextField tfThreshold = new JTextField();

    private TristateDropDown isAddNoise = new TristateDropDown();
    private JTabbedPane tabbedPane = new JTabbedPane();
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** A reference to the neuron update rule being edited. */
    private static final FitzhughNagumo prototypeRule = new FitzhughNagumo();


    public FitzhughNagumoRulePanel() {
        super();
        this.add(tabbedPane);
        mainTab.add("W. - recovery variable", tfw);
        mainTab.add("V. - membrane potential", tfv);
        mainTab.add("Constant background current. KEEP", tfIbg);
        mainTab.add("Threshold value to signal a spike. KEEP", tfThreshold);
    }



    @Override
    public void fillFieldValues(List<NeuronUpdateRule> ruleList) {
        FitzhughNagumo neuronRef = (FitzhughNagumo) ruleList.get(0);

        // Handle Add Noise
        if (!NetworkUtils.isConsistent(ruleList, FitzhughNagumo.class,
                "getAddNoise"))
            isAddNoise.setNull();
        else
            isAddNoise.setSelected(neuronRef.getAddNoise());

        // TODO: Use lambda expressions when we upgrade to be Java 8 compat

        //w
        ParameterGetter<NeuronUpdateRule, Double> gwGetter =
                new ParameterGetter<NeuronUpdateRule, Double>() {
                    @Override
                    public Double getParameter(NeuronUpdateRule source) {
                        return ((FitzhughNagumo) source).getW();
                    }
                };
        if (!NetworkUtils.isConsistent(ruleList, gwGetter)) {
            tfw.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfw.setText(Double.toString(gwGetter.getParameter(neuronRef)));
        }

        //v
        ParameterGetter<NeuronUpdateRule, Double> vGetter =
                new ParameterGetter<NeuronUpdateRule, Double>() {
                    @Override
                    public Double getParameter(NeuronUpdateRule source) {
                        return ((FitzhughNagumo) source).getV();
                    }
                };
        if (!NetworkUtils.isConsistent(ruleList, vGetter)) {
            tfv.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfv.setText(Double.toString(vGetter.getParameter(neuronRef)));
        }

        //iBg
        ParameterGetter<NeuronUpdateRule, Double> ibgGetter =
                new ParameterGetter<NeuronUpdateRule, Double>() {
                    @Override
                    public Double getParameter(NeuronUpdateRule source) {
                        return ((FitzhughNagumo) source).getiBg();
                    }
                };
        if (!NetworkUtils.isConsistent(ruleList, ibgGetter)) {
            tfIbg.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfIbg.setText(Double.toString(ibgGetter.getParameter(neuronRef)));
        }

        //threshold
        ParameterGetter<NeuronUpdateRule, Double> thrGetter =
                new ParameterGetter<NeuronUpdateRule, Double>() {
                    @Override
                    public Double getParameter(NeuronUpdateRule source) {
                        return ((FitzhughNagumo) source).getThreshold();
                    }
                };
        if (!NetworkUtils.isConsistent(ruleList, thrGetter)) {
            tfThreshold.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfThreshold.setText(Double.toString(thrGetter.getParameter(neuronRef)));
        }
    }

    @Override
    public void fillDefaultValues() {
        tfw.setText(Double.toString(prototypeRule.getW()));
        tfv.setText(Double.toString(prototypeRule.getV()));
        tfIbg.setText(Double.toString(prototypeRule.getiBg()));
        tfThreshold.setText(Double.toString(prototypeRule.getThreshold()));
    }

    @Override
    public void commitChanges(Neuron neuron) {
        if (!(neuron.getUpdateRule() instanceof FitzhughNagumo)) {
            neuron.setUpdateRule(prototypeRule.deepCopy());
        }

        writeValuesToRules(Collections.singletonList(neuron));
    }

    @Override
    public void commitChanges(List<Neuron> neurons) {
        if (isReplace()) {
            NeuronUpdateRule neuronRef = prototypeRule.deepCopy();
            for (Neuron n : neurons) {
                n.setUpdateRule(neuronRef.deepCopy());
            }
        }
        writeValuesToRules(neurons);
    }



    @Override
    protected void writeValuesToRules(List<Neuron> neurons) {
        int numNeurons = neurons.size();
        //w
        double w = Utils.doubleParsable(tfw);
        if (!Double.isNaN(w)) {
            for (int i = 0; i < numNeurons; i++) {
                ((FitzhughNagumo) neurons.get(i).getUpdateRule())
                        .setW(w);
            }
        }

        //v
        double v = Utils.doubleParsable(tfv);
        if (!Double.isNaN(v)) {
            for (int i = 0; i < numNeurons; i++) {
                ((FitzhughNagumo) neurons.get(i).getUpdateRule())
                        .setV(v);
            }
        }

        //iBg
        double ibg = Utils.doubleParsable(tfIbg);
        if (!Double.isNaN(v)) {
            for (int i = 0; i < numNeurons; i++) {
                ((FitzhughNagumo) neurons.get(i).getUpdateRule())
                        .setiBg(ibg);
            }
        }

        //threshold
        double thr = Utils.doubleParsable(tfThreshold);
        if (!Double.isNaN(thr)) {
            for (int i = 0; i < numNeurons; i++) {
                ((FitzhughNagumo) neurons.get(i).getUpdateRule())
                        .setThreshold(thr);
            }
        }
    }

    @Override
    protected NeuronUpdateRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }
}
