package org.simbrain.custom_sims.simulations.behaviorism;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * Simulation to demonstrate classical and operant conditioning.
 *
 * @author Tim Meyer
 * @author Jeff Yoshimi
 */
public class Behaviorism extends RegisteredSimulation {

    // TODO: Display probabilities of behaviors

    NetBuilder netBuilder;
    RotatingEntity mouse;
    OdorWorldEntity cheese, flower, fish;
    ControlPanel panel;
    NeuronGroup sensoryNet, actionNet, predictionNet;
    Neuron leftNeuron, straightNeuron, rightNeuron;
    Neuron cheeseNeuron, flowerNeuron, fishNeuron;
    Neuron errorNeuron;
    OdorWorldBuilder world;

    // Default values for these used by buttons
    int dispersion = 65;
    int fishX = 50;
    int fishY = 100;
    int flowerX = 200;
    int flowerY = 100;
    int cheeseX = 120;
    int cheeseY = 180;

    int probability = 0; // Temp to demo

    final int numNeurons = 3;



    public Behaviorism() {
        super();
    }

    /**
     * @param desktop
     */
    public Behaviorism(SimbrainDesktop desktop) {
        super(desktop);
    }

    /**
     * Run the simulation!
     */
    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();



        // Build a network
        netBuilder = sim.addNetwork(195, 9, 447, 296, "Behaviors");
        sensoryNet = netBuilder.addNeuronGroup(-9.25, 95.93, numNeurons);
        sensoryNet.getNeuronList().get(0).setLabel("B1");
        sensoryNet.getNeuronList().get(1).setLabel("B2");
        sensoryNet.getNeuronList().get(2).setLabel("B3");
        sensoryNet.getNeuronList().get(0).setAuxValue(.34);
        sensoryNet.getNeuronList().get(1).setAuxValue(.33);
        sensoryNet.getNeuronList().get(2).setAuxValue(.33);




        //net.getNetwork().getUpdateManager().clear();

        netBuilder.getNetwork().addUpdateAction(new NetworkUpdateAction() {
            @Override
            public void invoke() {
                //clear activation?
                //norm all probabilities
                normalizeNeurons();
                //pick a behavior
                double random = Math.random();
                if(random < sensoryNet.getNeuronList().get(0).getAuxValue()){
                    sensoryNet.getNeuronList().get(0).setActivation(1);
                } else if(random < sensoryNet.getNeuronList().get(1).getAuxValue()){
                    sensoryNet.getNeuronList().get(1).setActivation(1);
                }else{
                    sensoryNet.getNeuronList().get(2).setActivation(1);
                }
                //change behaviors not every iteration, thats too fast
                //
                //System.out.println("Probability:" + probability);
                //sensoryNet.getNeuronList().get(0).setActivation(probability);

            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public String getLongDescription() {
                return null;
            }
        });


        // // Create the odor world
        // world = sim.addOdorWorld(629, 9, 315, 383, "Three Objects");
        // world.getWorld().setObjectsBlockMovement(false);
        // mouse = world.addAgent(120, 245, "Mouse");
        // mouse.setHeading(90);
        //
        // // Set up world
        // cheese = world.addEntity(120, 180, "Swiss.gif",
        //         new double[] { 1, 0, 0 });
        // cheese.getSmellSource().setDispersion(65);
        // flower = world.addEntity(200, 100, "Pansy.gif",
        //         new double[] { 0, 1, 0 });
        // cheese.getSmellSource().setDispersion(65);
        // fish = world.addEntity(50, 100, "Fish.gif",
        //         new double[] { 0, 0, 1 });
        // cheese.getSmellSource().setDispersion(65);
        //
        // // Couple network to agent
        // sim.couple(straightNeuron, mouse.getEffector("Go-straight"));
        // sim.couple(rightNeuron, mouse.getEffector("Go-left"));
        // sim.couple(leftNeuron, mouse.getEffector("Go-right"));
        //
        // // Couple agent to network
        // sim.couple((SmellSensor) mouse.getSensor("Smell-Center"), 0,
        //         cheeseNeuron);
        // sim.couple((SmellSensor) mouse.getSensor("Smell-Center"), 1,
        //         flowerNeuron);
        // sim.couple((SmellSensor) mouse.getSensor("Smell-Center"), 2,
        //         fishNeuron);

        setUpControlPanel();

    }

