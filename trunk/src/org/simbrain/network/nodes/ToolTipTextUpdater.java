
package org.simbrain.network.nodes;

import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;

import org.simbrain.network.NetworkPanel;

/**
 * Input event handler for a network node that updates tool tip text
 * for its NetworkPanel as the mouse enters and exits that node.
 *
 * <p>Usage:
 * <code>
 * final PNode node = ...;
 * node.addInputEventListener(new ToolTipTextUpdater() {
 *     protected String getToolTipText() {
 *       return node.toString();
 *     }
 *   });
 * </code>
 * </p>
 */
abstract class ToolTipTextUpdater
    extends PBasicInputEventHandler{

    /**
     * Return a string to use as tool tip text.
     *
     * @return a string to use as tool tip text
     */
    protected abstract String getToolTipText();


    /** @see PBasicInputEventHandler */
    public final void mouseEntered(final PInputEvent event) {

        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
        //if (!networkPanel.isThreadRunning()) {
        networkPanel.setToolTipText(getToolTipText());
        //}
    }

    /** @see PBasicInputEventHandler */
    public final void mouseExited(final PInputEvent event) {

        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
        //if (!networkPanel.isThreadRunning()) {
        networkPanel.setToolTipText(null);
        //}
    }
}