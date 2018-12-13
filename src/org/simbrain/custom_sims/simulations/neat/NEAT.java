package org.simbrain.custom_sims.simulations.neat;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.custom_sims.simulations.neat.NodeGene.NodeType;
import org.simbrain.custom_sims.simulations.neat.util.NEATRandomizer;
import org.simbrain.custom_sims.simulations.simpleNeuroevolution.EvolveNet;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.util.math.DecayFunctions.GaussianDecayFunction;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorld;
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

//      Genome protoGene = Genome.builder()
//              .ofInputNodeGenesOfGroups(
//                      NodeGeneGroup.of("LeftSensor",
//                              new NodeGene(NodeType.input, new LinearRule(), 2.0, false), 3),
//                      NodeGeneGroup.of("MiddleSensor",
//                              new NodeGene(NodeType.input, new LinearRule(), 1.0, false), 3),
//                      NodeGeneGroup.of("RightSensor",
//                              new NodeGene(NodeType.input, new LinearRule(), 1.0, false), 3)
//              )
//              .ofOutputNodeGenesOfGroups(
//                      NodeGeneGroup.of("MoveStraight", new NodeGene(NodeType.output, new LinearRule())),
//                      NodeGeneGroup.of("TurnRight",    new NodeGene(NodeType.output, new LinearRule())),
//                      NodeGeneGroup.of("TurnLeft",     new NodeGene(NodeType.output, new LinearRule()))
//                      )
//              .build();

      // construct a pool of genomes with 2 inputs and 1 output
//      Pool pool = new Pool(protoGene, 2, Test::worldTestingMethod);
        Pool pool = new Pool(24, 9, 1525711735340L, 250, Test::worldTestingMethod);
        
        // run the pretrain (one mouse getting as many cheese as possible), max 40 generation
        pool.evolve(40, 20);
        
        // run the actual training (2 mice communicating to find the best path to get cheese)
        // max 10 generation. this one runs too slow
        pool.setEvaluationMethod(Test::worldTestingMethod2);
        Genome topGenome = pool.evolve(10, 20);
        Agent topAgent = new Agent(topGenome, pool.getEvaluationRandomizerSeed());
        agent = topAgent;
        
        // see Test#worldTestingIteration2
        w = topAgent.getWorld();
        n = topAgent.getNet();
        Network n2 = topAgent.getGenome().buildNetwork();
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
        sim.addNetwork(65, 520, 500, 500, new NetworkComponent("NEAT2", n2));

        cheese = new OdorWorldEntity(w);

        SmellSource mouse1Smell = new SmellSource();
        mouse1Smell.setDispersion(450);
        mouse1Smell.setDecayFunction(GaussianDecayFunction.create());
        mouse1Smell.setStimulusVector(new double[8]);
        newEntity = new OdorWorldEntity(w, EntityType.MOUSE);
        newEntity.setSmellSource(mouse1Smell);

        SmellSource mouse2Smell = new SmellSource();
        mouse2Smell.setDispersion(450);
        mouse2Smell.setDecayFunction(GaussianDecayFunction.create());
        mouse2Smell.setStimulusVector(new double[8]);
        pinnedMouse = new OdorWorldEntity(w, EntityType.MOUSE);
        pinnedMouse.setSmellSource(mouse2Smell);

        w.addEntity(newEntity);
        w.addEntity(pinnedMouse);
        w.addEntity(cheese);
        double[] smellVector = {1, 0.2};
        SmellSource smell = new SmellSource(smellVector);
        smell.setDispersion(240);
        smell.setDecayFunction(GaussianDecayFunction.create());
        cheese.setSmellSource(smell);
        newEntity.setLocation(450 / 2, 450 / 8 * 7);
        cheese.setLocation(450 / 4 + (rand.nextBoolean() ? 450 / 2 : 0), 450 / 8);
        pinnedMouse.setLocation(450 / 2 + 40, 450 / 8);

        sim.addOdorWorld(570, 15, 500, 500, "NEAT world", w);
//        sim.couple(n.getNeuronGroups().get(1).getNeuron(0), newEntity.getEffectors().get(0));
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
            for (int i = 0; i < 400; i++) {
                Test.worldTestingIteration2(agent, cheese, newEntity, pinnedMouse);
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