    private void setUpControlPanel() {

        panel = ControlPanel.makePanel(sim, "Control Panel", 5, 10);

        panel.addButton("Reward Agent", () -> {

            int locbiggest;
            for(Neuron n : sensoryNet.getNeuronList()) {
                if(n.getActivation() > 0){
                    locbiggest = n;
                }
            }
            sensoryNet.getNeuronList().get(locbiggest).setAuxValue(sensoryNet.getNeuronList().get(locbiggest).getAuxValue() + .1*(sensoryNet.getNeuronList().get(locbiggest).getAuxValue()));
            sim.iterate();
        });

        panel.addButton("Punish Agent", () -> {

            int locbiggest;
            for(Neuron n : sensoryNet.getNeuronList()) {
                if(n.getActivation() > 0){
                    locbiggest = n;
                }
            }
            sensoryNet.getNeuronList().get(locbiggest).setAuxValue(sensoryNet.getNeuronList().get(locbiggest).getAuxValue() - .1*(sensoryNet.getNeuronList().get(locbiggest).getAuxValue()));
            sim.iterate();
        });

        // // Move past cheese
        // panel.addButton("Cheese", () -> {
        //     net.getNetwork().clearActivations();
        //     mouse.setLocation(cheeseX, cheeseY + dispersion);
        //     mouse.setHeading(90);
        //     straightNeuron.forceSetActivation(1);
        //     sim.iterate(180);
        // });
        //
        // // Move past Fish
        // panel.addButton("Fish", () -> {
        //     net.getNetwork().clearActivations();
        //     mouse.setLocation(fishX, fishY + dispersion);
        //     mouse.setHeading(90);
        //     straightNeuron.forceSetActivation(1);
        //     sim.iterate(180);
        // });
        //
        // // Move past flower
        // panel.addButton("Flower", () -> {
        //     net.getNetwork().clearActivations();
        //     mouse.setLocation(flowerX, flowerY + dispersion);
        //     mouse.setHeading(90);
        //     straightNeuron.forceSetActivation(1);
        //     sim.iterate(180);
        // });
        //
        // // Cheese > Fish
        // panel.addButton("Cheese > Flower", () -> {
        //     net.getNetwork().clearActivations();
        //     mouse.setLocation(cheeseX, cheeseY + dispersion);
        //     mouse.setHeading(90);
        //     straightNeuron.forceSetActivation(1);
        //     sim.iterate(50);
        //     rightNeuron.forceSetActivation(1.5);
        //     sim.iterate(25);
        //     rightNeuron.forceSetActivation(0);
        //     sim.iterate(220);
        // });
        //
        // // Cheese > Flower
        // panel.addButton("Cheese > Fish", () -> {
        //     net.getNetwork().clearActivations();
        //     mouse.setLocation(cheeseX, cheeseY + dispersion);
        //     mouse.setHeading(90);
        //     straightNeuron.forceSetActivation(1);
        //     sim.iterate(50);
        //     leftNeuron.forceSetActivation(1.5);
        //     sim.iterate(25);
        //     leftNeuron.forceSetActivation(0);
        //     sim.iterate(220);
        // });

    }

    private  void normalizeNeurons() {
        int forNorm = 0;
        for(Neuron n : sensoryNet.getNeuronList()) {
            forNorm += n.getAuxValue();
        }
        for(Neuron n : sensoryNet.getNeuronList()) {
            n.setAuxValue(n.getAuxValue()/forNorm);
        }
    }
    @Override
    public String getName() {
        return "Behaviorism";
    }

    @Override
    public Behaviorism instantiate(SimbrainDesktop desktop) {
        return new Behaviorism(desktop);
    }

}
