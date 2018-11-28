package org.simbrain.custom_sims.simulations.patterns_of_activity;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.custom_sims.helper_classes.PlotBuilder;
import org.simbrain.custom_sims.simulations.edge_of_chaos.EdgeOfChaos;
import org.simbrain.network.connections.ConnectionStrategy;
import org.simbrain.network.connections.RadialGaussian;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.HexagonalGridLayout;
import org.simbrain.network.neuron_update_rules.KuramotoRule;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.ObjectSensor;
import org.simbrain.world.odorworld.sensors.Sensor;

import java.util.ArrayList;
import java.util.List;


/**
 * Simulate a set of oscillatory brain networks and display their projected
 * activity when exposed to inputs in a simple 2d world.
 */
public class ModularOscillatoryNetwork extends RegisteredSimulation {

    // References
    Network network;
    NetBuilder netBuilder;
    PlotBuilder plotBuilder;
    NeuronGroup sensory, motor, inputGroup;
    OdorWorldEntity mouse;
    List<OdorWorldEntity> worldEntities = new ArrayList<>();

    private int dispersion = 140;

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Set up world
        setUpWorld();

        // Set up network
        setUpNetwork();

        // Set up separate projections for each module
        addProjection(inputGroup, 8, 304, .01);
        addProjection(sensory, 359, 304, 1);
        addProjection(motor, 706, 304, 1);

        // Set up workspace updating
        //sim.getWorkspace().addUpdateAction((new ColorPlotKuramoto(this)));

    }

    private void setUpWorld() {

        OdorWorldBuilder world = sim.addOdorWorldTMX(591, 0, 459, 300, "empty.tmx");

        // Mouse
        mouse = world.addEntity(187, 113, OdorWorldEntity.EntityType.MOUSE);

        // Objects
        OdorWorldEntity cheese = world.addEntity(315, 31, OdorWorldEntity.EntityType.SWISS);
        worldEntities.add(cheese);
        OdorWorldEntity flower = world.addEntity(41, 31, OdorWorldEntity.EntityType.FLOWER);
        flower.getSmellSource().setDispersion(dispersion);
        worldEntities.add(flower);

        // Add sensors
        for(OdorWorldEntity entity : worldEntities) {
            ObjectSensor sensor = new ObjectSensor(mouse, entity.getEntityType());
            sensor.getDecayFunction().setDispersion(dispersion);
            mouse.addSensor(sensor);
        }
    }

    private void setUpNetwork() {

        // Set up network
        netBuilder = sim.addNetwork(10, 10, 581, 297,
            "Patterns of Activity");
        network = netBuilder.getNetwork();

        // Sensory network
        sensory = addModule(-115,10,49, "Sensory");
        //SynapseGroup recSensory = connectRadialGaussian(sensory,sensory);
        SynapseGroup recSensory = EdgeOfChaos.connectReservoir(network, sensory);
        recSensory.setLabel("Recurrent Sensory");

        // Motor Network
        motor = addModule(322, 10,16, "Motor");
        SynapseGroup recMotor = connectRadialGaussian(motor,motor);
        //SynapseGroup recMotor = EdgeOfChaos.connectReservoir(network, motor);
        recMotor.setLabel("Recurrent Motor");

        // Sensori-Motor Connection
        connectModules(sensory, motor, .5);

        // Input Network
        inputGroup = addInputGroup(-336, 30);

        // Input to Sensory Connection
        connectModules(inputGroup, sensory, .5);

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
        oscillator.setNaturalFrequency(.8);
        ng.setNeuronType(oscillator);
        HexagonalGridLayout.layoutNeurons(ng.getNeuronListUnsafe(), 40, 40);
        ng.setLocation(x,y);
        ng.setLabel(name);
        return ng;
    }

    private SynapseGroup connectRadialGaussian(NeuronGroup sourceNg, NeuronGroup targetNg) {
        SynapseGroup sg = new SynapseGroup(sourceNg, targetNg);
        ConnectionStrategy radialConnection = new RadialGaussian(RadialGaussian.DEFAULT_EE_CONST * 2, RadialGaussian.DEFAULT_EI_CONST * 1,
            RadialGaussian.DEFAULT_IE_CONST * 3, RadialGaussian.DEFAULT_II_CONST * 0,
            50);
        radialConnection.connectNeurons(sg);
        network.addGroup(sg);
        sg.setDisplaySynapses(false);
        return sg;
    }

    private SynapseGroup connectModules(NeuronGroup sourceNg, NeuronGroup targetNg, double density) {
        Sparse sparse = new Sparse();
        sparse.setConnectionDensity(density);
        sparse.setExcitatoryRatio(1);
        SynapseGroup sg = netBuilder.addSynapseGroup(sourceNg, targetNg, sparse);
        sg.setDisplaySynapses(false);
        return sg;
    }

    private void addProjection(NeuronGroup toPlot, int x, int y, double tolerance) {

        // Create projection component
        plotBuilder = sim.addProjectionPlot(x,y,362,320,toPlot.getLabel());
        plotBuilder.getProjectionModel().init(toPlot.size());
        plotBuilder.getProjectionModel().getProjector().setTolerance(tolerance);
        //plot.getProjectionModel().getProjector().useColorManager = false;

        // Coupling
        Producer inputProducer = sim.getProducer(toPlot, "getActivations");
        Consumer plotConsumer = sim.getConsumer(plotBuilder.getProjectionPlotComponent(), "addPoint");
        sim.tryCoupling(inputProducer, plotConsumer);

        //text of nearest world object to projection plot current dot
        Producer currentObject = sim.getProducer(mouse, "getNearbyObjects");
        Consumer plotText = sim.getConsumer(plotBuilder.getProjectionPlotComponent(), "setLabel");
        sim.tryCoupling(currentObject, plotText);

    }

    public ModularOscillatoryNetwork(SimbrainDesktop desktop) {
        super(desktop);
    }

    public ModularOscillatoryNetwork() {
        super();
    }

    @Override
    public String getName() {
        return "Modular Oscillatory Network";
    }

    @Override
    public ModularOscillatoryNetwork instantiate(SimbrainDesktop desktop) {
        return new ModularOscillatoryNetwork(desktop);
    }
}