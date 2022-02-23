package org.simbrain.custom_sims.simulations.test;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.ProductRule;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.SimbrainDesktop;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Playground for testing new features. A lot of stuff is commented out but
 * should work.
 *
 * To add a sim, copy paste this, put in whatever menu you want it to be in, at {@link #getSubmenuName()}, and be
 * sure to register it at {@link RegisteredSimulation}.
 */
public class lstmBlock extends RegisteredSimulation {


    public lstmBlock() {
        super();
    }

    /**
     * @param desktop
     */
    public lstmBlock(SimbrainDesktop desktop) {
        super(desktop);
    }

    /**
     * Run the simulation!
     */
    public void run() {


        // Parameters
        int INPUTS=25;
        double GRID_SPACE=50;

        Workspace workspace = sim.getWorkspace();
        // Clear workspace
        workspace.clearWorkspace();

        // Build Network
        NetworkComponent networkComponent=new NetworkComponent("LSTM Network");
        workspace.addWorkspaceComponent(networkComponent);
        Network network=networkComponent.getNetwork();

        // Create the four sigmoidal gates for the LSTM cell
        LineLayout gateLayout=new LineLayout(GRID_SPACE,LineLayout.LineOrientation.HORIZONTAL);
        gateLayout.setInitialLocation(new Point(0,0));
        List gates= new ArrayList();

        for(int i=0;i< 4;++i){
            Neuron neuron = new Neuron(network);
            neuron.setIncrement(1);
            neuron.setClamped(true);
            network.addNetworkModel(neuron);
            neuron.setLabel("Gate "+(i+1));
            gates.add(neuron);
        }
        gateLayout.layoutNeurons(gates);

        // Create the four units of the LSTM cell, three products (AND gates) and one sum (OR gate)
        // The load unit allows the info value to enter the store if the gate 1 is high
        Neuron load=new Neuron(network,new ProductRule());
        network.addNetworkModel(load);
        load.setLabel("Load");
        load.setX(0);load.setY(-GRID_SPACE);

        // The maintain unit allows the store value to remain in the store if gate 2 is high
        Neuron maintain=new Neuron(network,new ProductRule());
        maintain.setLabel("Maintain");
        network.addNetworkModel(maintain);
        maintain.setX(GRID_SPACE);maintain.setY(-GRID_SPACE);

        // The store unit takes the sum of the load and maintain signals
        Neuron store=new Neuron(network,new LinearRule());
        store.setLabel("Store");
        network.addNetworkModel(store);
        store.setX(0);store.setY(-2*GRID_SPACE);

        // The out unit allows the store value to be read out of the cell if gate 3 is high
        Neuron out=new Neuron(network,new ProductRule());
        out.setLabel("Out");
        network.addNetworkModel(out);
        out.setX(0);out.setY(-3*GRID_SPACE);

        // Create the internal connections of the LSTM cell, set all connections to a strength of 1
        network.addNetworkModel(new Synapse((Neuron) gates.get(0),load,1));
        network.addNetworkModel(new Synapse((Neuron) gates.get(1),load,1));
        network.addNetworkModel(new Synapse((Neuron) gates.get(2),maintain,1));
        network.addNetworkModel(new Synapse(store,maintain,1));
        network.addNetworkModel(new Synapse(load,store,1));
        network.addNetworkModel(new Synapse(maintain,store,1));
        network.addNetworkModel(new Synapse((Neuron) gates.get(3),out,1));
        network.addNetworkModel(new Synapse(store,out,1));


    }

    private String getSubmenuName() {
        return "LSTM";
    }

    @Override
    public String getName() {
        return "lstm";
    }

    @Override
    public lstmBlock instantiate(SimbrainDesktop desktop) {
        return new lstmBlock(desktop);
    }

}
