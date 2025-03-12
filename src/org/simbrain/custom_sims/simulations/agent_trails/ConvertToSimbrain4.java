package org.simbrain.custom_sims.simulations.agent_trails;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.custom_sims.helper_classes.PlotBuilder;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This script can be used to convert simbrain 3 workspaces to simbrain 4. It produces text which can be pasted into a
 * script file in Simbrain 4. Currently only loose neurons and synapses and some of their properties are captured but
 * it can easily be extended
 */
public class ConvertToSimbrain4 extends RegisteredSimulation {

    public ConvertToSimbrain4(SimbrainDesktop desktop) {
        super(desktop);
    }

    public ConvertToSimbrain4() {
        super();
    }

    @Override
    public void run() {
        var networkComponent = (NetworkComponent) (sim.getWorkspace().getComponentList().get(0));
        var network = networkComponent.getNetwork();

        var neuronList = network.getFlatNeuronList();

        var stringBuilder = new StringBuilder();

        stringBuilder.append("    val neuronList = buildList {\n");
        for (Neuron neuron : neuronList) {
            stringBuilder.append("        add(network.addNeuron {\n" +
                    "            location = point(" + neuron.getX() + ", " + neuron.getY() + ")\n" +
                    "            label = \"" + neuron.getLabel() + "\"\n" +
                    "        })\n");
        }
        stringBuilder.append("\n    }\n");

        stringBuilder.append("\n");

        stringBuilder.append("    val synapses = buildList {\n");
        for (Synapse synapse : network.getFlatSynapseList()) {
            stringBuilder.append("        add(network.addSynapse(neuronList[" + neuronList.indexOf(synapse.getSource()) + "], neuronList[" + neuronList.indexOf(synapse.getTarget()) + "]) {\n" +
                    "            strength = " + synapse.getStrength() + "\n" +
                    "        })\n");
        }
        stringBuilder.append("\n    }\n");

        System.out.println(stringBuilder.toString());

    }

    @Override
    public String getName() {
        return "Convert to Simbrain 4";
    }

    @Override
    public ConvertToSimbrain4 instantiate(SimbrainDesktop desktop) {
        return new ConvertToSimbrain4(desktop);
    }

}
