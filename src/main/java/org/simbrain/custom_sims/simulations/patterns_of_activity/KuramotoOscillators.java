package org.simbrain.custom_sims.simulations.patterns_of_activity;

import org.simbrain.custom_sims.Simulation;
import org.simbrain.custom_sims.simulations.edge_of_chaos.EdgeOfChaos;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.SynapseGroup;
import org.simbrain.network.layouts.HexagonalGridLayout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.neurongroups.NeuronGroup;
import org.simbrain.network.updaterules.KuramotoRule;
import org.simbrain.network.updaterules.LinearRule;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.piccolo.TMXUtils;
import org.simbrain.util.stats.distributions.NormalDistribution;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import java.util.ArrayList;
import java.util.List;

import static org.simbrain.network.core.NetworkUtilsKt.addNeuronGroup;


/**
 * Simulate a reservoir of neurons exposed to smell inputs
 * and visualize the "cognitive maps" that develop in a PCA projetion labelled
 * by environmental inputs.
 */
public class KuramotoOscillators extends Simulation {

    // References
    Network net;
    ProjectionComponent plot;
    NeuronGroup reservoirNet, predictionRes, inputNetwork;
    SynapseGroup predictionSg;
    Neuron errorNeuron;

    OdorWorldEntity mouse;
    SmellSensor smellSensor;

    private int netSize = 50;
    private int spacing = 40;
    private int dispersion = 140;

    private long seed = 42L;


    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Set up world
        setUpWorld();

        // Set up network
        setUpNetwork();

        // Set up plot
        setUpProjectionPlot();

