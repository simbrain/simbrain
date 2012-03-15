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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Default controls used by Controllers to manage updates.
 */
public class DefaultUpdateControls implements UpdateControls {

    /** The static logger for the class. */
    static final Logger LOGGER = Logger.getLogger(DefaultUpdateControls.class);

    /** The parent workspace. */
    private final Workspace workspace;

    /** The executor service for doing updates. */
    private final ExecutorService service;

    /**
     * Construct the default controls.
     *
     * @param workspace parent workspace
     * @param service executor service
     */
    public DefaultUpdateControls(Workspace workspace, ExecutorService service) {
        this.workspace = workspace;
        this.service = service;
    }

    /**
     * {@inheritDoc}
     */
    public List<? extends WorkspaceComponent> getComponents() {
        List<? extends WorkspaceComponent> components = workspace
                .getComponentList();
        synchronized (components) {
            components = new ArrayList<WorkspaceComponent>(components);
        }

        return components;
    }

    /**
     * {@inheritDoc}
     */
    public void updateComponent(final WorkspaceComponent component,
            final CompletionSignal signal) {

        // If update is turned off on this component, return
        if (component.getUpdateOn() == false) {
            signal.done();
            return;
        }

        Collection<ComponentUpdatePart> parts = component.getUpdateParts();

        final LatchCompletionSignal partsSignal = new LatchCompletionSignal(
                parts.size()) {
            public void done() {
                super.done();

                /*
                 * I'm not 100% sure this is safe. The JavaDocs don't say it
                 * isn't but they don't say it is either. If a deadlock occurs
                 * in the caller to updateComponent, this may be the issue.
                 */
                if (getLatch().getCount() <= 0) {
                    signal.done();
                }
            }
        };

        for (ComponentUpdatePart part : parts) {
            service.submit(part.getUpdate(partsSignal));
        }
    }

    /**
     * Update couplings.
     */
    public void updateCouplings() {
        workspace.getCouplingManager().updateAllCouplings();
        LOGGER.trace("couplings updated");
        workspace.getUpdator().notifyCouplingsUpdated();
    }


    /**
     * {@inheritDoc}
     */
    public void updateOutgoingCouplings(WorkspaceComponent component) {
        workspace.getCouplingManager().updateOutgoingCouplings(component);
    }

    /**
     * {@inheritDoc}
     */
    public void updateIncomingCouplings(WorkspaceComponent component) {
        workspace.getCouplingManager().updateIncomingCouplings(component);
    }
}
