package org.simbrain.custom_sims.simulations.patterns_of_activity;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.simulations.utils.ColorPlotKt;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.connections.ConnectionStrategy;
import org.simbrain.network.connections.RadialGaussian;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.HexagonalGridLayout;
import org.simbrain.network.neuron_update_rules.KuramotoRule;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.piccolo.TMXUtils;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import java.util.ArrayList;
import java.util.List;


/**
 * Simulate a reservoir of Kuramoto oscillators exposed to smell inputs
 * and visualize the "cognitive maps" that develop in a PCA projetion labelled
 * by environmental inputs.
 */
public class KuramotoOscillators extends RegisteredSimulation {

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
    private int maxDly = 12;
    private int dispersion = 140;

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

    }

    private void setUpNetwork() {

        // Set up network
        NetworkComponent nc = sim.addNetwork(10,10,337,499,
            "Patterns of Activity");
        net = nc.getNetwork();
        net.setTimeStep(0.5);

        // Main recurrent net
        List<Neuron> neuronList = new ArrayList<>();
        for (int ii = 0; ii < netSize; ++ii) {
            Neuron n = new Neuron(net);
            n.setUpdateRule(new KuramotoRule());
            if (Math.random() < 0.5) {
                n.setPolarity(Polarity.EXCITATORY);
                //((KuramotoRule) n.getUpdateRule()).setAddNoise(true);
            } else {
                n.setPolarity(Polarity.INHIBITORY);
                //((KuramotoRule) n.getUpdateRule()).setAddNoise(true);
            }
            ((KuramotoRule) n.getUpdateRule()).setNaturalFrequency(.1);
            //((KuramotoRule) n.getUpdateRule()).setNoiseGenerator(NormalDistribution.builder()
            //    .mean(0).standardDeviation(0.2).build());
            neuronList.add(n);
        }
        reservoirNet = new NeuronGroup(net, neuronList);
        reservoirNet.setLayout(new HexagonalGridLayout(spacing, spacing, (int) Math.sqrt(neuronList.size())));
        net.addNetworkModel(reservoirNet);
        reservoirNet.setLocation(150,-242);
        reservoirNet.applyLayout(-5, -85);
        reservoirNet.setLabel("Recurrent Layer");

        // Set up recurrent synapses
        //EdgeOfChaos.connectReservoir(network, reservoirNet);

        ConnectionStrategy recConnection = new RadialGaussian(RadialGaussian.DEFAULT_EE_CONST * 1, RadialGaussian.DEFAULT_EI_CONST * 3,
            RadialGaussian.DEFAULT_IE_CONST * 3, RadialGaussian.DEFAULT_II_CONST * 0,
            50);
        SynapseGroup recSyns = SynapseGroup.createSynapseGroup(reservoirNet, reservoirNet, recConnection);
        net.addNetworkModel(recSyns);
        recSyns.setLabel("Recurrent");

        // Inputs
        inputNetwork = net.addNeuronGroup(1, 1, 3);
        inputNetwork.setLocation(reservoirNet.getCenterX() - inputNetwork.getWidth()/2, reservoirNet.getMaxY()+150);
        inputNetwork.setLowerBound(-100);
        inputNetwork.setUpperBound(100);
        net.addNetworkModel(inputNetwork);
        inputNetwork.setLabel("Sensory Neurons");

        // Inputs to reservoir
        SynapseGroup inpSynG = SynapseGroup.createSynapseGroup(inputNetwork, reservoirNet,
            new Sparse(0.7, true, false));
        // TODO
        // inpSynG.setStrength(40, Polarity.EXCITATORY);
        // inpSynG.setRandomizers(NormalDistri bution.builder().mean(10).standardDeviation(2.5).build(),
        //     NormalDistribution.builder().mean(-10).standardDeviation(2.5).build());
        inpSynG.randomize();
        inpSynG.setDisplaySynapses(false);
        net.addNetworkModel(inpSynG);

        // Couple from mouse to input nodes
        sim.couple(smellSensor, inputNetwork);

        // Prediction net
        predictionRes = net.addNeuronGroup(1,1,  netSize);
        HexagonalGridLayout.layoutNeurons(predictionRes.getNeuronList(), spacing, spacing);
        predictionRes.setLocation(reservoirNet.getMinX()-reservoirNet.getWidth() - 300, reservoirNet.getMinY()-100);
        predictionRes.setLabel("Predicted States");
        predictionRes.setLowerBound(-10);
        predictionRes.setUpperBound(10);
        predictionSg = net.addSynapseGroup(reservoirNet, predictionRes);

        // Train prediction network
        net.addUpdateAction((new TrainPredictionNet(this)));

        // Error
        errorNeuron = net.addNeuron((int) predictionRes.getMinX()-70, (int) predictionRes.getMinY()-45);
        errorNeuron.setClamped(true);
        errorNeuron.setLabel("Error");

        // "Halo" based on prediction error
        sim.getWorkspace().addUpdateAction(ColorPlotKt.createColorPlotUpdateAction(
                plot.getProjector(),
                predictionRes,
                errorNeuron.getActivation()
        ));

    }

    private void setUpWorld() {

        // Set up odor world
        OdorWorldComponent oc = sim.addOdorWorld(346,10,510,499, "World");
        oc.getWorld().setObjectsBlockMovement(false);
        oc.getWorld().setUseCameraCentering(false);
        oc.getWorld().setTileMap(TMXUtils.loadTileMap("empty.tmx"));

        // Mouse
        mouse = oc.getWorld().addEntity(202, 176, EntityType.MOUSE);
        smellSensor = new SmellSensor(mouse, "Smell-Center", 0, 0);
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
        plot = sim.addProjectionPlot(855,10,452,499,"Cognitive Map");
        plot.getProjector().init(reservoirNet.size());
        plot.getProjector().setTolerance(1);
        //plot.getProjector().setUseColorManager(false);
        Producer inputProducer = sim.getProducer(reservoirNet, "getActivations");
        Consumer plotConsumer = sim.getConsumer(plot, "addPoint");
        sim.couple(inputProducer, plotConsumer);

        // Text of nearest world object to projection plot current dot
        Producer currentObject = sim.getProducer(mouse, "getNearbyObjects");
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
    public String getSubmenuName() {
        return "Cognitive Maps";
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