package org.simbrain.custom_sims.simulations.neat;

import javax.swing.SwingUtilities;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.util.neat.*;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.util.math.DecayFunctions.GaussianDecayFunction;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.effectors.StraightMovement;
import org.simbrain.world.odorworld.effectors.Turning;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

public class NEAT extends RegisteredSimulation {
    Agent agent;
    ControlPanel cp;
    Network n;
    OdorWorld w;
    OdorWorldEntity cheese;
    OdorWorldEntity newEntity;
    OdorWorldEntity pinnedMouse;

    public NEAT() {
        super();
    }

    public NEAT(SimbrainDesktop desktop) {
        super(desktop);
    }

    @Override
    public String getName() {
        return "NEAT";
    }
    
    private void train() {
        long startTime = System.currentTimeMillis();

     // Genome protoGene = Genome.builder()
     //         .ofInputNodeGenesOfGroups(
     //                 NodeGeneGroup.of("LeftSensor",
     //                         new NodeGene(NodeGene.NodeType.input, new LinearRule(), 2.0, false), 3),
     //                 NodeGeneGroup.of("MiddleSensor",
     //                         new NodeGene(NodeGene.NodeType.input, new LinearRule(), 1.0, false), 3),
     //                 NodeGeneGroup.of("RightSensor",
     //                         new NodeGene(NodeGene.NodeType.input, new LinearRule(), 1.0, false), 3)
     //         )
     //         .ofOutputNodeGenesOfGroups(
     //                 NodeGeneGroup.of("MoveStraight", new NodeGene(NodeGene.NodeType.output, new LinearRule())),
     //                 NodeGeneGroup.of("TurnRight",    new NodeGene(NodeGene.NodeType.output, new LinearRule())),
     //                 NodeGeneGroup.of("TurnLeft",     new NodeGene(NodeGene.NodeType.output, new LinearRule()))
     //                 )
     //         .build();

      // construct a pool of genomes with 2 inputs and 1 output
      //   NEATSimulation pool = new NEATSimulation(protoGene, 2, Test::worldTestingMethod2);
        NEATSimulation pool = new NEATSimulation(24, 9, 250, Test::worldTestingMethod);
        
        // run the pretrain (one mouse getting as many cheese as possible), max 40 generation
        Genome topGenome = pool.evolve(300, 20);
        
        // run the actual training (2 mice communicating to find the best path to get cheese)
        // max 10 generation. this one runs too slow
        // pool.setEvaluationMethod(Test::worldTestingMethod2);
        // Genome topGenome = pool.evolve(100, 20);
        Agent topAgent = new Agent(topGenome, pool.getEvaluationRandomizerSeed());
        agent = topAgent;
        
        // see Test#worldTestingIteration2
        w = topAgent.getWorld();
        n = topAgent.getNet();
        // Network n2 = topAgent.getGenome().buildNetwork();
        NEATRandomizer rand = topAgent.getRandomizer();

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);

        System.out.println("Elapsed time:" + duration / 1000.0 + " seconds.");

      // TODO: Should be able to set these in Genome
//      topAgent.getGenome().getInputNg().setLocation(0, 225);
//      topAgent.getGenome().getOutputNg().setLocation(0, 0);

//      NetworkPanel np = NetworkPanel.createNetworkPanel(n);

//      System.out.println(np.debugString());
        sim.addNetwork(65, 15, 500, 500, new NetworkComponent("NEAT", n));
        // sim.addNetwork(65, 520, 500, 500, new NetworkComponent("NEAT2", n2));

        cheese = new OdorWorldEntity(w);

