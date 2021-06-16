package org.simbrain.custom_sims.simulations.sorn;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.NetworkWrapper;
import org.simbrain.network.connections.RadialSimple;
import org.simbrain.network.connections.RadialSimple.SelectionStyle;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.update_actions.ConcurrentBufferedUpdate;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.workspace.gui.SimbrainDesktop;

import java.awt.*;
import java.util.ArrayList;

public class SORN extends RegisteredSimulation {


    private int numNeurons;

    private int gridSpace = 30;

    private int connRadius = 400;

    private int eeKIn = 25;

    private int ieKIn;// = (int)(0.2*numNeurons/10);

    private int eiKIn;// = (int)(numNeurons/50);

    private ProbabilityDistribution defWtPD = UniformDistribution.builder()
            .floor(0).ceil(1).build();
    
    private Network network;

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
        NetworkWrapper net = sim.addNetwork(10, 10, 543, 545,
                "Patterns of Activity");
        network = net.getNetwork();
        network.setTimeStep(1);
        buildNetwork();


    }

    public void buildNetwork() {
        ArrayList<Neuron> neurons = new ArrayList<>();
        ArrayList<Neuron> inhibitoryNeurons = new ArrayList<>();
        SORNNeuronRule sornRule = new SORNNeuronRule();
      //  sornRule.sethIP(400.0/numNeurons);
        for (int i = 0; i < numNeurons; i++) {
            Neuron n = new Neuron(network);
         //   sornRule.setMaxThreshold(0.5);
         //   sornRule.setThreshold(0.5 * Math.random() + 0.01);
            sornRule.setRefractoryPeriod(1);
            sornRule.setAddNoise(true);
            n.setPolarity(Polarity.EXCITATORY);
            n.setUpdateRule(sornRule.deepCopy());
            neurons.add(n);
        }
        SORNNeuronRule str = new SORNNeuronRule();
        for (int i = 0; i < (int) (numNeurons * 0.2); i++) {
            Neuron n = new Neuron(network);
           // str.setThreshold(0.8 * Math.random() + 0.01);
            str.setEtaIP(0); // No Homeostatic Plasticity
            str.setRefractoryPeriod(1);
            str.setAddNoise(true);
            n.setPolarity(Polarity.INHIBITORY);
            n.setUpdateRule(str.deepCopy());
            inhibitoryNeurons.add(n);
        }

        NeuronGroup ng = new NeuronGroup(network, neurons);
        GridLayout layout = new GridLayout(gridSpace, gridSpace, (int) Math.sqrt(numNeurons));
        ng.setLabel("Excitatory");
        network.addNetworkModel(ng);
        ng.setLayout(layout);
        ng.applyLayout(new Point(10, 10));

        NeuronGroup ngIn = new NeuronGroup(network, inhibitoryNeurons);
        layout = new GridLayout(gridSpace*2, gridSpace*2, (int) Math.sqrt(0.2 * numNeurons));
        ngIn.setLabel("Inhibitory");
        network.addNetworkModel(ngIn);
        ngIn.setLayout(layout);
        System.out.println(ngIn.size());
        int x_loc = (int) (Math.sqrt(numNeurons) * gridSpace + 300);
        ngIn.applyLayout(new Point(10, 10));

        defWtPD.setPolarity(Polarity.EXCITATORY);
        SynapseGroup sg_ee = connectGroups(network, ng, ng, eeKIn, defWtPD, Polarity.EXCITATORY,
                "Exc. \u2192 Exc.");
        connectGroups(network, ngIn, ng, ieKIn,
                UniformDistribution.builder().floor(-1).ceil(0).polarity(Polarity.INHIBITORY).build(),
                Polarity.INHIBITORY,
                "Inh. \u2192 Exc.");
        connectGroups(network, ng, ngIn, eiKIn, defWtPD, Polarity.EXCITATORY,
                "Exc. \u2192 Inh.");

        // Normalize newly created inhibitory connections
        for (Neuron n : neurons) {
            ((SORNNeuronRule) n.getUpdateRule()).init(n);
        }
        // Set up plasticity between exc and exc neurons
        AddSTDPRule stdp = new AddSTDPRule();
        stdp.setLearningRate(0.001);
        // TODO
        // sg_ee.setLearningRule(stdp, SimbrainConstants.Polarity.BOTH);

        ArrayList<Neuron> inNeurons = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            Neuron n = new Neuron(network);
            //SpikingThresholdRule inRule = new SpikingThresholdRule();
            //inRule.setThreshold(0.96);
            sornRule.setMaxThreshold(0.5);
            sornRule.setThreshold(0.5 * Math.random() + 0.01);
            n.setPolarity(Polarity.EXCITATORY);
            n.setUpdateRule(sornRule.deepCopy());
            inNeurons.add(n);
        }

        NeuronGroup input = new NeuronGroup(network, inNeurons);
        layout = new GridLayout(gridSpace, gridSpace, (int) Math.sqrt(0.4 * numNeurons));
        input.setLabel("Input");
        network.addNetworkModel(input);
        input.setLayout(layout);
        // Todo; get current location of ng above
        int y_loc = (int) (Math.sqrt(numNeurons) * gridSpace + 200);
        input.applyLayout(new Point(x_loc, y_loc));

        Sparse input_ee_con = new Sparse(0.05, false, false);
        SynapseGroup input_ee = SynapseGroup.createSynapseGroup(input, ng, input_ee_con,
                1.0, defWtPD, defWtPD);
        input_ee.setLabel("Input -> Excitatory");
        // TODO
        // input_ee.setLearningRule(stdp, Polarity.BOTH);
        // input_ee.setSpikeResponder(new Step(), Polarity.BOTH);
        network.addNetworkModel(input_ee);

