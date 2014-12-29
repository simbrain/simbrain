package org.simbrain.network.gui.dialogs.neuron;

import java.util.Collections;
import java.util.List;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.gui.ParameterGetter;
import org.simbrain.network.neuron_update_rules.AdExIFRule;
import org.simbrain.network.neuron_update_rules.IntegrateAndFireRule;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.Utils;
import org.simbrain.util.widgets.TristateDropDown;

public class AdExIFRulePanel extends AbstractNeuronRulePanel {

    /** Excitatory Reversal field. */
    private JTextField tfER = new JTextField();

    /** Inhibitory Reversal field. */
    private JTextField tfIR = new JTextField();

    /** Leak Reversal field. */
    private JTextField tfLR = new JTextField();

    private JTextField tfGeBar = new JTextField();
    
    private JTextField tfGiBar = new JTextField();
    
    private JTextField tfGL = new JTextField();
    
    private JTextField tfCap = new JTextField();

    /** Threshold for output function. */
    private JTextField tfThreshold = new JTextField();
    
    private JTextField tfPeak = new JTextField();
    
    private JTextField tfV_Reset = new JTextField();

    /** Bias for excitatory inputs. */
    private JTextField tfBgCurrent = new JTextField();

    private JTextField tfAdaptTC = new JTextField();
    
    private JTextField tfSlopeFactor = new JTextField();
    
    private JTextField tfAdaptCouplingConst = new JTextField();
    
    private JTextField tfAdaptResetParam = new JTextField();

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();
    
    /** Main tab. */
    private LabelledItemPanel currentTab = new LabelledItemPanel();

    /** Inputs tab. */
    private LabelledItemPanel adaptationTab = new LabelledItemPanel();

    /** Random tab. */
    private NeuronNoiseGenPanel randTab = new NeuronNoiseGenPanel();

    /** Add noise combo box. */
    private TristateDropDown isAddNoise = new TristateDropDown();
    
    /** A reference to the neuron update rule being edited. */
    private static final AdExIFRule prototypeRule = new AdExIFRule();

    /**
     * Creates an instance of this panel.
     */
    public AdExIFRulePanel() {
        super();
        this.add(tabbedPane);
        mainTab.addItem("Peak Voltage (mV)", tfPeak);
        mainTab.addItem("Threshold Voltage (mV)", tfThreshold);
        mainTab.addItem("Reset Voltage (mV)" , tfV_Reset);
        mainTab.addItem("Capacitance (pF)", tfCap);
        mainTab.addItem("Background Current (nA)", tfBgCurrent);
        mainTab.addItem("Slope Factor", tfSlopeFactor);
        currentTab.addItem("Leak Conductance (nS)", tfGL);
        currentTab.addItem("Max Ex. Conductance (nS)" , tfGeBar);
        currentTab.addItem("Max In. Conductance (nS)", tfGiBar);
        currentTab.addItem("Leak Reversal (mV)", tfLR);
        currentTab.addItem("Excitatory Reversal (mV)", tfER);
        currentTab.addItem("Inhibitory Reversal (mV)", tfIR);
        adaptationTab.addItem("Reset (nA)", tfAdaptResetParam);
        adaptationTab.addItem("Coupling Const.", tfAdaptCouplingConst);
        adaptationTab.addItem("Time Constant (ms)", tfAdaptTC);
        tabbedPane.add(mainTab, "Membrane Voltage");
        tabbedPane.add(currentTab, "Input Currents");
        tabbedPane.add(adaptationTab, "Adaptation");
    }
	
