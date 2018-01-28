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
 * Convenience class for cleaner use of update actions.
 * <p>
 * TODO: Do something similar for network actions
 *
 * @author Jeff Yoshimi
 */
public class UpdateActionAdapter implements UpdateAction {

    /**
     * A description string to be used in both short and long descriptions.
     */
    private String description;

    /**
     * Construct the action with a description string.
     *
     * @param description the description
     */
    public UpdateActionAdapter(String description) {
        this.description = description;
    }

    @Override
    public void invoke() {
        // This should be overridden
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getLongDescription() {
        return description;
    }


}
