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
package org.simbrain.network.update_actions.concurrency_tools;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * The underlying runnable consumer assigned to a thread, which consumes network
 * update tasks i.e. executes them.
 * 
 * @author Zach Tosi
 *
 */
public class Consumer implements Runnable {

	/** 
	 * The synchronizing barrier which causes this consumer to wait for other
	 * other entities to finish their tasks before this consumer attempts to
	 * take on more tasks.
	 */
	private volatile CyclicBarrier barrier;

	/** 
	 * The blocking queue containing tasks this consumer will attempt to
	 * execute.
	 */
	private final BlockingQueue<Task> taskQueue;
	
	/** 
	 * An optional int identifier number used to label this consumer should
	 * such a thing be desired.
	 */
	private final int id_No;
	
	/** 
	 * A tag indicating if this consumer is live and can/will continue to
	 * request and execute successfully requested tasks.
	 */
	private volatile boolean live = true;
	
	/**
	 * 
	 * @param barrier
	 * @param taskQueue
	 * @param no
	 */
	public Consumer(CyclicBarrier barrier, BlockingQueue<Task> taskQueue,
			int no) {
		this.barrier = barrier;
		this.taskQueue = taskQueue;
		this.id_No = no;
	}

	/**
	 * Executes tasks or waits on a cyclic barrier until a poison task is
	 * consumed which kills this consumer (sets {@link #live} to false).
	 */
	@Override
	public void run() {
		while(live) {
			try {
				Task t = taskQueue.take();
				if (t.isPoison()) {
					live = false;
					barrier.await();
				} else if (t.isWaiting()) {
					barrier.await();
				} else {
					t.perform();
				}
			} catch (InterruptedException | BrokenBarrierException e) {
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
