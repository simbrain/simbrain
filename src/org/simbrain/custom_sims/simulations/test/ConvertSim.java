package org.simbrain.custom_sims.simulations.test;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;

import javax.swing.*;

/**
 * A utility that can be used to read auto-generated script text from Simbrain 3.x to be used in converting it.  Can use
 * this to transfer a workspace from an older simbrain to this one.
 * <p>
 * (1) Run a conversion script in Simbrain 3.X.  (2) Copy the resulting "script text".  (3) Paste that text in the run
 * method of this class.  (4) Run this simulation.  (5) Save the resulting workspace.
 */
public class ConvertSim extends RegisteredSimulation {

    public ConvertSim() {
        super();
    }

    @Override
    public void run() {

        Workspace workspace = sim.getWorkspace();
        for (WorkspaceComponent component : workspace.getComponentList()) {
            if (component instanceof NetworkComponent) {
                System.out.println("Workspace workspace = sim.getWorkspace();\n");
                String netCompName = new String("netComp_" + component.getName());
                Network network = ((NetworkComponent) component).getNetwork();
                String netName = new String("net_" + component.getName());
                System.out.println("NetworkComponent " + netCompName + " = new NetworkComponent(\"" + netCompName + "\");");
                System.out.println("workspace.addWorkspaceComponent(" + netCompName + ");");
                System.out.println("");
                System.out.println("Network " + netName + " = " + netCompName + ".getNetwork();");
                for (Neuron neuron : network.getFlatNeuronList()) {
                    String neuronName = new String("nrn_" + neuron.getId());
                    System.out.println("Neuron " + neuronName + " = new Neuron(" + netName + ");");
                    System.out.println(neuronName + ".setX(" + neuron.getX() + ");");
                    System.out.println(neuronName + ".setY(" + neuron.getY() + ");");
                    System.out.println(neuronName + ".setLabel(\"" + neuron.getLabel() + "\");");
                    System.out.println(netName + ".addLooseNeuron(" + neuronName + ");");
                }
                for (Synapse synapse : network.getFlatSynapseList()) {
                    String synapseName = new String("syn_" + synapse.getId());
                    String sourceRef = new String(netName + ".getLooseNeuron(\"" + synapse.getSource().getId() + "\")");
                    String targetRef = new String(netName + ".getLooseNeuron(\"" + synapse.getTarget().getId() + "\")");
                    System.out.println("Synapse " + synapseName + " = new Synapse(" + sourceRef + "," + targetRef + "," + synapse.getStrength() + ");");
                    System.out.println(netName + ".addLooseSynapse(" + synapseName + ");");
                }

            }
        }
        JOptionPane.showMessageDialog(null, "<html><body><p style='width: 200px;'>" +
                "A string description of the nodes and " +
                "synapses of any currently open networks has been printed to console. That string can " +
                "be read into Simbrain 4 by pasting into the main body of the <u>Read Sim</u> simulation. " +
                "<br><br>Only loose neurons and synpases and " +
                "their positions and labels are currently saved. Neuron groups and neuron parameters are not saved " +
                "and must be manually re-created. No other component types are saved. All this could be added " +
                "but we just implemented enough to convert what we needed. Feel free to add more conversion abilities " +
                "if needed</p></body></html>"
        );

    }


    public ConvertSim(SimbrainDesktop desktop) {
        super(desktop);
    }

    public String getSubmenuName() {
        return "Utils";
    }

    @Override
    public String getName() {
        return "Convert Sim";
    }

    @Override
    public RegisteredSimulation instantiate(SimbrainDesktop desktop) {
        return new ConvertSim(desktop);
    }
}
