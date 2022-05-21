package org.simbrain.custom_sims.simulations.cortex;

import org.simbrain.custom_sims.Simulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseGroup2;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.updaterules.IntegrateAndFireRule;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.stats.ProbabilityDistribution;
import org.simbrain.util.stats.distributions.LogNormalDistribution;
import org.simbrain.workspace.gui.SimbrainDesktop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Model of canonical cortex (Douglas and Martin, 2004) using rat barrel cortex
 * as a reference (Lefort, Tomm, Sarria and Petersen, 2009). Users should be
 * able to inject current and see it propagate consistently with empirical
 * studies.
 * <p>
 * Also see Haeusler and Mass, 2007.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
public class CortexSimple extends Simulation {

    // Simulation Parameters
    int NUM_NEURONS = 120;
    int GRID_SPACE = 25;

    // Location and scale params for lognormal dist of all synapse groups
    double location = -1.0;
    double scale = .35;
    double exlocation = 0;
    double exscale = .5;

    double inlocation = 1;
    double inscale = .5;
    int numNeuPerLay = 300;

    // TODO: Membrane properties
    // TODO: Build using z coordinates

    // References
    Network net;

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Build network
        NetworkComponent nc = sim.addNetwork(10, 10, 550, 800,
            "Cortical Simulation");
        net = nc.getNetwork();
        buildNetwork();

        // Set up control panel
        // controlPanel();
    }

    private void controlPanel() {
        ControlPanel panel = ControlPanel.makePanel(sim, "Controller", 5, 10);
        panel.addButton("Inject current", () -> {
        });
    }


