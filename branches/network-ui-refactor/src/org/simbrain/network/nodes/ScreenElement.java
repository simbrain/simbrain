
package org.simbrain.network.nodes;

import java.awt.geom.Point2D;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;

import javax.swing.JPopupMenu;

import javax.swing.event.SwingPropertyChangeSupport;

import edu.umd.cs.piccolo.PNode;

import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;

import org.simbrain.network.NetworkPanel;

/**
 * Abstract screen element class, extends a Piccolo node
 * with property change, tool tip, property dialog, and ...
 * support.
 *
 * <p>
 * Subclasses of this class must implement the following methods:
 * <pre>
 * protected abstract String getToolTipText();
 * protected abstract JPopupMenu createContextMenu();
 * </pre>
 * </p>
 */
abstract class ScreenElement
    extends PNode {

    /** Network panel. */
    private NetworkPanel networkPanel;

    /** Context menu specific to this screen element. */
    private JPopupMenu contextMenu;

    /** Property change support. */
    private PropertyChangeSupport propertyChangeSupport;


    /**
     * Create a new abstract screen element.
     */
    protected ScreenElement() {
        this(null);
    }

    /**
     * Create a new abstract screen element with the specified network panel.
     *
     * @param networkPanel network panel for this screen element
     */
    protected ScreenElement(final NetworkPanel networkPanel) {

        super();
        propertyChangeSupport = new SwingPropertyChangeSupport(this);

        setNetworkPanel(networkPanel);
        setContextMenu(createContextMenu());

        addInputEventListener(new ContextMenuEventHandler());

        addInputEventListener(new ToolTipTextUpdater() {

                /** @see ToolTipTextUpdater */
                protected String getToolTipText() {
                    return ScreenElement.this.getToolTipText();
                }
            });
    }


    /**
     * Return a <code>String</code> to use as tool tip text for this screen element.
     * Return <code>null</code> to prevent the tool tip from displaying.
     *
     * @return a <code>String</code> to use as tool tip text for this screen element
     */
    protected abstract String getToolTipText();

    /**
     * Create and return a new context menu specific to this screen element.
     *
     * @return a new context menu specific to this screen element
     */
    protected abstract JPopupMenu createContextMenu();


    //
    // bound properties

    /**
     * Return the context menu specific to this screen element.
     *
     * @return the context menu specific to this screen element
     */
    public final JPopupMenu getContextMenu() {
        return contextMenu;
    }

    /**
     * Set the context menu specific to this screen element to <code>contextMenu</code>.
     *
     * @param contextMenu context menu specific to this screen element, must not be null
     */
    protected final void setContextMenu(final JPopupMenu contextMenu) {

        if (contextMenu == null) {
            throw new IllegalArgumentException("contextMenu must not be null");
        }

        JPopupMenu oldContextMenu = this.contextMenu;
        this.contextMenu = contextMenu;
        firePropertyChange("contextMenu", oldContextMenu, this.contextMenu);
    }

    /**
     * Return the network panel for this screen element.
     *
     * @return the network panel for this screen element
     */
    public final NetworkPanel getNetworkPanel() {
        return networkPanel;
    }

    /**
     * Set the network panel for this screen element to <code>networkPanel</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param networkPanel network panel for this screen element
     */
    public final void setNetworkPanel(final NetworkPanel networkPanel) {

        NetworkPanel oldNetworkPanel = this.networkPanel;
        this.networkPanel = networkPanel;
        firePropertyChange("networkPanel", oldNetworkPanel, this.networkPanel);
    }
    
    
    //
    // property change support

    /**
     * Fire a property change event to any registered property change listeners.
     *
     * @param propertyName property name
     * @param oldValue old property value
     * @param newValue new property value
     */
    protected final void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Add the specified property change listener.
     *
     * @param l property change listener to add
     */
    public final void addPropertyChangeListener(final PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    /**
     * Add the specified property change listener.
     *
     * @param propertyName property name
     * @param l property change listener to add
     */
    public final void addPropertyChangeListener(final String propertyName, final PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, l);
    }

    /**
     * Remove the specified property change listener.
     *
     * @param l property change listener to remove
     */
    public final void removePropertyChangeListener(final PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }
 
    /**
     * Remove the specified property change listener.
     *
     * @param propertyName property name
     * @param l property change listener to remove
     */
   public final void removePropertyChangeListener(final String propertyName, final PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, l);
    }


    /**
     * Screen element-specific context menu event handler.
     */
    private class ContextMenuEventHandler
        extends PBasicInputEventHandler {

        /**
         * Show the context menu.
         *
         * @param event event
         */
        private void showContextMenu(final PInputEvent event) {

            System.out.println("screen element show context menu");
            event.setHandled(true);
            NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
            JPopupMenu contextMenu = getContextMenu();
            Point2D canvasPosition = event.getCanvasPosition();
            contextMenu.show(networkPanel, (int) canvasPosition.getX(), (int) canvasPosition.getY());
        }

        /** @see PBasicInputEventHandler */
        public void mousePressed(final PInputEvent event) {

            System.out.println("screen element mouse pressed");
            if (event.isPopupTrigger()) {
                showContextMenu(event);
            }
        }

        /** @see PBasicInputEventHandler */
        public void mouseReleased(final PInputEvent event) {

            System.out.println("screen element mouse released");
            if (event.isPopupTrigger()) {
                showContextMenu(event);
            }
        }
    }
}
