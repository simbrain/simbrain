package org.simbrain.network.gui.dialogs.network.layout;

import org.simbrain.network.layouts.Layout;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>AbstractLayoutPanel</b>.
 */
public abstract class AbstractLayoutPanel extends LabelledItemPanel {

    /**
     * @return Returns the neuronLayout.
     */
    public abstract Layout getNeuronLayout();
}
