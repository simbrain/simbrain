package org.simbrain.util.widgets;

import javax.swing.*;
import java.util.List;

/**
 * JButton that "toggles" through states that are externally set.
 */
public final class ToggleButton extends JButton {

    private final List<Action> actions;

    public ToggleButton(final List<Action> actions) {
        super();
        this.actions = actions;
    }

    /**
     * Set the current action
     */
    public void setAction(String name) {

        var action = actions.stream().filter(a -> a.getValue(Action.NAME) == name).findFirst();
        if (action.isPresent()) {
            setAction(action.get());
            // no label for toolbar buttons
            setText("");
        }
    }
}