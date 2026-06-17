package org.simbrain.util;

import com.formdev.flatlaf.util.UIScale;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * <b>LabelledItemPanel</b> provides a panel for laying out labeled elements
 * neatly with all the labels and elements aligned down the screen.
 *
 * <p>Backed by MigLayout: labels sit in column 0 at their natural width and
 * fields in column 1, which grows to fill the available horizontal space so
 * every field shares a common width and their right edges line up (capped at
 * {@link #FIELD_MAX_WIDTH} so fields don't sprawl on very wide dialogs).
 *
 * @author David Fraser
 * @author Michael Harris
 * @author Zoë Tosi
 */
public class LabelledItemPanel extends JPanel {

    /**
     * The row to add the next labeled item to.
     */
    private int myNextItemRow = 0;

    /**
     * Maximum field width (unscaled px) before a field stops growing, so fields
     * grow to fill but never become unwieldy on wide dialogs.
     */
    private static final int FIELD_MAX_WIDTH = 360;

    public LabelledItemPanel() {
        init();
    }

    /**
     * Initializes the panel and layout manager.
     */
    private void init() {
        // fillx (not fill): the grid fills horizontally so the field column can grow,
        // but takes only its preferred height and stays top-anchored when the panel is
        // taller than its content (e.g. inside a tall scroll pane or tab).
        setLayout(new MigLayout(
            "fillx, aligny top, insets " + Theme.dialogInsetVertical + " " + Theme.dialogInsetHorizontal
                + " " + Theme.dialogInsetVertical + " " + Theme.dialogInsetHorizontal,
            "[]" + Theme.componentGap + "[grow, fill]"
        ));
    }

    private String labelCC(int gridx, int row) {
        return "cell " + gridx + " " + row + ", aligny center, gaptop " + Theme.componentGap;
    }

    private String itemCC(int gridx, int row) {
        return "cell " + gridx + " " + row + ", growx, wmax " + UIScale.scale(FIELD_MAX_WIDTH)
            + ", gaptop " + Theme.componentGap;
    }

    /**
     * Add a labeled item to the panel. The item is added to the row below the
     * last item added.
     *
     * @param labelText The label text for the item.
     * @param item      The item to be added.
     * @return The label created for the item.
     */
    public JLabel addItem(final String labelText, final JComponent item) {
        JLabel label = new JLabel(labelText);
        add(label, labelCC(0, myNextItemRow));
        add(item, itemCC(1, myNextItemRow));
        myNextItemRow++;
        return label;
    }

    /**
     * Provides support for multi-column labeled item panels. Adds a label and
     * item to the panel on the current row, at the specified column.
     *
     * @param label The label text for the item.
     * @param item  The item to be added.
     * @param col   desired layout column (1-based)
     */
    public void addItem(final JLabel label, final JComponent item, int col) {
        add(label, labelCC(2 * (col - 1), myNextItemRow));
        add(item, itemCC(2 * (col - 1) + 1, myNextItemRow));
    }

    /**
     * Adds a labeled item to the panel on the current row, at the specified
     * column.
     *
     * @param name The label text for the item.
     * @param item The item to be added.
     * @param col  desired layout column (1-based)
     */
    public void addItem(final String name, final JComponent item, int col) {
        addItem(new JLabel(name), item, col);
    }

    /**
     * Modification of addItem which takes a label, rather than text, as an
     * argument.
     *
     * @param label Label to be added
     * @param item  SimbrainComponent to be added
     */
    public void addItemLabel(final JLabel label, final JComponent item) {
        add(label, labelCC(0, myNextItemRow));
        add(item, itemCC(1, myNextItemRow));
        myNextItemRow++;
    }

    /**
     * Add a single item, without a label, which spans the columns.
     *
     * @param item the item to add
     */
    public void addItem(final JComponent item) {
        add(item, "cell 0 " + myNextItemRow + ", span, growx, gaptop " + Theme.componentGap);
        revalidate();
        myNextItemRow++;
    }

    /**
     * A function which adds an item without a label.
     *
     * @param item the desired item
     * @param col  the column in which it is to be deposited
     */
    public void addItem(final JComponent item, int col) {
        add(item, itemCC(2 * (col - 1) + 1, myNextItemRow));
        myNextItemRow++;
    }

    /**
     * Returns the current row.
     *
     * @return the next row where an item will be placed.
     */
    public int getMyNextItemRow() {
        return myNextItemRow;
    }

    /**
     * Sets the current row where the next item will be placed.
     *
     * @param myNextItemRow the desired row
     */
    public void setMyNextItemRow(int myNextItemRow) {
        this.myNextItemRow = myNextItemRow;
    }
}
