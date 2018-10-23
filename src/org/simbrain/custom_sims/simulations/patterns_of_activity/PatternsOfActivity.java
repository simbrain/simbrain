package org.simbrain.custom_sims.simulations.patterns_of_activity;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.RadialGaussian;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.HexagonalGridLayout;
import org.simbrain.network.neuron_update_rules.IntegrateAndFireRule;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.synapse_update_rules.STDPRule;
import org.simbrain.network.synapse_update_rules.spikeresponders.ConvolvedJumpAndDecay;
import org.simbrain.network.synapse_update_rules.spikeresponders.SpikeResponder;
import org.simbrain.network.synapse_update_rules.spikeresponders.UDF;
import org.simbrain.network.update_actions.ConcurrentBufferedUpdate;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.ProbDistributions.NormalDistribution;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.piccolo.TileMap;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


/**
 * * Architecture:
 * <p>
 * Four outputs are connected to two continuous valued neurons that control the
 * x and y velocity of the mouse.
 * <p>
 * The right two of the four spiking neurons each send one connection to the
 * rightmost continuous valued one, one with a strength of +1 and another with a
 * strength of -1. The same is the case for the two spiking neurons and one
 * continuous valued one on the left. Basically this results in each of the four
 * spiking neuron being responsible for up, down, left, and right movement
 * independently.
 * <p>
 * Synapses at every stage from the sensory net to the recurrent portion, within
 * the recurrent portion, and from the recurrent portion to the four outputs are
 * subjected to both STDP and synaptic normalization.
 * <p>
 * Each spiking neurons receives connections from only one spatial quadrant of
 * the reservoir. So for example the UP spiking output only receives inputs from
 * the (I think...) bottom left 256 recurrent neurons, while the DOWN spiking
 * output receives inputs from only the bottom right 256 recurrent neurons.
 * Connections within the recurrent neurons are based on distance in a gaussian
 * manner but with the parameters tuned to down regulate tails.
 */
public class PatternsOfActivity extends RegisteredSimulation {

    // References
    Network network;

    private int netSize = 1024;
    private int spacing = 40;
    private int maxDly = 10;
    private NeuronGroup recNeurons;
    private int dispersion = 160;
    double maxDist = Math.sqrt(2 * netSize * spacing);
    private STDPRule ruleEx = new STDPRule(5, 1, 20, 100, 0.001,
        true);
    private STDPRule ruleIn = new STDPRule(2.5, -2.5, 40, 40, 0.001,
        true);
    private SpikeResponder spkR = new UDF();
    private double quadrantDensity = 0.5;

    private List<Neuron> rtQuad = new ArrayList<>();
    private List<Neuron> lfQuad = new ArrayList<>();
    private List<Neuron> dwQuad = new ArrayList<>();
    private List<Neuron> upQuad = new ArrayList<>();

    private Neuron rtNeuron;
    private Neuron lfNeuron;
    private Neuron dwNeuron;
    private Neuron upNeuron;


