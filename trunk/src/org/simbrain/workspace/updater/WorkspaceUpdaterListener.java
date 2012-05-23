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

/**
 * The listener interface for observers interested in events related to
 * the workspace updater.
 */
public interface WorkspaceUpdaterListener {

    /**
     * Called when the couplings are updated.
     *
     * @param update The number of the update.
     */
    void updatedCouplings(int update);

    /**
     * Called when the update controller is changed.
     */
    void changedUpdateController();

    /**
     * Called when the number of threads on an update controller is changed.
     */
    void changeNumThreads();

    /**
     * Called every time the workspace is updated.
     */
    void workspaceUpdated();

    /**
     * Called when workspace "run" begins.
     */
    void updatingStarted();

    /**
     * Called when workspace "run" ends.
     */
    void updatingFinished();
}