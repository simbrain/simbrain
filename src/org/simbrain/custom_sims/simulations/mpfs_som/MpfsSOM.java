package org.simbrain.custom_sims.simulations.mpfs_som;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetworkWrapper;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.HexagonalGridLayout;
import org.simbrain.network.subnetworks.SOMNetwork;
import org.simbrain.network.trainers.SOMTrainer;
import org.simbrain.network.trainers.Trainer.DataNotInitializedException;
import org.simbrain.util.Utils;
import org.simbrain.workspace.gui.SimbrainDesktop;

/**
 * Self-organizing map represents the moral political family scale...
 *
 * @author Karie Moorman
 * @author Jeff Yoshimi
 */
public class MpfsSOM extends RegisteredSimulation {

    // References
    NetworkWrapper netWrapper;
    Network network;
    SOMTrainer trainer;
    SOMNetwork som;
    double[][] data; // input data
    double[][] affiliations; // "target" data to color nodes

    // Parameters
    int numSOMNodes = 20;

    // Associates neurons with average political self affiliations (target data
    // above) That is, the average political affiliation of the people whose
    // questions are input when this neuron "wins".
    Map<Neuron, Double> averagePoliticalAffiliation = new HashMap<Neuron, Double>();
    Map<Neuron, Integer> numberOfWins = new HashMap<Neuron, Integer>();

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Build network
        netWrapper = sim.addNetwork2(144, 11, 550, 680, "Moral-Political SOM");
        network = netWrapper.getNetwork();
        som = new SOMNetwork(network, numSOMNodes, 29,
                new Point2D.Double(0, 0));
        som.getSom().setLayout(new HexagonalGridLayout(40, 40, 5));
        som.getSom().applyLayout();
        network.addGroup(som);
        som.getInputLayer().setLayout(new GridLayout(70, 60, 5));
        som.getInputLayer().applyLayout();

        data = Utils.getDoubleMatrix(new File(
                "./src/org/simbrain/custom_sims/simulations/mpfs_som/mpfs_data.csv"));

        affiliations = Utils.getDoubleMatrix(new File(
                "./src/org/simbrain/custom_sims/simulations/mpfs_som/mpfs_self_affiliations.csv"));

        // TODO: Better to shuffle?
        //Collections.reverse(Arrays.asList(data));
        //Collections.reverse(Arrays.asList(affiliations));

        som.getTrainingSet().setInputData(data);

        som.getInputLayer().getNeuronList().get(0).setLabel("SF_1");
        som.getInputLayer().getNeuronList().get(1).setLabel("SF_2");
        som.getInputLayer().getNeuronList().get(2).setLabel("SF_3");
        som.getInputLayer().getNeuronList().get(3).setLabel("SF_4");
        som.getInputLayer().getNeuronList().get(4).setLabel("SF_5");
        som.getInputLayer().getNeuronList().get(5).setLabel("SF_6");
        som.getInputLayer().getNeuronList().get(6).setLabel("SF_7");
        som.getInputLayer().getNeuronList().get(7).setLabel("SF_8");
        som.getInputLayer().getNeuronList().get(8).setLabel("SF_9");
        som.getInputLayer().getNeuronList().get(9).setLabel("SF_10");
        som.getInputLayer().getNeuronList().get(10).setLabel("SF_11");
        som.getInputLayer().getNeuronList().get(11).setLabel("SF_12");
        som.getInputLayer().getNeuronList().get(12).setLabel("SF_13");
        som.getInputLayer().getNeuronList().get(13).setLabel("SF_14");
        som.getInputLayer().getNeuronList().get(14).setLabel("SF_15");
        som.getInputLayer().getNeuronList().get(15).setLabel("NP_1");
        som.getInputLayer().getNeuronList().get(16).setLabel("NP_2");
        som.getInputLayer().getNeuronList().get(17).setLabel("NP_3");
        som.getInputLayer().getNeuronList().get(18).setLabel("NP_4");
        som.getInputLayer().getNeuronList().get(19).setLabel("NP_5");
        som.getInputLayer().getNeuronList().get(20).setLabel("NP_6");
        som.getInputLayer().getNeuronList().get(21).setLabel("NP_7");
        som.getInputLayer().getNeuronList().get(22).setLabel("NP_8");
        som.getInputLayer().getNeuronList().get(23).setLabel("NP_9");
        som.getInputLayer().getNeuronList().get(24).setLabel("NP_10");
        som.getInputLayer().getNeuronList().get(25).setLabel("NP_11");
        som.getInputLayer().getNeuronList().get(26).setLabel("NP_12");
        som.getInputLayer().getNeuronList().get(27).setLabel("NP_13");
        som.getInputLayer().getNeuronList().get(28).setLabel("NP_14");

