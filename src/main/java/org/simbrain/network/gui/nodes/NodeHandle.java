package org.simbrain.network.gui.nodes;

import org.piccolo2d.PNode;
import org.piccolo2d.extras.handles.PHandle;
import org.piccolo2d.extras.util.PNodeLocator;
import org.simbrain.util.NetworkTheme;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * "Selection" handle for PNodes,
 */
public class NodeHandle extends PHandle {


    /**
     * Ordinary selection handle (themed selection color).
     */
    public static final Config SELECTION_STYLE = new Config();

    /**
     * Source-selection handle (themed source color).
     */
    public static final Config SOURCE_STYLE = new Config(0.2f, Config.Role.SOURCE);

    /**
     * Style used with interaction box selection.
     */
    public static final Config INTERACTION_BOX_SELECTION_STYLE = new Config(0.01f, 2, Config.Role.SELECTION);

    /**
     * Style used with interaction box source selection.
     */
    public static final Config INTERACTION_BOX_SOURCE_STYLE = new Config(0.09f, Config.Role.SOURCE);

    /**
     * Regular selections associated with a node.
     */
    private static final Map<PNode, NodeHandle> selections = new HashMap<>();

    /**
     * Source selections associated with a node.
     */
    private static final Map<PNode, NodeHandle> sources = new HashMap<>();

    /**
     * Style of a node handle. Thickness, color, etc.
     */
    private Config style;

    /**
     * Create with a specified style.
     */
    public NodeHandle(PNodeLocator locator, Config style) {
        super(locator);

        this.style = style;

        reset();
        setPickable(false);

        PNode parentNode = locator.getNode();
        parentNode.addChild(this);

        setPaint(null);
        if (style.thickness > 1) {
            setStroke(new BasicStroke(style.thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }
        setStrokePaint(style.resolveColor());

        // Force handle to check its location and size
        updateBounds();
        relocateHandle();
    }

    /**
     * Re-resolve this handle's stroke color from the active canvas theme. Called on a live light/dark switch
     * since handles are plain {@link PHandle} children and are not in the network panel's screen-element sweep.
     */
    public void refreshThemeColor() {
        setStrokePaint(style.resolveColor());
    }

    @Override
    public void parentBoundsChanged() {
        updateBounds();
        super.parentBoundsChanged();
    }

    /**
     * Update the bounds of this selection handle based on the size of its
     * parent plus an extension factor.
     */
    private void updateBounds() {
        PNode parentNode = ((PNodeLocator) getLocator()).getNode();
        // Different extension factor depending on whether the node being decorated is a neuron group node or not
        float ef = style.extensionFactor;

        double x = 0.0f - (parentNode.getBounds().getWidth() * ef);
        double y = 0.0f - (parentNode.getBounds().getHeight() * ef);
        double width = parentNode.getBounds().getWidth() + 2 * (parentNode.getBounds().getWidth() * ef);
        double height = parentNode.getBounds().getHeight() + 2 * (parentNode.getBounds().getHeight() * ef);

        // Without this repeated actions like clamping fail to remove old pixels
        this.reset();
        append(new Rectangle2D.Float((float) x, (float) y, (float) width, (float) height), false);
    }


    public static void addSelectionHandleTo(final PNode node) {
        addSelectionHandleTo(node, SELECTION_STYLE);
    }

    /**
     * Add a selection handle to the specified node, if one does not exist
     * already.
     *
     * @param node node to add the selection handle to, must not be null
     */
    public static void addSelectionHandleTo(final PNode node, Config style) {

        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }

        if (selections.containsKey(node)) {
            return;
        }

        PNodeLocator nodeLocator = new PNodeLocator(node);

        selections.put(node, new NodeHandle(nodeLocator, style));

    }

    /**
     * Remove the selection handle from the specified node, if any exist.
     *
     * @param node node to remove the selection handle(s) from, must not be null
     */
    public static void removeSelectionHandleFrom(final PNode node) {

        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }

        node.removeChildren(Collections.singleton(selections.get(node)));

        selections.remove(node);
    }

    public static void addSourceHandleTo(final PNode node) {
        addSourceHandleTo(node, SOURCE_STYLE);
    }

    /**
     * Add a source handle to the specified node, if one does not exist
     * already.
     *
     * @param node node to add the source handle to, must not be null
     */
    public static void addSourceHandleTo(final PNode node, Config style) {

        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }

        if (sources.containsKey(node)) {
            return;
        }

        PNodeLocator nodeLocator = new PNodeLocator(node);
        sources.put(node, new NodeHandle(nodeLocator, style));

    }

    /**
     * Remove the source handle from the specified node, if any exist.
     *
     * @param node node to remove the source handle from, must not be null
     */
    public static void removeSourceHandleFrom(final PNode node) {

        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }

        node.removeChildren(Collections.singleton(sources.get(node)));

        sources.remove(node);
    }

    /**
     * A config class holding the styling info on different types of handle.
     */
    public static class Config {

        /**
         * Whether a handle marks an ordinary selection or a connection source, determining which themed
         * canvas color it resolves against.
         */
        public enum Role {SELECTION, SOURCE}

        /**
         * Amount of space to add between the selected object and the selection
         * handle.
         */
        private float extensionFactor = 0.075f;

        /**
         * Role of this handle, mapped to a themed color at paint/refresh time.
         */
        private Role role = Role.SELECTION;

        private int thickness = 1;

        public Config(float extensionFactor, int thickness, Role role) {
            this.extensionFactor = extensionFactor;
            this.role = role;
            this.thickness = thickness;
        }

        public Config(float extensionFactor, Role role) {
            this.extensionFactor = extensionFactor;
            this.role = role;
        }

        public Config() {
        }

        public float getExtensionFactor() {
            return extensionFactor;
        }

        public Role getRole() {
            return role;
        }

        /**
         * The current themed stroke color for this handle's role, read live from {@link NetworkTheme} so it
         * always reflects the active light/dark mode.
         */
        public Color resolveColor() {
            return role == Role.SOURCE
                    ? NetworkTheme.INSTANCE.getCurrent().getSourceHandle()
                    : NetworkTheme.INSTANCE.getCurrent().getSelectionHandle();
        }
    }
}
