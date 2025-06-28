package org.simbrain.network.gui.nodes;

import org.piccolo2d.event.PBasicInputEventHandler;
import org.piccolo2d.event.PInputEvent;
import org.simbrain.network.gui.NetworkPanel;

/**
 * Input event handler for a network node that updates tool tip text for its
 * NetworkPanel as the mouse enters and exits that node.
 * <p>
 * Usage: <code>
 * final PNode node = ...;
 * node.addInputEventListener(new ToolTipTextUpdater() {
 * protected String getToolTipText() {
 * return node.toString();
 * }
 * });
 * </code>
 * </p>
 */
abstract class ToolTipTextUpdater extends PBasicInputEventHandler {

    /**
     * Network Panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * Construct a new tool tip text updater with a reference to a network
     * panel.
     *
     * @param networkPanel reference to network panel.
     */
    public ToolTipTextUpdater(NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
    }

    /**
     * Return a string to use as tool tip text.
     *
     * @return a string to use as tool tip text
     */
    protected abstract String getToolTipText();

    /**
     * @param event
     * @see PBasicInputEventHandler
     */
    public final void mouseEntered(final PInputEvent event) {

        event.setHandled(true);
        // if (!networkPanel.isThreadRunning()) {
        networkPanel.getCanvas().setToolTipText(getToolTipText());
        // }
    }

    /**
     * @param event
     * @see PBasicInputEventHandler
     */
    public final void mouseExited(final PInputEvent event) {

        event.setHandled(true);
        // if (!networkPanel.isThreadRunning()) {
        networkPanel.getCanvas().setToolTipText(null);
        // }
    }
}