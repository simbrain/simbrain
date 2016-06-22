package org.simbrain.custom.other_stuff;

import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.simulation.NetBuilder;
import org.simbrain.simulation.OdorWorldBuilder;
import org.simbrain.simulation.Simulation;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.RotatingEntity;

// TODO: Rename!

/**
 * Sample simulation to use as a model for your own simulations.
 */
public class TestSim {
    
    final Simulation sim;
    
    /**
     * @param desktop
     */
    public TestSim(SimbrainDesktop desktop) {
        sim = new Simulation(desktop);
    }
    
    /**
     * Run the simulation!
     */
    public void run() {
        
        // Build a network
        NetBuilder nb1= sim.addNetwork(10, 10, 450, 450, "My first network");
        // nb1.addNeurons(0, 0, 20, "horizontal line", "LinearRule");
        // nb1.addNeurons(0, 89, 20, "vertical line", "LinearRule");        
        // nb1.addNeurons(89, 89, 49, "grid", "LinearRule");
        NeuronGroup inputs = nb1.addNeuronGroup(0,300,10, "horizontal line", "DecayRule");
        inputs.setLabel("Inputs");
        NeuronGroup outputs = nb1.addNeuronGroup(0,0,10, "horizontal line", "DecayRule");
        outputs.setLabel("Outputs");
        // nb1.connectAllToAll(inputs, outputs);
        SynapseGroup in_out = nb1.addSynapseGroup(inputs, outputs);
        in_out.randomizeConnectionWeights(); // TODO Not working? 
        
        // Build an odor world
        OdorWorldBuilder ob1 = sim.addOdorWorld(460, 10, 450, 450, "My first world");
        RotatingEntity mouse = ob1.addAgent(20,20, "Mouse");
        
        // Coupling agent to network        
         sim.couple(mouse,inputs); // Agent sensors to neurons
         sim.couple(outputs, mouse); //Neurons to movement effectors
        
    }

}
