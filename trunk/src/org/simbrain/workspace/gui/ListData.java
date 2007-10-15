package org.simbrain.workspace.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * List of data for dragging and dropping items in the coupling menu.
 */
public class ListData implements Transferable {

    /** List of transferrable couplings. */
    private TransferrableCouplingList list;

    /** Type of data in the list. */
    public static final DataFlavor LIST_DATA_FLAVOR =
        new DataFlavor(ListData.class, "X-test/test; class=<java.lang.Object>; foo=bar");

    /**
     * Creates a new list of data.
     * @param list of transferrable couplings.
     */
    public ListData(final TransferrableCouplingList list) {
        this.list = list;
    }

    /**
     * Returns the transfer data.
     * @param flavor of data.
     * @return transfer data.
     * @throws IOException
     */
    public Object getTransferData(final DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        //System.out.println("in getTransferData");
        return list;
    }

    /**
     * Returns the transfer data's type.
     * @return type of transfer data
     */
    public DataFlavor[] getTransferDataFlavors() {
        //System.out.println("in getTransferDataFlavors");
        DataFlavor[] ret = {LIST_DATA_FLAVOR };
        return ret;
    }

    /**
     * Returns true if data flavor is supported.
     * @param flavor of data.
     * @return true if flavor supported.
     */
    public boolean isDataFlavorSupported(final DataFlavor flavor) {
        //System.out.println("in isDataFlavorSupported");
        return true;
    }

}