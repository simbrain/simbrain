/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.update_actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.simbrain.network.connections.Radial;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.listeners.GroupListener;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.network.listeners.NeuronListener;
import org.simbrain.network.neuron_update_rules.IzhikevichRule;
import org.simbrain.network.synapse_update_rules.spikeresponders.ConvolvedJumpAndDecay;
import org.simbrain.network.update_actions.concurrency_tools.BufferedUpdateTask;
import org.simbrain.network.update_actions.concurrency_tools.Consumer;
import org.simbrain.network.update_actions.concurrency_tools.PoisonTask;
import org.simbrain.network.update_actions.concurrency_tools.Task;
import org.simbrain.network.update_actions.concurrency_tools.WaitingTask;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.ProbDistribution;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.randomizer.PolarizedRandomizer;

/**
 * 
 * @author Zach Tosi
 *
 *         A class which performs a parallelized update of an entire network.
 *         Neurons are updated in chunks of ~100, and any given thread which is
 *         updating a neuron is also responsible for updating all the afferent
 *         synapses of that neuron. Update occurs synchronously using the same
 *         technique as buffered update, despite being concurrent.
 * 
 *         This class keeps its own separate list of neurons in the network
 *         because for networks with groups, it would be expensive to extract
 *         neuron groups from a generic groups list every invocation. Instead it
 *         implements a group and neuron listener and updates its internal set
 *         of neurons when/if neurons or neuron groups are added to or removed
 *         from the network.
 * 
 *         Parallelization in this class follows a classical consumer/producer
 *         framework. One thread: the producer has the job of filling a blocking
 *         queue with tasks for the consumers to take. The consumers take tasks
 *         from the queue and execute them until the queue is empty. In this
 *         way, consumers can take on tasks whenever they are available to do so
 *         and there are more tasks to complete, instead of having to wait on
 *         some other condition. In order to keep the sort of synchronization
 *         necessary for a neural network, a cyclic barrier ensures that all
 *         tasks have been completed before more tasks (amounting to the next
 *         network iteration/update) are allowed in the queue. Furthermore this
 *         class implements a synchronous or buffered update. The producer
 *         thread does not set the activation of each neuron to their buffer
 *         values until all tasks have been completed.
 *
 */
