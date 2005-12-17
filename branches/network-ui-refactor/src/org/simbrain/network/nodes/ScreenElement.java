
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
 * protected abstract String hasToolTipText();
 * protected abstract String getToolTipText();
 * protected abstract boolean hasContextMenu();
 * protected abstract JPopupMenu getContextMenu();
 * </pre>
 * </p>
 */
abstract class ScreenElement
    extends PNode {

    /** Network panel. */
    private NetworkPanel networkPanel;

    /** Property change support. */
    private PropertyChangeSupport propertyChangeSupport;


    /**
     * Create a new abstract screen element with the specified network panel.
     *
     * @param networkPanel network panel for this screen element
     */
    protected ScreenElement(final NetworkPanel networkPanel) {

        super();
        propertyChangeSupport = new SwingPropertyChangeSupport(this);

        setNetworkPanel(networkPanel);

        if (hasContextMenu()) {
            addInputEventListener(new ContextMenuEventHandler());
        }

        if (hasToolTipText()) {
            addInputEventListener(new ToolTipTextUpdater() {

                    /** @see ToolTipTextUpdater */
                    protected String getToolTipText() {
                        return ScreenElement.this.getToolTipText();
                    }
                });
        }
    }


    /**
     * Return <code>true</code> if this screen element has tool tip text.
     * If this screen element does not have tool tip text, a tool tip
     * event handler will not be registered.
     *
     * @see #getToolTipText
     * @return true if this screen element has tool tip text
     */
    protected abstract boolean hasToolTipText();

    /**
     * Return a <code>String</code> to use as tool tip text for this screen element.
     * Return <code>null</code> if this screen element does not have tool tip text
     * or to temporarily prevent the tool tip from displaying.
     *
     * @see #hasToolTipText
     * @return a <code>String</code> to use as tool tip text for this screen element
     */
    protected abstract String getToolTipText();

    /**
     * Return <code>true</code> if this screen element has a context menu.
     * If this screen element does not have a context menu, a context menu
     * event handler will not be registered.
     *
     * @see #getContextMenu
     * @return true if this screen element has a context menu.
     */
    protected abstract boolean hasContextMenu();

    /**
     * Return a context menu specific to this screen element.  Return
     * <code>null</code> if this screen element does not have a context
     * menu.
     *
     * @see #hasContextMenu
     * @return a context menu specific to this screen element
     */
    protected abstract JPopupMenu getContextMenu();


    //
    // bound properties

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

            event.setHandled(true);
            NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
            JPopupMenu contextMenu = getContextMenu();
            Point2D canvasPosition = event.getCanvasPosition();
            contextMenu.show(networkPanel, (int) canvasPosition.getX(), (int) canvasPosition.getY());
        }

        /** @see PBasicInputEventHandler */
        public void mousePressed(final PInputEvent event) {

            if (event.isPopupTrigger()) {
                showContextMenu(event);
            }
        }

        /** @see PBasicInputEventHandler */
        public void mouseReleased(final PInputEvent event) {

            if (event.isPopupTrigger()) {
                showContextMenu(event);
            }
        }
    }
}
