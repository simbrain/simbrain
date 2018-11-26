package org.simbrain.custom_sims.simulations.patterns_of_activity;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.custom_sims.helper_classes.PlotBuilder;
import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.ConnectionStrategy;
import org.simbrain.network.connections.RadialGaussian;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.HexagonalGridLayout;
import org.simbrain.network.neuron_update_rules.KuramotoRule;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.piccolo.TileMap;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Simulate a reservoir of Kuramoto oscillators exposed to smell inputs
 * and visualize the "cognitive maps" that develop in a PCA projetion labelled
 * by environmental inputs
 */
public class KuramotoOscillators extends RegisteredSimulation {

    // References
    Network network;
    PlotBuilder plot;
    NeuronGroup reservoirNet, predictionRes, sensoryNetL, sensoryNetR;
    SynapseGroup predictionSg;
    Neuron errorNeuron;

    OdorWorldEntity mouse;

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

        // Set up workspace updating
        sim.getWorkspace().addUpdateAction((new ColorPlotKuramoto(this)));

    }

    private void setUpNetwork() {

        // Set up network
        NetBuilder net = sim.addNetwork(10, 10, 543, 545,
            "Patterns of Activity");
        network = net.getNetwork();
        network.setTimeStep(0.5);

        // Main recurrent net
        List<Neuron> neuronList = new ArrayList<>();
        for (int ii = 0; ii < netSize; ++ii) {
            Neuron n = new Neuron(network);
            n.setUpdateRule(new KuramotoRule());
            if (Math.random() < 0.5) {
                n.setPolarity(Polarity.EXCITATORY);
                //((KuramotoRule) n.getUpdateRule()).setAddNoise(true);
            } else {
                n.setPolarity(Polarity.INHIBITORY);
                //((KuramotoRule) n.getUpdateRule()).setAddNoise(true);
            }
            //((KuramotoRule) n.getUpdateRule()).setAddNoise(true);
            //((KuramotoRule) n.getUpdateRule()).setNoiseGenerator(NormalDistribution.builder()
            //    .ofMean(0).ofStandardDeviation(0.2).build());
            neuronList.add(n);
        }
        reservoirNet = new NeuronGroup(network, neuronList);
        HexagonalGridLayout.layoutNeurons(reservoirNet.getNeuronListUnsafe(), spacing, spacing);
        reservoirNet.setLocation(150,-242);
        network.addGroup(reservoirNet);
        reservoirNet.setLabel("Recurrent Layer");

        // Set up recurrent synapses
        SynapseGroup recSyns = new SynapseGroup(reservoirNet, reservoirNet);
        ConnectionStrategy recConnection = new RadialGaussian(RadialGaussian.DEFAULT_EE_CONST * 3, RadialGaussian.DEFAULT_EI_CONST * 3,
            RadialGaussian.DEFAULT_IE_CONST * 3, RadialGaussian.DEFAULT_II_CONST * 3,
            20);
        recConnection.connectNeurons(recSyns);
        network.addGroup(recSyns);
        recSyns.setLabel("Recurrent");

        // Inputs
        sensoryNetL = net.addNeuronGroup(1, 1, 5);
        sensoryNetL.setLocation(reservoirNet.getMinX()-reservoirNet.getWidth() - 100, reservoirNet.getMaxY()+200);
        sensoryNetL.setLowerBound(-100);
        sensoryNetL.setUpperBound(100);

        sensoryNetR = net.addNeuronGroup(1, 1, 5);
        sensoryNetR.setLocation(reservoirNet.getMinX()+ reservoirNet.getWidth() + 100, reservoirNet.getMaxY()+200);
        sensoryNetR.setLowerBound(-100);
        sensoryNetR.setUpperBound(100);

        SynapseGroup inpSynGL = SynapseGroup.createSynapseGroup(sensoryNetL, reservoirNet,
            new Sparse(0.25, true, false));
        inpSynGL.setStrength(40, Polarity.EXCITATORY);
        inpSynGL.setStrength(-10, Polarity.INHIBITORY);
        for (Synapse s : inpSynGL.getAllSynapses()) {
            s.setDelay(ThreadLocalRandom.current().nextInt(2, maxDly/2));
        }
        SynapseGroup inpSynGR = SynapseGroup.createSynapseGroup(sensoryNetR, reservoirNet,
                new Sparse(0.25, true, false));
        inpSynGR.setStrength(40, Polarity.EXCITATORY);
        inpSynGL.setStrength(-10, Polarity.INHIBITORY);
        for (Synapse s : inpSynGR.getAllSynapses()) {
            s.setDelay(ThreadLocalRandom.current().nextInt(2, maxDly/2));
        }
        network.addGroup(inpSynGL);
        inpSynGL.setLabel("L. Sensor \u2192  Res.");
        network.addGroup(inpSynGR);
        inpSynGR.setLabel("R. Sensor \u2192  Res.");

        // Sensory Couplings
        sim.couple((SmellSensor) mouse.getSensor("Smell-Left"), sensoryNetL);
        sim.couple((SmellSensor) mouse.getSensor("Smell-Right"), sensoryNetR);
        network.addGroup(sensoryNetL);
        sensoryNetL.setLabel("Sensory Left");
        network.addGroup(sensoryNetR);
        sensoryNetR.setLabel("Sensory Right");

        // Motor Couplings
        // sim.couple(lfNeuron, mouse.getEffector("Go-left"));
        // sim.couple(rtNeuron, mouse.getEffector("Go-right"));
        // sim.couple(upNeuron, mouse.getEffector("Go-straight"));

        // Prediction net
        predictionRes = net.addNeuronGroup(1,1,  netSize);
        HexagonalGridLayout.layoutNeurons(predictionRes.getNeuronListUnsafe(), spacing, spacing);
        predictionRes.setLocation(reservoirNet.getMinX()-reservoirNet.getWidth() - 300, reservoirNet.getMinY()-100);
        predictionRes.setLabel("Predicted States");
        predictionRes.setLowerBound(-10);
        predictionRes.setUpperBound(10);
        predictionSg = net.addSynapseGroup(reservoirNet, predictionRes);

        // Some custom network updating
        network.addUpdateAction((new TrainPredictionNet(this)));

        // Error
        errorNeuron = net.addNeuron((int) predictionRes.getMinX()-70, (int) predictionRes.getMinY()-45);
        //errorNeuron.setClamped(true);
        errorNeuron.setLabel("Error");


    }

    private void setUpWorld() {

        // Set up odor world
        OdorWorldBuilder world = sim.addOdorWorld(547, 5, 504, 548, "World");
        world.getWorld().setObjectsBlockMovement(false);
        world.getWorld().setTileMap(TileMap.create("empty.tmx"));

        // Mouse
        mouse = world.addEntity(120, 245, OdorWorldEntity.EntityType.MOUSE);
        mouse.addSensor(new SmellSensor(mouse, "Smell-Right", Math.PI/5, 45));
        mouse.addSensor(new SmellSensor(mouse, "Smell-Left", -Math.PI/5, 45));
        mouse.setHeading(90);
        mouse.addDefaultSensorsEffectors();

        // Objects
        OdorWorldEntity cheese = world.addEntity(92, 220, OdorWorldEntity.EntityType.SWISS,
            new double[] {18, 0, 5, 10, 5});
        cheese.getSmellSource().setDispersion(dispersion);
        OdorWorldEntity flower = world.addEntity(190, 221, OdorWorldEntity.EntityType.FLOWER,
            new double[] {3, 18, 2, 5, 10});
        flower.getSmellSource().setDispersion(dispersion);
        OdorWorldEntity cow = world.addEntity(90, 50, OdorWorldEntity.EntityType.COW,
            new double[] {3, 7, 16, 19, 0});
        cow.getSmellSource().setDispersion(dispersion);
        OdorWorldEntity lion = world.addEntity(300, 54, OdorWorldEntity.EntityType.LION,
            new double[] {5, 2, 13, 16, 0});
        lion.getSmellSource().setDispersion(dispersion);
        OdorWorldEntity susi = world.addEntity(97, 331, OdorWorldEntity.EntityType.SUSI,
            new double[] {0, 12, 15, 20});
        susi.getSmellSource().setDispersion(dispersion);
        OdorWorldEntity steve = world.addEntity(315, 305, OdorWorldEntity.EntityType.STEVE,
            new double[] {12, 0, 20, 15});
        steve.getSmellSource().setDispersion(dispersion);

    }

    private void setUpProjectionPlot() {

        // Projection of main reservoir
        plot = sim.addProjectionPlot(31,21,450,419,"Reservoir States");
        plot.getProjectionModel().init(reservoirNet.size());
        plot.getProjectionModel().getProjector().setTolerance(5);
        plot.getProjectionModel().getProjector().useColorManager = false;
        Producer inputProducer = sim.getProducer(reservoirNet, "getActivations");
        Consumer plotConsumer = sim.getConsumer(plot.getProjectionPlotComponent(), "addPoint");
        sim.tryCoupling(inputProducer, plotConsumer);

        // Text of nearest world object to projection plot current dot
        Producer currentObject = sim.getProducer(mouse, "getNearbyObjects");
        Consumer plotText = sim.getConsumer(plot.getProjectionPlotComponent(), "setLabel");
        sim.tryCoupling(currentObject, plotText);

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