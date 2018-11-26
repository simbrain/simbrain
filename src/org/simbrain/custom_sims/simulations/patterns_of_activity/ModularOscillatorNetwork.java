package org.simbrain.custom_sims.simulations.patterns_of_activity;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.custom_sims.helper_classes.PlotBuilder;
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
import org.simbrain.world.odorworld.sensors.ObjectSensor;
import org.simbrain.world.odorworld.sensors.Sensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Simulate a set of oscillatory brain networks and display their projected
 * activity when exposed to inputs in a simple 2d world.
 */
public class ModularOscillatorNetwork extends RegisteredSimulation {

    // References
    Network network;
    NetBuilder netBuilder;

    PlotBuilder plot;
    NeuronGroup sensory, motor, inputGroup;
    Neuron rewardNeuron;

    OdorWorldEntity mouse;
    List<OdorWorldEntity> worldEntities = new ArrayList<>();

    private int dispersion = 140;

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Set up world
        addWorld();

        // Set up network
        addNetwork();

        // Set up separate projections for each module
        addProjection(inputGroup, 950, 400);
        addProjection(sensory, 950, 5);
        addProjection(motor, 500, 400);

        // Set up workspace updating
        //sim.getWorkspace().addUpdateAction((new ColorPlotKuramoto(this)));

    }

    private void addWorld() {

        OdorWorldBuilder world = sim.addOdorWorldTMX(547, 5,  400, 400, "largeWorld.tmx");

        // Mouse
        mouse = world.addEntity(120, 245, OdorWorldEntity.EntityType.MOUSE);

        // Objects
        OdorWorldEntity cheese = world.addEntity(92, 220, OdorWorldEntity.EntityType.SWISS);
        worldEntities.add(cheese);
        OdorWorldEntity flower = world.addEntity(290, 21, OdorWorldEntity.EntityType.FLOWER);
        flower.getSmellSource().setDispersion(dispersion);
        worldEntities.add(flower);

        // Add sensors
        for(OdorWorldEntity entity : worldEntities) {
            ObjectSensor sensor = new ObjectSensor(mouse, entity.getEntityType());
            sensor.getDecayFunction().setDispersion(dispersion);
            mouse.addSensor(sensor);
        }

    }

    private void addNetwork() {

        // Set up network
        netBuilder = sim.addNetwork(10, 10, 543, 545,
            "Patterns of Activity");
        network = netBuilder.getNetwork();
        network.setTimeStep(0.5);

        // Main network
        sensory = addModule(10,10,40, "Sensory");
        motor = addModule((int)sensory.getMinX(),
            (int)(sensory.getMinY() - sensory.getHeight()-200),40, "Motor");
        SynapseGroup sensoriMotor = connectModules(sensory, motor, .5);

        // Input Network
        inputGroup = addInputGroup((int) sensory.getCenterX()-20, (int) sensory.getMaxY()+150);
        SynapseGroup inputSensory = connectModules(inputGroup, sensory, .5);

        //TODO: Reward, Frontal

    }

    private NeuronGroup addInputGroup(int x, int y ) {

        // Alternate form would be based on vectors
        NeuronGroup ng = netBuilder.addNeuronGroup(x, y, mouse.getSensors().size());
        ng.setLabel("Object Sensors");
        int i = 0;
        for(Sensor sensor : mouse.getSensors()) {
            Neuron neuron = ng.getNeuron(i++);
            neuron.setLabel(sensor.getLabel());
            neuron.setClamped(true);
            sim.couple((ObjectSensor) sensor, neuron);
        }

        return ng;

    }


    private NeuronGroup addModule(int x, int y, int numNeurons, String name) {
        NeuronGroup ng = netBuilder.addNeuronGroup(x,y,numNeurons);
        KuramotoRule oscillator = new KuramotoRule();
        ng.setNeuronType(oscillator);
        HexagonalGridLayout.layoutNeurons(ng.getNeuronListUnsafe(), 40, 40);
        ng.setLocation(x,y);
        ng.setLabel(name);

        SynapseGroup recurrent = connectModules(ng, ng, .4);

        return ng;
    }

    private SynapseGroup connectModules(NeuronGroup sourceNg, NeuronGroup targetNg, double density) {
        Sparse sparse = new Sparse();
        sparse.setConnectionDensity(density);
        sparse.setExcitatoryRatio(.5); // Abstract this out?
        SynapseGroup sg = netBuilder.addSynapseGroup(sourceNg, targetNg, sparse);
        return sg;
    }

    private void addProjection(NeuronGroup toPlot, int x, int y) {

        // Create projection component
        plot = sim.addProjectionPlot(x,y,400,400,toPlot.getLabel());
        plot.getProjectionModel().init(toPlot.size());
        plot.getProjectionModel().getProjector().setTolerance(1);
        //plot.getProjectionModel().getProjector().useColorManager = false;

        // Coupling
        Producer inputProducer = sim.getProducer(toPlot, "getActivations");
        Consumer plotConsumer = sim.getConsumer(plot.getProjectionPlotComponent(), "addPoint");
        sim.tryCoupling(inputProducer, plotConsumer);

        //text of nearest world object to projection plot current dot
        //Producer currentObject = sim.getProducer(mouse, "getNearbyObjects");
        //Consumer plotText = sim.getConsumer(plot.getProjectionPlotComponent(), "setLabel");
        //sim.tryCoupling(currentObject, plotText);

    }

    public ModularOscillatorNetwork(SimbrainDesktop desktop) {
        super(desktop);
    }

    public ModularOscillatorNetwork() {
        super();
    }

    @Override
    public String getName() {
        return "Modular Oscillatory Network";
    }

    @Override
    public ModularOscillatorNetwork instantiate(SimbrainDesktop desktop) {
        return new ModularOscillatorNetwork(desktop);
    }

}