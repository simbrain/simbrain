package org.simbrain.workspace.couplingmanager;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;

public class ListData implements Transferable {
    
    private TransferrableCouplingList list;
    
    public static final DataFlavor ListDataFlavor = new DataFlavor(ListData.class, "X-test/test; class=<java.lang.Object>; foo=bar");
    
    public ListData(TransferrableCouplingList list) {
        this.list = list;
    }

    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        //System.out.println("in getTransferData");
        return list;
    }

    public DataFlavor[] getTransferDataFlavors() {
        //System.out.println("in getTransferDataFlavors");
        DataFlavor[] ret = { ListDataFlavor };
        return ret;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        //System.out.println("in isDataFlavorSupported");
        return true;
    }

}