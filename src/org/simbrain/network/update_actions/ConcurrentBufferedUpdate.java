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

import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.*;
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
import org.simbrain.network.update_actions.concurrency_tools.Task;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.math.ProbDistributions.LogNormalDistribution;
import org.simbrain.util.math.ProbDistributions.NormalDistribution;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Zoë Tosi
 * <p>
 * A class which performs a parallelized update of an entire network.
 * Any given thread which is updating a neuron is also responsible for
 * updating all the afferent synapses of that neuron. Update occurs
 * synchronously using the same technique as buffered update, despite
 * being concurrent.
 * <p>
 * This class keeps its own separate list of neurons in the network
 * because for networks with groups, it would be expensive to extract
 * neuron groups from a generic groups list every invocation. Instead it
 * implements a group and neuron listener and updates its internal set
 * of neurons when/if neurons or neuron groups are added to or removed
 * from the network.
 * <p>
 * Parallelization in this class follows a classical consumer/producer
 * framework. One thread: the producer has the job of filling a blocking
 * queue with tasks for the consumers to take. The consumers take tasks
 * from the queue and execute them until the queue is empty. In this
 * way, consumers can take on tasks whenever they are available to do so
 * and there are more tasks to complete, instead of having to wait on
 * some other condition. In order to keep the sort of synchronization
 * necessary for a neural network, a cyclic barrier ensures that all
 * tasks have been completed before more tasks (amounting to the next
 * network iteration/update) are allowed in the queue. Furthermore this
 * class implements a synchronous or buffered update. The producer
 * thread does not set the activation of each neuron to their buffer
 * values until all tasks have been completed.
 */
public class ConcurrentBufferedUpdate implements NetworkUpdateAction, NeuronListener, GroupListener {

    /**
     * The initial capacity of the set containing this class's private neuron
     * list, which is synchronized to the underlying network.
     */
    private static final int INITIAL_CAPACITY = (int) Math.ceil(15000 / 0.75);

    /**
     * A list of threads being used as our "consumers" of tasks (updating
     * neurons).
     */
    private final List<Consumer> consumerThreads = new ArrayList<Consumer>();

    /**
     * The current number of available processors to the JVM, should that value
     * change. Any changes to this value will be reflected in the number of
     * consumer threads upon subsequent invocation.
     */
    private volatile int currentAvailableProcessors;

    /**
     * This class's private set of neurons in the network, used for updating.
     */
    private final Set<Neuron> neurons = Collections.synchronizedSet(new HashSet<Neuron>(INITIAL_CAPACITY));

    private CyclicTaskQueue taskSet;

    /**
     * A count of the number of network changes which have taken place.
     */
    private AtomicInteger pendingOperations = new AtomicInteger(0);

    /**
     * A copy of the network.
     */
    private final Network network;

    /**
     * A reference to the current thread which is acting as a producer.
     */
    private volatile Thread producer;

    private final List<NeuronGroup> inputGroups = new ArrayList<NeuronGroup>();

    private final List<NeuronGroup> outputGroups = new ArrayList<NeuronGroup>();

    private Thread collectorThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                while (true) {
                    synchronized (lock) {
                        while (!collectionInProgress.get()) {
                            lock.wait();
                        }
                    }
                    // Wait for an entire second so that every individual neuron
                    // removed over the course of the second will be accounted
                    // for.
                    Thread.sleep(1000);
                    synchronized (collectionInProgress) {
                        taskSet.repopulateQueue(neurons);
                        collectionInProgress.getAndSet(false);
                        pendingOperations.set(pendingOperations.get() - ops);
                        ops = 0;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });

    private Object lock = new Object();

    private AtomicBoolean collectionInProgress = new AtomicBoolean();

    private volatile int ops = 0;

    //private final SynchronizationPoint syncPoint;

    private ExecutorService executors;

