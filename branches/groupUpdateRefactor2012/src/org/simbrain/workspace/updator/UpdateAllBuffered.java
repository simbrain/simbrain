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
package org.simbrain.workspace.updator;

import java.util.List;

import org.apache.log4j.Logger;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Update all components and couplings as follows. First update couplings using
 * a buffering system whereby the order in which they are updated does not
 * matter (read all producer values, write them to a buffer, then read all
 * buffer values and write them to the consumers). Then update all the
 * components.
 * 
 * In workspace updater, the following happens. Each component update call is
 * fed to an executor service which uses as many threads as it's configured to
 * use (it defaults to the number of available processors which can be changed.)
 * Then the executing thread waits on a countdown latch. Each component update
 * decrements the latch so that after the last update is complete, the thread
 * waiting on the latch wakes up and updates all the couplings.
 * 
 * @author jyoshimi
 */
public class UpdateAllBuffered implements UpdateAction {

    /** Provides access to workspace updater. */
    private final WorkspaceUpdater workspaceUpdater;
    
    /** The static logger for the class. */
    static final Logger LOGGER = Logger.getLogger(UpdateAllBuffered.class);

    /**
     * @param controls update controls
     */
    public UpdateAllBuffered(WorkspaceUpdater controls) {
        this.workspaceUpdater = controls;
    }

    /** 
     * {@inheritDoc}
     */
    public void invoke() {
        List<? extends WorkspaceComponent> components = workspaceUpdater
                .getComponents();

        int componentCount = components.size();

        if (componentCount < 1) {
            return;
        }

        LOGGER.trace("updating couplings");
        workspaceUpdater.updateCouplings();

        LOGGER.trace("creating latch");
        LatchCompletionSignal latch = new LatchCompletionSignal(
                componentCount);

        LOGGER.trace("updating components");
        for (WorkspaceComponent component : components) {
            workspaceUpdater.updateComponent(component, latch);
        }
        LOGGER.trace("waiting");
        latch.await();
        LOGGER.trace("update complete");    
     }


    @Override
    public String getDescription() {
        return "Buffered update of all components and couplings.";
    }

	@Override
	public String getLongDescription() {
		return getDescription();
	}


}
