
package org.simbrain.network;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.List;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

/**
 * Toggle button.
 */
final class ToggleButton
    extends JButton {

    /** List of interaction mode actions. */
    private final List actions;

    /** Index to current action. */
    private int index;


    /**
     * Create a new toggle button with the specified list of actions.
     *
     * @param actions list of actions, must not be null and must not be empty
     */
    ToggleButton(final List actions) {

        super();

        if (actions == null) {
            throw new IllegalArgumentException("actions must not be null");
        }
        if (actions.size() == 0) {
            throw new IllegalArgumentException("actions must not be empty");
        }

        index = 0;
        this.actions = new ArrayList(actions);
        updateAction();

        addActionListener(new ActionListener() {

                /** @see ActionListener */
                public void actionPerformed(final ActionEvent event) {
                    SwingUtilities.invokeLater(new Runnable() {

                            /** @see Runnable */
                            public void run() {
                                incrementIndex();
                                updateAction();
                            }
                        });
                }
            });
    }


    /**
     * Increment index.
     */
    private void incrementIndex() {
        index++;

        if (index >= actions.size()) {
            index = 0;
        }
    }

    /**
     * Update action.
     */
    private void updateAction() {
        setAction((Action) actions.get(index));
        // no label for toolbar buttons
        setText("");
    }
}