        SmellSource mouse1Smell = new SmellSource();
        mouse1Smell.setDispersion(450);
        mouse1Smell.setDecayFunction(GaussianDecayFunction.create());
        mouse1Smell.setStimulusVector(new double[8]);
        newEntity = new OdorWorldEntity(w, EntityType.MOUSE);
        SmellSensor leftSmellSensor = new SmellSensor(newEntity, "Smell-Left", 0.785, 32);
        SmellSensor centerSmellSensor = new SmellSensor(newEntity, "Smell-Center", 0, 0);
        SmellSensor rightSmellSensor = new SmellSensor(newEntity, "Smell-Right", -0.785, 32);
        StraightMovement straightMovement = new StraightMovement(newEntity);
        Turning turnLeft = new Turning(newEntity, Turning.LEFT);
        Turning turnRight = new Turning(newEntity, Turning.RIGHT);
        newEntity.addSensor(leftSmellSensor);
        newEntity.addSensor(centerSmellSensor);
        newEntity.addSensor(rightSmellSensor);
        newEntity.addEffector(straightMovement);
        newEntity.addEffector(turnLeft);
        newEntity.addEffector(turnRight);
        newEntity.setSmellSource(mouse1Smell);

        // SmellSource mouse2Smell = new SmellSource();
        // mouse2Smell.setDispersion(450);
        // mouse2Smell.setDecayFunction(GaussianDecayFunction.create());
        // mouse2Smell.setStimulusVector(new double[8]);
        // pinnedMouse = new OdorWorldEntity(w, EntityType.MOUSE);
        // SmellSensor leftSmellSensor2 = new SmellSensor(newEntity, "Smell-Left", 0.785, 32);
        // SmellSensor centerSmellSensor2 = new SmellSensor(newEntity, "Smell-Center", 0, 0);
        // SmellSensor rightSmellSensor2 = new SmellSensor(newEntity, "Smell-Right", -0.785, 32);
        // StraightMovement straightMovement2 = new StraightMovement(newEntity);
        // Turning turnLeft2 = new Turning(newEntity, Turning.LEFT);
        // Turning turnRight2 = new Turning(newEntity, Turning.RIGHT);
        // pinnedMouse.addSensor(leftSmellSensor2);
        // pinnedMouse.addSensor(centerSmellSensor2);
        // pinnedMouse.addSensor(rightSmellSensor2);
        // pinnedMouse.addEffector(straightMovement2);
        // pinnedMouse.addEffector(turnLeft2);
        // pinnedMouse.addEffector(turnRight2);
        // pinnedMouse.setSmellSource(mouse2Smell);

        w.addEntity(newEntity);
        // w.addEntity(pinnedMouse);
        w.addEntity(cheese);
        double[] smellVector = {1, 0.2};
        SmellSource smell = new SmellSource(smellVector);
        smell.setDecayFunction(GaussianDecayFunction.builder().dispersion(450).build());
        cheese.setSmellSource(smell);
        newEntity.setLocation(450 / 2, 450 / 8 * 7);
        cheese.setLocation(450 / 4 + (rand.nextBoolean() ? 450 / 2 : 0), 450 / 8);
        // pinnedMouse.setLocation(450 / 2 + 40, 450 / 8);

        sim.addOdorWorld(570, 15, 500, 500, "NEAT world", w);
//        sim.couple(n.getNeuronGroups().get(1).getLooseNeuron(0), newEntity.getEffectors().get(0));
//        sim.couple((SmellSensor) w.getObjectList().get(0).getSensor("Smell-Left"), n.getNeuronGroups().get(0));
//      JDialog dialog = np.displayPanelInWindow(np, "NEAT-XOR");
//      dialog.setSize(500, 500);
      // TODO: Pack should work. Override preferred size in netpanel?
      // dialog.pack();
    }

    private void addControlPanel() {
        cp = ControlPanel.makePanel(sim, "Control Panel", 0, 0);
        cp.addButton("Train new NEAT network", () -> {
            SwingUtilities.invokeLater(() -> {
                train();
            });
        });

        cp.addButton("Run", () -> {
            for (int i = 0; i < 800; i++) {
                // Test.worldTestingIteration2(agent, cheese, newEntity, pinnedMouse);
                Test.worldTestingIteration(agent, cheese, newEntity);
                sim.iterate();
            }
        });

        cp.addButton("Stop", () -> {

        });
    }

    @Override
    public void run() {
        sim.getWorkspace().clearWorkspace();
        addControlPanel();
    }

    @Override
    public RegisteredSimulation instantiate(SimbrainDesktop desktop) {
        return new NEAT(desktop);
    }

}
