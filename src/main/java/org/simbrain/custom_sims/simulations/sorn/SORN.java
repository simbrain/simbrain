package org.simbrain.custom_sims.simulations.sorn;

import org.simbrain.custom_sims.Simulation;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.connections.Direction;
import org.simbrain.network.connections.FixedDegree;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseGroup;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.neurongroups.NeuronGroup;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.stats.ProbabilityDistribution;
import org.simbrain.util.stats.distributions.UniformRealDistribution;
import org.simbrain.workspace.gui.SimbrainDesktop;

import java.awt.*;
import java.util.ArrayList;

public class SORN extends Simulation {

    private int numNeurons;

    private int gridSpace = 30;

    private int connRadius = 400;

    private int eeKIn = 25;

    private int ieKIn;// = (int)(0.2*numNeurons/10);

    private int eiKIn;// = (int)(numNeurons/50);

    private ProbabilityDistribution defWtPD = new UniformRealDistribution(0,1);

    private Network net;

    public SORN() {
        super();
    }

    public SORN(SimbrainDesktop sbd) {
        super(sbd);
        numNeurons = 1024;
        double percentInhib = 0.2;
        ieKIn = (int)(percentInhib*numNeurons/10);
        eiKIn = numNeurons/50;
    }

    @Override public SORN instantiate(SimbrainDesktop sbd) {
        return new SORN(sbd);
    }

    @Override
    public void run() {

        // Set up network
        NetworkComponent nc = sim.addNetwork(10, 10, 543, 545,
                "Patterns of Activity");
        net = nc.getNetwork();
        net.setTimeStep(1);
        buildNetwork();
    }

    public void buildNetwork() {
        var neurons = new ArrayList<Neuron>();
        var inhibitoryNeurons = new ArrayList<Neuron>();
        SORNNeuronRule sornRule = new SORNNeuronRule();
        //  sornRule.sethIP(400.0/numNeurons);
        for (int i = 0; i < numNeurons; i++) {
            Neuron n = new Neuron();
            // sornRule.setMaxThreshold(0.5);
            // sornRule.setThreshold(0.5 * Math.random() + 0.01);
            sornRule.setRefractoryPeriod(1);
            sornRule.setAddNoise(true);
            n.setPolarity(Polarity.EXCITATORY);
            n.setUpdateRule(sornRule.copy());
            neurons.add(n);
        }
        SORNNeuronRule str = new SORNNeuronRule();
        for (int i = 0; i < (int) (numNeurons * 0.2); i++) {
            Neuron n = new Neuron();
            // str.setThreshold(0.8 * Math.random() + 0.01);
            str.setEtaIP(0); // No Homeostatic Plasticity
            str.setRefractoryPeriod(1);
            str.setAddNoise(true);
            n.setPolarity(Polarity.INHIBITORY);
            n.setUpdateRule(str.copy());
            inhibitoryNeurons.add(n);
        }

        NeuronGroup ng = new NeuronGroup(neurons);
        GridLayout layout = new GridLayout(gridSpace, gridSpace, (int) Math.sqrt(numNeurons));
        ng.setLabel("Excitatory");
        net.addNetworkModel(ng);
        ng.setLayout(layout);
        ng.applyLayout(new Point(10, 10));

        NeuronGroup ngIn = new NeuronGroup(inhibitoryNeurons);
        layout = new GridLayout(gridSpace*2, gridSpace*2, (int) Math.sqrt(0.2 * numNeurons));
        ngIn.setLabel("Inhibitory");
        net.addNetworkModel(ngIn);
        ngIn.setLayout(layout);
        System.out.println(ngIn.getSize());
        int x_loc = (int) (Math.sqrt(numNeurons) * gridSpace + 300);
        ngIn.applyLayout(new Point(1810, 141));

        SynapseGroup sg_ee = connectGroups(net, ng, ng, eeKIn, defWtPD, Polarity.EXCITATORY,
                "Exc. \u2192 Exc.");
        connectGroups(net, ngIn, ng, ieKIn, new UniformRealDistribution(-1,0),
                Polarity.INHIBITORY,
                "Inh. \u2192 Exc.");
        connectGroups(net, ng, ngIn, eiKIn, defWtPD, Polarity.EXCITATORY,
                "Exc. \u2192 Inh.");

        // Normalize newly created inhibitory connections
        for (Neuron n : neurons) {
            SORN.normalizeInhibitoryFanIn(n);
        }
        // Set up plasticity between exc and exc neurons
        AddSTDPRule stdp = new AddSTDPRule();
        stdp.setLearningRate(0.001);
        // TODO
        // sg_ee.setLearningRule(stdp, SimbrainConstants.Polarity.BOTH);

        ArrayList<Neuron> inNeurons = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            Neuron n = new Neuron();
            //SpikingThresholdRule inRule = new SpikingThresholdRule();
            //inRule.setThreshold(0.96);
            sornRule.setMaxThreshold(0.5);
            sornRule.setThreshold(0.5 * Math.random() + 0.01);
            n.setPolarity(Polarity.EXCITATORY);
            n.setUpdateRule(sornRule.copy());
            inNeurons.add(n);
        }

        NeuronGroup input = new NeuronGroup(inNeurons);
        layout = new GridLayout(gridSpace, gridSpace, (int) Math.sqrt(0.4 * numNeurons));
        input.setLabel("Input");
        net.addNetworkModel(input);
        input.setLayout(layout);
        // Todo; get current location of ng above
        int y_loc = (int) (Math.sqrt(numNeurons) * gridSpace + 200);
        input.applyLayout(new Point(x_loc, y_loc));

