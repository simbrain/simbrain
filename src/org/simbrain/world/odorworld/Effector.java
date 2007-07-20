package org.simbrain.world.odorworld;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;

/**
 * <b>Effectors</b> represent commands which move an agent around.
 */
public class Effector implements ConsumingAttribute<Double> {

    /** The motor command. Right, Left, etc. */
    private String name;

    /** Reference to agent being commanded. */
    private OdorWorldAgent parent;

    /**
     * Construct an effector.
     * @param parent reference to parent.
     * @param command string command.
     */
    public Effector(OdorWorldAgent parent, String name) {
        this.name = name;
        this.parent = parent;
    }

    /**
     * {@inheritDoc}
     */
    public String getAttributeDescription() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public Consumer getParent() {
        return parent;
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(final Double value) {
        parent.setMotorCommand(name, value);
    }

}
