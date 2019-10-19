package org.simbrain.network.gui.nodes;

import org.piccolo2d.PNode;
import org.piccolo2d.extras.handles.PHandle;
import org.piccolo2d.extras.util.PNodeLocator;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;

public class NodeHandle extends PHandle {


    public static final Config SELECTION_STYLE = new Config();

    public static final Config SOURCE_STYLE = new Config(0.2f, Color.RED);

    public static final Config INTERACTION_BOX_SELECTION_STYLE = new Config(0.01f, 2, Color.green);

    public static final Config INTERACTION_BOX_SOURCE_STYLE = new Config(0.09f, Color.RED);

    private static final Map<PNode, NodeHandle> selections = new HashMap<>();

    private static final Map<PNode, NodeHandle> sources = new HashMap<>();

    private Config style;

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
        setStrokePaint(style.selectionColor);

        // Force handle to check its location and size
        updateBounds();
        relocateHandle();
    }

    /**
     * {@inheritDoc}
     */
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

        this.reset(); // TODO: Check with Heuer
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
        addSelectionHandleTo(node, SOURCE_STYLE);
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
         * Amount of space to add between the selected object and the selection
         * handle.
         */
        private float extensionFactor = 0.075f;

        /**
         * Color of selection boxes.
         */
        private Color selectionColor = Color.green;

        private int thickness = 1;

        public Config(float extensionFactor, int thickness, Color selectionColor) {
            this.extensionFactor = extensionFactor;
            this.selectionColor = selectionColor;
            this.thickness = thickness;
        }

        public Config(float extensionFactor, Color selectionColor) {
            this.extensionFactor = extensionFactor;
            this.selectionColor = selectionColor;
        }

        public Config() {
        }

        public float getExtensionFactor() {
            return extensionFactor;
        }

        public Color getSelectionColor() {
            return selectionColor;
        }
    }
}
