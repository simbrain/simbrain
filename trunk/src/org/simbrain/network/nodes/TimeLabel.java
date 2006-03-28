package org.simbrain.network.nodes;

import org.simbrain.network.NetworkPanel;

import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PText;

/**
 * The visible label showing the time step as defined by the model network class.
 */
public class TimeLabel extends PText {

    /** Reference to parent NetworkPanel. */
    private NetworkPanel networkPanel;

    /**
     * Construct time label with reference to networkPanel.
     *
     * @param netPanel reference to networkPanel
     */
    public TimeLabel(final NetworkPanel netPanel) {
        super();
        networkPanel = netPanel;
        this.addInputEventListener(new TimeLabelEventHandler());
        update();
    }

    /**
     * Update the text based on the netwokr time.
     */
    public void update() {
        setText(networkPanel.getNetwork().getTimeLabel());
    }

    /**
     * Reset time on double clicks.
     */
    private class TimeLabelEventHandler
        extends PBasicInputEventHandler {

        /** @see PBasicInputEventHandler */
        public void mousePressed(final PInputEvent event) {
            if (event.getClickCount() == 2) {
                networkPanel.getNetwork().setTime(0);
                update();
            }
        }

    }
}
