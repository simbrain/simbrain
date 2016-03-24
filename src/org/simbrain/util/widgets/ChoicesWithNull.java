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
package org.simbrain.util.widgets;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.simbrain.util.SimbrainConstants;

/**
 * <b>ChoicesWithNull</b> is a combo box with a null state. Used
 * used to edit properties that return that can be one of a 
 * a discrete set of states.  When the edited  objects
 * return different states, the null "..." is shown.
 */
@SuppressWarnings("serial")
public class ChoicesWithNull extends JComboBox<String> {

    /** Whether the combo box has the null string in it. */
    private boolean hasNull = false;

    /**
     * Default constructor.
     */
    public ChoicesWithNull() {
        super();
    }

    /**
     * Set the items in the combo box, e.g. {"Relative", "Absolute"} or
     * {"Happy","Neutral","Sad"}.
     * 
     * @param items the list of items to use as items in the combo box.
     */
    public void setItems(String[] items) {
        setModel(new DefaultComboBoxModel(items));
    }

    /**
     * Sets the drop down box to the null "..." state.
     */
    public void setNull() {
        if (!hasNull) {
            addItem(SimbrainConstants.NULL_STRING);
            setSelectedIndex(getItemCount() - 1);
            hasNull = true;
        }
    }
    // /**
    // * Sets the tristate drop down box to null. If the box does not have a
    // null
    // * entry (i.e. has only 2 items), adds a null entry and sets it as the
    // * selected item.
    // */
    // public void setNull() {
    // if (this.getItemCount() == 2) {
    // addItem(SimbrainConstants.NULL_STRING);
    // }
    // setSelectedIndex(NULL);
    // }
    //

    /**
     * Remove the null state from the combo box.
     */
    public void removeNull() {
        if (hasNull) {
            removeItem(SimbrainConstants.NULL_STRING);
            setSelectedIndex(getItemCount() - 1);
            hasNull = false;
        }
    }
    // /**
    // * Remove the null item.
    // */
    // public void removeNull() {
    // if (this.getItemCount() == 3) {
    // removeItem(SimbrainConstants.NULL_STRING);
    // }
    // }

    /**
     * Determines if the combo box is currently set to the null state.
     *
     * @return true or false if value is null
     */
    public boolean isNull() {
        if (this.getSelectedItem().toString()
                .equalsIgnoreCase(SimbrainConstants.NULL_STRING)) {
            return true;
        }
        return false;
    }
    // /**
    // * Determines if value is null.
    // *
    // * @return true or false if value is null
    // */
    // public boolean isNull() {
    // if (this.getSelectedIndex() == NULL) {
    // return true;
    // } else {
    // return false;
    // }
    // }

}