	@Override
	public void fillFieldValues(List<NeuronUpdateRule> ruleList) {
		AdExIFRule neuronRef = (AdExIFRule) ruleList.get(0);
		
        // Handle Add Noise
        if (!NetworkUtils.isConsistent(ruleList, AdExIFRule.class,
            "getAddNoise"))
            isAddNoise.setNull();
        else
            isAddNoise.setSelected(neuronRef.getAddNoise());
		
        // TODO: Use lambda expressions when we upgrade to be Java 8 compat
        
        // Excitatory Reversal
        ParameterGetter<NeuronUpdateRule, Double> erGetter =
        		new ParameterGetter<NeuronUpdateRule, Double>() {
			@Override
			public Double getParameter(NeuronUpdateRule source) {
				return ((AdExIFRule) source).getExReversal();
			}
        };
        if (!NetworkUtils.isConsistent(ruleList, erGetter)) {
        	tfER.setText(SimbrainConstants.NULL_STRING);
        } else {
        	tfER.setText(Double.toString(erGetter.getParameter(neuronRef)));
        }
        
        // Inhibitory Reversal
        ParameterGetter<NeuronUpdateRule, Double> irGetter =
        		new ParameterGetter<NeuronUpdateRule, Double>() {
			@Override
			public Double getParameter(NeuronUpdateRule source) {
				return ((AdExIFRule) source).getInReversal();
			}
        };
        if (!NetworkUtils.isConsistent(ruleList, irGetter)) {
        	tfIR.setText(SimbrainConstants.NULL_STRING);
        } else {
        	tfIR.setText(Double.toString(irGetter.getParameter(neuronRef)));
        }
        
        //Leak Reversal
        ParameterGetter<NeuronUpdateRule, Double> lrGetter =
        		new ParameterGetter<NeuronUpdateRule, Double>() {
			@Override
			public Double getParameter(NeuronUpdateRule source) {
				return ((AdExIFRule) source).getLeakReversal();
			}
        };
        if (!NetworkUtils.isConsistent(ruleList, lrGetter)) {
        	tfLR.setText(SimbrainConstants.NULL_STRING);
        } else {
        	tfLR.setText(Double.toString(lrGetter.getParameter(neuronRef)));
        }
        
        // Ex Max Conductance
        ParameterGetter<NeuronUpdateRule, Double> geBarGetter =
        		new ParameterGetter<NeuronUpdateRule, Double>() {
			@Override
			public Double getParameter(NeuronUpdateRule source) {
				return ((AdExIFRule) source).getG_e_bar();
			}
        };
        if (!NetworkUtils.isConsistent(ruleList, geBarGetter)) {
        	tfGeBar.setText(SimbrainConstants.NULL_STRING);
        } else {
        	tfGeBar.setText(Double.toString(geBarGetter
        			.getParameter(neuronRef)));
        }
        
        // In max Conductance
        ParameterGetter<NeuronUpdateRule, Double> giBarGetter =
        		new ParameterGetter<NeuronUpdateRule, Double>() {
			@Override
			public Double getParameter(NeuronUpdateRule source) {
				return ((AdExIFRule) source).getG_i_bar();
			}
        };
        if (!NetworkUtils.isConsistent(ruleList, giBarGetter)) {
        	tfGiBar.setText(SimbrainConstants.NULL_STRING);
        } else {
        	tfGiBar.setText(Double.toString(giBarGetter
        			.getParameter(neuronRef)));
        }
        
        // Leak Conductance
        ParameterGetter<NeuronUpdateRule, Double> gLGetter =
        		new ParameterGetter<NeuronUpdateRule, Double>() {
			@Override
			public Double getParameter(NeuronUpdateRule source) {
				return ((AdExIFRule) source).getG_L();
			}
        };
        if (!NetworkUtils.isConsistent(ruleList, gLGetter)) {
        	tfGL.setText(SimbrainConstants.NULL_STRING);
        } else {
        	tfGL.setText(Double.toString(gLGetter
        			.getParameter(neuronRef)));
        }
        
        // Membrane capacitance
        ParameterGetter<NeuronUpdateRule, Double> capGetter =
        		new ParameterGetter<NeuronUpdateRule, Double>() {
			@Override
			public Double getParameter(NeuronUpdateRule source) {
				return ((AdExIFRule) source).getMemCapacitance();
			}
        };
        if (!NetworkUtils.isConsistent(ruleList, capGetter)) {
        	tfCap.setText(SimbrainConstants.NULL_STRING);
        } else {
        	tfCap.setText(Double.toString(capGetter
        			.getParameter(neuronRef)));
        }
        
        ParameterGetter<NeuronUpdateRule, Double> threshGetter =
        		new ParameterGetter<NeuronUpdateRule, Double>() {
			@Override
			public Double getParameter(NeuronUpdateRule source) {
				return ((AdExIFRule) source).getV_Th();
			}
        };
        if (!NetworkUtils.isConsistent(ruleList, threshGetter)) {
        	tfThreshold.setText(SimbrainConstants.NULL_STRING);
        } else {
        	tfThreshold.setText(Double.toString(threshGetter
        			.getParameter(neuronRef)));
        }
        
        ParameterGetter<NeuronUpdateRule, Double> peakGetter =
        		new ParameterGetter<NeuronUpdateRule, Double>() {
			@Override
			public Double getParameter(NeuronUpdateRule source) {
				return ((AdExIFRule) source).getV_Peak();
			}
        };
        if (!NetworkUtils.isConsistent(ruleList, peakGetter)) {
        	tfPeak.setText(SimbrainConstants.NULL_STRING);
        } else {
        	tfPeak.setText(Double.toString(peakGetter
        			.getParameter(neuronRef)));
        }
        
        ParameterGetter<NeuronUpdateRule, Double> resetGetter =
        		new ParameterGetter<NeuronUpdateRule, Double>() {
			@Override
			public Double getParameter(NeuronUpdateRule source) {
				return ((AdExIFRule) source).getV_Reset();
			}
        };
        if (!NetworkUtils.isConsistent(ruleList, resetGetter)) {
        	tfV_Reset.setText(SimbrainConstants.NULL_STRING);
        } else {
        	tfV_Reset.setText(Double.toString(resetGetter
        			.getParameter(neuronRef)));
        }
        
        ParameterGetter<NeuronUpdateRule, Double> bgCurrGetter =
        		new ParameterGetter<NeuronUpdateRule, Double>() {
			@Override
			public Double getParameter(NeuronUpdateRule source) {
				return ((AdExIFRule) source).getI_bg();
			}
        };
        if (!NetworkUtils.isConsistent(ruleList, bgCurrGetter)) {
        	tfBgCurrent.setText(SimbrainConstants.NULL_STRING);
        } else {
        	tfBgCurrent.setText(Double.toString(bgCurrGetter
        			.getParameter(neuronRef)));
        }
        
        ParameterGetter<NeuronUpdateRule, Double> adaptTcGetter =
        		new ParameterGetter<NeuronUpdateRule, Double>() {
			@Override
			public Double getParameter(NeuronUpdateRule source) {
				return ((AdExIFRule) source).getTauW();
			}
        };
        if (!NetworkUtils.isConsistent(ruleList, adaptTcGetter)) {
        	tfAdaptTC.setText(SimbrainConstants.NULL_STRING);
        } else {
        	tfAdaptTC.setText(Double.toString(adaptTcGetter
        			.getParameter(neuronRef)));
        }
        
        ParameterGetter<NeuronUpdateRule, Double> slopeGetter =
        		new ParameterGetter<NeuronUpdateRule, Double>() {
			@Override
			public Double getParameter(NeuronUpdateRule source) {
				return ((AdExIFRule) source).getSlopeFactor();
			}
        };
        if (!NetworkUtils.isConsistent(ruleList, slopeGetter)) {
        	tfSlopeFactor.setText(SimbrainConstants.NULL_STRING);
        } else {
        	tfSlopeFactor.setText(Double.toString(slopeGetter
        			.getParameter(neuronRef)));
        }
        
        ParameterGetter<NeuronUpdateRule, Double> accGetter =
        		new ParameterGetter<NeuronUpdateRule, Double>() {
			@Override
			public Double getParameter(NeuronUpdateRule source) {
				return ((AdExIFRule) source).getA();
			}
        };
        if (!NetworkUtils.isConsistent(ruleList, accGetter)) {
        	tfAdaptCouplingConst.setText(SimbrainConstants.NULL_STRING);
        } else {
        	tfAdaptCouplingConst.setText(Double.toString(accGetter
        			.getParameter(neuronRef)));
        }
        
        ParameterGetter<NeuronUpdateRule, Double> adResetGetter =
        		new ParameterGetter<NeuronUpdateRule, Double>() {
			@Override
			public Double getParameter(NeuronUpdateRule source) {
				return ((AdExIFRule) source).getB();
			}
        };
        if (!NetworkUtils.isConsistent(ruleList, adResetGetter)) {
        	tfAdaptResetParam.setText(SimbrainConstants.NULL_STRING);
        } else {
        	tfAdaptResetParam.setText(Double.toString(adResetGetter
        			.getParameter(neuronRef)));
        }
        
	}

