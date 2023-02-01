/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui.nodes;

import org.piccolo2d.nodes.PImage;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.nodes.PText;
import org.piccolo2d.util.PBounds;
import org.simbrain.network.NetworkModel;
import org.simbrain.network.events.LocationEvents2;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.matrix.ZoeLayer;
import org.simbrain.util.ImageKt;
import org.simbrain.util.math.SimbrainMath;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import static org.simbrain.util.GeomKt.minus;
import static org.simbrain.util.GeomKt.plus;

// Cribbed from NeuronArrayNode. Find more stuff there.
public class ZoeLayerNode extends ScreenElement {

    protected ZoeLayer layer;

    /**
     * Only update bounds when this is null.
     */
    private PBounds boundsCache = null;

    /**
     * If true, show the image array as a grid; if false show it as a horizontal line.
     */
    private boolean gridMode = false;

    /**
     * Heavy stroke for clamped nodes.
     */
    private static final BasicStroke CLAMPED_STROKE = new BasicStroke(2f);

    /**
     * Stroke to use for bounding box. Toggles from clamped to default.
     */
    private Stroke boundaryStroke = DEFAULT_STROKE;

    /**
     * Square shape around array node.
     */
    private PPath borderBox;

    private PImage mainImage = new PImage();

    /**
     * Text showing info about the array.
     */
    private PText infoText;

    /**
     * Font for info text.
     */
    public static final Font INFO_FONT = new Font("Arial", Font.PLAIN, 8);

    /**
     * Parent network panel.
     */
    private final NetworkPanel networkPanel;

    public ZoeLayerNode(final NetworkPanel np, final ZoeLayer lyr) {
        super(np);
        this.layer = lyr;
        networkPanel = np;

        LocationEvents2 events = layer.getEvents();
        events.getDeleted().on(n -> removeFromParent());
        events.getUpdated().on(() -> {
            renderArrayToActivationsImage();
            updateInfoText();
        });
        // events.onClampChanged(this::updateClampStatus);
        events.getLocationChanged().on(this::pullViewPositionFromModel);

        // Info text
        infoText = new PText();
        infoText.setFont(INFO_FONT);
        addChild(infoText);
        infoText.offset(8, 8);
        updateInfoText();

        // Main image
        setPickable(true);
        addChild(mainImage);
        renderArrayToActivationsImage();
        borderBox = PPath.createRectangle(0, 0, getWidth(), getHeight());
        addChild(borderBox);

        pullViewPositionFromModel();

    }

    private void renderArrayToActivationsImage() {
        double[] activations = SimbrainMath.randomVector(100);
        int len = (int) Math.sqrt(activations.length);
        BufferedImage img = ImageKt.toSimbrainColorImage(
                activations,len,len);
        mainImage.setImage(img);
        // TODO: Adjust this to look nice
        // TODO: Magic numbers
        this.mainImage.setBounds(5, infoText.getHeight() + 10, 100, 50);
    }

    public void pullViewPositionFromModel() {
        Point2D point = minus(layer.getLocation(), new Point2D.Double(getWidth() / 2, getHeight() / 2));
        this.setGlobalTranslation(point);
    }

    /**
     * Update status text.
     */
    private void updateInfoText() {
        infoText.setText(layer.getLabel() + " " +  "nodes: " + layer.outputSize());
    }

    /**
     * Update the position of the model neuron based on the global coordinates
     * of this pnode.
     */
    public void pushViewPositionToModel() {
        Point2D p = this.getGlobalTranslation();
        layer.setLocation(plus(p, new Point2D.Double(getWidth() / 2, getHeight() / 2)));
    }

    @Override
    public void offset(double dx, double dy) {
        pushViewPositionToModel();
        super.offset(dx, dy);
    }

    @Override
    public NetworkModel getModel() {
        return layer;
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    protected boolean validateFullBounds() {
        if (boundsCache == null) {

            PBounds bounds = new PBounds();
            bounds.add(infoText.getFullBoundsReference());
            bounds.add(mainImage.getFullBoundsReference());

            removeChild(borderBox);
            borderBox = PPath.createRectangle(bounds.x, bounds.y, bounds.width, bounds.height);
            addChild(borderBox);
            borderBox.lowerToBottom();
            borderBox.setStroke(boundaryStroke);

            var ctr = bounds.getCenter2D();
            setBounds(borderBox.getBounds());
            centerBoundsOnPoint(ctr.getX(), ctr.getY());

            boundsCache = bounds;
        }
        return super.validateFullBounds();
    }

    @Override
    public boolean acceptsSourceHandle() {
        return true;
    }
}
