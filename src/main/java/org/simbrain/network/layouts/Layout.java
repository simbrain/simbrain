package org.simbrain.network.layouts;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.simbrain.network.core.Neuron;
import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.CopyableObject;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * Interface for all neuron layout managers, which arrange a set of neurons in
 * different ways.
 *
 * @author Jeff Yoshimi
 */
public abstract class Layout implements CopyableObject {

    /**
     * Called via reflection.
     */
    public static List<Class<? extends CopyableObject>> TYPE_LIST
            = List.of(LineLayout.class, GridLayout.class, HexagonalGridLayout.class);

    @Nullable
    @Override
    public List<Class<? extends CopyableObject>> getTypeList() {
        return TYPE_LIST;
    }

    /**
     * Layout a list of neurons.
     *
     * @param neurons the list of neurons
     */
    public abstract void layoutNeurons(List<Neuron> neurons);

    /**
     * @return the name of this layout type
     */
    public abstract String getDescription();

    /**
     * Set the initial position.
     *
     * @param initialPoint initial position
     */
    public abstract void setInitialLocation(final Point2D initialPoint);

    @Override
    public abstract Layout copy();

    @Override
    public String getName() {
        return getDescription();
    }

    /**
     * Helper class for editing layouts using
     * {@link org.simbrain.util.propertyeditor.AnnotatedPropertyEditor}.
     */
    public static class LayoutEditor implements CopyableObject {

        @UserParameter(label = "Layout")
        private Layout layout = new GridLayout();

        public Layout getLayout() {
            return layout;
        }

        public void setLayout(Layout layout) {
            this.layout = layout;
        }

        @Override
        public @NotNull String getName() {
            return "Layout";
        }

        @Override
        public @NotNull LayoutEditor copy() {
            LayoutEditor copy = new LayoutEditor();
            copy.layout = layout.copy();
            return copy;
        }
    }

}