	@Override
	public void fillDefaultValues() {
		tfER.setText(Double.toString(prototypeRule.getExReversal()));
		tfIR.setText(Double.toString(prototypeRule.getInReversal()));
		tfLR.setText(Double.toString(prototypeRule.getLeakReversal()));
		tfGeBar.setText(Double.toString(prototypeRule.getG_e_bar()));
		tfGiBar.setText(Double.toString(prototypeRule.getG_i_bar()));
		tfGL.setText(Double.toString(prototypeRule.getG_L()));
		tfCap.setText(Double.toString(prototypeRule.getMemCapacitance()));
		tfThreshold.setText(Double.toString(prototypeRule.getV_Th()));
		tfPeak.setText(Double.toString(prototypeRule.getV_Peak()));
		tfV_Reset.setText(Double.toString(prototypeRule.getV_Reset()));
		tfBgCurrent.setText(Double.toString(prototypeRule.getI_bg()));
		tfAdaptTC.setText(Double.toString(prototypeRule.getTauW()));
		tfSlopeFactor.setText(Double.toString(prototypeRule.getSlopeFactor()));
		tfAdaptCouplingConst.setText(Double.toString(prototypeRule.getA()));
		tfAdaptResetParam.setText(Double.toString(prototypeRule.getB()));
	}