    @Override
    public void run() {
        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Set up network
        NetBuilder net = sim.addNetwork(10, 10, 543, 545,
            "Patterns of Activity");
        network = net.getNetwork();
        network.setTimeStep(0.5);


        // Set up sensory group and odor world
        NeuronGroup sensoryNet = net.addNeuronGroup(-9.25, 95.93, 5);
        sensoryNet.setNeuronType(new IntegrateAndFireRule());
        sensoryNet.setPolarity(Polarity.EXCITATORY);
        sensoryNet.setLabel("Sensory");
        Neuron cheeseNeuron = sensoryNet.getNeuronList().get(0);
        cheeseNeuron.setLabel("Cheese");
        Neuron flowerNeuron = sensoryNet.getNeuronList().get(1);
        flowerNeuron.setLabel("Flower");

        // Set up odor world
        OdorWorldBuilder world = sim.addOdorWorld(547, 5, 504, 548, "World");
        world.getWorld().setObjectsBlockMovement(false);
        world.getWorld().setTileMap(new TileMap("empty.tmx"));
        OdorWorldEntity mouse = world.addEntity(120, 245, OdorWorldEntity.EntityType.MOUSE);
        mouse.addSensor(new SmellSensor(mouse, "Smell", 0, 0));
        mouse.setHeading(90);
        OdorWorldEntity cheese = world.addEntity(52, 220, OdorWorldEntity.EntityType.SWISS,
            new double[] {18, 0, 5, 10, 5});
        cheese.getSmellSource().setDispersion(dispersion);
        OdorWorldEntity flower = world.addEntity(266, 221, OdorWorldEntity.EntityType.FLOWER,
            new double[] {3, 18, 2, 5, 10});
        flower.getSmellSource().setDispersion(dispersion);
        OdorWorldEntity cow = world.addEntity(90, 23, OdorWorldEntity.EntityType.COW,
            new double[] {3, 7, 16, 19, 0});
        cow.getSmellSource().setDispersion(dispersion);
        OdorWorldEntity lion = world.addEntity(340, 34, OdorWorldEntity.EntityType.LION,
            new double[] {5, 2, 13, 16, 0});
        lion.getSmellSource().setDispersion(dispersion);
        OdorWorldEntity susi = world.addEntity(97, 351, OdorWorldEntity.EntityType.SUSI,
            new double[] {0, 12, 15, 20});
        susi.getSmellSource().setDispersion(dispersion);
        OdorWorldEntity steve = world.addEntity(335, 297, OdorWorldEntity.EntityType.STEVE,
            new double[] {12, 0, 20, 15});
        steve.getSmellSource().setDispersion(dispersion);

        // Set up neural net ==============================================================

        // Set up Recurrent portion
        List<Neuron> neuronList = new ArrayList<>();
        for (int ii = 0; ii < netSize; ++ii) {
            Neuron n = new Neuron(network);
            n.setUpdateRule(new NormIFRule(ii));
            if (Math.random() < 0.8) {
                n.setPolarity(Polarity.EXCITATORY);
                ((IntegrateAndFireRule) n.getUpdateRule()).setTimeConstant(30);
                ((IntegrateAndFireRule) n.getUpdateRule()).setRefractoryPeriod(3);
            } else {
                n.setPolarity(Polarity.INHIBITORY);
                ((IntegrateAndFireRule) n.getUpdateRule()).setTimeConstant(20);
                ((IntegrateAndFireRule) n.getUpdateRule()).setRefractoryPeriod(2);
            }
            ((IntegrateAndFireRule) n.getUpdateRule()).setAddNoise(true);
            ((IntegrateAndFireRule) n.getUpdateRule()).setNoiseGenerator(NormalDistribution.builder()
                .ofMean(0).ofStandardDeviation(0.2).build());
            neuronList.add(n);
        }
        recNeurons = new NeuronGroup(network, neuronList);
        new HexagonalGridLayout(spacing, spacing, (int) Math.sqrt(netSize))
            .layoutNeurons(recNeurons.getNeuronListUnsafe());
        sensoryNet.setLocation(recNeurons.getMaxX() + 300, recNeurons.getMinY() + 100);

        // Set up recurrent synapses
        SynapseGroup recSyns = new SynapseGroup(recNeurons, recNeurons);
        new RadialGaussian(RadialGaussian.DEFAULT_EE_CONST * 3, RadialGaussian.DEFAULT_EI_CONST * 3,
            RadialGaussian.DEFAULT_IE_CONST * 3, RadialGaussian.DEFAULT_II_CONST * 3,
            200).connectNeurons(recSyns);
//        recSyns.setStrength(10, Polarity.EXCITATORY);
//        recSyns.setStrength(-10, Polarity.INHIBITORY);
        initializeSynParameters(recSyns);
        for (Neuron n : neuronList) {
            for (Neuron m : neuronList) {
                if (Math.random() < 0.001 && n != m) {
                    Synapse s = new Synapse(n, m);
                    s.setStrength(n.getPolarity().value(10));
                    recSyns.addNewSynapse(s);
                    // Delays based on distance
                    double d = SimbrainMath.distance(s.getSource().getPosition3D(), s.getTarget().getPosition3D());
                    s.setDelay((int) (maxDly * d / maxDist));
                }
            }
        }

        // Set up input synapses (connections from sensory group to the recurrent group)
        SynapseGroup inpSynG = SynapseGroup.createSynapseGroup(sensoryNet, recNeurons,
            new Sparse(0.5, true, false));
        initializeSynParameters(inpSynG);
        inpSynG.setStrength(40, Polarity.EXCITATORY);
        //inpSynG.setStrength(-10, Polarity.INHIBITORY);
        for (Synapse s : inpSynG.getAllSynapses()) {
            s.setDelay(ThreadLocalRandom.current().nextInt(5, maxDly));
        }

        // Set up the first out group (comprised of LIF neurons to allow for STDP)
        NeuronGroup outGroup = new NeuronGroup(network, 4);
        int tmp = 0;
        for (Neuron n : outGroup.getNeuronList()) {
            if (tmp % 2 == 1) {
                n.setPolarity(Polarity.INHIBITORY);
            } else {
                n.setPolarity(Polarity.EXCITATORY);
            }
            n.setUpdateRule(new NormIFRule(netSize + tmp));
//            ((IntegrateAndFireRule) n.getUpdateRule()).setNoiseGenerator(NormalDistribution.builder()
//                    .ofMean(0).ofStandardDeviation(0.2).build());
            ((IntegrateAndFireRule) (n.getUpdateRule())).setBackgroundCurrent(14.99);
            tmp++;
        }
        rtNeuron = outGroup.getNeuron(0);
        lfNeuron = outGroup.getNeuron(1);
        dwNeuron = outGroup.getNeuron(2);
        upNeuron = outGroup.getNeuron(3);
        outGroup.setLocation(recNeurons.getMaxX() + 300, recNeurons.getMinY() + 800);

        // Set up the synapses between the recurrent network and the output
        // Each neuron recieves from one quadrant of the recurrent neurons in terms of location
        SynapseGroup rec2out = new SynapseGroup(recNeurons, outGroup);
        initializeSynParameters(rec2out);
        double xEdge = recNeurons.getCenterX();
        double yEdge = recNeurons.getCenterY();
        for (int ii = 0; ii < netSize; ++ii) {
            Neuron n = neuronList.get(ii);
            if (n.getPolarity() == Polarity.INHIBITORY) {
                continue;
            }
            double x = n.getX();
            double y = n.getY();
            if (y > yEdge) {
                if (x < xEdge && Math.random() < quadrantDensity) {
                    Synapse s = new Synapse(n, outGroup.getNeuron(2));
                    s.setDelay(ThreadLocalRandom.current().nextInt(5, 10));
                    rec2out.addNewSynapse(s);
                    dwQuad.add(n);
                    continue;
                }
                if (x > xEdge && Math.random() < quadrantDensity) {
                    Synapse s = new Synapse(n, outGroup.getNeuron(3));
                    s.setDelay(ThreadLocalRandom.current().nextInt(5, 10));
                    upQuad.add(n);
                    rec2out.addNewSynapse(s);
                    continue;
                }
            } else {
                if (x < xEdge && Math.random() < quadrantDensity) {
                    rtQuad.add(n);
                    Synapse s = new Synapse(n, outGroup.getNeuron(0));
                    s.setDelay(ThreadLocalRandom.current().nextInt(5, 10));
                    rec2out.addNewSynapse(s);
                    continue;
                }
                if (x > xEdge && Math.random() < quadrantDensity) {
                    lfQuad.add(n);
                    Synapse s = new Synapse(n, outGroup.getNeuron(1));
                    s.setDelay(ThreadLocalRandom.current().nextInt(5, 10));
                    rec2out.addNewSynapse(s);
                    continue;
                }
            }

        }
        rec2out.setConnectionManager(new AllToAll());

        // Set up the neurons that read from the spiking outputs (converting it to a continuous value) which
        // are coupled to the X and Y velocities of the mouse
        NeuronGroup readGroup = new NeuronGroup(network, 2);
        readGroup.setLocation(recNeurons.getMaxX() + 350, recNeurons.getMinY() + 300);
        for (Neuron n : readGroup.getNeuronList()) {
            n.setUpdateRule(new SigmoidalRule());
            n.getUpdateRule().setInputType(NeuronUpdateRule.InputType.SYNAPTIC);
            ((SigmoidalRule) n.getUpdateRule()).setLowerBound(-4);
            ((SigmoidalRule) n.getUpdateRule()).setUpperBound(4);
            ((SigmoidalRule) n.getUpdateRule()).setSlope(4);
            n.setUpperBound(4);
            n.setLowerBound(-4);
        }

        // Set up the connections to the read out neurons
        SynapseGroup out2read = new SynapseGroup(outGroup, readGroup);
        out2read.setSpikeResponder(new ConvolvedJumpAndDecay(20), Polarity.BOTH);
        out2read.addNewSynapse(new Synapse(outGroup.getNeuron(0), readGroup.getNeuron(0)));
        out2read.addNewSynapse(new Synapse(outGroup.getNeuron(1), readGroup.getNeuron(0)));
        out2read.addNewSynapse(new Synapse(outGroup.getNeuron(2), readGroup.getNeuron(1)));
        out2read.addNewSynapse(new Synapse(outGroup.getNeuron(3), readGroup.getNeuron(1)));
        out2read.setDisplaySynapses(false);
        out2read.setConnectionManager(new AllToAll());
        out2read.setUpperBound(1000000000, Polarity.BOTH);
        out2read.setLowerBound(-1000000000, Polarity.BOTH);

        // Make couplings
        sim.tryCoupling(sim.getProducer(readGroup.getNeuron(0), "getActivation"),
            sim.getConsumer(mouse, "setVelocityX"));
        sim.tryCoupling(sim.getProducer(readGroup.getNeuron(1), "getActivation"),
            sim.getConsumer(mouse, "setVelocityY"));
        sim.couple((SmellSensor) mouse.getSensor("Smell"), sensoryNet);

        // Add everything to the network
        network.addGroup(recNeurons);
        network.addGroup(inpSynG);
        network.addGroup(recSyns);
        network.addGroup(outGroup);
        network.addGroup(rec2out);
        network.addGroup(readGroup);
        network.addGroup(out2read);

        // Set up concurrent buffered update
        network.getUpdateManager().clear();
        network.getUpdateManager().addAction(ConcurrentBufferedUpdate.createConcurrentBufferedUpdate(network));

        //TODO: Doesn't currently work
        KeyListener k = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                switch (keyCode) {
                case KeyEvent.VK_UP:
                    ((IntegrateAndFireRule) upNeuron.getUpdateRule()).setBackgroundCurrent(20);
                    for (Neuron n : upQuad) {
                        ((IntegrateAndFireRule) n.getUpdateRule()).setBackgroundCurrent(14.5);
                    }
                    break;
                case KeyEvent.VK_DOWN:
                    ((IntegrateAndFireRule) dwNeuron.getUpdateRule()).setBackgroundCurrent(20);
                    for (Neuron n : dwQuad) {
                        ((IntegrateAndFireRule) n.getUpdateRule()).setBackgroundCurrent(14.5);
                    }
                    break;
                case KeyEvent.VK_LEFT:
                    System.out.println("LEFT");
                    ((IntegrateAndFireRule) lfNeuron.getUpdateRule()).setBackgroundCurrent(20);
                    for (Neuron n : lfQuad) {
                        ((IntegrateAndFireRule) n.getUpdateRule()).setBackgroundCurrent(14.5);
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    ((IntegrateAndFireRule) rtNeuron.getUpdateRule()).setBackgroundCurrent(20);
                    for (Neuron n : rtQuad) {
                        ((IntegrateAndFireRule) n.getUpdateRule()).setBackgroundCurrent(14.5);
                    }
                    break;
                }

            }

            @Override
            public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();
                switch (keyCode) {
                case KeyEvent.VK_UP:
                    ((IntegrateAndFireRule) upNeuron.getUpdateRule()).setBackgroundCurrent(14.95);
                    for (Neuron n : upQuad) {
                        ((IntegrateAndFireRule) n.getUpdateRule()).setBackgroundCurrent(13.5);
                    }
                    break;
                case KeyEvent.VK_DOWN:
                    ((IntegrateAndFireRule) dwNeuron.getUpdateRule()).setBackgroundCurrent(14.95);
                    for (Neuron n : dwQuad) {
                        ((IntegrateAndFireRule) n.getUpdateRule()).setBackgroundCurrent(13.5);
                    }
                    break;
                case KeyEvent.VK_LEFT:
                    System.out.println();
                    ((IntegrateAndFireRule) lfNeuron.getUpdateRule()).setBackgroundCurrent(14.95);
                    for (Neuron n : lfQuad) {
                        ((IntegrateAndFireRule) n.getUpdateRule()).setBackgroundCurrent(13.5);
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    ((IntegrateAndFireRule) rtNeuron.getUpdateRule()).setBackgroundCurrent(14.95);
                    for (Neuron n : rtQuad) {
                        ((IntegrateAndFireRule) n.getUpdateRule()).setBackgroundCurrent(13.5);
                    }
                    break;
                }
            }
        };


//        InputMap inMap = net.getNetworkPanel(sim).getInputMap();
//        ActionMap actMap = net.getNetworkPanel(sim).getActionMap();
//
//        inMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "Left");
//        actMap.put("Left", new leftAction());
    }


    /**
     * @param synG
     */
    private void initializeSynParameters(SynapseGroup synG) {
        synG.setLearningRule(ruleEx, Polarity.EXCITATORY);
        synG.setLearningRule(ruleIn, Polarity.INHIBITORY);
        synG.setSpikeResponder(spkR, Polarity.BOTH);
        synG.setUpperBound(200, Polarity.BOTH);
        synG.setLowerBound(-200, Polarity.BOTH);
        synG.setRandomizers(NormalDistribution.builder().ofMean(10).ofStandardDeviation(2.5).build(),
            NormalDistribution.builder().ofMean(-10).ofStandardDeviation(2.5).build());
        synG.randomizeConnectionWeights();
    }

    public PatternsOfActivity(SimbrainDesktop desktop) {
        super(desktop);
    }

    public PatternsOfActivity() {
        super();
    }

    @Override
    public String getName() {
        return "Patterns of activity";
    }

    @Override
    public PatternsOfActivity instantiate(SimbrainDesktop desktop) {
        return new PatternsOfActivity(desktop);
    }


    private double[] frEsts = new double[netSize + 4];
    private double[] graphicVal = new double[netSize + 4];

    /**
     * Extends the normal integrate and fire rule but also normalizes synapses
     */
    private class NormIFRule extends IntegrateAndFireRule {

        public int index;

        private double totalTimeS = 0.0;

        private final double nv = 1 / Math.log(4.98);

        private double saturation = 3000;

        //
        public NormIFRule(int index) {
            this.index = index;

        }

        @Override
        public void update(Neuron n) {

            double dt = n.getNetwork().getTimeStep();
            totalTimeS += dt;
            double tau_base = dt / 1E5;
//            frEsts[index] = (1 - (tau_base * Math.sqrt((spkCounts[index] + 1) / (totalTimeS + 50))) * frEsts[index])
//                    + (n.isSpike() ? 1 : 0) * (tau_base * Math.sqrt((spkCounts[index] + 1) / (totalTimeS + 50)));
            double spk = n.isSpike() ? 1 : 0;
            frEsts[index] = ((1 - tau_base) * frEsts[index])
                + spk * ((spk) + frEsts[index]) * tau_base;// * tau_base;
            graphicVal[index] = ((1 - 0.1 * dt) * graphicVal[index])
                + spk * ((spk) + graphicVal[index]) * 0.1 * dt;
            double nrmVal = saturation / (1 + Math.exp(-frEsts[index] * 100)) - saturation / 3;
            double totStrEx = 0;
            double totStrIn = 0;
            List<Synapse> toR = new ArrayList<>();
            for (int jj = 0; jj < n.getFanIn().size(); ++jj) {
                if (Math.abs(n.getFanIn().get(jj).getStrength()) < 0.1) {
                    toR.add(n.getFanIn().get(jj));
                    continue;
                }
                if (n.getFanIn().get(jj).getSource().getPolarity() == Polarity.EXCITATORY) {

                    totStrEx += Math.abs(n.getFanIn().get(jj).getStrength());
                } else {
                    totStrIn += Math.abs(n.getFanIn().get(jj).getStrength());
                }
            }
            if (Double.isInfinite(totStrEx) || Double.isNaN(totStrEx) || Double.isInfinite(totStrIn) || Double.isNaN(totStrIn)) {
                System.out.println();
            }
            for (Synapse s : toR) {
                n.getNetwork().removeSynapse(s);
            }
            for (int jj = 0; jj < n.getFanIn().size(); ++jj) {
                Synapse s = n.getFanIn().get(jj);
                if (s.getSource().getPolarity() == Polarity.EXCITATORY) {
                    if (totStrEx != 0) {
                        s.setStrength(s.getStrength() * (nrmVal / (totStrEx)));
                    }
                } else {
                    if (totStrIn != 0) {
                        s.setStrength(s.getStrength() * (nrmVal / (totStrIn)));
                    }
                }
                double dampFac = nv * Math.log(5 - (Math.abs(s.getStrength()) / 50));
                ((STDPRule) s.getLearningRule()).setDelta_w(
                    ((STDPRule) s.getLearningRule()).getDelta_w()
                        * dampFac);
                if (Double.isNaN(((STDPRule) s.getLearningRule()).getDelta_w()) ||
                    Double.isNaN(s.getStrength()))
                {
                    System.out.println();
                }
                if (Math.abs(s.getStrength()) > 200) {
                    double sgn = -Math.signum(s.getStrength());
                    ((STDPRule) s.getLearningRule()).setDelta_w(
                        sgn * Math.abs(((STDPRule) s.getLearningRule()).getDelta_w()));
                }
            }
            super.update(n);

        }

        @Override
        public String getName() {
            return "Norm. Integrate and Fire";
        }

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public double getGraphicalValue(Neuron n) {
            return 1000 * graphicVal[index];
        }

        public double getGraphicalLowerBound() {
            return 0;
        }

        public double getGraphicalUpperBound() {
            return 30;
        }

    }

}