
package org.simbrain.network.nodes;

import java.awt.event.InputEvent;

import java.awt.geom.Point2D;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import edu.umd.cs.piccolo.PNode;

import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventFilter;
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
 * protected abstract boolean isSelectable();
 * protected abstract boolean showSelectionHandle();
 * protected abstract boolean isDraggable();
 * protected abstract boolean hasToolTipText();
 * protected abstract String getToolTipText();
 * protected abstract boolean hasContextMenu();
 * protected abstract JPopupMenu getContextMenu();
 * protected abstract boolean hasPropertyDialog();
 * protected abstract JDialog getPropertyDialog();
 * </pre>
 * </p>
 */
public abstract class ScreenElement
    extends PNode {

    /** Network panel. */
    private NetworkPanel networkPanel;

    /**
     * Default Constructor. Used by Castor.
     */
    public ScreenElement() {
    }

    /**
     * Create a new abstract screen element with the specified network panel.
     *
     * @param networkPanel network panel for this screen element
     */
    protected ScreenElement(final NetworkPanel networkPanel) {
        super();
        setNetworkPanel(networkPanel);
        init();
    }

    /**
     * Initializes relevant data after a <code>ScreenElement</code> has been unmarshalled via Castor.
     *
     * @param networkPanel network panel
     */
    public void initCastor(final NetworkPanel networkPanel) {
        setNetworkPanel(networkPanel);
        init();
    }

    /**
     * Initialize this <code>ScreenElement</code>.
     */
    private void init() {
        if (hasContextMenu()) {
            addInputEventListener(new ContextMenuEventHandler());
        }

        if (hasPropertyDialog()) {
            addInputEventListener(new PropertyDialogEventHandler());
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
     * Return <code>true</code> if this screen element is selectable.
     * <p>
     * Being selectable requires that this screen element is pickable
     * as far as the Piccolo API is concerned, so if this method returns
     * <code>true</code>, be sure that this class also returns <code>true</code>
     * for its <code>getPickable()</code> method.
     * </p>
     *
     * @see edu.umd.cs.piccolo.PNode#getPickable
     * @see edu.umd.cs.piccolo.PNode#setPickable
     * @return true if this screen element is selectable
     */
    public abstract boolean isSelectable();

    /**
     * Return <code>true</code> if this screen element should show a
     * selection handle.
     * <p>
     * Showing a selection handle requires that this screen element is pickable
     * as far as the Piccolo API is concerned, so if this method returns
     * <code>true</code>, be sure that this class also returns <code>true</code>
     * for its <code>getPickable()</code> method.
     * </p>
     * <p>
     * Showing a selection handle also requires that this screen element is selectable,
     * so if this method returns <code>true</code>, be sure that this class also
     * returns <code>true</code> for its <code>isSelectable()</code> method.
     * </p>
     *
     * @see edu.umd.cs.piccolo.PNode#getPickable
     * @see edu.umd.cs.piccolo.PNode#setPickable
     * @see #isSelectable
     * @return true if this screen element should show a selection handle
     */
    public abstract boolean showSelectionHandle();

    /**
     * Return <code>true</code> if this screen element is draggable.
     * <p>
     * Being draggable requires that this screen element is pickable
     * as far as the Piccolo API is concerned, so if this method returns
     * <code>true</code>, be sure that this class also returns <code>true</code>
     * for its <code>getPickable()</code> method.
     * </p>
     * <p>
     * Being draggable also requires that this screen element is selectable,
     * so if this method returns <code>true</code>, be sure that this class also
     * returns <code>true</code> for its <code>isSelectable()</code> method.
     * </p>
     *
     * @see edu.umd.cs.piccolo.PNode#getPickable
     * @see edu.umd.cs.piccolo.PNode#setPickable
     * @see #isSelectable
     * @return true if this screen element is draggable
     */
    public abstract boolean isDraggable();

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

    /**
     * Return <code>true</code> if this screen element has a property dialog.
     * If this screen element does not have a property dialog, a property
     * dialog event handler will not be registered.
     *
     * @see #getPropertyDialog
     * @return true if this screen element has a property dialog
     */
    protected abstract boolean hasPropertyDialog();

    /**
     * Return a property dialog for this screen element.  Return
     * <code>null</code> if this screen element does not have a
     * property dialog.
     *
     * @see #hasPropertyDialog
     * @return a property dialog for this screen element
     */
    protected abstract JDialog getPropertyDialog();

    /**
     * Reset colors when default colors have been changed in <code>NetworkPreferences</code>.
     */
    public abstract void resetColors();

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

    /**
     * Property dialog event handler.
     */
    private class PropertyDialogEventHandler
        extends PBasicInputEventHandler {

        /**
         * Create a new property dialog event handler.
         */
        public PropertyDialogEventHandler() {
            super();
            setEventFilter(new PInputEventFilter(InputEvent.BUTTON1_MASK));
        }


        /** @see PBasicInputEventHandler */
        public void mouseClicked(final PInputEvent event) {

            if (event.getClickCount() == 2) {
                SwingUtilities.invokeLater(new Runnable() {
                        /** @see Runnable */
                        public void run() {
                            JDialog propertyDialog = ScreenElement.this.getPropertyDialog();
                            propertyDialog.pack();
                            propertyDialog.setLocationRelativeTo(null);
                            propertyDialog.setVisible(true);
                        }
                    });
            }
        }
    }
}