	@Override
	public void commitChanges(Neuron neuron) {
		
        if (!(neuron.getUpdateRule() instanceof AdExIFRule)) {
            neuron.setUpdateRule(prototypeRule.deepCopy());
        }

        writeValuesToRules(Collections.singletonList(neuron));
		
	}

	@Override
	public void commitChanges(List<Neuron> neurons) {
		
        if (isReplace()) {
            AdExIFRule neuronRef = prototypeRule.deepCopy();
            for (Neuron n : neurons) {
                n.setUpdateRule(neuronRef.deepCopy());
            }
        }
        writeValuesToRules(neurons);
	}

	@Override
	protected void writeValuesToRules(List<Neuron> neurons) {
        int numNeurons = neurons.size();
        // Ex reversal
        double exR = Utils.doubleParsable(tfER);
        if (!Double.isNaN(exR)) {
            for (int i = 0; i < numNeurons; i++) {
                ((AdExIFRule) neurons.get(i).getUpdateRule())
                	.setExReversal(exR);
            }
        }
        
        // In reversal
        double inR = Utils.doubleParsable(tfIR);
        if (!Double.isNaN(inR)) {
            for (int i = 0; i < numNeurons; i++) {
                ((AdExIFRule) neurons.get(i).getUpdateRule())
                	.setInReversal(inR);
            }
        }
        
        // Leak reversal
        double lR = Utils.doubleParsable(tfLR);
        if (!Double.isNaN(lR)) {
            for (int i = 0; i < numNeurons; i++) {
                ((AdExIFRule) neurons.get(i).getUpdateRule())
                	.setLeakReversal(lR);
            }
        }
        
        // Ex max conductance
        double eGBar = Utils.doubleParsable(tfGeBar);
        if (!Double.isNaN(eGBar)) {
            for (int i = 0; i < numNeurons; i++) {
                ((AdExIFRule) neurons.get(i).getUpdateRule()).setG_e_bar(eGBar);
            }
        }
        
        // In max conductance
        double iGBar = Utils.doubleParsable(tfGiBar);
        if (!Double.isNaN(iGBar)) {
            for (int i = 0; i < numNeurons; i++) {
                ((AdExIFRule) neurons.get(i).getUpdateRule()).setG_i_bar(iGBar);
            }
        }
        
        // Leak conductance
        double gL = Utils.doubleParsable(tfGL);
        if (!Double.isNaN(gL)) {
            for (int i = 0; i < numNeurons; i++) {
                ((AdExIFRule) neurons.get(i).getUpdateRule()).setG_L(gL);
            }
        }
        
        // Capacitance
        double cap = Utils.doubleParsable(tfCap);
        if (!Double.isNaN(cap)) {
            for (int i = 0; i < numNeurons; i++) {
                ((AdExIFRule) neurons.get(i).getUpdateRule())
                .setMemCapacitance(cap);
            }
        }
        
        // Threshold
        double thresh = Utils.doubleParsable(tfThreshold);
        if (!Double.isNaN(thresh)) {
            for (int i = 0; i < numNeurons; i++) {
                ((AdExIFRule) neurons.get(i).getUpdateRule()).setV_Th(thresh);
            }
        }
        
        // Peak
        double peak = Utils.doubleParsable(tfPeak);
        if (!Double.isNaN(peak)) {
            for (int i = 0; i < numNeurons; i++) {
                ((AdExIFRule) neurons.get(i).getUpdateRule()).setV_Peak(peak);
            }
        }
        
        // Reset
        double reset = Utils.doubleParsable(tfV_Reset);
        if (!Double.isNaN(reset)) {
            for (int i = 0; i < numNeurons; i++) {
                ((AdExIFRule) neurons.get(i).getUpdateRule()).setV_Reset(reset);
            }
        }
        
        // Bg Current
        double bg = Utils.doubleParsable(tfBgCurrent);
        if (!Double.isNaN(bg)) {
            for (int i = 0; i < numNeurons; i++) {
                ((AdExIFRule) neurons.get(i).getUpdateRule()).setI_bg(bg);
            }
        }
		
        // Adapt TC
        double atc = Utils.doubleParsable(tfAdaptTC);
        if (!Double.isNaN(atc)) {
            for (int i = 0; i < numNeurons; i++) {
                ((AdExIFRule) neurons.get(i).getUpdateRule()).setTauW(atc);
            }
        }
        
        // Slope Factor
        double sf = Utils.doubleParsable(tfSlopeFactor);
        if (!Double.isNaN(sf)) {
            for (int i = 0; i < numNeurons; i++) {
                ((AdExIFRule) neurons.get(i).getUpdateRule())
                	.setSlopeFactor(sf);
            }
        }
        
        // Adapt coupling constant
        double acc = Utils.doubleParsable(tfAdaptCouplingConst);
        if (!Double.isNaN(acc)) {
            for (int i = 0; i < numNeurons; i++) {
                ((AdExIFRule) neurons.get(i).getUpdateRule()).setA(acc);
            }
        }
        
        // Adapt Reset
        double ar = Utils.doubleParsable(tfAdaptResetParam);
        if (!Double.isNaN(ar)) {
            for (int i = 0; i < numNeurons; i++) {
                ((AdExIFRule) neurons.get(i).getUpdateRule()).setB(ar);
            }
        }
        
	}

	@Override
	protected AdExIFRule getPrototypeRule() {
		return prototypeRule.deepCopy();
	}

}
