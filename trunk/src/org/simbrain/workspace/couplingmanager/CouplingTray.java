/**
 * This is an example of a component, which serves as a DragSource as well as
 * Drop Target. To illustrate the concept, JList has been used as a droppable
 * target and a draggable source. Any component can be used instead of a JList.
 * The code also contains debugging messages which can be used for diagnostics
 * and understanding the flow of events.
 * 
 * @version 1.0
 */
// TODO: Add reference to author
package org.simbrain.workspace.couplingmanager;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JList;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;

/**
 * Tray which manages binding of couplings.  Most of the code handles drag and drop issues.
 *
 * @author jyoshimi
 *
 */
public class CouplingTray extends JList implements DropTargetListener {

    /**
     * Enables this component to be a dropTarget.
     */
    DropTarget dropTarget = null;

    /**
     * Constructor - initializes the DropTarget and DragSource.
     */
    public CouplingTray() {
        dropTarget = new DropTarget(this, this);
    }

    /**
     * Invoked when you are dragging over the DropSite.
     */
    public void dragEnter(DropTargetDragEvent event) {
        // System.out.println( "dragEnter");
        event.acceptDrag(DnDConstants.ACTION_MOVE);
    }

    /**
     * Invoked when you are exit the DropSite without dropping.
     */
    public void dragExit(DropTargetEvent event) {
        // System.out.println( "dragExit");
    }

    /**
     * Invoked when a drag operation is going on
     */
    public void dragOver(DropTargetDragEvent event) {
        // System.out.println( "dragOver");
    }

    /**
     * A drop has occurred
     */
    public void drop(DropTargetDropEvent event) {
        // System.out.println("We have a drop: " + this.getSelectedIndex());
        try {
            Transferable transferable = event.getTransferable();
            if (transferable.isDataFlavorSupported(ListData.ListDataFlavor)) {
                event.acceptDrop(DnDConstants.ACTION_MOVE);
                TransferrableCouplingList tcl = (TransferrableCouplingList)transferable.getTransferData(ListData.ListDataFlavor);
                ArrayList list = tcl.getList();

                if (tcl.getProducerOrConsumer().equalsIgnoreCase("consumers")) {
                    // Create unbound consumers
                    int index = this.getSelectedIndex();
                    for (int i = 0; i < list.size(); i++) {
                        Coupling coupling = new Coupling(((Consumer)list.get(i)).getDefaultConsumingAttribute());
                        if (index > -1) {
                            ((CouplingList) this.getModel()).insertElementAt(coupling, index+1);                            
                        } else {
                            ((CouplingList) this.getModel()).addElement((coupling));
                        }
                    }
                } else {
                    // Bind consumers to producers
                    int start = this.getSelectedIndex();
                    int index;
                    if (this.getSelectedIndex() == -1) {
                        return;
                    }
                    for (int i = 0; i < list.size(); i++) {
                        //TODO: Here and above!
                        ProducingAttribute producer = (((Producer)list.get(i)).getDefaultProducingAttribute());
                        index = start + i;
                        if (index >= this.getModel().getSize()) {
                            break;
                        } else {
                            ((CouplingList)this.getModel()).bindElementAt(producer, index);                            
                        }                        
                    }                                        
                }
                event.getDropTargetContext().dropComplete(true);
            } else {
                event.rejectDrop();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
            System.err.println("Exception" + exception.getMessage());
            event.rejectDrop();
        } catch (UnsupportedFlavorException ufException) {
            ufException.printStackTrace();
            System.err.println("Exception" + ufException.getMessage());
            event.rejectDrop();
        }
    }

    /**
     * Invoked if the usee modifies the current drop gesture
     */
    public void dropActionChanged(DropTargetDragEvent event) {
    }

    /**
     * A drag gesture has been initiated.
     */
    public void dragGestureRecognized(DragGestureEvent event) {
//        Object selected = getSelectedValue();
//        if (selected != null) {
//            StringSelection text = new StringSelection(selected.toString());
//        } else {
//            System.out.println("nothing was selected");
//        }
    }

    /**
     * This message goes to DragSourceListener, informing it that the dragging
     * has ended.
     */
    public void dragDropEnd(DragSourceDropEvent event) {
    }

    /**
     * This message goes to DragSourceListener, informing it that the dragging
     * has entered the DropSite.
     */
    public void dragEnter(DragSourceDragEvent event) {
        // System.out.println( " dragEnter");
    }

    /**
     * This message goes to DragSourceListener, informing it that the dragging
     * has exited the DropSite.
     */
    public void dragExit(DragSourceEvent event) {
        // System.out.println( "dragExit");
    }

    /**
     * This message goes to DragSourceListener, informing it that the dragging
     * is currently ocurring over the DropSite.
     */
    public void dragOver(DragSourceDragEvent event) {
        // System.out.println( "dragExit");
    }

    /**
     * Invoked when the user changes the dropAction.
     */
    public void dropActionChanged(DragSourceDragEvent event) {
        // System.out.println( "dropActionChanged");
    }

}
