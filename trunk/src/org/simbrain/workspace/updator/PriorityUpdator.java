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

import org.simbrain.workspace.WorkspaceComponent;

/**
 * Update the workspace using priorities. WorkspaceComponents are associated
 * with update priorities, and are updated in the order of these priorities.
 *
 * @author jyoshimi
 */
public class PriorityUpdator implements UpdateController {

    // This form of update is single-threaded.
    // TODO: Update in a way that uses "update parts". But there are no
    // use-cases for those yet.

    /**
     * {@inheritDoc}
     */
    public void doUpdate(final UpdateControls controls) {

        if (controls.getComponents().size() < 1) {
            return;
        }

        for (WorkspaceComponent component : controls.getComponents()) {
            // System.out.println("Updating " + component.getName());
            // System.out.println("Updating incoming couplings");
            controls.updateIncomingCouplings(component);
            // System.out.println("Updating component");
            component.update();
            // System.out.println("Updating outgoing couplings");
            controls.updateOutgoingCouplings(component);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return "Priority update";
    }

}
