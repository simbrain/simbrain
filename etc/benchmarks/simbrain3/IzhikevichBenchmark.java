package org.simbrain.benchmarks;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Consumer;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.neuron_update_rules.IzhikevichRule;
import org.simbrain.network.synapse_update_rules.spikeresponders.Step;
import org.simbrain.network.update_actions.ConcurrentBufferedUpdate;
import org.simbrain.util.math.ProbDistribution;
import org.simbrain.util.randomizer.Randomizer;

/**
 * Headless Izhikevich (2003) benchmark matched to Simbrain 4's IzhikevichBenchmark.kt, for measuring Simbrain 3 on
 * the same hardware. Free neurons and synapses, Step responders lasting one time step. The network is built by
 * {@link #izhikevichNeurons} and {@link #forEachSynapse}, which IzhikevichHeadedBenchmark shares.
 *
 * Usage: IzhikevichBenchmark <serial|concurrent> neurons p dtMs simMs warmupMs seed [weightScale]
 */
public class IzhikevichBenchmark {

    /**
     * Starts the recovery variable at u = b * v, as Simbrain 4 and Brian2 do. Simbrain 3's rule has no setter for it
     * and starts it at 0, which hyperpolarises the first ~50 ms of a run.
     */
    static void startRecovery(Neuron neuron) {
        IzhikevichRule rule = (IzhikevichRule) neuron.getUpdateRule();
        try {
            Field recovery = IzhikevichRule.class.getDeclaredField("recovery");
            recovery.setAccessible(true);
            recovery.setDouble(rule, rule.getB() * neuron.getActivation());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The network's neurons, not yet added to {@code net}: the first 80% excitatory regular-spiking to chattering
     * cells, the rest inhibitory fast-spiking to low-threshold cells, each with its own random parameter r drawn from
     * {@code rng} and a Gaussian noise current, starting at v = -65 and u = b * v.
     */
    static List<Neuron> izhikevichNeurons(Network net, int n, Random rng) {
        int numExc = (int) (n * 0.8);
        List<Neuron> neurons = new ArrayList<>();
        for (int i = 0; i < n; i++) {
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
            startRecovery(neuron);
            neurons.add(neuron);
        }
        return neurons;
    }

    /**
     * Gives {@code add} each synapse of the random network over {@code neurons}: connection probability {@code p}, no
     * self-connections, weights 0.5 U(0, 1) from excitatory sources and -U(0, 1) from inhibitory ones times
     * {@code scale}, frozen, with Step responders lasting one time step of {@code dt}. Draws from {@code rng} in the
     * order Simbrain 4's izhikevichSynapses does (Simbrain 4's generator differs, so the same seed gives a different
     * network there). Returns the number of synapses.
     */
    static long forEachSynapse(List<Neuron> neurons, double p, double scale, double dt, Random rng,
                               Consumer<Synapse> add) {
        int n = neurons.size();
        int numExc = (int) (n * 0.8);
        long synapses = 0;
        for (int src = 0; src < n; src++) {
            for (int tgt = 0; tgt < n; tgt++) {
                if (src == tgt || rng.nextDouble() >= p) {
                    continue;
                }
                double w = scale * (src < numExc ? 0.5 * rng.nextDouble() : -rng.nextDouble());
                Synapse s = new Synapse(neurons.get(src), neurons.get(tgt));
                s.forceSetStrength(w);
                Step step = new Step();
                step.setResponseDuration(dt);
                s.setSpikeResponder(step);
                s.setFrozen(true);
                add.accept(s);
                synapses++;
            }
        }
        return synapses;
    }

    public static void main(String[] args) {
        String mode = args[0];
        int n = Integer.parseInt(args[1]);
        double p = Double.parseDouble(args[2]);
        double dt = Double.parseDouble(args[3]);
        double simMs = Double.parseDouble(args[4]);
        double warmupMs = Double.parseDouble(args[5]);
        long seed = Long.parseLong(args[6]);
        Random rng = new Random(seed);
        double scale = args.length > 7 ? Double.parseDouble(args[7]) : 1.0;

        long t0 = System.nanoTime();
        Network net = new Network();
        net.setFireUpdates(false);
        net.setTimeStep(dt);
        List<Neuron> neurons = izhikevichNeurons(net, n, rng);
        for (Neuron neuron : neurons) {
            net.addNeuron(neuron);
        }
        long synapses = forEachSynapse(neurons, p, scale, dt, rng, net::addSynapse);
        if (mode.equals("concurrent")) {
            net.getUpdateManager().clear();
            net.getUpdateManager().addAction(ConcurrentBufferedUpdate.createConcurrentBufferedUpdate(net));
        }
        double buildS = (System.nanoTime() - t0) / 1e9;

        int warmupSteps = (int) Math.round(warmupMs / dt);
        int steps = (int) Math.round(simMs / dt);
        long t1 = System.nanoTime();
        for (int i = 0; i < warmupSteps; i++) {
            net.update();
        }
        double warmupS = (System.nanoTime() - t1) / 1e9;

        long spikes = 0;
        long t2 = System.nanoTime();
        for (int i = 0; i < steps; i++) {
            net.update();
            for (Neuron neuron : neurons) {
                if (neuron.isSpike()) {
                    spikes++;
                }
            }
        }
        double runS = (System.nanoTime() - t2) / 1e9;
        // Locale.ROOT: a decimal comma would break the JSON
        System.out.println(String.format(Locale.ROOT,
            "{\"simulator\":\"simbrain3-%s\",\"neurons\":%d,\"p\":%s,\"weight_scale\":%s,\"seed\":%d,\"workers\":%d,\"synapses\":%d,"
                + "\"dt_ms\":%s,\"sim_ms\":%s,\"warmup_ms\":%s,\"build_s\":%.4f,\"warmup_s\":%.4f,\"run_s\":%.4f,"
                + "\"rt_factor\":%.4f,\"rate_hz\":%.3f}",
            mode, n, p, scale, seed, mode.equals("concurrent") ? Runtime.getRuntime().availableProcessors() : 1, synapses,
            dt, simMs, warmupMs, buildS, warmupS, runS, runS / (simMs / 1000.0), spikes / (double) n / (simMs / 1000.0)));
        System.exit(0);
    }
}