    /**
     * A static factory method that creates a concurrent buffered update class
     * for a network. See {@link #ConcurrentBufferedUpdate(Network)}.
     *
     * @param network
     * @return
     */
    public static ConcurrentBufferedUpdate createConcurrentBufferedUpdate(final Network network) {
        ConcurrentBufferedUpdate cbu = new ConcurrentBufferedUpdate(network);
        //        for (int i = 0; i < cbu.currentAvailableProcessors; i++) {
        //            cbu.consumerThreads.add(new Consumer(cbu.taskSet, i));
        //            new Thread(cbu.consumerThreads.get(i)).start();
        //        }

        network.addGroupListener(cbu);
        network.addNeuronListener(cbu);
        // Checks for inconsistencies between the input and output group
        // lists and what neuron groups are labeled as input or output
        // groups in the network.
        for (NeuronGroup ng : network.getFlatNeuronGroupList()) {
            network.fireGroupChanged(ng, "Check In");
        }
        //System.out.println("Num neurons in task set: " + cbu.taskSet.size());
        cbu.collectorThread.start();
        return cbu;
    }

    /**
     * Creates the consumer threads, in a quantity equal to the number of
     * available processors, since the producer's job is small enough that
     * having more threads than processors will not be problematic (they are
     * also not active at the same time at least half the time). Also populates
     * this classes copy of the neurons in the network.
     *
     * @param network the network being updated by this updater.
     */
    private ConcurrentBufferedUpdate(final Network network) {
        this.network = network;
        currentAvailableProcessors = getAvailableConsumerProcessors();
        executors = Executors.newFixedThreadPool(currentAvailableProcessors);
        //syncPoint = new SynchronizationPoint(currentAvailableProcessors);
        for (Neuron n : network.getFlatNeuronList()) {
            neurons.add(n);
        }
        for (NeuronGroup ng : network.getFlatNeuronGroupList()) {
            neurons.addAll(ng.getNeuronList());
        }
        taskSet = new CyclicTaskQueue(neurons, currentAvailableProcessors);
    }

