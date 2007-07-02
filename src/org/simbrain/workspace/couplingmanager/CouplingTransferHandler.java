package org.simbrain.workspace.couplingmanager;

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

public class CouplingTransferHandler extends TransferHandler {
    
    private String consumerOrProducer;
    
    public CouplingTransferHandler(String consumerOrProducer) {
        this.consumerOrProducer = consumerOrProducer;
    }
    
    public boolean importData(JComponent c, Transferable t) {
        if (c instanceof JList) {
             JList list = (JList)c;
             return true;             
        }
        return false;
    }
    
    public boolean canImport(JComponent c, Transferable data, int action) {    
        return true;
    }
    
    public int getSourceActions(JComponent c) {
        return COPY;
    }
    
    protected void exportDone(JComponent c, Transferable data, int action) {
    }
    
    // Called when items are selected and dragged
    protected Transferable createTransferable(JComponent c) {
        if (c instanceof JList) {
            JList list = (JList)c;
            ArrayList data = new ArrayList();
            for (int i = 0; i < list.getSelectedValues().length; i++) {
                data.add(list.getSelectedValues()[i]);
            }
            TransferrableCouplingList tcl = new TransferrableCouplingList(data, consumerOrProducer);
            return new ListData(tcl);
        } else  {
            return null;
        }
    }

}