//        Sparse ee_input_con = new Sparse(0.01, false, false);
//        SynapseGroup ee_input = SynapseGroup.createSynapseGroup(ng, input, ee_input_con, 1.0, exRand, inRand);
//        ee_input.setLabel("Excitatory -> Input");
//        ee_input.setLearningRule(stdp, Polarity.BOTH);
//        ee_input.setSpikeResponder(new Step(), Polarity.BOTH);
//        network.addGroup(ee_input);

        Sparse input_ie_con = new Sparse(0.05, true, false);
        SynapseGroup input_ie = SynapseGroup.createSynapseGroup(input, ngIn,
                input_ie_con, 1.0, defWtPD, defWtPD);
        input_ie.setLabel("Input -> Inhibitory");
        // TODO
        // input_ie.setSpikeResponder(new Step(), Polarity.BOTH);

        network.addNetworkModel(input_ie);

//        Sparse ie_input_con = new Sparse(0.01, true, false);
//        SynapseGroup ie_input = SynapseGroup.createSynapseGroup(ngIn, input, input_ie_con, 1.0, exRand, inRand);
//        ie_input.setLabel("Inhibitory -> Input");
//        ie_input.setSpikeResponder(new Step(), Polarity.BOTH);
//        network.addGroup(ie_input);

        layout = new GridLayout(gridSpace, gridSpace, (int) Math.sqrt(0.2 * numNeurons));
        ngIn.setLayout(layout);
        //int x_loc = (int) (Math.sqrt(numNeurons) * gridSpace + 300);
        ngIn.applyLayout(new Point(x_loc, 10));

        for (Neuron n : neurons) {
            n.normalizeInhibitoryFanIn();
        }
        for (Neuron n : inhibitoryNeurons) {
            n.normalizeExcitatoryFanIn();
        }

//        for (Neuron n : input.getNeuronList()) {
//            n.normalizeInhibitoryFanIn();
//            n.normalizeExcitatoryFanIn();
//        }

        network.getUpdateManager().clear();
        network.getUpdateManager().addAction(ConcurrentBufferedUpdate.createConcurrentBufferedUpdate(network));
        network.updateTimeType();
    }

    private static SynapseGroup connectGroups(Network network, NeuronGroup src, NeuronGroup tar, int kIn,
                               ProbabilityDistribution random, Polarity polarity, String label) {
        double excRat = polarity != Polarity.INHIBITORY ? 1.0 : 0.0;
        RadialSimple connector = new RadialSimple();
        connector.setSelectMethod(SelectionStyle.IN);
        connector.setConMethod(RadialSimple.ConnectStyle.DETERMINISTIC);
        src.setPolarity(polarity);
        connector.setExcCons(kIn);
        connector.setInhCons(kIn);
        SynapseGroup sg = SynapseGroup
                .createSynapseGroup(src, tar, connector, excRat, random, random);
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

    @Override
    public String getSubmenuName() {
        return "Brain";
    }
}