        // NetworkLayoutManager.offsetNeuronGroup(som.getInputLayer(),
        // som.getSom(),
        // Direction.SOUTH, 250);

        // Set up control panel
        controlPanel();
    }

    private void controlPanel() {
        ControlPanel panel = ControlPanel.makePanel(sim, "Controller", 5, 10);
        panel.addButton("Train SOM", () -> {
            // TODO: Set up text fields?
            som.getSom().reset();
            som.getSom().randomizeIncomingWeights();
            som.getSom().randomize();
            som.getSom().setInitAlpha(1); // learning rate
            som.getSom().setAlphaDecayRate(.0005);
            som.getSom().setInitNeighborhoodSize(200); // in pixels
            som.getSom().setNeighborhoodDecayAmount(.005);
            trainer = new SOMTrainer(som);
            for (int i = 0; i < 100; i++) {
                try {
                    trainer.apply();
                     //System.out.println("Neighborhood size: "
                     //+ som.getSom().getNeighborhoodSize());
                     //System.out.println(
                     //"Learning rate: " + som.getSom().getAlpha());
                } catch (DataNotInitializedException e) {
                    e.printStackTrace();
                }
            }
            network.fireGroupUpdated(som);
            netWrapper.getNetworkComponent().update();
        });

        panel.addButton("Color SOM", () -> {

            // Compute average political affiliation
            // Kind of a hot mess. Is there an easier way?
            averagePoliticalAffiliation.clear();
            numberOfWins.clear();

            for (int i = 0; i < data.length; i++) {

                // Update the SOM
                som.getInputLayer().forceSetActivations(data[i]);
                som.update();

                // Update averagePoliticalAffiliation on current node
                Neuron winner = som.getSom().getWinner();
                if (numberOfWins.get(winner) == null) {
                    numberOfWins.put(winner, 1);
                } else {
                    int numWins = numberOfWins.get(winner) + 1;
                    numberOfWins.put(winner, numWins);
                }
                if (averagePoliticalAffiliation.get(winner) == null) {
                    averagePoliticalAffiliation.put(winner,
                            (Double) affiliations[i][0]);
                } else {
                    Double current = averagePoliticalAffiliation.get(winner);
                    averagePoliticalAffiliation.put(winner,
                            (current + (Double) affiliations[i][0]));
                }
            }
            for (Neuron neuron : averagePoliticalAffiliation.keySet()) {

                Double average = averagePoliticalAffiliation.get(neuron)
                        / numberOfWins.get(neuron);
                averagePoliticalAffiliation.put(neuron, average);
            }

            // Custom display of SOM neurons handled here
            for (Neuron neuron : averagePoliticalAffiliation.keySet()) {

                NeuronNode node = netWrapper.getNode(neuron);
                if (node == null) {
                    continue;
                }

                float val = averagePoliticalAffiliation.get(neuron)
                        .floatValue();
                neuron.setLabel("" + Utils.round(val, 2));

                if ((val < .1) && (val > -.1)) {
                    node.setCustomStrokeColor(Color.ORANGE);
                } else if (val > .01) {
                    node.setCustomStrokeColor(Color.blue);
                } else {
                    node.setCustomStrokeColor(Color.red);
                }

            }
            netWrapper.getNetwork().fireGroupUpdated(som);
            netWrapper.getNetworkComponent().update();
        });

    }

    public MpfsSOM(SimbrainDesktop desktop) {
        super(desktop);
    }

    public MpfsSOM() {
        super();
    };

    @Override
    public String getName() {
        return "Moral-Political SOM";
    }

    @Override
    public MpfsSOM instantiate(SimbrainDesktop desktop) {
        return new MpfsSOM(desktop);
    }

}
