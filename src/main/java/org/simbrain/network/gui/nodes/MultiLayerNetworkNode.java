package org.simbrain.network.gui.nodes;

import org.piccolo2d.nodes.PPath;
import org.piccolo2d.nodes.PText;
import org.simbrain.network.dl4j.MultiLayerNet;
import org.simbrain.network.gui.NetworkPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.stream.Collectors;

public class MultiLayerNetworkNode extends ScreenElement {

    private MultiLayerNet net;

    private NetworkPanel networkPanel;

    /**
     * Width in pixels of the main display box for ND4J arrays.
     */
    private final float boxWidth = 150;

    /**
     * Height in pixels of the main display box for ND4J arrays.
     */
    private final float boxHeight = 50;

    /**
     * Text showing info about the array.
     */
    private PText infoText;

    /**
     * Font for info text.
     */
    public static final Font INFO_FONT = new Font("Arial", Font.PLAIN, 8);

    private PPath box;


    public MultiLayerNetworkNode(NetworkPanel networkPanel, MultiLayerNet dl4jNet) {
        super(networkPanel);
        this.net = dl4jNet;

        box = PPath.createRectangle(0, 0, boxWidth, boxHeight);
        addChild(box);
        setPickable(true);
        this.setBounds(box.getFullBounds());

        // Info text
        infoText = new PText();
        infoText.setFont(INFO_FONT);
        addChild(infoText);
        infoText.offset(8, 8);
        updateInfoText();

        pushViewPositionToModel();
    }

    /**
     * Update status text.
     */
    private void updateInfoText() {
        infoText.setText(
                "Layer Sizes:\n" + net.getTopology()
                        .stream()
                        .map(Number::toString)
                        .collect(Collectors.joining(", "))
        );
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean showSelectionHandle() {
        return true;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    protected boolean hasToolTipText() {
        return false;
    }

    @Override
    protected String getToolTipText() {
        return null;
    }

    @Override
    protected boolean hasContextMenu() {
        return false;
    }

    @Override
    protected JPopupMenu getContextMenu() {
        return null;
    }

    @Override
    protected boolean hasPropertyDialog() {
        return false;
    }

    @Override
    protected JDialog getPropertyDialog() {
        return null;
    }

    @Override
    public void resetColors() {

    }

    /**
     * Update the position of the model neuron based on the global coordinates
     * of this pnode.
     */
    public void pushViewPositionToModel() {
        Point2D p = this.getGlobalTranslation();
        net.setLocation(p);
    }

    @Override
    public void offset(double dx, double dy) {
        super.offset(dx, dy);
        pushViewPositionToModel();
    }

    public MultiLayerNet getNet() {
        return net;
    }
}
