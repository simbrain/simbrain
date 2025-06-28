package org.simbrain.util.widgets;

import org.simbrain.util.ResourceManager;
import org.simbrain.util.Utils;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * An action that opens a help file in an external web browser.
 */
public final class ShowHelpAction extends AbstractAction {

    /**
     * Documentation URL.
     */
    private final String theURL;

    // TODO: Construct with URL; throw exceptions for bad pages

    /**
     * Create a help action that opens the specified URL (relative to
     * Simbrain/docs).
     *
     * @param actionName the name associated with this action
     * @param url        the url to open.
     */
    public ShowHelpAction(final String actionName, final String url) {
        super(actionName);
        this.theURL = url;
        putValue(SMALL_ICON, ResourceManager.getSmallIcon("menu_icons/Help.png"));
        putValue(SHORT_DESCRIPTION, "Show help via local web page");
    }

    /**
     * Create a help action that opens the specified URL (relative to the
     * "Pages" directory in Simbrain/docs).
     *
     * @param url the url to open.
     */
    public ShowHelpAction(final String url) {
        super("Help");
        this.theURL = url;
        putValue(SMALL_ICON, ResourceManager.getSmallIcon("menu_icons/Help.png"));
        putValue(SHORT_DESCRIPTION, "Show help via local web page");
    }

    public void actionPerformed(final ActionEvent event) {

        SwingUtilities.invokeLater(new Runnable() {
            /** @see Runnable */
            public void run() {
                Utils.displayURLInBrowser(theURL);
            }
        });
    }

}