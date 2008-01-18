package org.simbrain.workspace.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;

import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.Workspace;

/**
 * Packages an object with a jmenu item to make it easy to pass them along
 * through action events.
 * 
 */
public class SingleCouplingMenuItem extends JCheckBoxMenuItem {

    /** The default serial version ID. */
    private static final long serialVersionUID = 1L;
    
    /** Reference to producing attribute. */
    private final ProducingAttribute<?> source;

    /** Reference to consuming attribute. */
    private final ConsumingAttribute<?> target;

    /** The workspace this object belongs to. */
    private final Workspace workspace;
    
    /** The current coupling if there is one. */
    private final Coupling<?> coupling;
    
    /**
     * Creates a new instance.
     * 
     * @param workspace The parent workspace.
     * @param description The description of the menu item.
     * @param source The producingAttribute for the coupling.
     * @param target The consumingAttribute for the coupling.
     */
    @SuppressWarnings("unchecked")
    public SingleCouplingMenuItem(final Workspace workspace, final String description,
            final ProducingAttribute<?> source,
            final ConsumingAttribute<?> target) {
        super(description,
                workspace.getManager().containsCoupling(new Coupling(source, target)));
        this.workspace = workspace;
        this.source = source;
        this.target = target;
        
        addActionListener(listener);
        
        coupling = new Coupling(source, target);
    }
    
    /**
     * @return the consumingAttribute
     */
    public ConsumingAttribute<?> getConsumingAttribute() {
        return target;
    }

    /**
     * @return the producingAttribute
     */
    public ProducingAttribute<?> getProducingAttribute() {
        return source;
    }

    /**
     * Listens for events where this item is clicked.  If this item is selected when there
     * is no coupling one is created.  If it is selected, then the coupling is removed.
     */
    @SuppressWarnings("unchecked")
    private final ActionListener listener = new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
            if (getState()) {
                workspace.addCoupling(coupling);
                setSelected(true);
            } else {
                workspace.removeCoupling(coupling);
                setSelected(false);
            }
        }
    };
}
