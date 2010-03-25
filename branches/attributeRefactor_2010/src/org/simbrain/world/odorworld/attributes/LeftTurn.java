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
package org.simbrain.world.odorworld.attributes;

import org.simbrain.workspace.SingleAttributeConsumer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.odorworld.effectors.RotationEffector;

public class LeftTurn extends SingleAttributeConsumer<Double> {

    //TODO:
    //      - Make a multi-attribute consumer.  Add left, right, forward
    //      - Change name to RotatingEntityConsumer?
    RotationEffector effector;

    /** Parent component for this attribute holder. */
    WorkspaceComponent parent;
    
    public LeftTurn(WorkspaceComponent component, RotationEffector effector) {
        this.effector = effector;
        this.parent = component;
    }
    public String getDescription() {
        return effector.getParent().getName() + "-Left";
    }

    public WorkspaceComponent getParentComponent() {
        return parent;
    }

    public void setValue(Double value) {
        effector.setScaleFactor(1);
        effector.setTurnAmount(value);
    }

    public String getKey() {
        return getDescription();
    }

}
