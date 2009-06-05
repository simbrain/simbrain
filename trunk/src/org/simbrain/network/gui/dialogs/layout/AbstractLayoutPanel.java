package org.simbrain.network.gui.dialogs.layout;

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

    /**
     * Populate fields with current data.
     */
    public abstract void fillFieldValues();

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public abstract void commitChanges();
}
