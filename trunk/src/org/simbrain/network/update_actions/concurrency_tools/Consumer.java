package org.simbrain.network.update_actions.concurrency_tools;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Consumer implements Runnable {

	private volatile CyclicBarrier barrier;

	private final BlockingQueue<Task> taskQueue;
	
	private final int id_No;

	public Consumer(CyclicBarrier barrier, BlockingQueue<Task> taskQueue, int no) {
		this.barrier = barrier;
		this.taskQueue = taskQueue;
		this.id_No = no;
	}

	@Override
	public void run() {
		while(true) {
			try {
				Task t = taskQueue.take();
				if (t.isPoison()) {
					barrier.await();
				} else {
					t.perform();
				}
			} catch (InterruptedException | BrokenBarrierException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void setBarrier(CyclicBarrier barrier) {
		this.barrier = barrier;
	}

	public int getId_no() {
		return id_No;
	}
	
}
