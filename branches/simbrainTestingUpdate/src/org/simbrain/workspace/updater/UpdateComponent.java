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
 * Update a specific workspace component.
 *
 * @author jyoshimi
 */
public class UpdateComponent implements UpdateAction {

    /** Reference to component. */
    private final WorkspaceComponent component;

    /** Provides access to update controls. */
    private final WorkspaceUpdater updater;

    /**
     * @param component component to update
     */
    public UpdateComponent(WorkspaceUpdater updater,
            WorkspaceComponent component) {
        this.updater = updater;
        this.component = component;
    }

    /**
     * {@inheritDoc}
     */
    public void invoke() {
        // TODO: Is the below needed? Would component.update() suffice?
        LatchCompletionSignal latch = new LatchCompletionSignal(1);
        updater.updateComponent(component, latch);
        latch.await();
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        if (component == null) {
            return "Invalid action";
        } else if (component.getName() != null) {
            return "Update " + component.getName();
        } else {
            return "Action description is null";
        }
    }

    @Override
    public String getLongDescription() {
        return getDescription();
    }

    /**
     * Returns a reference to this component.
     *
     * @return the component
     */
    public WorkspaceComponent getComponent() {
        return component;
    }

}
