package org.simbrain.workspace.gui.couplingmanager;

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

/**
 * Handles the transfer of couplings.
 *
 */
public class CouplingTransferHandler extends TransferHandler {

    /** String value defining consumer or producer. */
    private String consumerOrProducer;

    /**
     * Constructs a new instance of the handler.
     * @param consumerOrProducer defines consumer or producer.
     */
    public CouplingTransferHandler(final String consumerOrProducer) {
        this.consumerOrProducer = consumerOrProducer;
    }

    /**
     * Imports data and returns true if import successful.
     * @param c Component.
     * @param t Transferable object.
     * @return true if data import successful.
     */
    public boolean importData(final JComponent c, final Transferable t) {
        return c instanceof JList;
    }

    /**
     * Returns true if data can be imported.
     * @param c Component.
     * @param data to be imported.
     * @param action to be taken.
     * @return true if data can be impoted.
     */
    public boolean canImport(final JComponent c, final Transferable data, final int action) {
        return true;
    }

    /**
     * Returns the source actions.
     * @param c Component.
     * @return source actions.
     */
    public int getSourceActions(final JComponent c) {
        return COPY;
    }

    /**
     * Exports data.
     * @param c Component.
     * @param data to be exported.
     * @param action to be taken.
     */
    protected void exportDone(final JComponent c, final Transferable data, final int action) {
    }

    /**
     * Called when items are selected and dragged.
     * @param c Component.
     * @return transferable object.
     */
    protected Transferable createTransferable(final JComponent c) {
        if (c instanceof JList) {
            JList list = (JList) c;
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
