package org.simbrain.network.gui.dialogs.neuron.rule_panels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.ParameterGetter;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronRulePanel;
import org.simbrain.network.gui.dialogs.neuron.NeuronNoiseGenPanel;
import org.simbrain.network.neuron_update_rules.FitzhughNagumo;
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.util.widgets.TristateDropDown;


/**
 * @author Amanda Pandey <amanda.pandey@gmail.com>
 */
public class FitzhughNagumoRulePanel extends AbstractNeuronRulePanel {

    /** A variable governs overall rate of recovery equation. */
    private JTextField tfA = new JTextField();
    
    /** Influence of V on recovery variable */
    private JTextField tfB = new JTextField();
    
    /** Influence of W on future values of W */
    private JTextField tfC = new JTextField();

    /** Constant background current. KEEP */
    private JTextField tfIbg = new JTextField();

    /** Threshold value to signal a spike. KEEP */
    private JTextField tfThreshold = new JTextField();

    private TristateDropDown isAddNoise = new TristateDropDown();
    private JTabbedPane tabbedPane = new JTabbedPane();
    private LabelledItemPanel mainTab = new LabelledItemPanel();
    /** Random tab. */
    private NeuronNoiseGenPanel randTab = new NeuronNoiseGenPanel();
    
    /** A reference to the neuron update rule being edited. */
    private static final FitzhughNagumo prototypeRule = new FitzhughNagumo();


    public FitzhughNagumoRulePanel() {
        super();
        this.add(tabbedPane);
        mainTab.addItem("A (Recovery Rate): ", tfA);
        mainTab.addItem("B (Rec. Voltage Dependence): ", tfB);
        mainTab.addItem("C (Rec. Self Dependence): ", tfC);
        mainTab.addItem("Background Current (nA)", tfIbg);
        mainTab.addItem("Spike threshold", tfThreshold);
        mainTab.addItem("Add noise: ", isAddNoise);
        tabbedPane.add(mainTab, "Properties");
        tabbedPane.add(randTab, "Noise");
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

        //a
        ParameterGetter<NeuronUpdateRule, Double> aGetter =
                new ParameterGetter<NeuronUpdateRule, Double>() {
                    @Override
                    public Double getParameter(NeuronUpdateRule source) {
                        return ((FitzhughNagumo) source).getA();
                    }
                };
        if (!NetworkUtils.isConsistent(ruleList, aGetter)) {
            tfA.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfA.setText(Double.toString(aGetter.getParameter(neuronRef)));
        }
        
        //b
        ParameterGetter<NeuronUpdateRule, Double> bGetter =
                new ParameterGetter<NeuronUpdateRule, Double>() {
                    @Override
                    public Double getParameter(NeuronUpdateRule source) {
                        return ((FitzhughNagumo) source).getB();
                    }
                };
        if (!NetworkUtils.isConsistent(ruleList, bGetter)) {
            tfB.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfB.setText(Double.toString(bGetter.getParameter(neuronRef)));
        }
        
        //c
        ParameterGetter<NeuronUpdateRule, Double> cGetter =
                new ParameterGetter<NeuronUpdateRule, Double>() {
                    @Override
                    public Double getParameter(NeuronUpdateRule source) {
                        return ((FitzhughNagumo) source).getC();
                    }
                };
        if (!NetworkUtils.isConsistent(ruleList, cGetter)) {
            tfC.setText(SimbrainConstants.NULL_STRING);
        } else {
            tfC.setText(Double.toString(cGetter.getParameter(neuronRef)));
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
        
        // Handle Noise
        if (!NetworkUtils.isConsistent(ruleList, FitzhughNagumo.class,
                "getAddNoise"))
            isAddNoise.setNull();
        else
            isAddNoise.setSelected(neuronRef.getAddNoise());

        randTab.fillFieldValues(getRandomizers(ruleList));
    }

    
    @Override
    public void fillDefaultValues() {
        tfA.setText(Double.toString(prototypeRule.getA()));
        tfB.setText(Double.toString(prototypeRule.getB()));
        tfC.setText(Double.toString(prototypeRule.getC()));
        tfIbg.setText(Double.toString(prototypeRule.getiBg()));
        tfThreshold.setText(Double.toString(prototypeRule.getThreshold()));
        randTab.fillDefaultValues();
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
        //a
        double a = Utils.doubleParsable(tfA);
        if (!Double.isNaN(a)) {
            for (int i = 0; i < numNeurons; i++) {
                ((FitzhughNagumo) neurons.get(i).getUpdateRule())
                        .setA(a);
            }
        }

        //b
        double b = Utils.doubleParsable(tfB);
        if (!Double.isNaN(b)) {
            for (int i = 0; i < numNeurons; i++) {
                ((FitzhughNagumo) neurons.get(i).getUpdateRule())
                        .setB(b);
            }
        }
        
        //c
        double c = Utils.doubleParsable(tfC);
        if (!Double.isNaN(c)) {
            for (int i = 0; i < numNeurons; i++) {
                ((FitzhughNagumo) neurons.get(i).getUpdateRule())
                        .setC(c);
            }
        }
        
        //iBg
        double ibg = Utils.doubleParsable(tfIbg);
        if (!Double.isNaN(ibg)) {
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
        
        // Add Noise?
        if (!isAddNoise.isNull()) {
            boolean addNoise = isAddNoise.getSelectedIndex() == TristateDropDown
                    .getTRUE();
            for (int i = 0; i < numNeurons; i++) {
                ((FitzhughNagumo) neurons.get(i).getUpdateRule())
                        .setAddNoise(addNoise);
            }
            if (addNoise) {
                randTab.commitRandom(neurons);
            }
        }
    }

    @Override
    protected NeuronUpdateRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }
}
