package org.simbrain.benchmarks;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.piccolo2d.PCamera;
import org.piccolo2d.PNode;
import org.piccolo2d.util.PPaintContext;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.desktop.NetworkDesktopComponent;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.neuron_update_rules.IzhikevichRule;
import org.simbrain.network.synapse_update_rules.spikeresponders.Step;
import org.simbrain.network.update_actions.ConcurrentBufferedUpdate;
import org.simbrain.plot.rasterchart.RasterPlotComponent;
import org.simbrain.util.math.ProbDistribution;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.PotentialProducer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.updater.InterceptingEventQueue;

/**
 * GUI-open ("headed") Izhikevich (2003) benchmark for Simbrain 3, following
 * etc/benchmarks/HEADED_SPEC.md. Same network as the "concurrent" variant of
 * {@link IzhikevichBenchmark} (same RNG draw order), but held in one NeuronGroup and one recurrent SynapseGroup and
 * iterated through the workspace.
 *
 * Usage: IzhikevichHeadedBenchmark headless|paced|guioff network|raster seed [simMs=2000] [warmupMs=500]
 */
public class IzhikevichHeadedBenchmark {

    static final int N = 1000;
    static final double P = 0.1;
    static final double DT = 0.5;

    /** Counts paints of the network canvas: a camera child covering everything that draws nothing. */
    static class FrameCounterNode extends PNode {
        final AtomicLong count = new AtomicLong();

        FrameCounterNode() {
            setPickable(false);
            setChildrenPickable(false);
            setBounds(-1e7, -1e7, 2e7, 2e7);
        }

        @Override
        protected void paint(PPaintContext paintContext) {
            count.incrementAndGet();
        }
    }

    /** Counts spikes after each network update, on the update thread. */
    static class SpikeCounter implements NetworkUpdateAction {
        final List<Neuron> neurons;
        volatile boolean counting = false;
        long spikes = 0;

        SpikeCounter(List<Neuron> neurons) {
            this.neurons = neurons;
        }

        @Override
        public void invoke() {
            if (!counting) {
                return;
            }
            for (int i = 0, n = neurons.size(); i < n; i++) {
                if (neurons.get(i).isSpike()) {
                    spikes++;
                }
            }
        }

        @Override
        public String getDescription() {
            return "Spike counter";
        }

        @Override
        public String getLongDescription() {
            return getDescription();
        }
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    static void run(String[] args) throws Exception {
        String mode = args[0];
        String variant = args[1];
        Random rng = new Random(Long.parseLong(args[2]));
        double simMs = args.length > 3 ? Double.parseDouble(args[3]) : 2000;
        double warmupMs = args.length > 4 ? Double.parseDouble(args[4]) : 500;
        if (!mode.equals("headless") && !mode.equals("paced") && !mode.equals("guioff")) {
            throw new IllegalArgumentException("mode must be headless|paced|guioff");
        }
        if (!variant.equals("network") && !variant.equals("raster")) {
            throw new IllegalArgumentException("variant must be network|raster");
        }
        boolean gui = !mode.equals("headless");

        long t0 = System.nanoTime();
        final Workspace workspace = new Workspace();
        if (gui) {
            // As in SimbrainDesktop.main: this is what paces GUI runs.
            InterceptingEventQueue eventQueue = new InterceptingEventQueue(workspace);
            workspace.setTaskSynchronizationManager(eventQueue);
            Toolkit.getDefaultToolkit().getSystemEventQueue().push(eventQueue);
        }

        Network net = new Network();
        net.setTimeStep(DT);
        int numExc = (int) (N * 0.8);
        List<Neuron> neurons = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            boolean exc = i < numExc;
            double r = rng.nextDouble();
            IzhikevichRule rule = new IzhikevichRule();
            rule.setiBg(0);
            rule.setThreshold(30);
            rule.setAddNoise(true);
            Randomizer noise = new Randomizer(ProbDistribution.NORMAL);
            noise.setParam1(0);
            if (exc) {
                rule.setA(0.02); rule.setB(0.2); rule.setC(-65 + 15 * r * r); rule.setD(8 - 6 * r * r);
                noise.setParam2(5);
            } else {
                rule.setA(0.02 + 0.08 * r); rule.setB(0.25 - 0.05 * r); rule.setC(-65); rule.setD(2);
                noise.setParam2(2);
            }
            rule.setNoiseGenerator(noise);
            Neuron neuron = new Neuron(net, rule);
            neuron.forceSetActivation(-65);
            neurons.add(neuron);
        }
        NeuronGroup ng = new NeuronGroup(net, neurons);
        ng.setLabel("Izhikevich");
        ng.setLayout(new GridLayout(20, 20, 32));
        ng.applyLayout(new java.awt.geom.Point2D.Double(0, 0));
        net.addGroup(ng);