//    Group locations (503.25,-521.61). (-174.62,328.62). (481.16,1268.68).

    void buildNetwork() {

        net.setTimeStep(0.2);

        // Make the layers.  Params from Petersen, 2009.
        int btwnLayerSpacing = 150;
        // resting potential, time constant, threshold, resistance
        NeuronGroup layer_23 = buildLayer(numNeuPerLay,
            new double[] {-71.5, .35},
            new double[] {29, 0.45},
            new double[] {-38.4, 0.2},
            new double[] {190, 4});
        layer_23.setLabel("Layer 2/3");
        NeuronGroup layer_4 = buildLayer(numNeuPerLay,
            new double[] {-66, 0.3},
            new double[] {34.8, 0.5},
            new double[] {-39.7, 0.2},
            new double[] {302, 4});
        layer_4.setLabel("Layer 4");
        NeuronGroup layer_56 = buildLayer(numNeuPerLay,
            new double[] {-62.8, 0.2},
            new double[] {31.7, 0.65},
            new double[] {-40, 0.25},
            new double[] {187, 4});
        layer_56.setLabel("Layer 5/6");

        double[] tmp = new double[3];
        double defMax = layer_4.size() * 2;
        double[] xlim = {0, defMax};
        double[] zlim = {0, defMax};
        for (int ii = 0; ii < layer_4.size(); ++ii) {
            Polarity pol = Math.random() < 0.2 ? Polarity.INHIBITORY : Polarity.EXCITATORY;

            double[] ylim = new double[] {0, defMax};
            random3Position(tmp, xlim, ylim, zlim);
            layer_56.getNeuronList().get(ii).setPosition3D(tmp);
            layer_56.getNeuronList().get(ii).setPolarity(pol);

            ylim = new double[] {defMax + 100, 2 * defMax + 100};
            random3Position(tmp, xlim, ylim, zlim);
            layer_4.getNeuronList().get(ii).setPosition3D(tmp);
            layer_4.getNeuronList().get(ii).setPolarity(pol);

            ylim = new double[] {2 * defMax + 200, 3 * defMax + 200};
            random3Position(tmp, xlim, ylim, zlim);
            layer_23.getNeuronList().get(ii).setPosition3D(tmp);
            layer_23.getNeuronList().get(ii).setPolarity(pol);

        }
        layer_23.setLocation(500, 300);
        layer_4.setLocation(-150, 1120);
        layer_56.setLocation(500, 1850);

        // Connect layers
        Map<String, SynapseGroup2> synGroups = new HashMap<>();

        synGroups.put("L2/3 Rec.", connectLayers(layer_23, layer_23, .12));
        synGroups.put("L4 Rec.", connectLayers(layer_4, layer_4, .24));
        synGroups.put("L5/6 Rec.", connectLayers(layer_56, layer_56, .24));
        synGroups.put("L4 \u2192 L2/3", connectLayers(layer_4, layer_23, .14));
        synGroups.put("L2/3 \u2192 L4", connectLayers(layer_23, layer_4, .01));
        synGroups.put("L4 \u2192 L5/6", connectLayers(layer_4, layer_56, .08));
        synGroups.put("L5/6 \u2192 L4", connectLayers(layer_56, layer_4, .007));
        synGroups.put("L2/3 \u2192 L5/6", connectLayers(layer_23, layer_56, .08));
        synGroups.put("L5/6 \u2192 L2/3", connectLayers(layer_56, layer_23, .03));

        for (String sgn : synGroups.keySet()) {
            SynapseGroup2 sg = synGroups.get(sgn);
            for (Synapse s : sg.getSynapses()) {
                s.setDelay(this.getDelay(s.getSource().getPosition3D(), s.getTarget().getPosition3D(),
                    Math.sqrt(2 * (600 * 600) + (2000 * 2000)), 20));
            }
            sg.setLabel(sgn);
        }

        //TODO
        //layer_4.fireGroupUpdated();
        // Todo; Add labels

        // Use concurrent buffered update
        // net.getUpdateManager().clear();
        // net.getUpdateManager().addAction(ConcurrentBufferedUpdate
        //     .createConcurrentBufferedUpdate(net));
    }

    private NeuronGroup buildLayer(int numNeurons,
                                   double[] restingPotential, double[] timeConstant, double[] threshold,
                                   double[] resistance) {

        //GridLayout layout = new GridLayout(GRID_SPACE, GRID_SPACE,
        //    (int) Math.sqrt(numNeurons));
        List<Neuron> neurons = new ArrayList<Neuron>(numNeurons);
        ThreadLocalRandom locR = ThreadLocalRandom.current();
        for (int i = 0; i < numNeurons; i++) {
            Neuron neuron = new Neuron(net);
            IntegrateAndFireRule rule = new IntegrateAndFireRule();
            rule.setRestingPotential(restingPotential[0] + locR.nextDouble() * restingPotential[1]);
            rule.setTimeConstant(timeConstant[0] + locR.nextDouble() * timeConstant[1]);
            rule.setThreshold(threshold[0] + locR.nextDouble() * threshold[1]);
            rule.setResistance(resistance[0] + locR.nextDouble() * resistance[1]);
            rule.setBackgroundCurrent(0);
            rule.setResetPotential(restingPotential[0] + locR.nextDouble() * restingPotential[1]);
            neuron.setUpdateRule(rule);
            neuron.setLowerBound(rule.getRestingPotential() - 10);
            neuron.setUpperBound(rule.getThreshold());
            neurons.add(neuron);
        }
        NeuronGroup ng = new NeuronGroup(net, neurons);
        net.addNetworkModel(ng);
        //ng.setLayout(layout);
        //ng.applyLayout();
        return ng;
    }

    private SynapseGroup2 connectLayers(NeuronGroup src, NeuronGroup tar,
                                       double sparsity) {

        ProbabilityDistribution exRand = new LogNormalDistribution(exlocation, exscale, false);
        ProbabilityDistribution inRand = new LogNormalDistribution(exlocation, exscale, true);

        Sparse con = new Sparse(sparsity, false, false);

        SynapseGroup2 sg = SynapseGroup.createSynapseGroup(src, tar, con, 0.65);
        // sg.setRandomizers(exRand, inRand);
        sg.setLabel("Synapses");

        // TODO
        // sg.setUpperBound(200, Polarity.EXCITATORY);
        // sg.setLowerBound(0, Polarity.EXCITATORY);
        // sg.setLowerBound(-200, Polarity.INHIBITORY);
        // sg.setUpperBound(0, Polarity.INHIBITORY);
        //
        // sg.setSpikeResponder(new UDF(), Polarity.BOTH);

        net.addNetworkModel(sg);

        return sg;

    }

    private void random3Position(double[] data, double[] xlim, double[] ylim, double[] zlim) {
        data[0] = ThreadLocalRandom.current().nextDouble(xlim[0], xlim[1]);
        data[1] = ThreadLocalRandom.current().nextDouble(ylim[0], ylim[1]);
        data[2] = ThreadLocalRandom.current().nextDouble(zlim[0], zlim[1]);
    }

    public int getDelay(double[] xyz1, double[] xyz2, double maxDist, double maxDly) {
        double dist = SimbrainMath.distance(xyz1, xyz2);
        return (int) (((dist / maxDist) * maxDly) / net.getTimeStep());
    }

    public CortexSimple(SimbrainDesktop desktop) {
        super(desktop);
    }

    public CortexSimple() {
        super();
    }

    private String getSubmenuName() {
        return "Brain";
    }

    @Override
    public String getName() {
        return "Cortical circuit";
    }

    @Override
    public CortexSimple instantiate(SimbrainDesktop desktop) {
        return new CortexSimple(desktop);
    }

}