public class ConcurrentBufferedUpdate implements NetworkUpdateAction,
        NeuronListener, GroupListener {

    /** Each task will consist of updating no more than CHUNK_SIZE neurons. */
    private static final int CHUNK_SIZE = 100;

    /**
     * The initial capacity of the set containing this class's private neuron
     * list, which is synchronized to the underlying network.
     */
    private static final int INITIAL_CAPACITY = (int) Math.ceil(15000 / 0.75);

    /**
     * A list of threads being used as our "consumers" of tasks (updating
     * neurons).
     */
    private final List<Thread> consumerThreads = new ArrayList<Thread>();

    /**
     * The blocking queue which contains all tasks relevant to updating the
     * network (insofar as mechanics are concerned). Update actions like
     * NeuronGroupRecorder do not actually alter the network's state and are
     * <b>NOT</b> bundled in with this class's update algorithm. They must be
     * added as independent actions.
     */
    private final BlockingQueue<Task> tasksQueue;

    /**
     * The current number of available processors to the JVM, should that value
     * change. Any changes to this value will be reflected in the number of
     * consumer threads upon subsequent invocation.
     */
    private volatile int currentAvailableProcessors;

    /**
     * The barrier which prevents producer/consumer threads from moving on until
     * the entire network has been brought into the next time step.
     */
    private volatile CyclicBarrier synchronizingBarrier;

    /**
     * This class's private set of neurons in the network, used for updating.
     */
    private final Set<Neuron> neurons = Collections
            .synchronizedSet(new HashSet<Neuron>(INITIAL_CAPACITY));

    /** A count of the number of network changes which have taken place. */
    private AtomicInteger pendingOperations = new AtomicInteger(0);

    /** A copy of the network. */
    private final Network network;

    /** A reference to the current thread which is acting as a producer. */
    private volatile Thread producer;

    /**
     * Indicating whether or not this class and consumer threads are dead and
     * can no longer process network updates.
     */
    private volatile boolean dead = false;

    /** A signal to shut down this class and its consumer threads. */
    private final AtomicBoolean shutdownSignal = new AtomicBoolean(false);

    /**
     * A static factory method that creates a concurrent buffered update class
     * for a network. See {@link #ConcurrentBufferedUpdate(Network)}.
     * 
     * @param network
     * @return
     */
    public static ConcurrentBufferedUpdate createConcurrentBufferedUpdate(
            final Network network) {
        ConcurrentBufferedUpdate cbu = new ConcurrentBufferedUpdate(network);
        network.addGroupListener(cbu);
        network.addNeuronListener(cbu);
        return cbu;
    }

    /**
     * Creates the consumer threads, in a quantity equal to the number of
     * available processors, since the producer's job is small enough that
     * having more threads than processors will not be problematic (they are
     * also not active at the same time at least half the time). Also populates
     * this classes copy of the neurons in the network.
     * 
     * @param network
     *            the network being updated by this updater.
     */
    private ConcurrentBufferedUpdate(final Network network) {
        this.network = network;
        currentAvailableProcessors = getAvailableConsumerProcessors();
        tasksQueue = new LinkedBlockingQueue<Task>();
        // +1 because Producer thread doesn't get its own processor, its job is
        // too small
        synchronizingBarrier = new CyclicBarrier(currentAvailableProcessors + 1);
        for (int i = 0; i < currentAvailableProcessors; i++) {
            consumerThreads.add(new Thread(new Consumer(synchronizingBarrier,
                    tasksQueue, i)));
            consumerThreads.get(i).start();
        }
        for (Neuron n : network.getFlatNeuronList()) {
            neurons.add(n);
        }
        for (NeuronGroup ng : network.getFlatNeuronGroupList()) {
            neurons.addAll(ng.getNeuronList());
        }
    }

    @Override
    public void invoke() {
        if (dead) {
            throw new IllegalStateException(
                    "ConcurrentBufferedUpdate cannot be"
                            + " invoked once it has been shutdown");
        }
        producer = Thread.currentThread();
        try {
            if (currentAvailableProcessors
                != getAvailableConsumerProcessors())
            {
                availableProcessorChange();
            }
            int chunkSize = neurons.size() / 100;
            synchronized (neurons) {
                int i = 0;
                Neuron[] block = new Neuron[chunkSize];
                for (Neuron n : neurons) {
                    if (i >= chunkSize) {
                        i = 0;
                        tasksQueue.put(new BufferedUpdateTask(block));
                        block = new Neuron[chunkSize];
                    }
                    block[i] = n;
                    i++;
                }
                if (i > 1) {
                    tasksQueue.put(new BufferedUpdateTask(block));
                }
            }
            synchronized (shutdownSignal) {
                if (!shutdownSignal.get()) {
                    synchronized (tasksQueue) {
                        for (int i = 0; i < currentAvailableProcessors; i++) {
                            tasksQueue.put(new WaitingTask());
                        }
                    }
                } else {
                    synchronized (tasksQueue) {
                        for (int i = 0; i < currentAvailableProcessors; i++) {
                            tasksQueue.put(new PoisonTask());
                        }
                    }
                    dead = true;
                    synchronizingBarrier.await();
                    consumerThreads.clear();
                    tasksQueue.clear();
                    return;
                }
            }
            synchronizingBarrier.await();
            network.setUpdateCompleted(true);
            while (pendingOperations.get() > 0) {
                synchronized (producer) {
                    wait();
                }
            }
            synchronized (neurons) {
                for (Neuron n : neurons) {
                    n.setToBufferVals();
                }
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Re-checks the number of processors available to the JVM and adjusts the
     * number of consumer threads accordingly.
     */
    private void availableProcessorChange() {
        int newAvailableProcessors = getAvailableConsumerProcessors();
        if (newAvailableProcessors <= 0) {
            return;
        }
        synchronizingBarrier = new CyclicBarrier(newAvailableProcessors);
        while (newAvailableProcessors < currentAvailableProcessors) {
            consumerThreads.remove(consumerThreads.size() - 1);
            currentAvailableProcessors--;
        }
        while (newAvailableProcessors > currentAvailableProcessors) {
            Thread consumer = new Thread(new Consumer(synchronizingBarrier,
                    tasksQueue, currentAvailableProcessors));
            consumerThreads.add(consumer);
            consumer.start();
            currentAvailableProcessors++;
        }
    }

    /**
     * Permanently shuts down the consumer threads and thus the functionality of
     * this class.
     */
    public void shutdown() {
        shutdownSignal.set(true);
    }

    @Override
    public String getDescription() {
        return "Parallel Buffered Update";
    }

    @Override
    public String getLongDescription() {
        return "Parallel Buffered Update (All Neurons)";
    }

    @Override
    public void groupAdded(NetworkEvent<Group> e) {
        if (e.getObject() instanceof NeuronGroup) {
            pendingOperations.incrementAndGet();
            synchronized (neurons) {
                for (Neuron n : ((NeuronGroup) e.getObject()).getNeuronList()) {
                    neurons.add(n);
                }
            }
            decrementPendingOperations();
        } else if (e.getObject() instanceof Subnetwork) {
            List<NeuronGroup> neuronGroups = ((Subnetwork) e.getObject())
                    .getNeuronGroupList();
            for (NeuronGroup ng : neuronGroups) {
                groupAdded(new NetworkEvent<Group>(network, null, ng));
            }
        } else {
            return;
        }

    }

    @Override
    public void groupRemoved(NetworkEvent<Group> e) {
        if (e.getObject() instanceof NeuronGroup) {
            pendingOperations.incrementAndGet();
            synchronized (neurons) {
                for (Neuron n : ((NeuronGroup) e.getObject()).getNeuronList()) {
                    neurons.remove(n);
                }
            }
            decrementPendingOperations();
        } else if (e.getObject() instanceof Subnetwork) {
            List<NeuronGroup> neuronGroups = ((Subnetwork) e.getObject())
                    .getNeuronGroupList();
            for (NeuronGroup ng : neuronGroups) {
                groupRemoved(new NetworkEvent<Group>(network, null, ng));
            }
        } else {
            return;
        }
    }

    @Override
    public void groupChanged(NetworkEvent<Group> networkEvent,
            String changeDescription) {
        return;
    }

    @Override
    public void groupParameterChanged(NetworkEvent<Group> networkEvent) {
        return;
    }

    @Override
    public void groupUpdated(Group group) {
        return;
    }

    @Override
    public void neuronChanged(NetworkEvent<Neuron> networkEvent) {
        return;
    }

    @Override
    public void neuronTypeChanged(NetworkEvent<NeuronUpdateRule> networkEvent) {
        return;
    }

    @Override
    public void labelChanged(NetworkEvent<Neuron> networkEvent) {
        return;
    }

    @Override
    public void neuronAdded(NetworkEvent<Neuron> networkEvent) {
        pendingOperations.incrementAndGet();
        neurons.add(networkEvent.getObject());
        decrementPendingOperations();
    }

    @Override
    public void neuronMoved(NetworkEvent<Neuron> networkEvent) {
        return;
    }

    @Override
    public void neuronRemoved(NetworkEvent<Neuron> networkEvent) {
        pendingOperations.incrementAndGet();
        neurons.remove(networkEvent.getObject());
        decrementPendingOperations();
    }
    
    public void excludeNeurons(Collection<Neuron> exclude) {
        for (Neuron n : exclude) {
            neurons.remove(n);
        }
    }
    
    public void includeNeurons(Collection<Neuron> include) {
        for (Neuron n : include) {
            neurons.add(n);
        }
    }

    public int getAvailableConsumerProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    private int decrementPendingOperations() {
        if (producer != null) {
            synchronized (producer) {
                producer.notify();
            }
        }
        return pendingOperations.decrementAndGet();
    }

    /**
     * Test main to demonstrate performance improvements over serial updates
     * without a GUI.
     * 
     * @param args
     */
    public static void main(String[] args) {
        final int numNeurons = 10000;
        final int lambda = 575;
        
        System.out.println(System.getProperty("java.vm.name"));
        Scanner keyboard = new Scanner(System.in);
        System.out.println("Press any key, then ENTER.");
        String beginToken = keyboard.next();
        long start = System.nanoTime();
        Network net = new Network();
        net.setTimeStep(0.5);
        NeuronGroup ng = new NeuronGroup(net, numNeurons);
        ng.setRecordAsSpikes(true);
        ng.setLabel(beginToken);
        IzhikevichRule upRule = new IzhikevichRule();
        upRule.setiBg(13);
        ng.setNeuronType(upRule);
        for (Neuron n : ng.getNeuronList()) {
            if (Math.random() < 0.25) {
                n.setPolarity(Polarity.INHIBITORY);
            } else {
                n.setPolarity(Polarity.EXCITATORY);
            }
        }
        GridLayout gl = new GridLayout();
        gl.layoutNeurons(ng.getNeuronList());
        PolarizedRandomizer exRand = new PolarizedRandomizer(
                Polarity.EXCITATORY, ProbDistribution.LOGNORMAL);
        PolarizedRandomizer inRand = new PolarizedRandomizer(
                Polarity.INHIBITORY, ProbDistribution.LOGNORMAL);
        exRand.setParam1(0.01);
        exRand.setParam2(0.01);
        inRand.setParam1(0.035);
        inRand.setParam2(0.02);
        System.out.println("Begin Network Construction...");
        SynapseGroup sg = SynapseGroup.createSynapseGroup(ng, ng,
                new Radial(lambda),
                0.75, exRand, inRand);
//        STDPRule stdp = new STDPRule();
//        stdp.setLearningRate(0.01);
//        sg.setLearningRule(stdp, Polarity.BOTH);
        sg.setSpikeResponder(new ConvolvedJumpAndDecay(), Polarity.EXCITATORY);
        //sg.setUseGroupLevelSettings(false);
        ConvolvedJumpAndDecay inhibSR = new ConvolvedJumpAndDecay();
        inhibSR.setTimeConstant(6.0);
        sg.setSpikeResponder(inhibSR, Polarity.INHIBITORY);
        net.addGroup(ng);
        net.addGroup(sg);
        long end = System.nanoTime();
        System.out.println("End Network construction");
        System.out.println("Time: "
                + SimbrainMath.roundDouble((end - start) / Math.pow(10, 9), 6));
        System.out.println(ng.size() + " neurons.");
        System.out.println(sg.size() + " synapses.");
        System.out.println("Begin Test? (Y/N)");
        String cont = keyboard.next();
        keyboard.close();
        if (!(cont.matches("Y") || cont.matches("y"))) {
            return;
        }
        final int TEST_ITERATIONS = 500;
        net.getUpdateManager().clear();
        net.getUpdateManager().addAction(new ConcurrentBufferedUpdate(net));
        // Quick tune up...
        for (int i = 0; i < 1000; i++) {
            net.update();
        }
        System.out.println("End tune-up");
        for (NetworkUpdateAction nua : net.getUpdateManager().getActionList()) {
            if (nua instanceof ConcurrentBufferedUpdate) {
                ((ConcurrentBufferedUpdate) nua).shutdown();
            }
        }

        // SerialExecution
        ng.startRecording();
        start = System.nanoTime();
        net.getUpdateManager().clear();
        net.getUpdateManager().addAction(new UpdateGroup(sg));
        net.getUpdateManager().addAction(new UpdateGroup(ng));
        net.getUpdateManager().addAction(new NeuronGroupRecorder(ng));
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            net.update();
        }
        ng.stopRecording();
        end = System.nanoTime();
        System.out.println("Serial: "
                + SimbrainMath.roundDouble((end - start) / Math.pow(10, 9), 6));

        net.getUpdateManager().clear();
        net.getUpdateManager().addAction(new ConcurrentBufferedUpdate(net));
        net.getUpdateManager().addAction(new NeuronGroupRecorder(ng));
        ng.startRecording();
        start = System.nanoTime();
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            net.update();
        }
        ng.stopRecording();
        end = System.nanoTime();
        System.out.println("Parallel: "
                + SimbrainMath.roundDouble((end - start) / Math.pow(10, 9), 6));
        for (NetworkUpdateAction nua : net.getUpdateManager().getActionList()) {
            if (nua instanceof ConcurrentBufferedUpdate) {
                ((ConcurrentBufferedUpdate) nua).shutdown();
            }
        }
        return;
    }

}
