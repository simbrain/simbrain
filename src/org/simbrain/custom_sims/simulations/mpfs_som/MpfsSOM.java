package org.simbrain.custom_sims.simulations.mpfs_som;

import java.awt.geom.Point2D;
import java.io.File;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.network.core.Network;
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
        NetBuilder net = sim.addNetwork(10, 10, 550, 800,
                "MPFS SOM");
        network = net.getNetwork();
        SOMNetwork som = new SOMNetwork(network, 20, 29, new Point2D.Double(0, 0));
        //som.getSom().setLayout(new HexagonalGridLayout(40, 40, 5));
        //som.getSom().applyLayout();
        network.addGroup(som);
        
        double[][] data = Utils.getDoubleMatrix(
                new File(
                        "./src/org/simbrain/custom_sims/simulations/mpfs_som/mpfs_data.csv"));

        som.getTrainingSet().setInputData(data);
        
        som.getInputLayer().getNeuronList().get(0).setLabel("Test One");
        som.getInputLayer().getNeuronList().get(1).setLabel("Test Two");
        
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