    //int z = 0;
    @Override
    public void invoke() {
        producer = Thread.currentThread();
        // Update input neurons accordingly
        for (int i = 0, n = inputGroups.size(); i < n; i++) {
            inputGroups.get(i).readNextInputs();
        }
        try {
            synchronized (taskSet) {
                //System.out.println(z++);
                List<Future<Task>> results = executors.invokeAll(taskSet.getCallableTasks());
                for (int i = 0; i < results.size(); i++) {
                    for (int j = 0; j < ((BufferedUpdateTask) results.get(i).get()).getHosts().length; j++) {
                        ((BufferedUpdateTask) results.get(i).get()).getHosts()[j].setToBufferVals();
                    }
                }
            }
            while (pendingOperations.get() > 0) {
                synchronized (producer) {
                    producer.wait();
                }
            }
        } catch (InterruptedException | ExecutionException e1) {
            e1.getCause().printStackTrace();
            e1.printStackTrace();
        }
        //        synchronized (neurons) {
        //            for (int i = 0, n = taskSet.taskArray.length; i < n; i++) {
        //                for (int j = 0, m = taskSet.taskArray[i].getHosts().length;
        //                        j < m; j++)
        //                {
        //                    taskSet.taskArray[i].getHosts()[j].setToBufferVals();
        //                }
        //            }
        //        }
        for (int i = 0, n = outputGroups.size(); i < n; i++) {
            outputGroups.get(i).writeActsToFile();
        }
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
                taskSet.repopulateQueue(neurons);
            }
            decrementPendingOperations();
        } else if (e.getObject() instanceof Subnetwork) {
            List<NeuronGroup> neuronGroups = ((Subnetwork) e.getObject()).getNeuronGroupList();
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
            taskSet.repopulateQueue(neurons);
            decrementPendingOperations();
        } else if (e.getObject() instanceof Subnetwork) {
            List<NeuronGroup> neuronGroups = ((Subnetwork) e.getObject()).getNeuronGroupList();
            for (NeuronGroup ng : neuronGroups) {
                groupRemoved(new NetworkEvent<Group>(network, null, ng));
            }
        } else {
            return;
        }
    }

    @Override
    public void groupChanged(NetworkEvent<Group> networkEvent, String changeDescription) {
        if (networkEvent.getObject() instanceof NeuronGroup) {
            NeuronGroup ng = (NeuronGroup) networkEvent.getObject();
            synchronized (outputGroups) {
                if (ng.isRecording()) {
                    if (!outputGroups.contains(ng)) {
                        outputGroups.add(ng);
                    }
                } else {
                    if (outputGroups.contains(ng)) {
                        outputGroups.remove(ng);
                    }
                }
            }
            synchronized (inputGroups) {
                if (ng.isInputMode()) {
                    if (!inputGroups.contains(ng)) {
                        inputGroups.add(ng);
                    }
                } else {
                    if (inputGroups.contains(ng)) {
                        inputGroups.remove(ng);
                    }
                }
            }
        }
        return;
    }

    @Override
    public void groupParameterChanged(NetworkEvent<Group> networkEvent) {
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
        synchronized (collectionInProgress) {
            neurons.add(networkEvent.getObject());
            ops++;
            if (!collectionInProgress.get()) {
                collectionInProgress.getAndSet(true);
                synchronized (lock) {
                    lock.notify();
                }
            }
        }
    }

    @Override
    public void neuronMoved(NetworkEvent<Neuron> networkEvent) {
        return;
    }

    @Override
    public void neuronRemoved(NetworkEvent<Neuron> networkEvent) {
        pendingOperations.incrementAndGet();
        synchronized (collectionInProgress) {
            neurons.remove(networkEvent.getObject());
            ops++;
            if (!collectionInProgress.get()) {
                collectionInProgress.getAndSet(true);
                synchronized (lock) {
                    lock.notify();
                }
            }
        }
    }

    public List<NeuronGroup> getInputGroups() {
        return new ArrayList<NeuronGroup>(inputGroups);
    }

    private int getAvailableConsumerProcessors() {
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
     * A memory saving version of a BlockingQueue specifically set up to prevent
     * memory leaks in the form of LinkedList nodes and with the express purpose
     * of making synchronization points easier to implement.
     *
     * @author Zoë Tosi
     */
    public class CyclicTaskQueue {

        // TODO: Make this dependent on the number of neurons to be update and
        // the number of available processor cores.
        //private static final int DEFAULT_TASK_PARTITION_RATIO = 128;

        private AtomicInteger indexPtr;

        private BufferedUpdateTask[] taskArray;

        private int taskPartition;

        //        public CyclicTaskQueue(Collection<Neuron> tasks) {
        //            this(tasks, DEFAULT_TASK_PARTITION_RATIO);
        //        }

        public CyclicTaskQueue(Collection<Neuron> tasks, int taskPartition) {
            this.taskPartition = taskPartition;
            repopulateQueue(tasks);
        }

        public synchronized void repopulateQueue(Collection<Neuron> tasks) {
            setUpCallableTasks();
            int chunkSize = (int) Math.floor(tasks.size() / taskPartition);
            int remainingTasks = tasks.size() % taskPartition;
            Iterator<Neuron> taskIter = tasks.iterator();
            if (remainingTasks != 0) {
                taskArray = new BufferedUpdateTask[taskPartition + 1];
                indexPtr = new AtomicInteger(taskPartition + 1);
                for (int i = 0; i < taskPartition; i++) {
                    Neuron[] neurArr = new Neuron[chunkSize];
                    for (int j = 0; j < chunkSize; j++) {
                        neurArr[j] = taskIter.next();
                    }
                    taskArray[i] = new BufferedUpdateTask(neurArr);
                }
                Neuron[] neurArr = new Neuron[remainingTasks];
                for (int j = 0; j < remainingTasks; j++) {
                    neurArr[j] = taskIter.next();
                }
                taskArray[taskPartition] = new BufferedUpdateTask(neurArr);
            } else {
                taskArray = new BufferedUpdateTask[taskPartition];
                indexPtr = new AtomicInteger(taskPartition);
                for (int i = 0; i < taskPartition; i++) {
                    Neuron[] neurArr = new Neuron[chunkSize];
                    for (int j = 0; j < chunkSize; j++) {
                        neurArr[j] = taskIter.next();
                    }
                    taskArray[i] = new BufferedUpdateTask(neurArr);
                }
            }
            indexPtr.set(taskArray.length);
        }

        public Task take() {
            //            synchronized (indexPtr) {
            //                if (indexPtr.get() >= taskArray.length) {
            //                    syncPoint.setWorkAvailable(false);
            //                    return syncPoint;
            //                }
            return taskArray[indexPtr.getAndIncrement()];

        }

        List<Callable<Task>> taskList = new ArrayList<Callable<Task>>();

        public void setUpCallableTasks() {
            taskList.clear();
            taskList = new ArrayList<Callable<Task>>();
            int chunkSize = neurons.size() / currentAvailableProcessors;
            Neuron[] neuronTasks = null;
            int i = 0;
            int j = 0;
            int k = 1;
            for (Neuron n : neurons) {
                if (j % chunkSize == 0) {
                    if (neuronTasks != null) {
                        taskList.add(new CallableTask(new BufferedUpdateTask(neuronTasks)));
                        //System.out.println(neuronTasks.length + " " + k++);
                    }
                    i = 0;
                    neuronTasks = new Neuron[((neurons.size() - j) > chunkSize) ? chunkSize : (neurons.size() - j)];
                }
                neuronTasks[i] = n;
                i++;
                j++;
            }
            taskList.add(new CallableTask(new BufferedUpdateTask(neuronTasks)));
            //System.out.println(neuronTasks.length + " " + k++);
        }

        public List<Callable<Task>> getCallableTasks() {
            return taskList;
        }

        public int size() {
            int size = 0;
            for (BufferedUpdateTask but : taskArray) {
                size += but.getHosts().length;
            }
            return size;
        }

        public void reset() {
            synchronized (indexPtr) {
                indexPtr.set(0);
                // syncPoint.setWorkAvailable(true);
            }
        }

    }

    private static class CallableTask implements Callable<Task> {

        public final Task t;

        public CallableTask(Task t) {
            this.t = t;
        }

        @Override
        public Task call() throws Exception {
            t.perform();
            return t;
        }

    }

    /**
     * Test main to demonstrate performance improvements over serial updates
     * without a GUI.
     *
     * @param args
     */
    public static void main(String[] args) {
        final int numNeurons = 1992;
        double density = .1;
        System.out.println(System.getProperty("java.vm.name"));
        Scanner keyboard = new Scanner(System.in);
        System.out.println("Press any key, then ENTER.");
        String beginToken = keyboard.next();
        long start = System.nanoTime();
        Network net = new Network();
        net.setFireUpdates(false);
        net.setTimeStep(0.1);
        NeuronGroup ng = new NeuronGroup(net, numNeurons);
        ng.setRecordAsSpikes(true);
        ng.setLabel(beginToken);
        IzhikevichRule upRule = new IzhikevichRule();
        upRule.setiBg(0);
        upRule.setAddNoise(true);
        ng.setNeuronType(upRule);
        ProbabilityDistribution rand = NormalDistribution.create();
        for (Neuron neuron : ng.getNeuronList()) {
            IzhikevichRule iz = new IzhikevichRule();
            if (Math.random() < 0.2) {
                neuron.setPolarity(Polarity.INHIBITORY);
                iz.setRefractoryPeriod(1.0);
                double rVal = Math.random();
                iz.setA(0.02 + (0.08 * rVal));
                iz.setB(0.25 - (0.05 * rVal));
                iz.setC(-65);
                iz.setD(2);
                ((NormalDistribution) rand).setStandardDeviation(0.5);
            } else {
                neuron.setPolarity(Polarity.EXCITATORY);
                iz.setRefractoryPeriod(2.0);
                iz.setA(0.02);
                iz.setB(0.2);
                double rVal = Math.random();
                rVal *= rVal;
                iz.setC(-65.0 + (15.0 * rVal));
                iz.setD(8.0 - (6 * rVal));
                ((NormalDistribution) rand).setStandardDeviation(1.2);
            }
            iz.setiBg(3.5);
            iz.setAddNoise(true);
            iz.setNoiseGenerator(rand);
            neuron.setUpdateRule(iz);
        }
        GridLayout gl = new GridLayout();
        gl.layoutNeurons(ng.getNeuronList());
        ProbabilityDistribution exRand =
                LogNormalDistribution.builder()
                        .polarity(Polarity.EXCITATORY)
                        .location(0.25)
                        .scale(1)
                        .build();

        ProbabilityDistribution inRand =
                LogNormalDistribution.builder()
                        .polarity(Polarity.INHIBITORY)
                        .location(2)
                        .scale(2)
                        .build();

        System.out.println("Begin Network Construction...");
        SynapseGroup sg = SynapseGroup.createSynapseGroup(
                ng,
                ng,
                new Sparse(density, false, false),
                .8,
                exRand,
                inRand
                );
        for (Synapse s : sg.getAllSynapses()) {
            s.setId(null);
            s.setFrozen(true);
            s.forceSetStrength(s.getStrength() / 5);
        }
        sg.setSpikeResponder(new ConvolvedJumpAndDecay(), Polarity.EXCITATORY);
        ConvolvedJumpAndDecay inhibJD = new ConvolvedJumpAndDecay();
        inhibJD.setTimeConstant(6);
        sg.setSpikeResponder(inhibJD, Polarity.INHIBITORY);
        //STDPRule stdp = new STDPRule();
        //stdp.setLearningRate(0.0001);
        //sg.setLearningRule(stdp, Polarity.BOTH);
        net.addGroup(ng);
        net.addGroup(sg);
        long end = System.nanoTime();
        System.out.println("End Network construction");
        System.out.println("Time: " + SimbrainMath.roundDouble((end - start) / Math.pow(10, 9), 6));
        System.out.println(ng.size() + " neurons.");
        System.out.println(sg.size() + " synapses.");
        System.out.println("Begin Test? (Y/N)");
        String cont = keyboard.next();
        keyboard.close();
        if (!(cont.matches("Y") || cont.matches("y"))) {
            return;
        }
        net.getUpdateManager().clear();
        ConcurrentBufferedUpdate cbu = ConcurrentBufferedUpdate.createConcurrentBufferedUpdate(net);
        net.getUpdateManager().addAction(cbu);
        int siz = 0;
        for (BufferedUpdateTask but : cbu.taskSet.taskArray) {
            siz += but.getHosts().length;
        }
        //        System.out.println(siz);
        System.out.println();
        for (int i = 0; i < 10000; i++) {
            net.update();
            //            int upC = i+1;
            //            int synC = i+1;
            ////            for (Neuron n : ng.getNeuronList()) {
            //                if(((IzhikevichRule) n.getUpdateRule()).upCount.intValue() != upC) {
            //                	System.out.println("UP " + upC + " " + ((IzhikevichRule) n.getUpdateRule()).upCount.intValue());
            //                }
            //                if(n.syncCount.intValue() != synC) {
            //                	System.out.println("SYN " + synC + " " + n.syncCount.intValue());
            //                }
            //            }
        }
        //
        //        for (Neuron n : ng.getNeuronList()) {
        //            System.out.println(((IzhikevichRule) n.getUpdateRule()).getFiringRate(n));
        //        }
        // // final int TEST_ITERATIONS = 500;
        //net.getUpdateManager().clear();
        //ConcurrentBufferedUpdate cbu = ConcurrentBufferedUpdate
        //        .createConcurrentBufferedUpdate(net);
        //net.getUpdateManager().addAction(cbu);
        //        System.out.println(cbu.currentAvailableProcessors);
        // Quick tune up...
        for (int i = 0; i < 10000; i++) {
            if (i % 100 == 0) {
                System.out.println(i + "...");
            }
            net.update();
        }
        //        // System.out.println("End tune-up");
        //        // for (NetworkUpdateAction nua :
        //        // net.getUpdateManager().getActionList()) {
        //        // if (nua instanceof ConcurrentBufferedUpdate) {
        //        // ((ConcurrentBufferedUpdate) nua).shutdown();
        //        // }
        //        // }
        //
        //        // SerialExecution
        //       ng.startRecording();
        //        // start = System.nanoTime();
        //        // net.getUpdateManager().clear();
        //        // net.getUpdateManager().addAction(new UpdateGroup(sg));
        //        // net.getUpdateManager().addAction(new UpdateGroup(ng));
        //        // net.getUpdateManager().addAction(new NeuronGroupRecorder(ng));
        //        // for (int i = 0; i < TEST_ITERATIONS; i++) {
        //        // net.update();
        //        // }
        //        // ng.stopRecording();
        //        // end = System.nanoTime();
        //        // System.out.println("Serial: "
        //        // + SimbrainMath.roundDouble((end - start) / Math.pow(10, 9), 6));
        //
        //        // net.getUpdateManager().clear();
        //        // net.getUpdateManager().addAction(new ConcurrentBufferedUpdate(net));
        //        // net.getUpdateManager().addAction(new NeuronGroupRecorder(ng));
        //        // ng.startRecording();
        start = System.nanoTime();
        ng.startRecording(new File("outs.csv"));
        for (int i = 0; i < 100000; i++) {
            net.update();
        }
        //ng.stopRecording();
        end = System.nanoTime();
        System.out.println("Parallel: " + SimbrainMath.roundDouble((end - start) / Math.pow(10, 9), 6));
        System.exit(0);
        return;
    }

    @Override
    public void groupUpdated(Group group) {
    }

}
