package org.simbrain.world.odorworld.attributes;

import org.simbrain.workspace.SingleAttributeConsumer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.odorworld.effectors.RotationEffector;

public class RotationConsumer extends SingleAttributeConsumer<Double> {

    RotationEffector effector;

    /** Parent component for this attribute holder. */
    WorkspaceComponent parent;
    
    public RotationConsumer(WorkspaceComponent component, RotationEffector effector) {
        this.effector = effector;
        this.parent = component;
    }
    public String getDescription() {
        return effector.getParent().getName() + ": Rotator";
    }

    public WorkspaceComponent getParentComponent() {
        return parent;
    }

    public void setValue(Double value) {
        effector.setScaleFactor(value);
    }

    public String getKey() {
        return getDescription();
    }

}
