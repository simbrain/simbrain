package org.simbrain.custom_sims.simulations.patterns_of_activity;

import org.simbrain.custom_sims.Simulation;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.connections.RadialGaussian;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseGroup2;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.HexagonalGridLayout;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.spikeresponders.UDF;
import org.simbrain.network.synapse_update_rules.STDPRule;
import org.simbrain.network.synapse_update_rules.spikeresponders.SpikeResponder;
import org.simbrain.network.updaterules.IntegrateAndFireRule;
import org.simbrain.network.util.ScalarDataHolder;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.piccolo.TMXUtils;
import org.simbrain.util.stats.distributions.NormalDistribution;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.simbrain.network.connections.RadialGaussianKt.*;


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
public class PatternsOfActivity extends Simulation {

    // References
    Network net;

    private int netSize = 1024;
    private int spacing = 40;
    private int maxDly = 12;
    private NeuronGroup recurrentNetwork;
    private int dispersion = 140;
    double maxDist = Math.sqrt(2 * netSize * spacing);
    private STDPRule ruleEx = new STDPRule(5, 1, 20, 100, 0.001,
        true);
    private STDPRule ruleExRec = new STDPRule(5.2, .8, 25, 100, 0.001,
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

    private double[] frEsts = new double[netSize + 4];
    private double[] graphicVal = new double[netSize + 4];


    private NeuronGroup outputNeurons;
    OdorWorldEntity mouse;

    @Override
    public void run() {
        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Set up network
        NetworkComponent nc = sim.addNetwork(10, 10, 543, 545,
            "Patterns of Activity");
        net = nc.getNetwork();
        net.setTimeStep(0.5);

        // Set up sensory group and odor world
        NeuronGroup sensoryNetL =  net.addNeuronGroup(-9.25, 95.93, 5);
        // TODO: Removed random spike chance for now
        sensoryNetL.setNeuronType(new IntegrateAndFireRule());
        sensoryNetL.setPolarity(Polarity.EXCITATORY);
        sensoryNetL.setLabel("Sensory Left");

        // Set up sensory group and odor world
        NeuronGroup sensoryNetR =  net.addNeuronGroup(-9.25, 155.93, 5);
        sensoryNetR.setNeuronType(new IntegrateAndFireRule());
        sensoryNetR.setPolarity(Polarity.EXCITATORY);
        sensoryNetR.setLabel("Sensory Right");

        // Build odor world
        mouse = buildWorld();

        // Create network
        buildNetwork(sensoryNetL, sensoryNetR, mouse);

        // Set up plots
        addProjection(sensoryNetL, 8, 562, 1, "getActivations");
        addProjection(recurrentNetwork, 368, 562, 24, "getSubsampledActivations");
        addProjection(outputNeurons, 723, 562, .1, "getActivations");

    }

    private OdorWorldEntity buildWorld() {
        // Set up odor world
        OdorWorldComponent oc = sim.addOdorWorld(547, 5, 504, 548, "World");
        oc.getWorld().setObjectsBlockMovement(false);
        oc.getWorld().setUseCameraCentering(false);
        oc.getWorld().setTileMap(TMXUtils.loadTileMap("empty.tmx"));
        OdorWorldEntity mouse = oc.getWorld().addEntity(120, 245, EntityType.MOUSE);
        mouse.addSensor(new SmellSensor("Smell-Right", 36.0, 40));
        mouse.addSensor(new SmellSensor("Smell-Left", -36.0, 40));
        mouse.setHeading(90);
        OdorWorldEntity cheese = oc.getWorld().addEntity(72, 220, EntityType.SWISS,
            new double[] {18, 0, 5, 10, 5});
        cheese.getSmellSource().setDispersion(dispersion);
        OdorWorldEntity flower = oc.getWorld().addEntity(190, 221, EntityType.FLOWER,
            new double[] {3, 18, 2, 5, 10});
        flower.getSmellSource().setDispersion(dispersion);
        OdorWorldEntity cow = oc.getWorld().addEntity(90, 50, EntityType.COW,
            new double[] {3, 7, 16, 19, 0});
        cow.getSmellSource().setDispersion(dispersion);
        OdorWorldEntity lion = oc.getWorld().addEntity(300, 54, EntityType.LION,
            new double[] {5, 2, 13, 16, 0});
        lion.getSmellSource().setDispersion(dispersion);
        OdorWorldEntity susi = oc.getWorld().addEntity(97, 331, EntityType.SUSI,
            new double[] {0, 12, 15, 20});
        susi.getSmellSource().setDispersion(dispersion);
        OdorWorldEntity steve = oc.getWorld().addEntity(315, 305, EntityType.STEVE,
            new double[] {12, 0, 20, 15});
        steve.getSmellSource().setDispersion(dispersion);
        return mouse;
    }

    private void buildNetwork(NeuronGroup sensoryNetL, NeuronGroup sensoryNetR, OdorWorldEntity mouse) {
        // Set up Recurrent portion
        List<Neuron> neuronList = new ArrayList<>();
        for (int ii = 0; ii < netSize; ++ii) {
            Neuron n = new Neuron(net);
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
            ((IntegrateAndFireRule) n.getUpdateRule()).setBackgroundCurrent(18);
            ((IntegrateAndFireRule) n.getUpdateRule()).setNoiseGenerator(
                    new NormalDistribution(0, 0.2));
            neuronList.add(n);
        }
        recurrentNetwork = new NeuronGroup(net, neuronList);
        recurrentNetwork.setLabel("Recurrent network");
        new HexagonalGridLayout(spacing, spacing, (int) Math.sqrt(netSize))
            .layoutNeurons(recurrentNetwork.getNeuronList());
        sensoryNetL.setLocation(recurrentNetwork.getMaxX() + 300, recurrentNetwork.getMinY() + 100);
        sensoryNetR.setLocation(recurrentNetwork.getMaxX() + 300, recurrentNetwork.getMinY() + 100);

        // Set up recurrent synapses
        SynapseGroup2 recSyns = SynapseGroup.createSynapseGroup(recurrentNetwork, recurrentNetwork,
        new RadialGaussian(DEFAULT_EE_CONST * 3, DEFAULT_EI_CONST * 3,
            DEFAULT_IE_CONST * 3, DEFAULT_II_CONST * 3, .25, 200));
        //new Sparse(0.10, false, false)
        //        .connectNeurons(recSyns);
        // initializeSynParameters(recSyns);
        // TODO
        // recSyns.setLearningRule(ruleExRec, Polarity.EXCITATORY);
        for (Neuron n : neuronList) {
            for (Neuron m : neuronList) {
                if (Math.random() < 0.002 && n != m) {
                    Synapse s = new Synapse(n, m);
                    s.setStrength(n.getPolarity().value(20));
                    recSyns.addSynapse(s);
                    // Delays based on distance
                }
            }
        }
        for(Synapse s : recSyns.getSynapses()) {
            double d = SimbrainMath.distance(s.getSource().getPosition3D(), s.getTarget().getPosition3D());
            s.setDelay((int) (maxDly * d / maxDist));
        }

        // Set up input synapses (connections from sensory group to the recurrent group)
        SynapseGroup2 inpSynGL = SynapseGroup.createSynapseGroup(sensoryNetL, recurrentNetwork,
            new Sparse(0.25, true, false));
        // initializeSynParameters(inpSynGL);
        // TODO
        // inpSynGL.setStrength(50, Polarity.EXCITATORY);
        //inpSynGL.setStrength(-10, Polarity.INHIBITORY);
        for (Synapse s : inpSynGL.getSynapses()) {
            s.setDelay(ThreadLocalRandom.current().nextInt(2, maxDly/2));
            if(s.getTarget().getPolarity() == Polarity.INHIBITORY) {
                inpSynGL.removeSynapse(s);
            }
        }
        SynapseGroup2 inpSynGR = SynapseGroup.createSynapseGroup(sensoryNetR, recurrentNetwork,
                new Sparse(0.25, true, false));
        // initializeSynParameters(inpSynGR);
        // TODO
        // inpSynGR.setStrength(50, Polarity.EXCITATORY);
        //inpSynGL.setStrength(-10, Polarity.INHIBITORY);
        for (Synapse s : inpSynGR.getSynapses()) {
            s.setDelay(ThreadLocalRandom.current().nextInt(2, maxDly/2));
            if(s.getTarget().getPolarity() == Polarity.INHIBITORY) {
                inpSynGR.removeSynapse(s);
            }
        }

        // Set up the first out group (comprised of LIF neurons to allow for STDP)
        NeuronGroup outGroup = new NeuronGroup(net, 4);
        int tmp = 0;
        for (Neuron n : outGroup.getNeuronList()) {
            if (tmp % 2 == 1) {
                n.setPolarity(Polarity.INHIBITORY);
            } else {
                n.setPolarity(Polarity.EXCITATORY);
            }
            n.setUpdateRule(new NormIFRule(netSize + tmp));
            //((IntegrateAndFireRule) n.getUpdateRule()).setNoiseGenerator(NormalDistribution.builder()
            //        .ofMean(0).ofStandardDeviation(0.2).build());
            ((IntegrateAndFireRule) (n.getUpdateRule())).setBackgroundCurrent(19.9);
            tmp++;
        }
        rtNeuron = outGroup.getNeuron(0);
        lfNeuron = outGroup.getNeuron(1);
        dwNeuron = outGroup.getNeuron(2);
        upNeuron = outGroup.getNeuron(3);
        outGroup.setLocation(recurrentNetwork.getMaxX() + 300, recurrentNetwork.getMinY() + 800);

        // Set up the synapses between the recurrent network and the output
        // Each neuron recieves from one quadrant of the recurrent neurons in terms of location
        SynapseGroup2 rec2out = SynapseGroup.createSynapseGroup(recurrentNetwork, outGroup);
        // initializeSynParameters(rec2out);
        double xEdge = recurrentNetwork.getCenterX();
        double yEdge = recurrentNetwork.getCenterY();
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
                    rec2out.addSynapse(s);
                    dwQuad.add(n);
                    continue;
                }
                if (x > xEdge && Math.random() < quadrantDensity) {
                    Synapse s = new Synapse(n, outGroup.getNeuron(3));
                    s.setDelay(ThreadLocalRandom.current().nextInt(5, 10));
                    upQuad.add(n);
                    rec2out.addSynapse(s);
                    continue;
                }
            } else {
                if (x < xEdge && Math.random() < quadrantDensity) {
                    rtQuad.add(n);
                    Synapse s = new Synapse(n, outGroup.getNeuron(0));
                    s.setDelay(ThreadLocalRandom.current().nextInt(5, 10));
                    rec2out.addSynapse(s);
                    continue;
                }
                if (x > xEdge && Math.random() < quadrantDensity) {
                    lfQuad.add(n);
                    Synapse s = new Synapse(n, outGroup.getNeuron(1));
                    s.setDelay(ThreadLocalRandom.current().nextInt(5, 10));
                    rec2out.addSynapse(s);
                    continue;
                }
            }

        }
        // rec2out.setConnectionManager(new AllToAll());

        // Set up the neurons that read from the spiking outputs (converting it to a continuous value) which
        // are coupled to the X and Y velocities of the mouse
        outputNeurons = new NeuronGroup(net, 2);
        outputNeurons.setLabel("Motor outputs");
        outputNeurons.setLocation(recurrentNetwork.getMaxX() + 350, recurrentNetwork.getMinY() + 380);
        for (Neuron n : outputNeurons.getNeuronList()) {
            n.setUpdateRule(new SigmoidalRule());
            ((SigmoidalRule) n.getUpdateRule()).setLowerBound(-4);
            ((SigmoidalRule) n.getUpdateRule()).setUpperBound(4);
            ((SigmoidalRule) n.getUpdateRule()).setSlope(4);
            n.setUpperBound(4);
            n.setLowerBound(-4);
        }

        // Set up the connections to the read out neurons
        SynapseGroup2 out2read = SynapseGroup.createSynapseGroup(outGroup, outputNeurons);
        // TODO
        // out2read.setSpikeResponder(new ConvolvedJumpAndDecay(20), Polarity.BOTH);
        // out2read.addSynapse(new Synapse(outGroup.getNeuron(0), outputNeurons.getNeuron(0)));
        // out2read.addSynapse(new Synapse(outGroup.getNeuron(1), outputNeurons.getNeuron(0)));
        // out2read.addSynapse(new Synapse(outGroup.getNeuron(2), outputNeurons.getNeuron(1)));
        // out2read.addSynapse(new Synapse(outGroup.getNeuron(3), outputNeurons.getNeuron(1)));
        // out2read.setDisplaySynapses(false);
        // out2read.setConnectionManager(new AllToAll());
        // out2read.setUpperBound(1000000000, Polarity.BOTH);
        // out2read.setLowerBound(-1000000000, Polarity.BOTH);

        // Make couplings
        sim.couple(sim.getProducer(outputNeurons.getNeuron(0), "getActivation"),
            sim.getConsumer(mouse, "setVelocityX"));
        sim.couple(sim.getProducer(outputNeurons.getNeuron(1), "getActivation"),
            sim.getConsumer(mouse, "setVelocityY"));
        sim.couple((SmellSensor) mouse.getSensor("Smell-Left"), sensoryNetL);
        sim.couple((SmellSensor) mouse.getSensor("Smell-Right"), sensoryNetR);

        // Add everything to the network
        net.addNetworkModelAsync(recurrentNetwork);
        net.addNetworkModelAsync(inpSynGL);
        net.addNetworkModelAsync(inpSynGR);
        net.addNetworkModelAsync(recSyns);
        net.addNetworkModelAsync(outGroup);
        net.addNetworkModelAsync(rec2out);
        net.addNetworkModelAsync(outputNeurons);
        net.addNetworkModelAsync(out2read);
        net.addNetworkModelAsync(sensoryNetL);
        net.addNetworkModelAsync(sensoryNetR);

        // Set up concurrent buffered update
        // net.getUpdateManager().clear();
        // net.getUpdateManager().addAction(ConcurrentBufferedUpdate.createConcurrentBufferedUpdate(net));
    }


    private void initializeSynParameters(SynapseGroup synG) {
        // TODO
        // synG.setLearningRule(ruleEx, Polarity.EXCITATORY);
        // synG.setLearningRule(ruleIn, Polarity.INHIBITORY);
        // synG.setSpikeResponder(spkR, Polarity.BOTH);
        // synG.setUpperBound(200, Polarity.EXCITATORY);
        // synG.setLowerBound(0, Polarity.EXCITATORY);
        // synG.setLowerBound(-200, Polarity.INHIBITORY);
        // synG.setUpperBound(0, Polarity.INHIBITORY);
        synG.setRandomizers(
                new NormalDistribution(10, 2.5),
                new NormalDistribution(-10, 2.5));
        synG.randomize();
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

    /**
     * Extends the normal integrate and fire rule but also normalizes synapses
     */
    private class NormIFRule extends IntegrateAndFireRule {

        public final int index;

        private double totalTimeS = 0.0;

        private final double nv = 1 / Math.log(4.98);

        private double saturation = 3000;

        @Override
        public NormIFRule deepCopy() {
            NormIFRule ifn = new NormIFRule(index);
            ifn.setRestingPotential(getRestingPotential());
            ifn.setResetPotential(getResetPotential());
            ifn.setThreshold(getThreshold());
            ifn.setBackgroundCurrent(getBackgroundCurrent());
            ifn.setTimeConstant(getTimeConstant());
            ifn.setResistance(getResistance());
            ifn.setAddNoise(getAddNoise());
            ifn.setNoiseGenerator(getNoiseGenerator().deepCopy());
            return ifn;
        }
        //
        public NormIFRule(int index) {
            super();
            this.index = index;
            frEsts[index] = 0.01;
        }

        @Override
        public void apply(Neuron n, ScalarDataHolder data) {

            double dt = n.getNetwork().getTimeStep();
            totalTimeS += dt;
            double tau_base = dt / 1E5;
            //frEsts[index] = (1 - (tau_base * Math.sqrt((spkCounts[index] + 1) / (totalTimeS + 50))) * frEsts[index])
            //        + (n.isSpike() ? 1 : 0) * (tau_base * Math.sqrt((spkCounts[index] + 1) / (totalTimeS + 50)));
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
                s.delete();
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
            super.apply(n, n.getDataHolder());

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

    private void addProjection(NeuronGroup toPlot, int x, int y, double tolerance, String methodName) {

        // Create projection component
        ProjectionComponent pc = sim.addProjectionPlot(x, y, 362, 320, toPlot.getLabel());
        pc.getProjector().init(toPlot.size());
        pc.getProjector().setTolerance(tolerance);
        pc.getProjector().getColorManager().setColoringMethod("Bayesian");

        // Coupling
        Producer inputProducer = sim.getProducer(toPlot, methodName);
        Consumer plotConsumer = sim.getConsumer(pc, "addPoint");
        sim.couple(inputProducer, plotConsumer);

        // Text of nearest world object to projection plot current dot
        Producer currentObject = sim.getProducer(mouse, "getNearbyObjects");
        Consumer plotText = sim.getConsumer(pc, "setLabel");
        sim.couple(currentObject, plotText);


    }
    private String getSubmenuName() {
        return "Cognitive Maps";
    }


}