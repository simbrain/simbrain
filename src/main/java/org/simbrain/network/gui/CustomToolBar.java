package org.simbrain.network.gui;

import javax.swing.*;

/**
 * <b>CustomToolBar</b> is custom tool bar for the Network Panel whose key
 * bindings have been removed so that users may use keyboard shortcuts even
 * while the toolbar is in focus.
 * <p>
 * TODO: Option to undo this for accessibility purposes.
 */
public final class CustomToolBar extends JToolBar {

    /**
     * Private default constructor.
     */
    public CustomToolBar() {
        super();
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("pressed UP"), "none");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("pressed DOWN"), "none");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("pressed LET"), "none");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("pressed RIGHT"), "none");
    }
}