        SynapseGroup sg = new SynapseGroup(ng, ng);
        sg.setLabel("Recurrent");
        sg.setDisplaySynapses(false);
        ng.addOutgoingSg(sg);
        ng.addIncomingSg(sg);
        long synapses = 0;
        for (int src = 0; src < N; src++) {
            for (int tgt = 0; tgt < N; tgt++) {
                if (src == tgt || rng.nextDouble() >= P) {
                    continue;
                }
                double w = src < numExc ? 0.5 * rng.nextDouble() : -rng.nextDouble();
                Synapse s = new Synapse(neurons.get(src), neurons.get(tgt));
                s.forceSetStrength(w);
                Step step = new Step();
                step.setResponseDuration(DT);
                s.setSpikeResponder(step);
                s.setFrozen(true);
                sg.addSynapseUnsafe(s);
                synapses++;
            }
        }
        net.addGroup(sg);
        net.getUpdateManager().clear();
        net.getUpdateManager().addAction(ConcurrentBufferedUpdate.createConcurrentBufferedUpdate(net));
        SpikeCounter spikeCounter = new SpikeCounter(neurons);
        net.getUpdateManager().addAction(spikeCounter);

        final NetworkComponent nc = new NetworkComponent("Izhikevich", net);
        final RasterPlotComponent raster = variant.equals("raster") ? new RasterPlotComponent("Raster") : null;
        final FrameCounterNode frameCounter = new FrameCounterNode();
        final NetworkPanel[] panelRef = new NetworkPanel[1];

        if (gui) {
            SwingUtilities.invokeAndWait(() -> {
                SimbrainDesktop desktop = new SimbrainDesktop(workspace);
                JFrame frame = desktop.getFrame();
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                frame.setBounds(0, 0, 1600, 1000);
                frame.setVisible(true);
                workspace.addWorkspaceComponent(nc);
                NetworkDesktopComponent ndc = (NetworkDesktopComponent) desktop.getDesktopComponent(nc);
                ndc.getParentFrame().setBounds(10, 10, 800, 800);
                panelRef[0] = ndc.getNetworkPanel();
                PCamera camera = panelRef[0].getCanvas().getCamera();
                camera.addChild(frameCounter);
                if (raster != null) {
                    workspace.addWorkspaceComponent(raster);
                    desktop.getDesktopComponent(raster).getParentFrame().setBounds(820, 10, 700, 500);
                }
            });
        } else {
            workspace.addWorkspaceComponent(nc);
            if (raster != null) {
                workspace.addWorkspaceComponent(raster);
            }
        }
        if (raster != null) {
            PotentialProducer producer = NetworkComponent.getNeuronGroupProducer(nc, ng, "getSpikeIndexes");
            PotentialConsumer consumer = raster.getPotentialConsumers().get(0);
            workspace.addCoupling(new Coupling<>(producer, consumer));
        }
        if (gui) {
            settle();
            SwingUtilities.invokeAndWait(() -> {
                panelRef[0].zoomToFitPage(true);
                if (mode.equals("guioff")) {
                    panelRef[0].setGuiOn(false);
                }
            });
            settle();
        }
        double buildS = (System.nanoTime() - t0) / 1e9;

        int warmupSteps = (int) Math.round(warmupMs / DT);
        int steps = (int) Math.round(simMs / DT);
        long t1 = System.nanoTime();
        iterate(workspace, warmupSteps);
        double warmupS = (System.nanoTime() - t1) / 1e9;
        if (gui) {
            settle();
        }

        spikeCounter.counting = true;
        long frames0 = frameCounter.count.get();
        long t2 = System.nanoTime();
        iterate(workspace, steps);
        double runS = (System.nanoTime() - t2) / 1e9;
        long frames = frameCounter.count.get() - frames0;
        spikeCounter.counting = false;

        double simS = simMs / 1000.0;
        System.out.println(String.format(
            "{\"simulator\":\"simbrain3-gui\",\"variant\":\"%s\",\"mode\":\"%s\",\"neurons\":%d,\"synapses\":%d,"
                + "\"dt_ms\":%s,\"sim_ms\":%s,\"build_s\":%.4f,\"warmup_s\":%.4f,\"run_s\":%.4f,\"rt_factor\":%.4f,"
                + "\"iterations_per_s\":%.2f,\"frames\":%d,\"frames_per_s\":%.2f,\"rate_hz\":%.3f}",
            variant, mode, N, synapses, DT, simMs, buildS, warmupS, runS, runS / simS, steps / runS, frames,
            frames / runS, spikeCounter.spikes / (double) N / simS));
        System.out.flush();
        System.exit(0);
    }

    /** Runs a fixed number of workspace iterations on the workspace updater thread and blocks until done. */
    static void iterate(Workspace workspace, int iterations) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        workspace.iterate(latch, iterations);
        latch.await();
    }

    /** Waits for the event queue to drain: repeated empty round trips through the EDT, with short pauses. */
    static void settle() throws Exception {
        for (int i = 0; i < 5; i++) {
            EventQueue.invokeAndWait(() -> { });
            Thread.sleep(200);
        }
    }
}
