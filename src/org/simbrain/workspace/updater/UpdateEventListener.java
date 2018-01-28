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

import org.simbrain.workspace.WorkspaceComponent;

/**
 * The listener interface for observers interested in events related to component updates and update actions.
 *
 * @author Matt Watson, Tim Shea
 */
public interface UpdateEventListener {

    /**
     * Called before an update action is invoked.
     *
     * @param action The action to be invoked.
     * @param nanoTime The time in nanaseconds at which the event was triggered.
     */
    void beforeUpdateAction(UpdateAction action, long nanoTime);

    /**
     * Called after an update action is invoked.
     *
     * @param action The action which was invoked.
     * @param nanoTime The time in nanaseconds at which the event was triggered.
     */
    void afterUpdateAction(UpdateAction action, long nanoTime);

    /**
     * Called when a component update begins.
     *
     * @param component The component being updated.
     * @param update The number of the update.
     * @param thread The thread doing the update.
     * @param nanoTime The time in nanaseconds at which the event was triggered.
     */
    void beforeComponentUpdate(WorkspaceComponent component, int update, int thread, long nanoTime);

    /**
     * Called when a component update ends.
     *
     * @param component The component that was updated.
     * @param update The number of the update.
     * @param thread The thread doing the update.
     * @param nanoTime The time in nanaseconds at which the event was triggered.
     */
    void afterComponentUpdate(WorkspaceComponent component, int update, int thread, long nanoTime);

}