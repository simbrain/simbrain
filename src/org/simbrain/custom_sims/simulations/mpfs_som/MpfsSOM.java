package org.simbrain.custom_sims.simulations.mpfs_som;

import java.awt.geom.Point2D;
import java.io.File;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.network.core.Network;
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
    Network network;

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Build network
        NetBuilder net = sim.addNetwork(500, 10, 550, 680,
                "MPFS SOM");
        network = net.getNetwork();
        SOMNetwork som = new SOMNetwork(network, 20, 29, new Point2D.Double(0, 0));
        som.getSom().setLayout(new HexagonalGridLayout(60, 60, 5));
        som.getSom().applyLayout();
        network.addGroup(som);
        som.getInputLayer().setLayout(new GridLayout(70,60, 5));
        som.getInputLayer().applyLayout();
        
        double[][] data = Utils.getDoubleMatrix(
                new File(
                        "./src/org/simbrain/custom_sims/simulations/mpfs_som/mpfs_data.csv"));

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
        
        
        //NetworkLayoutManager.offsetNeuronGroup(som.getInputLayer(), som.getSom(),
        //        Direction.SOUTH, 250);
        
        
        som.getSom().setInitAlpha(1); // learning rate
        som.getSom().setAlphaDecayRate(.00005);
        som.getSom().setInitNeighborhoodSize(200); // in pixels
        som.getSom().setNeighborhoodDecayAmount(.005);
        SOMTrainer trainer = new SOMTrainer(som);
        for(int i = 0; i < 100; i++) {
            try {
                trainer.apply();
                System.out.println(
                        "Neighborhoo size: " + som.getSom().getNeighborhoodSize());
                System.out.println("Learning rate: " + som.getSom().getAlpha());
            } catch (DataNotInitializedException e) {
                e.printStackTrace();
            }           
        }
        network.fireGroupUpdated(som);
 
        // Set up control panel
         controlPanel();
    }

    private void controlPanel() {
        ControlPanel panel = ControlPanel.makePanel(sim, "Controller", 5, 10);
        panel.addButton("Test", () -> {
            System.out.println("Test");
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
        return "Moral Political Family Scale SOM";
    }

    @Override
    public MpfsSOM instantiate(SimbrainDesktop desktop) {
        return new MpfsSOM(desktop);
    }

}
