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
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.HexagonalGridLayout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.neuron_update_rules.BinaryRule;
import org.simbrain.network.neuron_update_rules.DecayRule;
import org.simbrain.network.neuron_update_rules.KuramotoRule;
import org.simbrain.network.neuron_update_rules.NakaRushtonRule;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.EntityType;
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
        addProjection(sensory, 359, 304, .1);
        addProjection(motor, 706, 304, .5);

        // Set up workspace updating
        //sim.getWorkspace().addUpdateAction((new ColorPlotKuramoto(this)));

    }

    private void setUpNetwork() {

        // Set up network
        netBuilder = sim.addNetwork(10, 10, 581, 297,
            "Patterns of Activity");
        network = netBuilder.getNetwork();

        // Sensory network
        sensory = addModule(-115, 10, 49, "Sensory", new DecayRule());
        //SynapseGroup recSensory = connectRadialGaussian(sensory,sensory);
        SynapseGroup recSensory = EdgeOfChaos.connectReservoir(network, sensory, 1.9, 4);
        recSensory.setLabel("Recurrent Sensory");

        // Motor Network
        motor = addModule(322, 10, 16, "Motor", new KuramotoRule());
        //SynapseGroup recMotor = connectRadialGaussian(motor, motor);
        SynapseGroup recMotor = EdgeOfChaos.connectReservoir(network, motor, 1.9, 4);
        recMotor.setLabel("Recurrent Motor");

        // Sensori-Motor Connection
        connectModules(sensory, motor, .3, .5);

        // Input Network
        inputGroup = addInputGroup(-385, 107);

    }

    private NeuronGroup addInputGroup(int x, int y) {

        // Alternate form would be based on vectors
        NeuronGroup ng = netBuilder.addNeuronGroup(x, y, mouse.getSensors().size());
        ng.setLayout(new LineLayout(LineLayout.LineOrientation.VERTICAL));
        ng.applyLayout();
        ng.setLabel("Object Sensors");
        int i = 0;
        for (Sensor sensor : mouse.getSensors()) {
            Neuron neuron = ng.getNeuron(i++);
            neuron.setLabel(sensor.getLabel());
            neuron.setClamped(true);
            sim.couple((ObjectSensor) sensor, neuron);
        }

        // Hard coded for two input neurons
        Neuron neuron1 = ng.getNeuron(0);
        Neuron neuron2 = ng.getNeuron(1);

        // Make spatial connections to sensory group
        double yEdge = sensory.getCenterY();
        for (int j = 0; j < sensory.getNeuronList().size(); j++) {
            Neuron tarNeuron = sensory.getNeuronList().get(j);
            double yloc = tarNeuron.getY();
            if (yloc < yEdge) {
                netBuilder.connect(neuron1, tarNeuron,1);
            } else {
                netBuilder.connect(neuron2, tarNeuron,1);
            }
        }

        return ng;

    }

    private void connectSpatial(Neuron srcNeuron) {

    }

    private NeuronGroup addBinaryModule(int x, int y, int numNeurons, String name) {
        NeuronGroup ng = netBuilder.addNeuronGroup(x, y, numNeurons);
        BinaryRule rule = new BinaryRule();
        ng.setNeuronType(rule);
        HexagonalGridLayout.layoutNeurons(ng.getNeuronListUnsafe(), 40, 40);
        ng.setLocation(x, y);
        ng.setLabel(name);
        return ng;
    }

    private NeuronGroup addModule(int x, int y, int numNeurons, String name, NeuronUpdateRule rule) {
        NeuronGroup ng = netBuilder.addNeuronGroup(x, y, numNeurons);
        //KuramotoRule rule = new KuramotoRule();
        //NakaRushtonRule rule = new NakaRushtonRule();
        //rule.setNaturalFrequency(.1);
        ng.setNeuronType(rule);
        for (Neuron neuron : ng.getNeuronList()) {
            if (Math.random() < .5) {
                neuron.setPolarity(SimbrainConstants.Polarity.EXCITATORY);
            } else {
                neuron.setPolarity(SimbrainConstants.Polarity.INHIBITORY);
            }
        }
        HexagonalGridLayout.layoutNeurons(ng.getNeuronListUnsafe(), 40, 40);
        ng.setLocation(x, y);
        ng.setLabel(name);
        return ng;
    }

    private SynapseGroup connectRadialGaussian(NeuronGroup sourceNg, NeuronGroup targetNg) {
        ConnectionStrategy radialConnection = new RadialGaussian(RadialGaussian.DEFAULT_EE_CONST * 1, RadialGaussian.DEFAULT_EI_CONST * 2,
            RadialGaussian.DEFAULT_IE_CONST * 3, RadialGaussian.DEFAULT_II_CONST * 0,
            50);
        SynapseGroup sg = SynapseGroup.createSynapseGroup(sourceNg, targetNg, radialConnection);
        network.addGroup(sg);
        sg.setDisplaySynapses(false);
        return sg;
    }

    private SynapseGroup connectModules(NeuronGroup sourceNg, NeuronGroup targetNg, double density, double exRatio) {
        Sparse sparse = new Sparse();
        sparse.setConnectionDensity(density);
        SynapseGroup sg = SynapseGroup.createSynapseGroup(sourceNg, targetNg, sparse, exRatio, null, null);
        network.addGroup(sg);
        sg.setDisplaySynapses(false);
        return sg;
    }


    private void setUpWorld() {

        OdorWorldBuilder world = sim.addOdorWorldTMX(591, 0, 459, 300, "empty.tmx");

        // Mouse
        mouse = world.addEntity(187, 113, EntityType.MOUSE);

        // Objects
        OdorWorldEntity cheese = world.addEntity(315, 31, EntityType.SWISS);
        worldEntities.add(cheese);
        OdorWorldEntity flower = world.addEntity(41, 31, EntityType.FLOWER);
        flower.getSmellSource().setDispersion(dispersion);
        worldEntities.add(flower);

        // Add sensors
        for (OdorWorldEntity entity : worldEntities) {
            ObjectSensor sensor = new ObjectSensor(mouse, entity.getEntityType());
            sensor.getDecayFunction().setDispersion(dispersion);
            mouse.addSensor(sensor);
        }
    }

    private void addProjection(NeuronGroup toPlot, int x, int y, double tolerance) {

        // Create projection component
        plotBuilder = sim.addProjectionPlot(x, y, 362, 320, toPlot.getLabel());
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