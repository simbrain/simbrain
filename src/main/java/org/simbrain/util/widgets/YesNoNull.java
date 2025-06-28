package org.simbrain.util.widgets;

import java.awt.*;

/**
 * <b>YesNoNull</b> creates a combo box that has three states; True, false
 * and null. It is used where it needs to be determined if multiple items have
 * the same value, or are inconsistent. (e.g. Multiple selected neurons.)
 */
@SuppressWarnings("serial")
public class YesNoNull extends ChoicesWithNull {

    /**
     * Integer value for true.
     */
    private static final int TRUE = 0;

    /**
     * Integer value for false.
     */
    private static final int FALSE = 1;

    /**
     * Integer value for null.
     */
    private static final int NULL = 2;

    public YesNoNull() {
        super();
        addItem("Yes");
        addItem("No");
        setPreferredSize(new Dimension(80, getPreferredSize().height));
        setMaximumSize(new Dimension(80, getPreferredSize().height));
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
        return this.getSelectedIndex() == TRUE;
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
