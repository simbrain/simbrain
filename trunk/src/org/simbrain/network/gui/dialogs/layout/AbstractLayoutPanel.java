/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui.dialogs.layout;

import org.simbrain.network.layouts.Layout;
import org.simbrain.util.LabelledItemPanel;

/**
 * <b>AbstractLayoutPanel</b>.
 */
public abstract class AbstractLayoutPanel extends LabelledItemPanel {

    /**
     * The layout object represented by this panel.
     */
    protected Layout layout;

    /**
     * Populate fields with current data.
     */
    public abstract void fillFieldValues();

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public abstract void commitChanges();

    /**
     * Returns the layout object being edited by this panel.
     *
     * @return The layout object.
     */
    public Layout getNeuronLayout() {
        return layout;
    }

    /**
     * Set the layout being edited by this panel.
     *
     * @param layout the new layout
     */
    public void setNeuronLayout(Layout layout) {
        this.layout = layout;
        fillFieldValues();
    }

}
