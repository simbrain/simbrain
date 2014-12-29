package org.simbrain.network.update_actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.groups.Group;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.listeners.GroupListener;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.network.listeners.NeuronListener;
import org.simbrain.network.update_actions.concurrency_tools.BufferedUpdateTask;
import org.simbrain.network.update_actions.concurrency_tools.Consumer;
import org.simbrain.network.update_actions.concurrency_tools.PoisonTask;
import org.simbrain.network.update_actions.concurrency_tools.Task;

public class ConcurrentBufferedUpdate implements NetworkUpdateAction, NeuronListener, GroupListener {

	private final List<Thread> consumerThreads = new ArrayList<Thread>();
	
	private final BlockingQueue<Task> tasksQueue;
	
	private volatile int currentAvailableProcessors;
	
	private volatile CyclicBarrier synchronizingBarrier;
	
	private final Set<Neuron> neurons = Collections.synchronizedSet(
			new HashSet<Neuron>(15000));
	
	private AtomicInteger pendingOperations = new AtomicInteger(0);
	
	private final Network network;
	
	private volatile Thread producer;
	
	public ConcurrentBufferedUpdate(Network network) {
		this.network = network;
		currentAvailableProcessors = getAvailableConsumerProcessors();
		tasksQueue = new LinkedBlockingQueue<Task>();
		synchronizingBarrier = new CyclicBarrier(currentAvailableProcessors
				+ 1);
		for (int i = 0; i < currentAvailableProcessors; i++) {
			consumerThreads.add(new Thread(new Consumer(synchronizingBarrier,
					tasksQueue, i)));
			//Start the consumers, but make them wait.
			//tasksQueue.add(new PoisonTask()); 
			consumerThreads.get(i).start();
		}
		for (Neuron n : network.getFlatNeuronList()) {
			neurons.add(n);
		}
		for (NeuronGroup ng : network.getFlatNeuronGroupList()) {
			neurons.addAll(ng.getNeuronList());
		}
		// TODO: Make a static initializer
		network.addGroupListener(this);
		network.addNeuronListener(this);
	}
	
	@Override
	public void invoke() {
		producer = Thread.currentThread();
		try {
//			if (currentAvailableProcessors != getAvailableConsumerProcessors()) {
//				availableProcessorChange();
//			}
			synchronized(neurons) {
				int i = 0;
				Neuron [] block = new Neuron[100];
				for (Neuron n : neurons) {
					if (i >= 100) {
						i = 0;
						tasksQueue.put(new BufferedUpdateTask(block));
						block = new Neuron[100];
					}
					block[i] = n;
					i++;
				}
				if (i > 1) {
					tasksQueue.put(new BufferedUpdateTask(block));
				}
			}
			for (int i = 0; i < currentAvailableProcessors; i++) {
				tasksQueue.put(new PoisonTask());
			}
			synchronizingBarrier.await();
			while (pendingOperations.get() > 0) {
				synchronized(this) {
					wait();
				}
			}
			for(Neuron n : neurons) {
				n.setToBufferVals();
			}
		} catch (InterruptedException | BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
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
			consumerThreads.add(new Thread(new Consumer(synchronizingBarrier,
					tasksQueue, currentAvailableProcessors)));
			currentAvailableProcessors++;
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
			synchronized(neurons) {
				for (Neuron n : ((NeuronGroup)e.getObject()).getNeuronList()) {
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
			synchronized(neurons) {
				for (Neuron n : ((NeuronGroup)e.getObject()).getNeuronList()) {
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
	
	public int getAvailableConsumerProcessors() {
		return Runtime.getRuntime().availableProcessors();
	}
	
	private int decrementPendingOperations() {
		synchronized(this) {
			producer.notify();
		}
		return pendingOperations.decrementAndGet();
	}
}
