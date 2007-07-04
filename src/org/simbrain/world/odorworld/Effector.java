package org.simbrain.world.odorworld;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;

/**
 * <b>Effectors</b> represent commands which move an agent around.
 */
public class Effector implements ConsumingAttribute<Double> {

    /** The motor command. Right, Left, etc. */
    private String command;

    /** Reference to agent being commanded. */
    private OdorWorldAgent parent;

    /**
     * Construct an effector.
     * @param parent reference to parent.
     * @param command string command.
     */
    public Effector(OdorWorldAgent parent, String command) {
        super();
        this.command = command;
        this.parent = parent;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return command;
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
        parent.setMotorCommand(command, value);
    }

}
