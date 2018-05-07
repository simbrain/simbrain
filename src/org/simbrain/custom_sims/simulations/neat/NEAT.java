package org.simbrain.custom_sims.simulations.neat;

import javax.swing.JDialog;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.custom_sims.simulations.neat.NodeGene.NodeType;
import org.simbrain.custom_sims.simulations.simpleNeuroevolution.EvolveNet;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.util.environment.SmellSource.DecayFunction;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.entities.BasicEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

public class NEAT extends RegisteredSimulation {
    Agent agent;
    ControlPanel cp;
    Network n;
    OdorWorld w;
    BasicEntity cheese;
    RotatingEntity newEntity;

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
        Pool pool = new Pool(24, 3, 500, Test::worldTestingMethod);

      // Run the evolutionary algorithm
        Genome topGenome = pool.evolve(100, 20);
        Agent topAgent = new Agent(topGenome);
        agent = topAgent;
        System.out.println(topGenome.connectionGenes.size());
        w = topAgent.getWorld();
        n = topAgent.getNet();

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);

        System.out.println("Elapsed time:" + duration / 1000.0 + " seconds.");

      // TODO: Should be able to set these in Genome
//      topAgent.getGenome().getInputNg().setLocation(0, 225);
//      topAgent.getGenome().getOutputNg().setLocation(0, 0);

//      NetworkPanel np = NetworkPanel.createNetworkPanel(n);

//      System.out.println(np.debugString());
        sim.addNetwork(65, 15, 500, 500, new NetworkComponent("NEAT", n));

        cheese = new BasicEntity(w);
        newEntity = new RotatingEntity(w);
        w.addAgent(newEntity);
        w.addEntity(cheese);
        double[] smellVector = {1, 0.2};
        SmellSource smell = new SmellSource(smellVector);
        smell.setDispersion(240);
        smell.setDecayFunction(DecayFunction.GAUSSIAN);
        cheese.setSmellSource(smell);
        newEntity.setLocation(450 / 8, 450 / 2);
        cheese.setLocation(450 / 8 + 40, 450 / 2);

        sim.addOdorWorld(570, 15, 500, 500, "NEAT world", w);
        sim.couple(n.getNeuronGroups().get(1).getNeuron(0), newEntity.getEffectors().get(0));
        sim.couple((SmellSensor) w.getObjectList().get(0).getSensor("Smell-Left"), n.getNeuronGroups().get(0));
//      JDialog dialog = np.displayPanelInWindow(np, "NEAT-XOR");
//      dialog.setSize(500, 500);
      // TODO: Pack should work. Override preferred size in netpanel?
      // dialog.pack();
    }

    private void addControlPanel() {
        cp = ControlPanel.makePanel(sim, "Control Panel", 0, 0);
        cp.addButton("Train new NEAT network", () -> {
            train();
        });

        cp.addButton("Run", () -> {
            for (int i = 0; i < 400; i++) {
                Test.worldTestingIteration(agent, cheese, newEntity);
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
