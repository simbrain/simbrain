package org.simbrain.simulation;

import java.util.Hashtable;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.workspace.AttributeManager;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.PotentialProducer;
import org.simbrain.workspace.UmatchedAttributesException;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.entities.RotatingEntity;
import org.simbrain.world.odorworld.sensors.Sensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;

//TODO: Document everything
public class Simulation {

    final SimbrainDesktop desktop;
    final Workspace workspace;

    Hashtable<Network, NetworkComponent> netMap = new Hashtable();
    Hashtable<OdorWorld, OdorWorldComponent> odorMap = new Hashtable();

    /**
     * @param desktop
     */
    public Simulation(SimbrainDesktop desktop) {
        super();
        this.desktop = desktop;
        this.workspace = desktop.getWorkspace();
    }

    /**
     * @return the desktop
     */
    public SimbrainDesktop getDesktop() {
        return desktop;
    }

    // Neurons to agent. So far just one to one
    public void couple(NeuronGroup ng, RotatingEntity entity) {
        AttributeManager producers = netMap.get(ng.getParentNetwork())
                .getAttributeManager();
        AttributeManager consumers = odorMap.get(entity.getParentWorld())
                .getAttributeManager();

        PotentialProducer straightProducer = producers.createPotentialProducer(
                ng.getNeuronList().get(0), "getActivation", double.class);
        PotentialProducer leftProducer = producers.createPotentialProducer(
                ng.getNeuronList().get(1), "getActivation", double.class);
        PotentialProducer rightProducer = producers.createPotentialProducer(
                ng.getNeuronList().get(2), "getActivation", double.class);

        PotentialConsumer straightConsumer = producers
                .createPotentialConsumer(entity, "goStraight", double.class);
        PotentialConsumer leftConsumer = producers
                .createPotentialConsumer(entity, "turnLeft", double.class);
        PotentialConsumer rightConsumer = producers
                .createPotentialConsumer(entity, "turnRight", double.class);

        Coupling straightCoupling = new Coupling(straightProducer,
                straightConsumer);
        Coupling leftCoupling = new Coupling(leftProducer, leftConsumer);
        Coupling rightCoupling = new Coupling(rightProducer, rightConsumer);

        try {
            workspace.getCouplingManager().addCoupling(straightCoupling);
            workspace.getCouplingManager().addCoupling(leftCoupling);
            workspace.getCouplingManager().addCoupling(rightCoupling);
        } catch (UmatchedAttributesException e) {
            e.printStackTrace();
        }
    }

    // Agent to neurons
    public void couple(RotatingEntity entity, NeuronGroup ng) {
        AttributeManager producers = odorMap.get(entity.getParentWorld())
                .getAttributeManager();
        AttributeManager consumers = netMap.get(ng.getParentNetwork())
                .getAttributeManager();

        PotentialProducer sensoryProducer = producers.createPotentialProducer(
                ((SmellSensor) entity.getSensors().get(0)), "getCurrentValue",
                double[].class);
        PotentialConsumer sensoryConsumer = consumers
                .createPotentialConsumer(ng, "setActivations", double[].class);
        
        Coupling sensoryCoupling = new Coupling(sensoryProducer,
                sensoryConsumer);
        try {
            workspace.getCouplingManager().addCoupling(sensoryCoupling);
        } catch (UmatchedAttributesException e) {
            e.printStackTrace();
        }
    }

    public NetBuilder addNetwork(int x, int y, int width, int height,
            String name) {
        NetworkComponent networkComponent = new NetworkComponent(name);
        workspace.addWorkspaceComponent(networkComponent);
        desktop.getDesktopComponent(networkComponent).getParentFrame()
                .setBounds(x, y, width, height);
        netMap.put(networkComponent.getNetwork(), networkComponent);
        return new NetBuilder(networkComponent);
    }

    public OdorWorldBuilder addOdorWorld(int x, int y, int width, int height,
            String name) {
        OdorWorldComponent odorWorldComponent = new OdorWorldComponent(name);
        workspace.addWorkspaceComponent(odorWorldComponent);
        desktop.getDesktopComponent(odorWorldComponent).getParentFrame()
                .setBounds(x, y, width, height);
        odorMap.put(odorWorldComponent.getWorld(), odorWorldComponent);
        return new OdorWorldBuilder(odorWorldComponent);
    }

    // TODO: Same kind of thing as above for odorworld, plots, etc.

    /**
     * @return the workspace
     */
    public Workspace getWorkspace() {
        return workspace;
    }

    public void couple(Sensor sensor, int stimulusDimension, Neuron leftInput) {
        AttributeManager producers = odorMap.get(sensor.getParent().getParentWorld())
                .getAttributeManager();
        AttributeManager consumers = netMap
                .get(leftInput.getNetwork())
                .getAttributeManager();

        PotentialProducer agentSensor =
             producers.createPotentialProducer(sensor,
              "getCurrentValue", double.class,
              new Class[] { int.class },
              new Object[] { stimulusDimension });
        PotentialConsumer sensoryNeuron = consumers
                .createPotentialConsumer(leftInput, "forceSetActivation", double.class);
        
        Coupling agentToNeuronCoupling = new Coupling(agentSensor,
                sensoryNeuron);
        
        try {
            workspace.getCouplingManager().addCoupling(agentToNeuronCoupling);
        } catch (UmatchedAttributesException e) {
            e.printStackTrace();
        }        
    }

    public void couple(Neuron straight, Effector effector) {

        AttributeManager producers = netMap
                .get(straight.getNetwork())
                .getAttributeManager();
        AttributeManager consumers = odorMap.get(effector.getParent().getParentWorld())
                .getAttributeManager();
        
        PotentialProducer effectorNeuron = producers
                .createPotentialProducer(straight, "getActivation", double.class);
        
        PotentialConsumer agentEffector =
             consumers.createPotentialConsumer(effector,
              "setAmount", double.class);
  
        Coupling neuronToAgentCoupling = new Coupling(effectorNeuron,
                agentEffector);
        
        try {
            workspace.getCouplingManager().addCoupling(neuronToAgentCoupling);
        } catch (UmatchedAttributesException e) {
            e.printStackTrace();
        }       
    }

}
