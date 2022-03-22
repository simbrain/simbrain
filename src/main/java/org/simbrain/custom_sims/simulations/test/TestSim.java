package org.simbrain.custom_sims.simulations.test;

import org.simbrain.custom_sims.Simulation;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.plot.barchart.BarChartComponent;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.SimbrainDesktop;

import java.awt.*;
import java.util.List;

import static org.simbrain.network.connections.ConnectionUtilitiesKt.randomizeAndPolarizeSynapses;
import static org.simbrain.network.connections.SparseKt.connectSparse;

/**
 * Playground for testing new features.
 *
 * To add a sim, copy paste this, put in whatever menu you want it to be in, at {@link #getSubmenuName()}, and be
 * sure to register it at {@link Simulation}.
 */
public class TestSim extends Simulation {


    public TestSim() {
        super();
    }

    /**
     * @param desktop
     */
    public TestSim(SimbrainDesktop desktop) {
        super(desktop);
    }

    /**
     * Run the simulation!
     */
    public void run() {

        // PARAMETERS
        int numNeurons = 5;
        double sparsity = .3;
        double excitatoryRatio = .5;

        Workspace workspace = sim.getWorkspace();

        // CLEAR CURRENT WORKSPACE
        workspace.clearWorkspace();

        // BUILD NETWORK
        NetworkComponent networkComponent = new NetworkComponent("Recurrent Network");
        workspace.addWorkspaceComponent(networkComponent);
        sim.getDesktop().getDesktopComponent(networkComponent).getParentFrame().setBounds(20, 20, 446, 337);

        Network network = networkComponent.getNetwork();
        NeuronGroup ng = new NeuronGroup(network, numNeurons);
        ng.setLabel("Recurrent network");
        // ng.setNeuronType("DecayRule");
        ng.setUpperBound(10);
        network.addNetworkModel(ng);
        ng.randomize();

        // LAYOUT NEURONS
        LineLayout layout = new LineLayout(10,10,50, LineLayout.LineOrientation.HORIZONTAL);
        layout.layoutNeurons((List<Neuron>) network.getLooseNeurons());

        // CREATE SYNAPSES
        connectSparse(network.getFlatNeuronList(),
                network.getFlatNeuronList(), sparsity, false, false);
        randomizeAndPolarizeSynapses(network.getModels(Synapse.class), excitatoryRatio);

        //MAKE BARCHART
        BarChartComponent barChart = new BarChartComponent("Bar Chart of Recurrent Network");
        barChart.getModel().setBarColor(Color.blue);
        barChart.getModel().setAutoRange(false);
        barChart.getModel().setUpperBound(12);
        workspace.addWorkspaceComponent(barChart);
        sim.getDesktop().getDesktopComponent(barChart).getParentFrame().setBounds(500,20,537,345);

        // COUPLING NETWORK TO BARCHART
        Producer neuronProducer = workspace.getCouplingManager().getProducer(ng, "getActivations");
        Consumer barChartConsumer =  workspace.getCouplingManager().getConsumer(barChart.getModel(),
                "setBarValues");
        workspace.getCouplingManager().createCoupling(neuronProducer, barChartConsumer);

        workspace.iterate();

    }

    private String getSubmenuName() {
        return "Test";
    }

    @Override
    public String getName() {
        return "Test Sim";
    }

    @Override
    public TestSim instantiate(SimbrainDesktop desktop) {
        return new TestSim(desktop);
    }

}