        // // "Halo" based on prediction error
        // sim.getWorkspace().addUpdateAction(ColorPlotKt.createColorPlotUpdateAction(
        //         plot.getProjector(),
        //         predictionRes,
        //         errorNeuron.getActivation()
        // ));

    }

    private void setUpNetwork() {

        // Set up network
        NetworkComponent nc = sim.addNetwork(10, 10, 443, 450,
            "Patterns of Activity");
        net = nc.getNetwork();
        net.setTimeStep(0.5);

        // Main recurrent net
        List<Neuron> neuronList = new ArrayList<>();
        for (int ii = 0; ii < netSize; ++ii) {
            Neuron n = new Neuron();
            // Allostatic also works pretty nicely here
            n.setUpdateRule(new KuramotoRule());
            if (Math.random() < 0.5) {
                n.setPolarity(Polarity.EXCITATORY);
            } else {
                n.setPolarity(Polarity.INHIBITORY);
            }
            // ((KuramotoRule) n.getUpdateRule()).setNaturalFrequency(.1);
            //((KuramotoRule) n.getUpdateRule()).setNoiseGenerator(NormalDistribution.builder()
            //    .mean(0).standardDeviation(0.2).build());
            neuronList.add(n);
        }
        reservoirNet = new NeuronGroup(neuronList);
        reservoirNet.setLayout(new HexagonalGridLayout(spacing, spacing, (int) Math.sqrt(neuronList.size())));
        net.addNetworkModel(reservoirNet);
        reservoirNet.setLocation(185,50);
        reservoirNet.applyLayout(-5, -85);
        reservoirNet.setLabel("Reservoir");

        // Set up recurrent synapses
        var recurrentSyns = EdgeOfChaos.connectReservoir(net, reservoirNet, .1, 4, seed);
        recurrentSyns.setLabel("Synapses");

        // ConnectionStrategy recConnection = new RadialGaussian(DEFAULT_EE_CONST * 1, DEFAULT_EI_CONST * 3,
        //     DEFAULT_IE_CONST * 3, DEFAULT_II_CONST * 0, .25, 50);
        // SynapseGroup2 recSyns = new SynapseGroup2(reservoirNet, reservoirNet, recConnection);
        // net.addNetworkModelAsync(recSyns);
        // recSyns.setLabel("Recurrent");

        // Inputs
        inputNetwork = addNeuronGroup(net, 1, 1, 3);
        inputNetwork.setLowerBound(-100);
        inputNetwork.setUpperBound(100);
        inputNetwork.getNeuronList().stream().map(Neuron::getUpdateRule).forEach(r -> {
            var linearRule = (LinearRule) r;
            linearRule.setAddNoise(true);
            linearRule.setNoiseGenerator(new NormalDistribution(0.0, .1));
        });
        inputNetwork.setLabel("Sensory Neurons");
        inputNetwork.setLayout(new LineLayout());
        inputNetwork.applyLayout();
        net.addNetworkModel(inputNetwork);

        // Inputs to reservoir
        var sparseExcitatory = new Sparse(0.7, true, false);
        sparseExcitatory.setPercentExcitatory(100);
        SynapseGroup inputToRes = new SynapseGroup(inputNetwork, reservoirNet, sparseExcitatory);

        inputToRes.getConnectionStrategy().setExRandomizer(new NormalDistribution(10.0, 1.0));
        inputToRes.setDisplaySynapses(false);
        net.addNetworkModel(inputToRes);
        inputToRes.randomizeExcitatory();

        inputNetwork.setLocation(130, 660);

        // Couple from mouse to input nodes
        sim.couple(smellSensor, inputNetwork);

        // Prediction net
        // predictionRes = addNeuronGroup(net, 1,1,  netSize);
        // HexagonalGridLayout.layoutNeurons(predictionRes.getNeuronList(), spacing, spacing);
        // predictionRes.setLocation(500, 63);
        // predictionRes.setLabel("Predicted States");
        // predictionRes.setLowerBound(-10);
        // predictionRes.setUpperBound(10);
        // predictionSg = addSynapseGroup(net, reservoirNet, predictionRes);

        // Train prediction network
        // net.addUpdateAction((new TrainPredictionNet(this)));

        // Error
        // errorNeuron = addNeuron(net, 380, 500);
        // errorNeuron.setClamped(true);
        // errorNeuron.setLabel("Error");

    }

    private void setUpWorld() {

        // Set up odor world
        OdorWorldComponent oc = sim.addOdorWorld(445, 14, 426, 437, "World");
        oc.getWorld().setObjectsBlockMovement(false);
        oc.getWorld().setUseCameraCentering(false);
        oc.getWorld().setTileMap(TMXUtils.loadTileMap("empty.tmx"));

        // Mouse
        mouse = oc.getWorld().addEntity(202, 176, EntityType.MOUSE);
        smellSensor = new SmellSensor("Smell-Center", 0, 0);
        mouse.addSensor(smellSensor);
        mouse.setHeading(90);

        // Objects
        OdorWorldEntity cheese = oc.getWorld().addEntity(55, 306, EntityType.SWISS,
            new double[] {25,0,0});
        cheese.getSmellSource().setDispersion(dispersion);
        OdorWorldEntity flower = oc.getWorld().addEntity(351, 311, EntityType.FLOWER,
            new double[] {0,25,0});
        flower.getSmellSource().setDispersion(dispersion);
        OdorWorldEntity fish = oc.getWorld().addEntity(160, 14, EntityType.FISH,
            new double[] {0,0,25});
        fish.getSmellSource().setDispersion(dispersion);
    }

    private void setUpProjectionPlot() {

        // Projection of main reservoir
        plot = sim.addProjectionPlot2(858, 12, 460, 437,"Cognitive Map");
        plot.getProjector().setTolerance(20);
        //plot.getProjector().setUseColorManager(false);
        Producer inputProducer = sim.getProducer(reservoirNet, "getActivationArray");
        Consumer plotConsumer = sim.getConsumer(plot, "addPoint");
        sim.couple(inputProducer, plotConsumer);

        // Text of nearest world object to projection plot current dot
        Producer currentObject = sim.getProducer(mouse, "getNearbyObjectName");
        Consumer plotText = sim.getConsumer(plot, "setLabel");
        sim.couple(currentObject, plotText);

    }

    public KuramotoOscillators(SimbrainDesktop desktop) {
        super(desktop);
    }

    public KuramotoOscillators() {
        super();
    }

    @Override
    public String getName() {
        return "Kuramoto Oscillators";
    }

    @Override
    public KuramotoOscillators instantiate(SimbrainDesktop desktop) {
        return new KuramotoOscillators(desktop);
    }

}