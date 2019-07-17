package org.simbrain.custom_sims.simulations.test;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.OdorWorldWrapper;
import org.simbrain.custom_sims.helper_classes.Simulation;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.neuron_update_rules.*;
import org.simbrain.util.Pair;
import org.simbrain.util.geneticalgorithm.Agent;
import org.simbrain.util.geneticalgorithm.Population;
import org.simbrain.util.geneticalgorithm.odorworld.NetworkEntityGenome;
import org.simbrain.util.math.SimbrainRandomizer;
import org.simbrain.util.neat.NetworkGenome;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.effectors.StraightMovement;
import org.simbrain.world.odorworld.effectors.Turning;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.ObjectSensor;

import java.util.Arrays;

/**
 * Utility to convert older workspace files to new ones. Developing the code here and then will use it
 * in older 3.x Simbrain. For instructions on use see {@link ReadSim}.
 */
public class ConvertSim extends RegisteredSimulation {

    /**
     * Construct sim
     */
    public ConvertSim() {
        super();
    }

    /**
     * @param desktop
     */
    public ConvertSim(SimbrainDesktop desktop) {
        super(desktop);
    }



    @Override
    public String getName() {
        return "Convert Sim";
    }

    @Override
    public void run() {
        
        Workspace workspace = sim.getWorkspace();
        for (WorkspaceComponent component : workspace.getComponentList()) {
            if (component instanceof NetworkComponent) {
                System.out.println("Workspace workspace = sim.getWorkspace();\n");
                String netCompName = new String("netComp_" + component.getName());
                Network network = ((NetworkComponent)component).getNetwork();
                String netName = new String("net_" + component.getName());
                System.out.println("NetworkComponent " + netCompName + " = new NetworkComponent(\"" + netCompName + "\");");
                System.out.println("workspace.addWorkspaceComponent(" + netCompName + ");");
                System.out.println("");
                System.out.println("Network " + netName + " = " + netCompName + ".getNetwork();");
                for(Neuron neuron : network.getFlatNeuronList()) {
                    String neuronName = new String("nrn_" + neuron.getId());
                    System.out.println("Neuron " + neuronName + " = new Neuron(" + netName + ");");
                    System.out.println(neuronName + ".setX(" + neuron.getX() + ");");
                    System.out.println(neuronName + ".setY(" + neuron.getY() + ");");
                    System.out.println(neuronName + ".setLabel(\"" + neuron.getLabel() + "\");");
                    System.out.println(netName + ".addLooseNeuron(" + neuronName + ");");
                }
                for(Synapse synapse : network.getFlatSynapseList()) {
                    String synapseName = new String("syn_" + synapse.getId());
                    String sourceRef = new String(netName + ".getLooseNeuron(\"" + synapse.getSource().getId() +"\")");
                    String targetRef = new String(netName + ".getLooseNeuron(\"" + synapse.getTarget().getId() +"\")");
                    System.out.println("Synapse " + synapseName + " = new Synapse(" + sourceRef + "," + targetRef + "," + synapse.getStrength() + ");");
                    System.out.println(netName + ".addLooseSynapse(" + synapseName + ");");
                }
            }
        }
    }

    @Override
    public RegisteredSimulation instantiate(SimbrainDesktop desktop) {
        return new ConvertSim(desktop);
    }
}
