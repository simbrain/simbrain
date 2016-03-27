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

import java.awt.Dimension;

/**
 * <b>YesNoNull</b> creates a combo box that has three states; True, false
 * and null. It is used where it needs to be determined if multiple items have
 * the same value, or are inconsistent. (e.g. Multiple selected neurons.)
 */
@SuppressWarnings("serial")
public class YesNoNull extends ChoicesWithNull  {

    /** Integer value for true. */
    private static final int TRUE = 0;

    /** Integer value for false. */
    private static final int FALSE = 1;

    /** Integer value for null. */
    private static final int NULL = 2;

    /**
     * Default constructor.
     */
    public YesNoNull() {
        super();
        addItem("Yes");
        addItem("No");
        setPreferredSize(new Dimension(60, getPreferredSize().height));
        setMaximumSize(new Dimension(60, getPreferredSize().height));
    }

    /**
     * Create custom three-state combo box using text other than "Yes" and "No".
     *
     * @param itemOne Add first item to combo box
     * @param itemTwo Add second item to combo box
     */
    public YesNoNull(final String itemOne, final String itemTwo) {
        super();
        addItem(itemOne);
        addItem(itemTwo);
    }

    /**
     * Determines if index is selected true.
     *
     * @return True or false value
     */
    public boolean isSelected() {
        if (this.getSelectedIndex() == TRUE) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the selected item.
     *
     * @param val Value to be set as
     */
    public void setSelected(final boolean val) {
        if (val) {
            setSelectedIndex(TRUE);
        } else {
            setSelectedIndex(FALSE);
        }
    }

    /**
     * @return false value.
     */
    public static int getFALSE() {
        return FALSE;
    }

    /**
     * @return null value.
     */
    public static int getNULL() {
        return NULL;
    }

    /**
     * @return true value.
     */
    public static int getTRUE() {
        return TRUE;
    }

}
