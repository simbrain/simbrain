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

import org.simbrain.workspace.Coupling;

/**
 * Updates a coupling
 *
 * @author jyoshimi
 */
public class UpdateCoupling implements UpdateAction {

    /** of couplings to update. */
    private final Coupling<?> coupling;

    /**
     * Construct the action.
     *
     * @param coupling coupling to update
     */
    public UpdateCoupling(Coupling<?> coupling) {
        this.coupling = coupling;
    }

    /**
     * {@inheritDoc}
     */
    public void invoke() {
        coupling.setBuffer();
        coupling.update();
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        if (coupling == null) {
            return "Invalid action";
        } else {
            return "Update coupling ("
                    + coupling.getProducer().getParentComponent().getName()
                    + ">"
                    + coupling.getConsumer().getParentComponent().getName()
                    + ")";
        }
    }

    @Override
    public String getLongDescription() {
        if (coupling == null) {
            return "Invalid action";
        } else {
            return coupling.toString();
        }
    }

    /**
     * @return the coupling
     */
    public Coupling<?> getCoupling() {
        return coupling;
    }

}
