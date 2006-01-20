package org.simbrain.network;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Network key adapter.
 */
public class NetworkKeyAdapter extends KeyAdapter {

    /** Network panel. */
    private NetworkPanel networkPanel;

    /**
     * Network key adapter.
     *
     * @param networkPanel Network panel
     */
    public NetworkKeyAdapter(final NetworkPanel networkPanel) {

        this.networkPanel = networkPanel;

    }
    /**
     * Responds to key pressed events.
     *
     * @param e Key event
     */
    public void keyPressed(final KeyEvent e) {
        int keycode = e.getKeyCode();
        switch (keycode) {
        case KeyEvent.VK_LEFT:

            if (e.isShiftDown()) {
                networkPanel.nudge(-1, 0);
            } else {
                networkPanel.decrementSelectedObjects();
            }

            break;

        case KeyEvent.VK_RIGHT:

            if (e.isShiftDown()) {
                networkPanel.nudge(1, 0);
            } else {
                networkPanel.incrementSelectedObjects();
            }

            break;

        case KeyEvent.VK_UP:

            if (e.isShiftDown()) {
                networkPanel.nudge(0, -1);
            } else {
                networkPanel.incrementSelectedObjects();
            }

            break;

        case KeyEvent.VK_DOWN:

            if (e.isShiftDown()) {
                networkPanel.nudge(0, 1);
            } else {
                networkPanel.decrementSelectedObjects();
            }

            break;

        case KeyEvent.VK_U:

            networkPanel.clearSelection();

            break;

        default:

            break;

        }
    }
}
