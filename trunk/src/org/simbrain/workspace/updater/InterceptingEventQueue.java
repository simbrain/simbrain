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
package org.simbrain.workspace.updater;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.InvocationEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.simbrain.workspace.Workspace;

/**
 * An event queue implementing the TaskSynchronizationManager interface. When
 * AWT events are received, each invocation event is wrapped in a synchronizing
 * object. If the queueTasks toggle is on, each invocation event is queued until
 * the releaseTasks() method is called. When runTasks() is called, all queued
 * events are executed.
 *
 * @author Matt Watson
 */
public class InterceptingEventQueue extends EventQueue implements
        TaskSynchronizationManager {
    /** the static logger for this class. */
    private static final Logger LOGGER = Logger
            .getLogger(InterceptingEventQueue.class);

    /** The workspace this object is associated with. */
    private final Workspace workspace;

    /** Internal queue for invocation events. */
    private Queue<AWTEvent> queue = new ConcurrentLinkedQueue<AWTEvent>();

    /** Flag for event queue toggling. */
    private boolean paused = false;

    /** Lock for paused flag access. */
    private Object lock = new Object();

    /** Latch used for tracking when events are done. */
    private volatile CompletionSignal signal = null;

    /**
     * A wrapper which calls the underlying signal. This is needed because the
     * synchronizing event is created prior to the latch. The event needs to
     * have a reference to call. Synchronization is critical to making this work
     * properly.
     */
    private final CompletionSignal deQueueSignal = new CompletionSignal() {
        public void done() {
            signal.done();
        }
    };

    /**
     * Creates a new instance for the given workspace.
     *
     * @param workspace The workspace to associate this event queue with.
     */
    public InterceptingEventQueue(final Workspace workspace) {
        this.workspace = workspace;
    }

    /**
     * Starts queuing tasks.
     */
    public void queueTasks() {
        synchronized (lock) {
            paused = true;
        }
    }

    /**
     * Releases the queuing flag.
     */
    public void releaseTasks() {
        synchronized (lock) {
            paused = false;
        }
    }

    /**
     * Executes all queued invocation events.
     */
    public void runTasks() {
        LOGGER.debug("starting runTasks");

        Collection<AWTEvent> events = new ArrayList<AWTEvent>();

        // Add queued events to the list
        for (AWTEvent event; (event = queue.poll()) != null;) {
            events.add(event);
        }

        LatchCompletionSignal signal = new LatchCompletionSignal(events.size());
        this.signal = signal;

        // Post all events
        for (AWTEvent event : events) {
            LOGGER.debug("event unqueued: " + event);
            super.postEvent(event);
        }

        // Wait for posted events to complete
        signal.await();

        this.signal = null;

        LOGGER.debug("finished runTasks");
    }


    /**
     * Posts AWTEvents. If the event is an InvocationEvent, it's wrapped in a
     * synchronizingInvocationEvent. If queuing is on, these invocation events
     * are queued.
     *
     * @param event The AWTEvent to post.
     */
    public void postEvent(final AWTEvent event) {
        LOGGER.trace("event posted: " + event);

        // final CompletionSignal signal = this.signal;

        if (event instanceof InvocationEvent) {
            synchronized (lock) {
                if (paused) {
                    LOGGER.trace("event queued: " + event);

                    queue.add(new SynchronizingInvocationEvent(
                            (InvocationEvent) event, workspace, deQueueSignal));
                } else {
                    LOGGER.trace("event passed: " + event);

                    super.postEvent(new SynchronizingInvocationEvent(
                            (InvocationEvent) event, workspace,
                            CompletionSignal.IGNORE));
                }
            }
        } else {
            super.postEvent(event);
        }
    }
}