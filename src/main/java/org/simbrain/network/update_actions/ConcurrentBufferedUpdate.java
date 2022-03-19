// /*
//  * Part of Simbrain--a java-based neural network kit
//  * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
//  *
//  * This program is free software; you can redistribute it and/or modify
//  * it under the terms of the GNU General Public License as published by
//  * the Free Software Foundation; either version 2 of the License, or
//  * (at your option) any later version.
//  *
//  * This program is distributed in the hope that it will be useful,
//  * but WITHOUT ANY WARRANTY; without even the implied warranty of
//  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  * GNU General Public License for more details.
//  *
//  * You should have received a copy of the GNU General Public License
//  * along with this program; if not, write to the Free Software
//  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//  */
// package org.simbrain.network.update_actions;
//
// import org.simbrain.network.connections.Sparse;
// import org.simbrain.network.core.Network;
// import org.simbrain.network.core.NetworkUpdateAction;
// import org.simbrain.network.core.Neuron;
// import org.simbrain.network.core.Synapse;
// import org.simbrain.network.events.NetworkEvents;
// import org.simbrain.network.groups.NeuronGroup;
// import org.simbrain.network.groups.SynapseGroup;
// import org.simbrain.network.layouts.GridLayout;
// import org.simbrain.network.neuron_update_rules.IzhikevichRule;
// import org.simbrain.network.synapse_update_rules.spikeresponders.ConvolvedJumpAndDecay;
// import org.simbrain.network.update_actions.concurrency_tools.BufferedUpdateTask;
// import org.simbrain.network.update_actions.concurrency_tools.Task;
// import org.simbrain.util.SimbrainConstants.Polarity;
// import org.simbrain.util.math.ProbDistributions.LogNormalDistribution;
// import org.simbrain.util.math.ProbDistributions.NormalDistribution;
// import org.simbrain.util.math.ProbabilityDistribution;
// import org.simbrain.util.math.SimbrainMath;
// import org.simbrain.workspace.updater.UpdateAction;
//
// import java.io.File;
// import java.util.*;
// import java.util.concurrent.*;
// import java.util.concurrent.atomic.AtomicBoolean;
// import java.util.concurrent.atomic.AtomicInteger;
//
// /**
//  * A class which performs a parallelized update of an entire network. Any given
//  * thread which is updating a neuron is also responsible for updating all the
//  * afferent synapses of that neuron. Update occurs synchronously using the same
//  * technique as buffered update, except across multiple threads; execution does
//  * not continue until each thread has completed its update task.
//  * <p>
//  * ConcurrentBufferedUpdate contains an internal representation of the neurons
//  * in the network which it parses out into smaller tasks to be executed in parallel.
//  * This model updates itself based on changes to the network.
//  * <p>
//  * @author Zoë Tosi
//  */
// public class ConcurrentBufferedUpdate implements UpdateAction {
//
//     /**
//      * The initial capacity of the set containing this class's private neuron
//      * list, which is synchronized to the underlying network.
//      */
//     private static final int INITIAL_CAPACITY = (int) Math.ceil(15000 / 0.75);
//
//     /**
//      * The current number of available processors to the JVM, should that value
//      * change. Any changes to this value will be reflected in the number of
//      * consumer threads upon subsequent invocation.
//      */
//     private volatile int currentAvailableProcessors;
//
//     /**
//      * This class's private set of neurons in the network, used for updating.
//      */
//     private final transient Set<Neuron> neurons = Collections.synchronizedSet(new HashSet<Neuron>(INITIAL_CAPACITY));
//
//     private transient TaskList taskSet;
//
//     /**
//      * A count of the number of network changes which have taken place.
//      */
//     private AtomicInteger pendingOperations = new AtomicInteger(0);
//
//     /**
//      * A copy of the network.
//      */
//     private final transient Network network;
//
//     /**
//      * A reference to the current thread which is invoking ConcurrenBufferedUpdate's {@link #invoke()}
//      * method.
//      */
//     private transient volatile Thread invocationThread;
//
//     /**
//      * A reference to all "input groups"--groups that are being driven by an external input
//      * that must be updated by reading the next input as opposed to doing a standard
//      * buffered update. These are the neurons being driven by the experimenter.
//      */
//     private final List<NeuronGroup> inputGroups = new ArrayList<NeuronGroup>();
//
//     /**
//      * A reference to all neuron groups for which activity is being recorded and saved
//      * to a file.
//      */
//     private final List<NeuronGroup> outputGroups = new ArrayList<NeuronGroup>();
//
//     /**
//      * An object used for synchronize blocks across methods. Used to prevent race
//      * conditions between the threads organizing and accounting for the parts of the network
//      * and listener/event threads.
//      */
//     private Object lock = new Object();
//
//     private AtomicBoolean collectionInProgress = new AtomicBoolean();
//
//     private volatile int ops = 0;
//
//     private transient ExecutorService executors;
//
//     private transient Thread collectorThread = new Thread(new Runnable() {
//         @Override
//         public void run() {
//             try {
//                 while (true) {
//                     synchronized (lock) {
//                         while (!collectionInProgress.get()) {
//                             lock.wait();
//                         }
//                     }
//                     // Wait for an entire second so that every individual neuron
//                     // removed over the course of the second will be accounted
//                     // for.
//                     Thread.sleep(1000);
//                     synchronized (collectionInProgress) {
//                         taskSet.populateList(neurons);
//                         collectionInProgress.getAndSet(false);
//                         pendingOperations.set(pendingOperations.get() - ops);
//                         ops = 0;
//                     }
//                 }
//             } catch (InterruptedException e) {
//                 e.printStackTrace();
//             }
//         }
//     });
//
//     /**
//      * A static factory method that creates a concurrent buffered update class
//      * for a network. See {@link #ConcurrentBufferedUpdate(Network)}.
//      */
//     public static ConcurrentBufferedUpdate createConcurrentBufferedUpdate(final Network network) {
//         ConcurrentBufferedUpdate cbu = new ConcurrentBufferedUpdate(network);
//         cbu.collectorThread.start();
//         return cbu;
//     }
//
//
//     @Override
//     public String toString() {
//         return "Concurrent Buffered Update:\n" + taskSet;
//     }
//
//     /**
//      * Creates the consumer threads, in a quantity equal to the number of
//      * available processors, since the invocationThread's job is small enough that
//      * having more threads than processors will not be problematic (they are
//      * also not active at the same time at least half the time). Also populates
//      * this classes copy of the neurons in the network.
//      *
//      * @param network the network being updated by this updater.
//      */
//     private ConcurrentBufferedUpdate(final Network network) {
//         this.network = network;
//         currentAvailableProcessors = getAvailableConsumerProcessors();
//         executors = Executors.newFixedThreadPool(currentAvailableProcessors);
//         //syncPoint = new SynchronizationPoint(currentAvailableProcessors);
//         for (Neuron n : network.getFlatNeuronList()) {
//             neurons.add(n);
//         }
//         for (NeuronGroup ng : network.getFlatNeuronGroupList()) {
//             neurons.addAll(ng.getNeuronList());
//         }
//         taskSet = new TaskList(neurons, currentAvailableProcessors);
//
//         initListeners();
//
//     }
//
//     private void initListeners() {
//
//         NetworkEvents event = network.getEvents();
//
//         event.onModelAdded(n -> {
//             if(!(n instanceof Neuron)) {
//                 return;
//             }
//             pendingOperations.incrementAndGet();
//             synchronized (collectionInProgress) {
//                 neurons.add((Neuron) n);
//                 ops++;
//                 if (!collectionInProgress.get()) {
//                     collectionInProgress.getAndSet(true);
//                     synchronized (lock) {
//                         lock.notify();
//                     }
//                 }
//             }
//         });
//
//         event.onModelRemoved(n -> {
//             if(!(n instanceof Neuron)) {
//                 return;
//             }
//             pendingOperations.incrementAndGet();
//             synchronized (collectionInProgress) {
//                 neurons.remove(n);
//                 ops++;
//                 if (!collectionInProgress.get()) {
//                     collectionInProgress.getAndSet(true);
//                     synchronized (lock) {
//                         lock.notify();
//                     }
//                 }
//             }
//         });
//     }
//
//     //TODO
//
//     //private void groupAdded(Group group) {
//     //    if (group instanceof NeuronGroup) {
//     //        NeuronGroup ng = (NeuronGroup) group;
//     //        pendingOperations.incrementAndGet();
//     //        synchronized (neurons) {
//     //            for (Neuron n : ng.getNeuronList()) {
//     //                neurons.add(n);
//     //            }
//     //            taskSet.populateList(neurons);
//     //        }
//     //        decrementPendingOperations();
//     //        ng.addPropertyChangeListener(
//     //                evt -> {
//     //                    if ("update".equals(evt.getPropertyName())) {
//     //                        synchronized (outputGroups) {
//     //                            if (ng.getActivationRecorder().isRecording()) {
//     //                                if (!outputGroups.contains(ng)) {
//     //                                    outputGroups.add(ng);
//     //                                }
//     //                            } else {
//     //                                if (outputGroups.contains(ng)) {
//     //                                    outputGroups.remove(ng);
//     //                                }
//     //                            }
//     //                        }
//     //                        synchronized (inputGroups) {
//     //                            if (ng.isInputMode()) {
//     //                                if (!inputGroups.contains(ng)) {
//     //                                    inputGroups.add(ng);
//     //                                }
//     //                            } else {
//     //                                if (inputGroups.contains(ng)) {
//     //                                    inputGroups.remove(ng);
//     //                                }
//     //                            }
//     //                        }
//     //                    }
//     //                });
//     //    } else if (group instanceof  Subnetwork) {
//     //        for(NeuronGroup ng : ((Subnetwork) group).getNeuronGroupList()) {
//     //            groupAdded(ng);
//     //        }
//     //    }
//     //}
//     //
//     //private void groupRemoved(Group group) {
//     //    if (group instanceof NeuronGroup) {
//     //        pendingOperations.incrementAndGet();
//     //        synchronized (neurons) {
//     //            for (Neuron n : ((NeuronGroup) group).getNeuronList()) {
//     //                neurons.remove(n);
//     //            }
//     //        }
//     //        taskSet.populateList(neurons);
//     //        decrementPendingOperations();
//     //    } else if (group instanceof Subnetwork) {
//     //        List<NeuronGroup> neuronGroups = ((Subnetwork) group).getNeuronGroupList();
//     //        for (NeuronGroup ng : neuronGroups) {
//     //            groupRemoved(ng);
//     //        }
//     //    } else {
//     //        return;
//     //    }
//     //}
//
//     @Override
//     public void invoke() {
//         invocationThread = Thread.currentThread();
//         // Update input neurons accordingly
//         for (int i = 0, n = inputGroups.size(); i < n; i++) {
//             inputGroups.get(i).updateInputs();
//         }
//         try {
//             synchronized (taskSet) {
//                 List<Future<Task>> results = executors.invokeAll(taskSet.getCallableTasks());
//                 for (int i = 0; i < results.size(); i++) {
//                     for (int j = 0; j < ((BufferedUpdateTask) results.get(i).get()).getHosts().length; j++)
//                     {
//                         ((BufferedUpdateTask) results.get(i).get()).getHosts()[j].update();
//                     }
//                 }
//             }
//             while (pendingOperations.get() > 0) {
//                 synchronized (invocationThread) {
//                     invocationThread.wait();
//                 }
//             }
//         } catch (InterruptedException | ExecutionException e1) {
//             e1.getCause().printStackTrace();
//             e1.printStackTrace();
//         }
//         for (int i = 0, n = outputGroups.size(); i < n; i++) {
//             outputGroups.get(i).getActivationRecorder().writeActsToFile();
//         }
//     }
//
//     @Override
//     public String getDescription() {
//         return "Parallel Buffered Update";
//     }
//
//     @Override
//     public String getLongDescription() {
//         return "Parallel Buffered Update (All Neurons)";
//     }
//
//     public List<NeuronGroup> getInputGroups() {
//         return new ArrayList<NeuronGroup>(inputGroups);
//     }
//
//     private int getAvailableConsumerProcessors() {
//         return Runtime.getRuntime().availableProcessors();
//     }
//
//     private int decrementPendingOperations() {
//         if (invocationThread != null) {
//             synchronized (invocationThread) {
//                 invocationThread.notify();
//             }
//         }
//         return pendingOperations.decrementAndGet();
//     }
//
//     /**
//      * A list of update tasks, comprised of some discrete portion of neurons
//      * in the network that need updating. Each chunk comprises an update task
//      * for a single thread in {@link ConcurrentBufferedUpdate}. TaskList takes
//      * a flat list of neurons and organizes them into those discrete tasks that it
//      * then contains.
//      *
//      * @author Zoë Tosi
//      */
//     public class TaskList {
//
//         private static final int DEFAULT_CHUNK_SIZE = 64;
//
//         private BufferedUpdateTask[] taskArray;
//
//         private List<Callable<Task>> taskList = new ArrayList<>();
//
//         private int taskPartition;
//
//         public TaskList(Collection<Neuron> tasks, int taskPartition) {
//             this.taskPartition = taskPartition;
//             populateList(tasks);
//         }
//
//         public synchronized void populateList(Collection<Neuron> tasks) {
//             setUpCallableTasks();
//             int chunkSize = (int) Math.floor(tasks.size() / taskPartition);
//             int remainingTasks = tasks.size() % taskPartition;
//             Iterator<Neuron> taskIter = tasks.iterator();
//             if (remainingTasks != 0) {
//                 taskArray = new BufferedUpdateTask[taskPartition + 1];
//                 for (int i = 0; i < taskPartition; i++) {
//                     Neuron[] neurArr = new Neuron[chunkSize];
//                     for (int j = 0; j < chunkSize; j++) {
//                         neurArr[j] = taskIter.next();
//                     }
//                     taskArray[i] = new BufferedUpdateTask(neurArr);
//                 }
//                 Neuron[] neurArr = new Neuron[remainingTasks];
//                 for (int j = 0; j < remainingTasks; j++) {
//                     neurArr[j] = taskIter.next();
//                 }
//                 taskArray[taskPartition] = new BufferedUpdateTask(neurArr);
//             } else {
//                 taskArray = new BufferedUpdateTask[taskPartition];
//                 for (int i = 0; i < taskPartition; i++) {
//                     Neuron[] neurArr = new Neuron[chunkSize];
//                     for (int j = 0; j < chunkSize; j++) {
//                         neurArr[j] = taskIter.next();
//                     }
//                     taskArray[i] = new BufferedUpdateTask(neurArr);
//                 }
//             }
//         }
//
//         public void setUpCallableTasks() {
//             taskList.clear();
//             taskList = new ArrayList<>();
//             int noChunks = (int) Math.ceil((double)neurons.size()/DEFAULT_CHUNK_SIZE);
//             int chunkSize;
//             if (noChunks <= 2 * currentAvailableProcessors) {
//                 chunkSize = DEFAULT_CHUNK_SIZE;
//             } else {
//                 int procRat = 2 * (int) Math.floor(SimbrainMath.log2(noChunks/(currentAvailableProcessors)));
//                 chunkSize = procRat * DEFAULT_CHUNK_SIZE;
//             }
//             if(noChunks == 0) {
//                 return;
//             }
//             Neuron[] neuronTasks = null;
//             int i = 0;
//             int j = 0;
//             for (Neuron n : neurons) {
//                 if (j % chunkSize == 0) {
//                     if (neuronTasks != null) {
//                         taskList.add(new CallableTask(new BufferedUpdateTask(neuronTasks)));
//                         //System.out.println(neuronTasks.length + " " + k++);
//                     }
//                     i = 0;
//                     neuronTasks = new Neuron[((neurons.size() - j) > chunkSize) ? chunkSize : (neurons.size() - j)];
//                 }
//                 neuronTasks[i] = n;
//                 i++;
//                 j++;
//             }
//             taskList.add(new CallableTask(new BufferedUpdateTask(neuronTasks)));
//         }
//
//         public List<Callable<Task>> getCallableTasks() {
//             return taskList;
//         }
//
//         public int size() {
//             int size = 0;
//             for (BufferedUpdateTask but : taskArray) {
//                 size += but.getHosts().length;
//             }
//             return size;
//         }
//
//
//         @Override
//         public String toString() {
//             StringBuilder ret = new StringBuilder();
//             // Number of tasks should be an even multiple of processors
//             ret.append("Task list contains " + size() + " neurons across " + taskPartition + " processors\n");
//             int i = 1;
//             for (Callable<Task> task : getCallableTasks()) {
//                 ret.append("Task " +  i + " handles " + ((BufferedUpdateTask)((CallableTask)task).t).getHosts().length +
//                         " neurons \n");
//                 i++;
//             }
//             return ret.toString();
//         }
//
//     }
//
//     private static class CallableTask implements Callable<Task> {
//
//         public final Task t;
//
//         public CallableTask(Task t) {
//             this.t = t;
//         }
//
//         @Override
//         public Task call() throws Exception {
//             t.perform();
//             return t;
//         }
//
//     }
//
//     /**
//      * Test main to demonstrate performance improvements over serial updates
//      * without a GUI.
//      *
//      * @param args
//      */
//     public static void main(String[] args) {
//         final int numNeurons = 1992;
//         double density = .1;
//         System.out.println(System.getProperty("java.vm.name"));
//         Scanner keyboard = new Scanner(System.in);
//         System.out.println("Press any key, then ENTER.");
//         String beginToken = keyboard.next();
//         long start = System.nanoTime();
//         Network net = new Network();
//         net.setTimeStep(0.1);
//         NeuronGroup ng = new NeuronGroup(net, numNeurons);
//         ng.getActivationRecorder().setRecordAsSpikes(true);
//         ng.setLabel(beginToken);
//         IzhikevichRule upRule = new IzhikevichRule();
//         upRule.setiBg(0);
//         upRule.setAddNoise(true);
//         ng.setNeuronType(upRule);
//         ProbabilityDistribution rand = NormalDistribution.create();
//         for (Neuron neuron : ng.getNeuronList()) {
//             IzhikevichRule iz = new IzhikevichRule();
//             if (Math.random() < 0.2) {
//                 neuron.setPolarity(Polarity.INHIBITORY);
//                 iz.setRefractoryPeriod(1.0);
//                 double rVal = Math.random();
//                 iz.setA(0.02 + (0.08 * rVal));
//                 iz.setB(0.25 - (0.05 * rVal));
//                 iz.setC(-65);
//                 iz.setD(2);
//                 ((NormalDistribution) rand).setStandardDeviation(0.5);
//             } else {
//                 neuron.setPolarity(Polarity.EXCITATORY);
//                 iz.setRefractoryPeriod(2.0);
//                 iz.setA(0.02);
//                 iz.setB(0.2);
//                 double rVal = Math.random();
//                 rVal *= rVal;
//                 iz.setC(-65.0 + (15.0 * rVal));
//                 iz.setD(8.0 - (6 * rVal));
//                 ((NormalDistribution) rand).setStandardDeviation(1.2);
//             }
//             iz.setiBg(3.5);
//             iz.setAddNoise(true);
//             iz.setNoiseGenerator(rand);
//             neuron.setUpdateRule(iz);
//         }
//         GridLayout gl = new GridLayout();
//         gl.layoutNeurons(ng.getNeuronList());
//         ProbabilityDistribution exRand =
//             LogNormalDistribution.builder()
//                 .polarity(Polarity.EXCITATORY)
//                 .location(0.25)
//                 .scale(1)
//                 .build();
//
//         ProbabilityDistribution inRand =
//             LogNormalDistribution.builder()
//                 .polarity(Polarity.INHIBITORY)
//                 .location(2)
//                 .scale(2)
//                 .build();
//
//         System.out.println("Begin Network Construction...");
//         SynapseGroup sg = SynapseGroup.createSynapseGroup(
//             ng,
//             ng,
//             new Sparse(density, false, false),
//             .8,
//             exRand,
//             inRand
//         );
//         for (Synapse s : sg.getAllSynapses()) {
//             s.setId(null);
//             s.setFrozen(true);
//             s.forceSetStrength(s.getStrength() / 5);
//         }
//         // TODO: Disabled when refactoring synapse group, which now has separate inhibitory and excitatory sr's
//         // sg.setSpikeResponder(new ConvolvedJumpAndDecay(), Polarity.EXCITATORY);
//         ConvolvedJumpAndDecay inhibJD = new ConvolvedJumpAndDecay();
//         inhibJD.setTimeConstant(6);
//         // sg.setSpikeResponder(inhibJD, Polarity.INHIBITORY);
//         //STDPRule stdp = new STDPRule();
//         //stdp.setLearningRate(0.0001);
//         //sg.setLearningRule(stdp, Polarity.BOTH);
//
//         // TODO
//         //net.addGroup(ng);
//         //net.addGroup(sg);
//         long end = System.nanoTime();
//         System.out.println("End Network construction");
//         System.out.println("Time: " + SimbrainMath.roundDouble((end - start) / Math.pow(10, 9), 6));
//         System.out.println(ng.size() + " neurons.");
//         System.out.println(sg.size() + " synapses.");
//         System.out.println("Begin Test? (Y/N)");
//         String cont = keyboard.next();
//         keyboard.close();
//         if (!(cont.matches("Y") || cont.matches("y"))) {
//             return;
//         }
//         net.getUpdateManager().clear();
//         ConcurrentBufferedUpdate cbu = ConcurrentBufferedUpdate.createConcurrentBufferedUpdate(net);
//         net.getUpdateManager().addAction(cbu);
//         int siz = 0;
//         for (BufferedUpdateTask but : cbu.taskSet.taskArray) {
//             siz += but.getHosts().length;
//         }
//         //        System.out.println(siz);
//         System.out.println();
//         for (int i = 0; i < 10000; i++) {
//             net.update();
//             //            int upC = i+1;
//             //            int synC = i+1;
//             ////            for (Neuron n : ng.getNeuronList()) {
//             //                if(((IzhikevichRule) n.getUpdateRule()).upCount.intValue() != upC) {
//             //                	System.out.println("UP " + upC + " " + ((IzhikevichRule) n.getUpdateRule()).upCount.intValue());
//             //                }
//             //                if(n.syncCount.intValue() != synC) {
//             //                	System.out.println("SYN " + synC + " " + n.syncCount.intValue());
//             //                }
//             //            }
//         }
//         //
//         //        for (Neuron n : ng.getNeuronList()) {
//         //            System.out.println(((IzhikevichRule) n.getUpdateRule()).getFiringRate(n));
//         //        }
//         // // final int TEST_ITERATIONS = 500;
//         //net.getUpdateManager().clear();
//         //ConcurrentBufferedUpdate cbu = ConcurrentBufferedUpdate
//         //        .createConcurrentBufferedUpdate(net);
//         //net.getUpdateManager().addAction(cbu);
//         //        System.out.println(cbu.currentAvailableProcessors);
//         // Quick tune up...
//         for (int i = 0; i < 10000; i++) {
//             if (i % 100 == 0) {
//                 System.out.println(i + "...");
//             }
//             net.update();
//         }
//         //        // System.out.println("End tune-up");
//         //        // for (NetworkUpdateAction nua :
//         //        // net.getUpdateManager().getActionList()) {
//         //        // if (nua instanceof ConcurrentBufferedUpdate) {
//         //        // ((ConcurrentBufferedUpdate) nua).shutdown();
//         //        // }
//         //        // }
//         //
//         //        // SerialExecution
//         //       ng.startRecording();
//         //        // start = System.nanoTime();
//         //        // net.getUpdateManager().clear();
//         //        // net.getUpdateManager().addAction(new UpdateGroup(sg));
//         //        // net.getUpdateManager().addAction(new UpdateGroup(ng));
//         //        // net.getUpdateManager().addAction(new NeuronGroupRecorder(ng));
//         //        // for (int i = 0; i < TEST_ITERATIONS; i++) {
//         //        // net.update();
//         //        // }
//         //        // ng.stopRecording();
//         //        // end = System.nanoTime();
//         //        // System.out.println("Serial: "
//         //        // + SimbrainMath.roundDouble((end - start) / Math.pow(10, 9), 6));
//         //
//         //        // net.getUpdateManager().clear();
//         //        // net.getUpdateManager().addAction(new ConcurrentBufferedUpdate(net));
//         //        // net.getUpdateManager().addAction(new NeuronGroupRecorder(ng));
//         //        // ng.startRecording();
//         start = System.nanoTime();
//         ng.getActivationRecorder().startRecording(new File("outs.csv"));
//         for (int i = 0; i < 100000; i++) {
//             net.update();
//         }
//         //ng.stopRecording();
//         end = System.nanoTime();
//         System.out.println("Parallel: " + SimbrainMath.roundDouble((end - start) / Math.pow(10, 9), 6));
//         System.exit(0);
//         return;
//     }
//
// }
