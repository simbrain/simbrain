
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import org.simbrain.util.Utils;

/**
 * Show help action, opens help file <code>Network.html</code>
 * in an external web browser.
 */
public final class ShowHelpAction
    extends AbstractAction {

    /**
     * Create a new show help action.
     */
    public ShowHelpAction() {
        super("Help");

    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {

        SwingUtilities.invokeLater(new Runnable() {
                /** @see Runnable */
                public void run() {
                    Utils.showQuickRef("Network.html");
                }
            });
    }
}