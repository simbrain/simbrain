package org.simbrain.network.dialog.network.layout;

import org.simbrain.util.LabelledItemPanel;
import org.simnet.layouts.Layout;

/**
 * <b>AbstractLayoutPanel</b>.
 */
public abstract class AbstractLayoutPanel extends LabelledItemPanel {

    /**
     * @return Returns the neuronLayout.
     */
    public abstract Layout getNeuronLayout();
}