        Sparse input_ee_con = new Sparse(0.05, false, false);
        SynapseGroup input_ee = new SynapseGroup(input, ng, input_ee_con);
        input_ee.getConnectionStrategy().setPercentExcitatory(1.0);
        input_ee.getConnectionStrategy().setExRandomizer(defWtPD);
        input_ee.getConnectionStrategy().setInRandomizer(defWtPD);
        input_ee.setLabel("Input -> Excitatory");
        // TODO
        // input_ee.setLearningRule(stdp, Polarity.BOTH);
        // input_ee.setSpikeResponder(new Step(), Polarity.BOTH);
        net.addNetworkModel(input_ee);

//        Sparse ee_input_con = new Sparse(0.01, false, false);
//        SynapseGroup ee_input = new SynapseGroup2(ng, input, ee_input_con, 1.0, exRand, inRand);
//        ee_input.setLabel("Excitatory -> Input");
//        ee_input.setLearningRule(stdp, Polarity.BOTH);
//        ee_input.setSpikeResponder(new Step(), Polarity.BOTH);
//        network.addGroup(ee_input);

        Sparse input_ie_con = new Sparse(0.05, true, false);
        SynapseGroup input_ie = new SynapseGroup(input, ngIn, input_ie_con);
        input_ie.getConnectionStrategy().setPercentExcitatory(1.0);
        input_ie.getConnectionStrategy().setExRandomizer(defWtPD);
        input_ie.getConnectionStrategy().setInRandomizer(defWtPD);
        input_ie.setLabel("Input -> Inhibitory");
        // TODO
        // input_ie.setSpikeResponder(new Step(), Polarity.BOTH);

        net.addNetworkModel(input_ie);

//        Sparse ie_input_con = new Sparse(0.01, true, false);
//        SynapseGroup ie_input = new SynapseGroup2(ngIn, input, input_ie_con, 1.0, exRand, inRand);
//        ie_input.setLabel("Inhibitory -> Input");
//        ie_input.setSpikeResponder(new Step(), Polarity.BOTH);
//        network.addGroup(ie_input);

        layout = new GridLayout(gridSpace, gridSpace, (int) Math.sqrt(0.2 * numNeurons));
        ngIn.setLayout(layout);
        //int x_loc = (int) (Math.sqrt(numNeurons) * gridSpace + 300);
        ngIn.applyLayout(new Point(x_loc, 10));

        for (Neuron n : neurons) {
            normalizeInhibitoryFanIn(n);
        }
        for (Neuron n : inhibitoryNeurons) {
            normalizeExcitatoryFanIn(n);
        }

//        for (Neuron n : input.getNeuronList()) {
//            n.normalizeInhibitoryFanIn();
//            n.normalizeExcitatoryFanIn();
//        }

        // net.getUpdateManager().clear();
        // net.getUpdateManager().addAction(ConcurrentBufferedUpdate.createConcurrentBufferedUpdate(net));
        // net.updateTimeType();
    }

    // TODO: Possibly move to NetworkUtils.kt as extension functions
    /**
     * Normalizes the excitatory synaptic strengths impinging on this neuron,
     * that is finds the sum of the exctiatory weights and divides each weight
     * value by that sum.
     */
    public static void normalizeExcitatoryFanIn(Neuron neuron) {
        double sum = 0;
        double str = 0;
        for (int i = 0, n = neuron.getFanIn().size(); i < n; i++) {
            str = neuron.getFanIn().get(i).getStrength();
            if (str > 0) {
                sum += str;
            }
        }
        Synapse s = null;
        for (int i = 0, n = neuron.getFanIn().size(); i < n; i++) {
            s = neuron.getFanIn().get(i);
            str = s.getStrength();
            if (str > 0) {
                s.setStrength(s.getStrength() / sum);
            }
        }
    }

    // TODO: Possibly move to NetworkUtils.kt as extension functions
    public static void normalizeInhibitoryFanIn(Neuron neuron) {
        double sum = 0;
        double str = 0;
        for (int i = 0, n = neuron.getFanIn().size(); i < n; i++) {
            str = neuron.getFanIn().get(i).getStrength();
            if (str < 0) {
                sum -= str;
            }
        }
        Synapse s = null;
        for (int i = 0, n =  neuron.getFanIn().size(); i < n; i++) {
            s =  neuron.getFanIn().get(i);
            str = s.getStrength();
            if (str < 0) {
                s.setStrength(s.getStrength() / sum);
            }
        }
    }

    private static SynapseGroup connectGroups(Network network, NeuronGroup src, NeuronGroup tar, int kIn,
                                              ProbabilityDistribution random, Polarity polarity, String label) {
        double excRat = polarity != Polarity.INHIBITORY ? 1.0 : 0.0;
        FixedDegree connector = new FixedDegree();
        connector.setDirection(Direction.IN);
        connector.setDegree(kIn);
        // connector.setSelectMethod(SelectionStyle.IN);
        // connector.setConMethod(RadialSimple.ConnectStyle.DETERMINISTIC);
        // src.setPolarity(polarity);
        // connector.setExcCons(kIn);
        // connector.setInhCons(kIn);
        SynapseGroup sg = new SynapseGroup(src, tar, connector);
        sg.getConnectionStrategy().setPercentExcitatory(excRat);
        sg.getConnectionStrategy().setExRandomizer(random);
        sg.getConnectionStrategy().setInRandomizer(random);
        sg.setLabel(label);
        // TODO
        // sg.setSpikeResponder(new Step(),
        //         SimbrainConstants.Polarity.BOTH);
        network.addNetworkModel(sg);
        return sg;
    }

    @Override
    public String getName(){
        return "Self-Organizing Recurrent Network";
    }

    private String getSubmenuName() {
        return "Brain";
    }
}
